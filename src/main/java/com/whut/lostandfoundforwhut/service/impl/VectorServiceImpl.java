package com.whut.lostandfoundforwhut.service.impl;

import com.alibaba.dashscope.embeddings.MultiModalEmbeddingParam;
import com.alibaba.dashscope.embeddings.MultiModalEmbedding;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingResult;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingItemBase;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingItemText;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingItemImage;
import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.whut.lostandfoundforwhut.service.IVectorService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

import com.whut.lostandfoundforwhut.model.dto.TextEmbeddingDTO;
import com.whut.lostandfoundforwhut.model.entity.Item;

/**
 * 向量数据库服务实现 - 仅支持ChromaDB存储
 */
@Slf4j
@Service
public class VectorServiceImpl implements IVectorService {

    @Value("${app.vector-store.enabled:false}")
    private boolean vectorStoreEnabled;

    @Value("${app.vector-store.collection-name:item_texts}")
    private String collectionName;

    @Value("${app.vector-store.chroma-url:http://localhost:8000}")
    private String chromaUrl;

    @Value("${ai.ali.api-key:}")
    private String dashScopeApiKey;

    private ChromaEmbeddingStore embeddingStore;
    private boolean initialized = false; // 标记是否已初始化

    @PostConstruct
    public void initializeCollection() {
        // 检查是否启用向量存储
        if (!vectorStoreEnabled) {
            log.info("向量数据库功能已禁用 (app.vector-store.enabled=false)");
            initialized = false;
            return;
        }

        try {
            // 创建 ChromaDB 存储实例
            this.embeddingStore = ChromaEmbeddingStore.builder()
                    .baseUrl(chromaUrl)
                    .collectionName(collectionName)
                    .build();

            this.initialized = true;
            log.info("ChromaDB向量数据库初始化成功，集合名称：{}，连接地址：{}", collectionName, chromaUrl);
            System.out.println("ChromaDB向量数据库初始化成功，集合名称：" + collectionName + "，连接地址：" + chromaUrl);
        } catch (Exception e) {
            log.error("ChromaDB向量数据库初始化失败: {}", e.getMessage(), e);
            this.initialized = false;
        }
    }

    /**
     * 检查是否已初始化
     */
    private void checkInitialized() {
        if (!vectorStoreEnabled) {
            throw new IllegalStateException("向量数据库功能未启用，请设置 app.vector-store.enabled=true");
        }
        if (!initialized) {
            throw new IllegalStateException("向量数据库未初始化");
        }
    }

    @Override
    public void addTextToCollection(TextEmbeddingDTO textEmbeddingDTO) {
        if (!vectorStoreEnabled) {
            log.debug("向量数据库功能已禁用，跳过添加文本到集合: {}", textEmbeddingDTO.getId());
            return;
        }

        checkInitialized();

        try {
            String id = textEmbeddingDTO.getId();
            String text = textEmbeddingDTO.getText();

            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("文本嵌入ID不能为空");
            }
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("文本内容不能为空");
            }

            Embedding embedding = generateEmbedding(text);

            // 尝试先删除已存在的ID，以避免ID冲突
            try {
                embeddingStore.removeAll(List.of(id));
            } catch (Exception e) {
                // 如果删除失败，继续添加
                log.warn("删除已存在的向量ID失败，将继续添加: {}", id, e);
            }

            embeddingStore.add(id, embedding);

            log.info("文本已添加到向量数据库，ID：{}", id);
        } catch (IllegalArgumentException e) {
            log.error("添加文本到向量数据库失败，参数错误：{}", e.getMessage());
            throw new RuntimeException("添加文本到向量数据库失败，参数错误", e);
        } catch (Exception e) {
            log.error("添加文本到向量数据库失败，ID：{}", textEmbeddingDTO.getId(), e);
            throw new RuntimeException("添加文本到向量数据库失败", e);
        }
    }

    @Override
    public void addToVectorDatabase(Item item) {
        try {
            String itemDescription = item.getDescription();
            TextEmbeddingDTO textEmbeddingDTO = TextEmbeddingDTO.builder()
                    .id("item_" + item.getId())
                    .text(itemDescription)
                    .build();
            addTextToCollection(textEmbeddingDTO);
            log.info("物品信息已添加到向量数据库，ID：{}", item.getId());
        } catch (Exception e) {
            log.error("添加到向量数据库时发生异常，物品ID：{}", item.getId(), e);
            // 这里不抛出异常，因为向量数据库的失败不应影响主业务流程
        }
    }

    @Override
    public void addImagesToVectorDatabase(Item item, String imageUrl) {
        try {
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                // 处理单张图片的多模态嵌入：结合文本描述和图片信息
                String itemDescription = item.getDescription() != null ? item.getDescription() : "";

                // 为整个物品创建一个多模态嵌入，包含文本描述和图片
                String itemId = "item_" + item.getId();

                System.out.println("单张图片: " + imageUrl);

                // 创建多模态嵌入（文本+单张图片）
                Embedding embedding = generateMultimodalEmbedding(itemDescription, imageUrl);

                if (embedding != null) {
                    // 尝试先删除已存在的ID
                    try {
                        embeddingStore.removeAll(List.of(itemId));
                    } catch (Exception e) {
                        log.warn("删除已存在的向量ID失败，将继续添加: {}", itemId, e);
                    }

                    embeddingStore.add(itemId, embedding);

                    log.info("物品单张图片多模态信息已添加到向量数据库，物品ID：{}", item.getId());
                }
            }
        } catch (Exception e) {
            log.error("添加物品单张图片到向量数据库时发生异常，物品ID：{}", item.getId(), e);
            // 这里不抛出异常，因为向量数据库的失败不应影响主业务流程
        }
    }

    @Override
    public void addImagesToVectorDatabases(Item item, List<String> imageUrls) {
        try {
            if (imageUrls != null && !imageUrls.isEmpty()) {
                // 处理多模态嵌入：结合文本描述和所有图片信息
                String itemDescription = item.getDescription() != null ? item.getDescription() : "";

                // 为整个物品创建一个多模态嵌入，包含文本描述和所有图片
                String itemId = "item_" + item.getId();

                System.out.println("图片" + imageUrls);

                // 创建多模态嵌入（文本+所有图片）
                Embedding embedding = generateMultimodalEmbeddings(itemDescription, imageUrls);

                if (embedding != null) {
                    // 尝试先删除已存在的ID
                    try {
                        embeddingStore.removeAll(List.of(itemId));
                    } catch (Exception e) {
                        log.warn("删除已存在的向量ID失败，将继续添加: {}", itemId, e);
                    }

                    embeddingStore.add(itemId, embedding);

                    log.info("物品多模态信息已添加到向量数据库，物品ID：{}，图片数量：{}", item.getId(), imageUrls.size());
                }
            }
        } catch (Exception e) {
            log.error("添加物品图片到向量数据库时发生异常，物品ID：{}", item.getId(), e);
            // 这里不抛出异常，因为向量数据库的失败不应影响主业务流程
        }
    }

    @Override
    public void updateVectorDatabase(Item item) {
        try {
            String itemDescription = item.getDescription() != null ? item.getDescription() : "未提供描述";
            // 先删除旧的向量数据
            deleteFromCollection("item_" + item.getId());

            TextEmbeddingDTO textEmbeddingDTO = TextEmbeddingDTO.builder()
                    .id("item_" + item.getId())
                    .text(itemDescription)
                    .build();
            addTextToCollection(textEmbeddingDTO);
            log.info("向量数据库中物品信息已更新，ID：{}", item.getId());
        } catch (Exception e) {
            log.error("更新向量数据库时发生异常，物品ID：{}", item.getId(), e);
            // 这里不抛出异常，因为向量数据库的失败不应影响主业务流程
        }
    }

    @Override
    public void removeFromVectorDatabase(Long itemId) {
        try {
            deleteFromCollection("item_" + itemId);
            log.info("向量数据库中物品信息已删除，ID：{}", itemId);
        } catch (Exception e) {
            log.error("删除向量数据库条目时发生异常，物品ID：{}", itemId, e);
            // 这里不抛出异常，因为向量数据库的失败不应影响主业务流程
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<String> searchInCollection(String query, int maxResults) {
        if (!vectorStoreEnabled) {
            log.debug("向量数据库功能已禁用，返回空搜索结果");
            return List.of();
        }

        checkInitialized();

        try {
            if (query == null || query.trim().isEmpty()) {
                log.warn("查询文本为空，返回空搜索结果");
                return List.of();
            }

            if (maxResults <= 0) {
                log.warn("搜索结果数量必须大于0，返回空搜索结果");
                return List.of();
            }

            Embedding queryEmbedding = generateEmbedding(query);

            List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, maxResults);

            List<String> results = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : relevant) {
                results.add(match.embeddingId());
            }
            log.info("向量搜索完成，查询：{}，返回结果数量：{}", query, results.size());
            return results;
        } catch (Exception e) {
            log.error("向量搜索失败，查询：{}", query, e);
            throw new RuntimeException("向量搜索失败", e);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getCollectionSize() {
        if (!vectorStoreEnabled) {
            log.debug("向量数据库功能已禁用，返回0");
            return 0;
        }

        checkInitialized();

        try {
            // 使用通用查询来获取所有项目
            Embedding queryEmbedding = generateEmbedding("anything");
            List<EmbeddingMatch<TextSegment>> allItems = embeddingStore.findRelevant(queryEmbedding, 10000);
            int size = allItems.size();
            log.info("获取集合大小完成，当前大小：{}", size);
            return size;
        } catch (Exception e) {
            log.error("获取集合大小失败", e);
            return 0;
        }
    }

    @Override
    public void deleteFromCollection(String id) {
        if (!vectorStoreEnabled) {
            log.debug("向量数据库功能已禁用，跳过删除操作: {}", id);
            return;
        }

        checkInitialized();

        try {
            if (id == null || id.trim().isEmpty()) {
                log.warn("要删除的ID为空，跳过删除操作");
                return;
            }

            // ChromaDB Java客户端不直接支持按ID删除，所以我们尝试移除所有匹配的ID
            try {
                embeddingStore.removeAll(List.of(id));
                log.info("从ChromaDB向量数据库删除条目完成，ID：{}", id);
            } catch (Exception e) {
                log.warn("ChromaDB不直接支持按ID删除，请考虑重新构建集合。ID：{}", id, e);
                // 记录但不抛出异常，因为这是ChromaDB的限制而非程序错误
            }
        } catch (Exception e) {
            log.error("从向量数据库删除条目失败，ID：{}", id, e);
            throw new RuntimeException("删除条目失败", e);
        }
    }

    @Override
    public void clearCollection() {
        if (!vectorStoreEnabled) {
            log.debug("向量数据库功能已禁用，跳过清空操作");
            return;
        }

        checkInitialized();

        try {
            // 重新创建实例来清空数据
            // 由于ChromaDB Java客户端没有直接的删除全部数据的方法，我们重新创建实例
            this.embeddingStore = ChromaEmbeddingStore.builder()
                    .baseUrl(chromaUrl)
                    .collectionName(collectionName)
                    .build();
            log.info("ChromaDB向量数据库集合已清空并重建");
        } catch (Exception e) {
            log.error("清空向量数据库集合失败", e);
            throw new RuntimeException("清空集合失败", e);
        }
    }

    /**
     * 生成文本的嵌入向量
     * 
     * @param text 输入文本
     * @return 嵌入向量
     */
    public Embedding generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("输入文本为空，使用默认嵌入向量");
            return Embedding.from(new float[384]); // 返回零向量
        }

        if (dashScopeApiKey == null || dashScopeApiKey.trim().isEmpty()) {
            log.warn("DashScope API密钥未配置/为空，将使用简化嵌入向量（仅用于演示）");
            return Embedding.from(computeSimpleEmbedding(text));
        }

        try {
            TextEmbeddingParam param = TextEmbeddingParam
                    .builder()
                    .apiKey(dashScopeApiKey)
                    .model("text-embedding-v1")
                    .texts(Arrays.asList(text))
                    .build();
            TextEmbedding textEmbedding = new TextEmbedding();
            TextEmbeddingResult result = textEmbedding.call(param);
            List<TextEmbeddingResultItem> items = result.getOutput().getEmbeddings();
            if (items != null && !items.isEmpty()) {
                List<Double> vector = items.get(0).getEmbedding();
                List<Float> floatVector = vector.stream()
                        .map(Double::floatValue)
                        .collect(Collectors.toList());
                float[] embeddingArray = new float[floatVector.size()];
                for (int i = 0; i < floatVector.size(); i++) {
                    embeddingArray[i] = floatVector.get(i);
                }
                return Embedding.from(embeddingArray);
            } else {
                log.warn("向量结果为空，使用简化嵌入向量");
                return Embedding.from(computeSimpleEmbedding(text));
            }
        } catch (Exception e) {
            log.error("调用DashScope API失败，使用简化嵌入向量。文本长度：{}", text.length(), e);
            return Embedding.from(computeSimpleEmbedding(text));
        }
    }

    /**
     * 生成多模态嵌入向量（文本+单张图片）
     * 
     * @param text     输入文本
     * @param imageUrl 图片URL
     * @return 嵌入向量
     */
    public Embedding generateMultimodalEmbedding(String text, String imageUrl) {
        System.out.println("进入单张处理");
        if (dashScopeApiKey == null || dashScopeApiKey.trim().isEmpty()) {
            log.warn("DashScope API密钥未配置/为空，将使用简化嵌入向量（仅用于演示）");
            String combinedText = text;
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                combinedText += " " + imageUrl;
            }
            return Embedding.from(computeSimpleEmbedding(combinedText));
        }

        // 使用SDK方法
        try {
            Embedding sdkResult = generateMultimodalEmbeddingWithSDK(text, imageUrl);
            if (sdkResult != null) {
                log.debug("单图片多模态嵌入生成成功（SDK方式）");
                return sdkResult;
            }
        } catch (Exception e) {
            log.warn("使用SDK生成单图片多模态嵌入失败: {}", e.getMessage());
        }

        // 如果SDK方法失败，返回简化嵌入向量
        log.warn("单图片多模态向量结果为空，使用简化嵌入向量");
        String combinedText = text;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            combinedText += " " + imageUrl;
        }
        return Embedding.from(computeSimpleEmbedding(combinedText));
    }

    /**
     * 生成多模态嵌入向量（文本+图像列表）
     * 优先使用DashScope SDK，失败时回退到HTTP API
     * 
     * @param text        输入文本
     * @param base64Image Base64编码的图像数据
     * @return 嵌入向量
     */
    public Embedding generateMultimodalEmbeddings(String text, List<String> imageUrls) {
        System.out.println("进入多张处理");
        if (dashScopeApiKey == null || dashScopeApiKey.trim().isEmpty()) {
            log.warn("DashScope API密钥未配置/为空，将使用简化嵌入向量（仅用于演示）");
            String combinedText = text;
            if (imageUrls != null && !imageUrls.isEmpty()) {
                combinedText += String.join(" ", imageUrls);
            }
            return Embedding.from(computeSimpleEmbedding(combinedText));
        }

        // 使用SDK方法
        try {
            Embedding sdkResult = generateMultimodalEmbeddingWithSDK(text, imageUrls);
            if (sdkResult != null) {
                log.debug("多模态嵌入生成成功（SDK方式）");
                return sdkResult;
            }
        } catch (Exception e) {
            log.warn("使用SDK生成多模态嵌入失败: {}", e.getMessage());
        }

        // 如果SDK方法失败，返回简化嵌入向量
        log.warn("多模态向量结果为空，使用简化嵌入向量");
        String combinedText = text;
        if (imageUrls != null && !imageUrls.isEmpty()) {
            combinedText += String.join(" ", imageUrls);
        }
        return Embedding.from(computeSimpleEmbedding(combinedText));
    }

    /**
     * 使用DashScope SDK生成多模态嵌入向量（单张图片版本）
     * 
     * @param text     输入文本
     * @param imageUrl 图片URL
     * @return 嵌入向量
     */
    private Embedding generateMultimodalEmbeddingWithSDK(String text, String imageUrl)
            throws UploadFileException, NoApiKeyException {

        List<MultiModalEmbeddingItemBase> contents = new ArrayList<>();

        // 添加文本内容
        if (text != null && !text.trim().isEmpty()) {
            MultiModalEmbeddingItemText textContent = new MultiModalEmbeddingItemText(text);
            contents.add(textContent);
        }
        System.out.println("添加文本内容: " + contents);

        // 添加单张图像内容
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            try {
                // 将图片URL转换为Base64
                log.debug("开始转换图片: {}", imageUrl);
                String base64Image = imageUrlToBase64(imageUrl);
                if (base64Image != null && !base64Image.isEmpty()) {
                    // 提取纯Base64数据（去除data:image/...;base64,前缀）
                    String pureBase64 = base64Image;
                    if (base64Image.startsWith("data:image/")) {
                        int commaIndex = base64Image.indexOf(",");
                        if (commaIndex > 0) {
                            pureBase64 = base64Image.substring(commaIndex + 1);
                        }
                    }

                    MultiModalEmbeddingItemImage imageContent = new MultiModalEmbeddingItemImage(pureBase64);
                    contents.add(imageContent);
                    log.debug("成功添加图片内容到多模态内容列表");
                } else {
                    log.warn("转换图片到Base64失败，结果为空: {}", imageUrl);
                }
            } catch (Exception e) {
                log.warn("转换图片URL到Base64失败: {}", imageUrl, e);
            }
        }

        if (contents.isEmpty()) {
            log.warn("多模态内容为空，无法生成嵌入向量");
            return null;
        }

        // 检查API密钥是否有效
        if (dashScopeApiKey == null || dashScopeApiKey.trim().isEmpty()) {
            log.warn("DashScope API密钥未配置");
            throw new NoApiKeyException();
        }

        log.debug("准备调用DashScope API，模型: tongyi-embedding-vision-plus，内容数量: {}", contents.size());

        // 构建参数 - 使用正确的参数格式
        MultiModalEmbeddingParam param = MultiModalEmbeddingParam.builder()
                .apiKey(dashScopeApiKey)
                .model("tongyi-embedding-vision-plus") // 使用正确的模型名称，与Python示例保持一致
                .contents(contents)
                .build();

        // 调用API
        MultiModalEmbedding multiModalEmbedding = new MultiModalEmbedding();
        MultiModalEmbeddingResult result = multiModalEmbedding.call(param);

        // 检查API调用结果
        if (result == null) {
            log.warn("DashScope API调用失败，返回结果为空");
            return null;
        }

        // 解析结果
        if (result.getOutput() != null) {
            try {
                // 直接访问output中的embedding字段
                Object output = result.getOutput();

                // 尝试获取embedding字段
                java.lang.reflect.Method getEmbeddingMethod = output.getClass().getMethod("getEmbedding");
                Object embeddingObj = getEmbeddingMethod.invoke(output);

                if (embeddingObj instanceof List) {
                    List<?> embeddingList = (List<?>) embeddingObj;
                    if (!embeddingList.isEmpty()) {
                        float[] embeddingArray = new float[embeddingList.size()];
                        for (int i = 0; i < embeddingList.size(); i++) {
                            Object value = embeddingList.get(i);
                            if (value instanceof Number) {
                                embeddingArray[i] = ((Number) value).floatValue();
                            } else {
                                log.warn("嵌入向量中包含非数字值: {}", value);
                                return null;
                            }
                        }
                        log.debug("成功生成多模态嵌入向量，维度: {}", embeddingArray.length);
                        return Embedding.from(embeddingArray);
                    }
                }
            } catch (Exception e) {
                log.warn("从SDK结果中直接提取嵌入向量时发生异常: {}", e.getMessage());
                // 回退到反射方式
                Embedding embedding = extractEmbeddingFromResult(result);
                if (embedding != null) {
                    return embedding;
                }
            }
        } else {
            log.warn("SDK返回的结果中output字段为空");
        }

        log.warn("SDK返回的单图片多模态嵌入结果解析失败");
        return null;
    }

    /**
     * 使用DashScope SDK生成多模态嵌入向量（多张图片版本）
     * 
     * @param text      输入文本
     * @param imageUrls 图片URL列表
     * @return 嵌入向量
     */
    private Embedding generateMultimodalEmbeddingWithSDK(String text, List<String> imageUrls)
            throws UploadFileException, NoApiKeyException {

        List<MultiModalEmbeddingItemBase> contents = new ArrayList<>();

        // 添加文本内容
        if (text != null && !text.trim().isEmpty()) {
            MultiModalEmbeddingItemText textContent = new MultiModalEmbeddingItemText(text);
            contents.add(textContent);
        }

        // 添加多张图像内容
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String imageUrl : imageUrls) {
                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    try {
                        // 将图片URL转换为Base64
                        log.debug("开始转换图片: {}", imageUrl);
                        String base64Image = imageUrlToBase64(imageUrl);
                        if (base64Image != null && !base64Image.isEmpty()) {
                            // 提取纯Base64数据（去除data:image/...;base64,前缀）
                            String pureBase64 = base64Image;
                            if (base64Image.startsWith("data:image/")) {
                                int commaIndex = base64Image.indexOf(",");
                                if (commaIndex > 0) {
                                    pureBase64 = base64Image.substring(commaIndex + 1);
                                }
                            }

                            MultiModalEmbeddingItemImage imageContent = new MultiModalEmbeddingItemImage(pureBase64);
                            contents.add(imageContent);
                            log.debug("成功添加图片内容到多模态内容列表");
                        } else {
                            log.warn("转换图片到Base64失败，结果为空: {}", imageUrl);
                        }
                    } catch (Exception e) {
                        log.warn("转换图片URL到Base64失败: {}", imageUrl, e);
                    }
                }
            }
        }

        if (contents.isEmpty()) {
            log.warn("多模态内容为空，无法生成嵌入向量");
            return null;
        }

        // 检查API密钥是否有效
        if (dashScopeApiKey == null || dashScopeApiKey.trim().isEmpty()) {
            log.warn("DashScope API密钥未配置");
            throw new NoApiKeyException();
        }

        log.debug("准备调用DashScope API，模型: multimodal-embedding-v1，内容数量: {}", contents.size());

        // 构建参数 - 使用正确的参数格式
        MultiModalEmbeddingParam param = MultiModalEmbeddingParam.builder()
                .apiKey(dashScopeApiKey)
                .model("multimodal-embedding-v1") // 使用正确的模型名称
                .contents(contents)
                .build();

        // 调用API
        MultiModalEmbedding multiModalEmbedding = new MultiModalEmbedding();
        MultiModalEmbeddingResult result = multiModalEmbedding.call(param);

        // 检查API调用结果
        if (result == null) {
            log.warn("DashScope API调用失败，返回结果为空");
            return null;
        }

        // 解析结果
        if (result.getOutput() != null) {
            try {
                // 直接访问output中的embedding字段
                Object output = result.getOutput();

                // 尝试获取embedding字段
                java.lang.reflect.Method getEmbeddingMethod = output.getClass().getMethod("getEmbedding");
                Object embeddingObj = getEmbeddingMethod.invoke(output);

                if (embeddingObj instanceof List) {
                    List<?> embeddingList = (List<?>) embeddingObj;
                    if (!embeddingList.isEmpty()) {
                        float[] embeddingArray = new float[embeddingList.size()];
                        for (int i = 0; i < embeddingList.size(); i++) {
                            Object value = embeddingList.get(i);
                            if (value instanceof Number) {
                                embeddingArray[i] = ((Number) value).floatValue();
                            } else {
                                log.warn("嵌入向量中包含非数字值: {}", value);
                                return null;
                            }
                        }
                        log.debug("成功生成多模态嵌入向量，维度: {}", embeddingArray.length);
                        return Embedding.from(embeddingArray);
                    }
                }
            } catch (Exception e) {
                log.warn("从SDK结果中直接提取嵌入向量时发生异常: {}", e.getMessage());
                // 回退到反射方式
                Embedding embedding = extractEmbeddingFromResult(result);
                if (embedding != null) {
                    return embedding;
                }
            }
        } else {
            log.warn("SDK返回的结果中output字段为空");
        }

        log.warn("SDK返回的多模态嵌入结果解析失败");
        return null;
    }

    /**
     * 计算简单嵌入向量（用于演示）
     * 
     * @param text 输入文本
     * @return 简单嵌入向量
     */
    private float[] computeSimpleEmbedding(String text) {
        if (text == null || text.isEmpty()) {
            return new float[1536]; // 修改为1536维以匹配ChromaDB集合要求
        }

        byte[] bytes = text.getBytes();
        float[] vector = new float[1536]; // 修改为1536维以匹配ChromaDB集合要求

        if (bytes.length == 0) {
            return vector;
        }

        for (int i = 0; i < bytes.length; i++) {
            vector[i % vector.length] += bytes[i];
        }

        // 归一化向量
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= (float) norm;
            }
        }

        return vector;
    }

    /**
     * 将图片文件转换为Base64格式
     * 按照DashScope官方示例格式生成
     *
     * @param imagePath 图片文件路径
     * @return Base64编码的图片字符串，格式为 "data:image/[format];base64,[base64_data]"
     * @throws Exception 当文件不存在或读取失败时抛出异常
     */
    public String imageToBase64(String imagePath) throws Exception {
        try {
            byte[] imageBytes;
            try (FileInputStream fis = new FileInputStream(imagePath)) {
                imageBytes = fis.readAllBytes();
            }

            String imageData = Base64.getEncoder().encodeToString(imageBytes);

            // 获取图片格式
            String formatType;
            String lowerImagePath = imagePath.toLowerCase();
            if (lowerImagePath.endsWith(".png")) {
                formatType = "png";
            } else if (lowerImagePath.endsWith(".jpg") || lowerImagePath.endsWith(".jpeg")) {
                formatType = "jpeg";
            } else {
                formatType = "jpeg"; // 默认格式
            }

            // 构造正确的Base64格式，严格按照官方示例
            return "data:image/" + formatType + ";base64," + imageData;
        } catch (Exception e) {
            log.error("图片转Base64失败，路径：{}", imagePath, e);
            throw new Exception("错误：图片转Base64失败：" + e.getMessage());
        }
    }

    /**
     * 通过反射安全地从MultiModalEmbeddingResult中提取嵌入向量
     * 
     * @param result SDK返回的结果对象
     * @return 嵌入向量，如果无法提取则返回null
     */
    private Embedding extractEmbeddingFromResult(MultiModalEmbeddingResult result) {
        try {
            // 尝试获取output对象
            Object output = result.getOutput();
            if (output == null) {
                log.debug("SDK结果的output为null");
                return null;
            }

            // 尝试常见的方法名来获取嵌入向量
            String[] methodNames = { "getEmbeddings", "getEmbedding", "getResult", "getData", "getVectors" };
            Class<?> outputClass = output.getClass();

            for (String methodName : methodNames) {
                try {
                    java.lang.reflect.Method method = outputClass.getMethod(methodName);
                    Object embeddingsObj = method.invoke(output);

                    if (embeddingsObj instanceof List) {
                        List<?> embeddingsList = (List<?>) embeddingsObj;
                        if (!embeddingsList.isEmpty()) {
                            Object firstItem = embeddingsList.get(0);
                            if (firstItem instanceof List) {
                                // 嵌入向量是List<Float>或List<Double>的形式
                                List<?> vectorList = (List<?>) firstItem;
                                if (!vectorList.isEmpty()) {
                                    float[] embeddingArray = new float[vectorList.size()];
                                    for (int i = 0; i < vectorList.size(); i++) {
                                        Object value = vectorList.get(i);
                                        if (value instanceof Number) {
                                            embeddingArray[i] = ((Number) value).floatValue();
                                        } else {
                                            log.warn("嵌入向量中包含非数字值: {}", value);
                                            return null;
                                        }
                                    }
                                    return Embedding.from(embeddingArray);
                                }
                            } else if (firstItem instanceof Number) {
                                // 嵌入向量是扁平化的List<Float>形式
                                float[] embeddingArray = new float[embeddingsList.size()];
                                for (int i = 0; i < embeddingsList.size(); i++) {
                                    Object value = embeddingsList.get(i);
                                    if (value instanceof Number) {
                                        embeddingArray[i] = ((Number) value).floatValue();
                                    } else {
                                        log.warn("嵌入向量中包含非数字值: {}", value);
                                        return null;
                                    }
                                }
                                return Embedding.from(embeddingArray);
                            }
                        }
                    }
                } catch (NoSuchMethodException e) {
                    // 方法不存在，继续尝试下一个
                    continue;
                } catch (Exception e) {
                    log.debug("调用方法 {} 时发生异常: {}", methodName, e.getMessage());
                }
            }

            log.debug("无法从SDK结果中提取有效的嵌入向量");
            return null;

        } catch (Exception e) {
            log.warn("反射提取嵌入向量时发生异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从远程URL下载图片并转换为Base64格式
     *
     * @param imageUrl 远程图片URL
     * @return Base64编码的图片字符串，格式为 "data:image/[format];base64,[base64_data]"
     * @throws Exception 当下载失败或转换失败时抛出异常
     */
    private String downloadAndConvertToBase64(String imageUrl) throws Exception {
        try {
            java.net.URL url = new java.net.URL(imageUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

            // 设置请求头
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; Java HttpClient)");
            connection.setConnectTimeout(10000); // 10秒连接超时
            connection.setReadTimeout(30000); // 30秒读取超时

            // 检查响应码
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new Exception("下载图片失败，响应码: " + responseCode);
            }

            // 读取图片数据
            byte[] imageBytes = connection.getInputStream().readAllBytes();

            // 获取图片格式（从URL后缀或Content-Type）
            String formatType;
            String lowerImageUrl = imageUrl.toLowerCase();
            if (lowerImageUrl.endsWith(".png")) {
                formatType = "png";
            } else if (lowerImageUrl.endsWith(".jpg") || lowerImageUrl.endsWith(".jpeg")) {
                formatType = "jpeg";
            } else {
                // 尝试从Content-Type获取
                String contentType = connection.getContentType();
                if (contentType != null) {
                    if (contentType.contains("png")) {
                        formatType = "png";
                    } else if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                        formatType = "jpeg";
                    } else {
                        formatType = "jpeg"; // 默认格式
                    }
                } else {
                    formatType = "jpeg"; // 默认格式
                }
            }

            // 编码为Base64
            String imageData = Base64.getEncoder().encodeToString(imageBytes);

            // 构造正确的Base64格式
            return "data:image/" + formatType + ";base64," + imageData;

        } catch (Exception e) {
            log.error("下载并转换远程图片失败，URL：{}", imageUrl, e);
            throw new Exception("错误：下载并转换远程图片失败：" + e.getMessage());
        }
    }

    /**
     * 将图片URL转换为Base64格式
     * 支持本地文件路径和远程URL
     *
     * @param imageUrl 图片URL（可以是本地文件名或远程URL）
     * @return Base64编码的图片字符串，格式为 "data:image/[format];base64,[base64_data]"
     * @throws Exception 当文件不存在或读取失败时抛出异常
     */
    private String imageUrlToBase64(String imageUrl) throws Exception {
        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                throw new Exception("图片URL不能为空");
            }

            log.debug("处理图片URL: {}", imageUrl);

            // 检查是否为远程URL
            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                // 远程URL，需要下载图片
                log.debug("检测到远程URL，将下载图片: {}", imageUrl);
                return downloadAndConvertToBase64(imageUrl);
            } else {
                // 本地文件路径，可能包含域名或协议部分需要清理
                String cleanFileName;

                // 如果URL包含域名部分（如 http://example.com/uploads/image/filename.png），提取文件名
                if (imageUrl.contains("/")) {
                    // 提取最后一个斜杠后的文件名
                    cleanFileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
                    log.debug("从完整路径中提取文件名: {} -> {}", imageUrl, cleanFileName);
                } else {
                    // 纯文件名
                    cleanFileName = imageUrl;
                }

                // 构造完整路径
                String imagePath = System.getProperty("user.dir") + "/uploads/image/" + cleanFileName;
                log.debug("处理本地图片文件: {} -> {}", cleanFileName, imagePath);
                return imageToBase64(imagePath);
            }
        } catch (Exception e) {
            log.error("处理图片失败: {}", imageUrl, e);
            throw e;
        }
    }
}