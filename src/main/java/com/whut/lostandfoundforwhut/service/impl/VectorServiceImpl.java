package com.whut.lostandfoundforwhut.service.impl;

import com.whut.lostandfoundforwhut.service.IVectorService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

import com.whut.lostandfoundforwhut.model.entity.Item;

/**
 * 向量数据库服务实现L
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
    private HttpClient httpClient;

    @PostConstruct
    public void initializeCollection() {
        // 初始化HTTP客户端
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        // 检查是否启用向量存储
        if (!vectorStoreEnabled) {
            log.info("向量数据库功能已禁用 (app.vector-store.enabled=false)");
            initialized = false;
            return;
        }

        try {
            // 删除可能存在的旧集合（解决维度不匹配问题）
            deleteExistingCollection();

            // 创建 ChromaDB 存储实例（1024维度）
            this.embeddingStore = ChromaEmbeddingStore.builder()
                    .baseUrl(chromaUrl)
                    .collectionName(collectionName)
                    .build();

            // 强制初始化集合 - 通过添加一个测试向量来确保集合被创建
            initializeChromaCollection();

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
                    // 确保集合存在
                    ensureCollectionExists();

                    // 尝试先删除已存在的ID
                    try {
                        embeddingStore.removeAll(List.of(itemId));
                        log.debug("已删除已存在的向量ID: {}", itemId);
                    } catch (Exception e) {
                        // 如果删除失败（比如集合不存在），记录日志但继续执行
                        log.debug("删除已存在的向量ID时出现预期异常（可能是首次添加）: {}", itemId);
                    }

                    try {
                        embeddingStore.add(itemId, embedding);
                        log.info("物品单张图片多模态信息已添加到向量数据库，物品ID：{}", item.getId());
                    } catch (Exception e) {
                        log.error("添加向量数据失败，物品ID：{}", item.getId(), e);
                        // 这里也不抛出异常，因为向量数据库的失败不应影响主业务流程
                    }
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
                    // 确保集合存在
                    ensureCollectionExists();

                    // 尝试先删除已存在的ID
                    try {
                        embeddingStore.removeAll(List.of(itemId));
                        log.debug("已删除已存在的向量ID: {}", itemId);
                    } catch (Exception e) {
                        // 如果删除失败（比如集合不存在），记录日志但继续执行
                        log.debug("删除已存在的向量ID时出现预期异常（可能是首次添加）: {}", itemId);
                    }

                    try {
                        embeddingStore.add(itemId, embedding);
                        log.info("物品多模态信息已添加到向量数据库，物品ID：{}，图片数量：{}", item.getId(), imageUrls.size());
                    } catch (Exception e) {
                        log.error("添加向量数据失败，物品ID：{}", item.getId(), e);
                        // 这里也不抛出异常，因为向量数据库的失败不应影响主业务流程
                    }
                }
            }
        } catch (Exception e) {
            log.error("添加物品图片到向量数据库时发生异常，物品ID：{}", item.getId(), e);
            // 这里不抛出异常，因为向量数据库的失败不应影响主业务流程
        }
    }

    @Override
    public void updateVectorDatabase(Item item, String imageUrl) {
        try {
            String itemDescription = item.getDescription() != null ? item.getDescription() : "未提供描述";
            // 先删除旧的向量数据
            deleteFromCollection("item_" + item.getId());

            addImagesToVectorDatabase(item, imageUrl);

            log.info("向量数据库中物品信息已更新，ID：{}，图片URL：{}", item.getId(), imageUrl);
        } catch (Exception e) {
            log.error("更新向量数据库时发生异常，物品ID：{}，图片URL：{}", item.getId(), imageUrl, e);
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
    public List<String> searchInCollection(String query, String imageUrl, int maxResults) {
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

            Embedding queryEmbedding;
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                // 使用多模态嵌入（文本+图片）
                queryEmbedding = generateMultimodalEmbedding(query, imageUrl);
            } else {
                // 只使用文本嵌入
                queryEmbedding = generateEmbedding(query);
            }

            List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, maxResults);

            List<String> results = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : relevant) {
                results.add(match.embeddingId());
            }
            log.info("向量搜索完成，查询：{}，图片URL：{}，返回结果数量：{}", query, imageUrl, results.size());
            return results;
        } catch (Exception e) {
            log.error("向量搜索失败，查询：{}，图片URL：{}", query, imageUrl, e);
            throw new RuntimeException("向量搜索失败", e);
        }
    }

    @Override
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
     * 生成文本的嵌入向量（简化版本，仅使用简单嵌入）
     * 
     * @param text 输入文本
     * @return 嵌入向量
     */
    public Embedding generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("输入文本为空，使用默认嵌入向量");
            return Embedding.from(new float[1024]); // 返回零向量，使用1024维以匹配DashScope模型
        }

        // 直接使用简化嵌入向量（移除了DashScope TextEmbedding API调用）
        log.debug("使用简化嵌入向量处理文本");
        return Embedding.from(computeSimpleEmbedding(text));
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

        // 使用Http方法
        try {
            Embedding httpResult = generateMultimodalEmbeddingViaHttp(text, imageUrl);
            if (httpResult != null) {
                log.debug("单图片多模态嵌入生成成功（Http方式）");
                return httpResult;
            }
        } catch (Exception e) {
            log.warn("使用HTTP生成单图片多模态嵌入失败: {}", e.getMessage());
        }

        // 如果Http方法失败，返回简化嵌入向量
        log.warn("单图片多模态向量结果为空，使用简化嵌入向量");
        String combinedText = text;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            combinedText += " " + imageUrl;
        }
        return Embedding.from(computeSimpleEmbedding(combinedText));
    }

    /**
     * 生成多模态嵌入向量（文本+图像列表）
     * 使用HTTP API方式
     * 
     * @param text      输入文本
     * @param imageUrls 图像数据
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

        // 使用HTTP方法处理多张图片
        try {
            Embedding httpResult = generateMultimodalEmbeddingsViaHttp(text, imageUrls);
            if (httpResult != null) {
                log.debug("多图片多模态嵌入生成成功（HTTP方式）");
                return httpResult;
            }
        } catch (Exception e) {
            log.warn("使用HTTP生成多图片多模态嵌入失败: {}", e.getMessage());
        }

        // 如果HTTP方法失败，返回简化嵌入向量
        log.warn("多图片多模态向量结果为空，使用简化嵌入向量");
        String combinedText = text;
        if (imageUrls != null && !imageUrls.isEmpty()) {
            combinedText += String.join(" ", imageUrls);
        }
        return Embedding.from(computeSimpleEmbedding(combinedText));
    }

    /**
     * 通过HTTP API生成多模态嵌入向量（单张图片版本）
     */
    private Embedding generateMultimodalEmbeddingViaHttp(String text, String imageUrl) {
        try {
            // 构建API请求体
            StringBuilder requestBody = new StringBuilder();
            requestBody.append("{");
            requestBody.append("\"model\":\"multimodal-embedding-v1\",");
            requestBody.append("\"input\":{");
            requestBody.append("\"contents\":[");
            requestBody.append("{");
            requestBody.append("\"text\":\"").append(text != null ? text.replace("\"", "\\\"") : "").append("\"");

            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                requestBody.append(",\"image\":\"").append(imageUrl.replace("\"", "\\\"")).append("\"");
            }

            requestBody.append("}");
            requestBody.append("]}");
            requestBody.append(",\"parameters\":{");
            requestBody.append("\"output_type\":\"dense\",");
            requestBody.append("\"fps\":0.5");
            requestBody.append("}}");

            String jsonBody = requestBody.toString();
            log.info("HTTP请求体: {}", jsonBody);

            // 构建HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://dashscope.aliyuncs.com/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding"))
                    .header("Authorization", "Bearer " + dashScopeApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            // 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("HTTP响应状态码: {}", response.statusCode());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                log.debug("HTTP响应体: {}", responseBody);

                // 解析响应中的embeddings数组 - DashScope multimodal-embedding-v1返回多个embedding对象
                int embeddingsStart = responseBody.indexOf("\"embeddings\":[");
                if (embeddingsStart != -1) {
                    // 找到embeddings数组的结束位置
                    int embeddingsEnd = responseBody.lastIndexOf("]");
                    if (embeddingsEnd != -1) {
                        String embeddingsPart = responseBody.substring(embeddingsStart + 14, embeddingsEnd);

                        // 提取第一个embedding（可以是text、image或video类型的embedding）
                        int firstEmbeddingStart = embeddingsPart.indexOf("\"embedding\":[");
                        if (firstEmbeddingStart != -1) {
                            firstEmbeddingStart = embeddingsPart.indexOf("[", firstEmbeddingStart);
                            int firstEmbeddingEnd = findMatchingBracket(embeddingsPart, firstEmbeddingStart);
                            if (firstEmbeddingEnd != -1) {
                                String embeddingStr = embeddingsPart.substring(firstEmbeddingStart + 1,
                                        firstEmbeddingEnd);
                                String[] values = embeddingStr.split(",");

                                float[] embeddingArray = new float[values.length];
                                for (int i = 0; i < values.length; i++) {
                                    try {
                                        embeddingArray[i] = Float.parseFloat(values[i].trim());
                                    } catch (NumberFormatException e) {
                                        log.warn("解析嵌入向量数值失败: {}", values[i]);
                                        return null;
                                    }
                                }

                                log.info("通过HTTP API成功生成单图片多模态嵌入向量，维度: {}", embeddingArray.length);
                                return Embedding.from(embeddingArray);
                            }
                        }
                    }
                }

                log.warn("无法从HTTP响应中提取嵌入向量");
                return null;
            } else {
                log.warn("HTTP API调用失败，状态码: {}，响应: {}", response.statusCode(), response.body());
                return null;
            }

        } catch (Exception e) {
            log.warn("通过HTTP API生成多模态嵌入向量时发生异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 通过HTTP API生成多模态嵌入向量（多张图片版本）
     */
    private Embedding generateMultimodalEmbeddingsViaHttp(String text, List<String> imageUrls) {
        try {
            // 构建API请求体
            StringBuilder requestBody = new StringBuilder();
            requestBody.append("{");
            requestBody.append("\"model\":\"multimodal-embedding-v1\",");
            requestBody.append("\"input\":{");
            requestBody.append("\"contents\":[");

            // 添加文本内容
            if (text != null && !text.trim().isEmpty()) {
                requestBody.append("{\"text\":\"").append(text.replace("\"", "\\\"")).append("\"}");
                if (imageUrls != null && !imageUrls.isEmpty()) {
                    requestBody.append(",");
                }
            }

            // 添加图片内容
            if (imageUrls != null && !imageUrls.isEmpty()) {
                for (int i = 0; i < imageUrls.size(); i++) {
                    String imageUrl = imageUrls.get(i);
                    requestBody.append("{\"image\":\"").append(imageUrl.replace("\"", "\\\"")).append("\"}");
                    if (i < imageUrls.size() - 1) {
                        requestBody.append(",");
                    }
                }
            }

            requestBody.append("]}");
            requestBody.append(",\"parameters\":{");
            requestBody.append("\"output_type\":\"dense\",");
            requestBody.append("\"fps\":0.5");
            requestBody.append("}}");

            String jsonBody = requestBody.toString();
            log.info("HTTP请求体（多图片）: {}", jsonBody);

            // 构建HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://dashscope.aliyuncs.com/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding"))
                    .header("Authorization", "Bearer " + dashScopeApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            // 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("HTTP响应状态码（多图片）: {}", response.statusCode());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                log.debug("HTTP响应体（多图片）: {}", responseBody);

                // 解析响应中的embeddings数组
                int embeddingsStart = responseBody.indexOf("\"embeddings\":[");
                if (embeddingsStart != -1) {
                    int embeddingsEnd = responseBody.lastIndexOf("]");
                    if (embeddingsEnd != -1) {
                        String embeddingsPart = responseBody.substring(embeddingsStart + 14, embeddingsEnd);

                        // 提取第一个embedding
                        int firstEmbeddingStart = embeddingsPart.indexOf("\"embedding\":[");
                        if (firstEmbeddingStart != -1) {
                            firstEmbeddingStart = embeddingsPart.indexOf("[", firstEmbeddingStart);
                            int firstEmbeddingEnd = findMatchingBracket(embeddingsPart, firstEmbeddingStart);
                            if (firstEmbeddingEnd != -1) {
                                String embeddingStr = embeddingsPart.substring(firstEmbeddingStart + 1,
                                        firstEmbeddingEnd);
                                String[] values = embeddingStr.split(",");

                                float[] embeddingArray = new float[values.length];
                                for (int i = 0; i < values.length; i++) {
                                    try {
                                        embeddingArray[i] = Float.parseFloat(values[i].trim());
                                    } catch (NumberFormatException e) {
                                        log.warn("解析嵌入向量数值失败: {}", values[i]);
                                        return null;
                                    }
                                }

                                log.info("通过HTTP API成功生成多图片多模态嵌入向量，维度: {}", embeddingArray.length);
                                return Embedding.from(embeddingArray);
                            }
                        }
                    }
                }

                log.warn("无法从HTTP响应中提取嵌入向量（多图片）");
                return null;
            } else {
                log.warn("HTTP API调用失败（多图片），状态码: {}，响应: {}", response.statusCode(), response.body());
                return null;
            }

        } catch (Exception e) {
            log.warn("通过HTTP API生成多图片多模态嵌入向量时发生异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 计算简单嵌入向量（用于演示）
     * 
     * @param text 输入文本
     * @return 简单嵌入向量
     */
    private float[] computeSimpleEmbedding(String text) {
        if (text == null || text.isEmpty()) {
            return new float[1024]; // 修改为1024维以匹配DashScope multimodal-embedding-v1模型要求
        }

        byte[] bytes = text.getBytes();
        float[] vector = new float[1024]; // 修改为1024维以匹配DashScope multimodal-embedding-v1模型要求

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
     * 删除现有的ChromaDB集合（用于解决维度不匹配问题）
     */
    private void deleteExistingCollection() {
        try {
            String deleteUrl = chromaUrl + "/api/v1/collections/" + collectionName;
            HttpRequest deleteRequest = HttpRequest.newBuilder()
                    .uri(URI.create(deleteUrl))
                    .DELETE()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || response.statusCode() == 404) {
                log.info("成功删除现有集合: {} (状态码: {})", collectionName, response.statusCode());
            } else {
                log.warn("删除集合时收到意外状态码: {}，响应: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.debug("删除现有集合时发生异常（可能是集合不存在）: {}", e.getMessage());
        }
    }

    /**
     * 查找字符串中匹配的括号位置
     * 
     * @param str        字符串
     * @param startIndex 开始查找的位置（应指向第一个左括号）
     * @return 匹配的右括号位置，未找到则返回-1
     */
    private int findMatchingBracket(String str, int startIndex) {
        if (str == null || startIndex < 0 || startIndex >= str.length() || str.charAt(startIndex) != '[') {
            return -1;
        }

        int bracketCount = 0;
        for (int i = startIndex; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '[') {
                bracketCount++;
            } else if (c == ']') {
                bracketCount--;
                if (bracketCount == 0) {
                    return i;
                }
            }
        }

        return -1; // 未找到匹配的右括号
    }

    /**
     * 强制初始化ChromaDB集合
     * 通过添加和删除一个测试向量来确保集合被正确创建
     */
    private void initializeChromaCollection() {
        try {
            // 创建一个测试向量来强制初始化集合
            String testId = "init_test_" + System.currentTimeMillis();
            float[] testVector = new float[1024];
            testVector[0] = 1.0f; // 设置一个非零值

            Embedding testEmbedding = Embedding.from(testVector);

            // 添加测试向量
            embeddingStore.add(testId, testEmbedding);
            log.debug("测试向量已添加到集合中，ID: {}", testId);

            // 立即删除测试向量
            embeddingStore.removeAll(List.of(testId));
            log.debug("测试向量已从集合中删除，ID: {}", testId);

            log.info("ChromaDB集合初始化完成");
        } catch (Exception e) {
            log.error("初始化ChromaDB集合时发生异常: {}", e.getMessage(), e);
            throw new RuntimeException("ChromaDB集合初始化失败", e);
        }
    }

    /**
     * 确保ChromaDB集合存在
     * 如果集合不存在，则重新创建
     */
    private void ensureCollectionExists() {
        try {
            // 尝试执行一个简单的查询来检查集合是否存在
            Embedding dummyEmbedding = Embedding.from(new float[1024]);
            embeddingStore.findRelevant(dummyEmbedding, 1);
            log.debug("ChromaDB集合存在且可访问");
        } catch (Exception e) {
            log.warn("ChromaDB集合不存在或不可访问，正在重新创建: {}", e.getMessage());
            try {
                // 重新创建集合
                this.embeddingStore = ChromaEmbeddingStore.builder()
                        .baseUrl(chromaUrl)
                        .collectionName(collectionName)
                        .build();

                // 强制初始化
                initializeChromaCollection();
                log.info("ChromaDB集合已重新创建");
            } catch (Exception reCreateException) {
                log.error("重新创建ChromaDB集合失败: {}", reCreateException.getMessage(), reCreateException);
                throw new RuntimeException("无法确保ChromaDB集合存在", reCreateException);
            }
        }
    }

}