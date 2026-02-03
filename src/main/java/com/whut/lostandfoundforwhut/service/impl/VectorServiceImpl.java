package com.whut.lostandfoundforwhut.service.impl;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import com.whut.lostandfoundforwhut.service.IVectorService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.whut.lostandfoundforwhut.model.dto.TextEmbeddingDTO;

/**
 * 向量数据库服务实现 - 支持ChromaDB和内存存储两种模式
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

    private Object embeddingStore; // 使用 Object 类型以支持多种存储类型
    private boolean useChroma = true; // 标记是否使用 ChromaDB
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
            // 尝试创建 ChromaDB 存储实例
            ChromaEmbeddingStore chromaStore = ChromaEmbeddingStore.builder()
                    .baseUrl(chromaUrl)
                    .collectionName(collectionName)
                    .build();

            this.embeddingStore = chromaStore;
            this.useChroma = true;
            this.initialized = true;
            log.info("ChromaDB向量数据库初始化成功，集合名称：{}，连接地址：{}", collectionName, chromaUrl);
            System.out.println("ChromaDB向量数据库初始化成功，集合名称：" + collectionName + "，连接地址：" + chromaUrl);
        } catch (Exception e) {
            log.warn("ChromaDB向量数据库初始化失败，将使用内存存储作为备用方案。错误详情: {}", e.getMessage());
            log.debug("ChromaDB初始化异常堆栈:", e);
            fallbackToInMemoryStore();
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

    /**
     * 回退到内存存储
     */
    private void fallbackToInMemoryStore() {
        this.embeddingStore = new InMemoryEmbeddingStore<>();
        this.useChroma = false;
        this.initialized = true;
        log.info("内存向量数据库初始化成功");
        System.out.println("内存向量数据库初始化成功");
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

            if (useChroma) {
                ChromaEmbeddingStore chromaStore = (ChromaEmbeddingStore) embeddingStore;

                // 尝试先删除已存在的ID，以避免ID冲突
                try {
                    chromaStore.removeAll(List.of(id));
                } catch (Exception e) {
                    // 如果删除失败，继续添加
                    log.warn("删除已存在的向量ID失败，将继续添加: {}", id, e);
                }

                chromaStore.add(id, embedding);
            } else {
                InMemoryEmbeddingStore<TextSegment> inMemoryStore = (InMemoryEmbeddingStore<TextSegment>) embeddingStore;

                // 移除已存在的ID
                inMemoryStore.removeAll(List.of(id));

                inMemoryStore.add(id, embedding, TextSegment.from(text));
            }

            log.info("文本已添加到向量数据库，ID：{}，使用存储类型：{}", id, useChroma ? "ChromaDB" : "内存");
        } catch (IllegalArgumentException e) {
            log.error("添加文本到向量数据库失败，参数错误：{}", e.getMessage());
            throw new RuntimeException("添加文本到向量数据库失败，参数错误", e);
        } catch (Exception e) {
            log.error("添加文本到向量数据库失败，ID：{}", textEmbeddingDTO.getId(), e);
            throw new RuntimeException("添加文本到向量数据库失败", e);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<String> searchInCollection(String query, int k) {
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

            if (k <= 0) {
                log.warn("搜索结果数量必须大于0，返回空搜索结果");
                return List.of();
            }

            Embedding queryEmbedding = generateEmbedding(query);

            List<EmbeddingMatch<TextSegment>> relevant;
            if (useChroma) {
                relevant = ((ChromaEmbeddingStore) embeddingStore).findRelevant(queryEmbedding, k);
            } else {
                relevant = ((InMemoryEmbeddingStore<TextSegment>) embeddingStore).findRelevant(queryEmbedding, k);
            }

            List<String> results = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : relevant) {
                results.add(match.embeddingId());
            }
            log.info("向量搜索完成，查询：{}，返回结果数量：{}，使用存储类型：{}", query, results.size(), useChroma ? "ChromaDB" : "内存");
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
            // 使用一个简单的查询来估算集合大小
            Embedding queryEmbedding = generateEmbedding("anything");

            List<EmbeddingMatch<TextSegment>> allItems;
            if (useChroma) {
                // 尝试获取最大可能数量的项目
                allItems = ((ChromaEmbeddingStore) embeddingStore).findRelevant(queryEmbedding, 10000);
            } else {
                allItems = ((InMemoryEmbeddingStore<TextSegment>) embeddingStore).findRelevant(queryEmbedding, 10000);
            }
            int size = allItems.size();
            log.info("获取集合大小完成，当前大小：{}，使用存储类型：{}", size, useChroma ? "ChromaDB" : "内存");
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

            if (useChroma) {
                // ChromaDB Java客户端不直接支持按ID删除，所以我们尝试移除所有匹配的ID
                try {
                    ((ChromaEmbeddingStore) embeddingStore).removeAll(List.of(id));
                    log.info("从ChromaDB向量数据库删除条目完成，ID：{}", id);
                } catch (Exception e) {
                    log.warn("ChromaDB不直接支持按ID删除，请考虑重新构建集合。ID：{}", id, e);
                    // 记录但不抛出异常，因为这是ChromaDB的限制而非程序错误
                }
            } else {
                ((InMemoryEmbeddingStore<TextSegment>) embeddingStore).removeAll(List.of(id));
                log.info("从内存向量数据库删除条目完成，ID：{}", id);
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
            if (useChroma) {
                // 对于ChromaDB，重新创建实例
                this.embeddingStore = ChromaEmbeddingStore.builder()
                        .baseUrl(chromaUrl)
                        .collectionName(collectionName)
                        .build();
            } else {
                // 对于内存存储，创建新的实例
                this.embeddingStore = new InMemoryEmbeddingStore<>();
            }
            log.info("向量数据库集合已清空，使用存储类型：{}", useChroma ? "ChromaDB" : "内存");
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
    private Embedding generateEmbedding(String text) {
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
     * 计算简单嵌入向量（用于演示）
     * 
     * @param text 输入文本
     * @return 简单嵌入向量
     */
    private float[] computeSimpleEmbedding(String text) {
        if (text == null || text.isEmpty()) {
            return new float[384];
        }

        byte[] bytes = text.getBytes();
        float[] vector = new float[384]; // 使用固定的384维向量

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
}