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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.whut.lostandfoundforwhut.model.dto.TextEmbeddingDTO;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;

/**
 * 向量数据库服务实现 - 支持ChromaDB和内存存储两种模式
 */
@Slf4j
@Service
public class VectorServiceImpl implements IVectorService {

    @Value("${app.vector-store.collection-name:item_texts}")
    private String collectionName;

    @Value("${app.vector-store.chroma-url:http://localhost:8000}")
    private String chromaUrl;

    @Value("${app.vector-store.chroma-persistence-path:./chroma}")
    private String persistencePath;

    @Value("${ai.ali.api-key:}")
    private String dashScopeApiKey;

    private Object embeddingStore; // 使用 Object 类型以支持多种存储类型
    private boolean useChroma = true; // 标记是否使用 ChromaDB

    // 用于存储物品ID与其元数据的映射关系，以便进行状态筛选
    private final ConcurrentHashMap<String, Object> metadataMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeCollection() {
        System.out.println("=== DEBUG: chromaUrl = " + chromaUrl);
        System.out.println("=== DEBUG: chromaUrl 是否包含 /api/v2: " + chromaUrl.contains("/api/v2"));
        try {
            log.info("正在检查 ChromaDB 服务可用性，连接地址：{}", chromaUrl);

            // 首先检查 ChromaDB 服务是否可用
            if (!isChromaServerAvailable()) {
                log.warn("ChromaDB 服务不可用，将使用内存存储作为备用方案");
                fallbackToInMemoryStore();
                return;
            }

            System.out.println("正在初始化向量数据库，尝试使用 ChromaDB 模式，连接地址：" + chromaUrl + "，集合名称：" + collectionName);

            // 尝试创建 ChromaDB 存储实例
            ChromaEmbeddingStore chromaStore;
            try {
                // 先尝试创建集合
                chromaStore = ChromaEmbeddingStore.builder()
                        .baseUrl(chromaUrl)
                        .collectionName(collectionName)
                        .build();

                // 尝试执行一个简单操作以验证连接
                // 使用更简单的连接测试方法
                try {
                    Embedding testEmbedding = Embedding.from(new float[] { 0.1f, 0.2f, 0.3f });
                    String testId = "test_connection";
                    chromaStore.add(testId, testEmbedding);
                    chromaStore.removeAll(java.util.Arrays.asList(testId));
                } catch (Exception connectionTestException) {
                    log.warn("ChromaDB连接测试失败，可能由于API版本不兼容，错误详情: {}", connectionTestException.getMessage());
                    log.debug("ChromaDB连接测试异常堆栈:", connectionTestException);
                    // 即使连接测试失败，也尝试使用ChromaDB（某些版本的API可能存在差异）
                    // 这里可以选择继续使用ChromaDB或者回退到内存存储
                    // 根据错误类型判断是否真正需要回退
                    if (connectionTestException.getMessage() != null &&
                            connectionTestException.getMessage().contains("405")) {
                        // HTTP 405 错误表示API方法不被支持，需要回退
                        fallbackToInMemoryStore();
                        return;
                    }
                }

            } catch (Exception initException) {
                log.warn("ChromaDB连接初始化失败，错误详情: {}", initException.getMessage());
                log.error("ChromaDB连接初始化失败，完整错误信息: ", initException);
                fallbackToInMemoryStore();
                return;
            }

            this.embeddingStore = chromaStore;
            this.useChroma = true;
            log.info("ChromaDB向量数据库初始化成功，集合名称：{}，连接地址：{}", collectionName, chromaUrl);
            System.out.println("ChromaDB向量数据库初始化成功，集合名称：" + collectionName + "，连接地址：" + chromaUrl);
        } catch (Exception e) {
            log.warn("ChromaDB向量数据库初始化失败，将使用内存存储作为备用方案。错误详情: {}", e.getMessage());
            log.debug("ChromaDB初始化异常堆栈:", e);
            fallbackToInMemoryStore();
        }
    }

    /**
     * 检查 ChromaDB 服务是否可用 (v2 API)
     */
    private boolean isChromaServerAvailable() {
        try {
            // 创建配置了超时的RestTemplate
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000); // 5秒连接超时
            factory.setReadTimeout(5000); // 5秒读取超时

            RestTemplate restTemplate = new RestTemplate(factory);

            String healthUrl = chromaUrl + "/api/v1/heartbeat";
            log.info("正在检测ChromaDB服务: {}", healthUrl);

            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            boolean isAvailable = response.getStatusCode().is2xxSuccessful();

            log.info("ChromaDB 服务心跳检测结果: {} (状态码: {})",
                    isAvailable ? "可用" : "不可用", response.getStatusCode());

            return isAvailable;

        } catch (ResourceAccessException e) {
            log.error("无法连接到 ChromaDB 服务: {} - {}", chromaUrl, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("ChromaDB 服务检查出现异常", e);
            return false;
        }
    }

    /**
     * 回退到内存存储
     */
    private void fallbackToInMemoryStore() {
        this.embeddingStore = new InMemoryEmbeddingStore<TextSegment>();
        this.useChroma = false;
        log.info("内存向量数据库初始化成功");
        System.out.println("内存向量数据库初始化成功");
    }

    @Override
    public void addTextToCollection(TextEmbeddingDTO textEmbeddingDTO) {
        try {
            String id = textEmbeddingDTO.getId();
            String text = textEmbeddingDTO.getText();
            Embedding embedding = generateEmbedding(text);

            if (useChroma) {
                ((ChromaEmbeddingStore) embeddingStore).add(id, embedding);
            } else {
                ((InMemoryEmbeddingStore<TextSegment>) embeddingStore).add(id, embedding, TextSegment.from(text));
            }

            // 存储元数据映射
            if (textEmbeddingDTO.getMetadata() != null) {
                metadataMap.put(id, textEmbeddingDTO.getMetadata());
            }

            log.info("文本已添加到向量数据库，ID：{}，使用存储类型：{}", id, useChroma ? "ChromaDB" : "内存");
        } catch (Exception e) {
            log.error("添加文本到向量数据库失败，ID：{}", textEmbeddingDTO.getId(), e);
            throw new RuntimeException("添加文本到向量数据库失败", e);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<String> searchInCollection(String query, int k) {
        try {
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
    public List<String> searchInCollectionByStatus(String query, int k, Integer statusFilter) {
        try {
            Embedding queryEmbedding = generateEmbedding(query);

            List<EmbeddingMatch<TextSegment>> relevant;
            if (useChroma) {
                relevant = ((ChromaEmbeddingStore) embeddingStore).findRelevant(queryEmbedding, 10000); // 获取更多结果以供筛选
            } else {
                relevant = ((InMemoryEmbeddingStore<TextSegment>) embeddingStore).findRelevant(queryEmbedding, 10000); // 获取更多结果以供筛选
            }

            List<String> results = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : relevant) {
                String id = match.embeddingId();

                // 检查元数据中的状态是否匹配
                Object metadata = metadataMap.get(id);
                if (metadata instanceof com.whut.lostandfoundforwhut.model.dto.ItemMetadata) {
                    com.whut.lostandfoundforwhut.model.dto.ItemMetadata itemMetadata = (com.whut.lostandfoundforwhut.model.dto.ItemMetadata) metadata;
                    if (statusFilter == null || itemMetadata.getStatus().equals(statusFilter)) {
                        results.add(id);
                        if (results.size() >= k) {
                            break; // 达到所需数量就停止
                        }
                    }
                } else {
                    // 如果没有元数据或者元数据类型不匹配，仍然添加到结果中
                    results.add(id);
                    if (results.size() >= k) {
                        break;
                    }
                }
            }
            log.info("向量搜索完成（按状态筛选），查询：{}，状态筛选：{}，返回结果数量：{}，使用存储类型：{}",
                    query, statusFilter, results.size(), useChroma ? "ChromaDB" : "内存");
            return results;
        } catch (Exception e) {
            log.error("按状态筛选的向量搜索失败，查询：{}，状态：{}", query, statusFilter, e);
            throw new RuntimeException("向量搜索失败", e);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getCollectionSize() {
        try {
            Embedding queryEmbedding = generateEmbedding("anything");

            List<EmbeddingMatch<TextSegment>> allItems;
            if (useChroma) {
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
        try {
            if (useChroma) {
                log.warn("ChromaDB Java客户端不直接支持按ID删除，请考虑重新构建集合");
            }
            // 无论哪种存储方式，都从元数据映射中删除
            metadataMap.remove(id);
            log.info("从向量数据库删除条目完成，ID：{}，使用存储类型：{}", id, useChroma ? "ChromaDB" : "内存");
        } catch (Exception e) {
            log.error("从向量数据库删除条目失败，ID：{}", id, e);
            throw new RuntimeException("删除条目失败", e);
        }
    }

    @Override
    public void clearCollection() {
        try {
            if (useChroma) {
                // 对于ChromaDB，重新创建实例
                embeddingStore = ChromaEmbeddingStore.builder()
                        .baseUrl(chromaUrl)
                        .collectionName(collectionName)
                        .build();
            } else {
                // 对于内存存储，创建新的实例
                embeddingStore = new InMemoryEmbeddingStore<TextSegment>();
            }
            // 清空元数据映射
            metadataMap.clear();
            log.info("向量数据库集合已清空，使用存储类型：{}", useChroma ? "ChromaDB" : "内存");
        } catch (Exception e) {
            log.error("清空向量数据库集合失败", e);
            throw new RuntimeException("清空集合失败", e);
        }
    }

    private Embedding generateEmbedding(String text) {
        // 补全：API密钥空判断，恢复原有的容错降级逻辑
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
            log.error("调用DashScope API失败，使用简化嵌入向量", e);
            return Embedding.from(computeSimpleEmbedding(text));
        }
    }

    private float[] computeSimpleEmbedding(String text) {
        byte[] bytes = text.getBytes();
        float[] vector = new float[384];
        for (int i = 0; i < bytes.length; i++) {
            vector[i % vector.length] += bytes[i];
        }
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