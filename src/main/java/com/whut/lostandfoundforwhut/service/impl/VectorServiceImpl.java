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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whut.lostandfoundforwhut.model.entity.Item;

/**
 * 向量数据库服务实现L
 */
@Slf4j
@Service
public class VectorServiceImpl implements IVectorService {

    @Value("${app.vector-store.collection-name:item_image_vector}")
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

        try {
            // 创建 ChromaDB 存储实例
            this.embeddingStore = ChromaEmbeddingStore.builder()
                    .baseUrl(chromaUrl)
                    .collectionName(collectionName)
                    .build();

            this.initialized = true;
            log.info("ChromaDB向量数据库初始化成功，集合名称：{}，连接地址：{}", collectionName, chromaUrl);
        } catch (Exception e) {
            log.error("ChromaDB向量数据库初始化失败: {}", e.getMessage(), e);
            this.initialized = false;
        }
    }

    /**
     * 检查是否已初始化
     */
    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("向量数据库未初始化");
        }
    }

    /**
     * 检查指定ID的向量是否已存在
     * 
     * @param itemId 向量ID（格式：item_123）
     * @return 是否存在
     */
    private boolean isVectorExists(String itemId) {
        try {
            // 使用一个简单的查询来检查是否存在该ID
            Embedding dummyEmbedding = Embedding.from(new float[1024]);
            List<EmbeddingMatch<TextSegment>> results = embeddingStore.findRelevant(dummyEmbedding, 1000);

            // 检查返回结果中是否包含指定ID
            return results.stream()
                    .map(EmbeddingMatch::embeddingId)
                    .anyMatch(id -> id.equals(itemId));
        } catch (Exception e) {
            log.debug("检查向量是否存在时出现异常: {}", itemId, e);
            return false; // 出现异常时认为不存在
        }
    }

    @Override
    public void addImagesToVectorDatabase(Item item, List<String> imageUrls) {
        try {
            if (imageUrls != null && !imageUrls.isEmpty()) {
                // 处理多模态嵌入：结合文本描述和所有图片信息
                String itemDescription = item.getDescription() != null ? item.getDescription() : "";

                // 为整个物品创建一个多模态嵌入，包含文本描述和所有图片
                String itemId = "item_" + item.getId();

                // 创建多模态嵌入（文本+所有图片）
                Embedding embedding = generateMultimodalEmbedding(itemDescription, imageUrls);

                if (embedding != null) {

                    // 检查并删除已存在的ID（避免首次添加时的异常）
                    if (isVectorExists(itemId)) {
                        try {
                            embeddingStore.removeAll(List.of(itemId));
                            log.debug("已删除已存在的向量ID: {}", itemId);
                        } catch (Exception e) {
                            log.warn("删除已存在的向量ID失败: {}", itemId, e);
                        }
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
    public void updateVectorDatabase(Item item, List<String> imageUrls) {
        try {
            // 先删除旧的向量数据
            removeFromVectorDatabase(item.getId());

            addImagesToVectorDatabase(item, imageUrls);

            log.info("向量数据库中物品信息已更新，ID：{}，图片URLs：{}", item.getId(), imageUrls);
        } catch (Exception e) {
            log.error("更新向量数据库时发生异常，物品ID：{}，图片URLs：{}", item.getId(), imageUrls, e);
            // 这里不抛出异常，因为向量数据库的失败不应影响主业务流程
        }
    }

    @Override
    public void removeFromVectorDatabase(Long itemId) {
        checkInitialized();

        try {
            String id = "item_" + itemId;

            // 先检查向量是否存在，避免不必要的异常
            if (isVectorExists(id)) {
                try {
                    embeddingStore.removeAll(List.of(id));
                    log.info("向量数据库中物品信息已删除，ID：{}", itemId);
                } catch (Exception e) {
                    log.warn("删除向量条目失败，ID：{}", id, e);
                }
            } else {
                log.debug("要删除的向量ID不存在: {}", id);
            }
        } catch (Exception e) {
            log.error("删除向量数据库条目时发生异常，物品ID：{}", itemId, e);
            // 这里不抛出异常，因为向量数据库的失败不应影响主业务流程
        }
    }

    @Override
    public List<String> searchInCollection(String query, List<String> imageUrls, int maxResults) {
        checkInitialized();

        try {
            // 检查查询文本和图片是否都为空
            if ((query == null || query.trim().isEmpty()) &&
                    (imageUrls == null || imageUrls.isEmpty())) {
                log.warn("查询文本和图片都为空，返回空搜索结果");
                return List.of();
            }

            // 如果只有查询文本为空但有图片，允许继续处理
            if (query == null || query.trim().isEmpty()) {
                log.info("查询文本为空，但有图片，进行纯图片搜索");
                query = "";
            }

            if (maxResults <= 0) {
                log.warn("搜索结果数量必须大于0，返回空搜索结果");
                return List.of();
            }

            Embedding queryEmbedding;
            List<String> imageUrlsToSearch = new ArrayList<>();
            // 只有当imageUrl不为空且不为空白字符串时才添加
            if (imageUrls != null && !imageUrls.isEmpty()) {
                imageUrlsToSearch.addAll(imageUrls);
            }
            queryEmbedding = generateMultimodalEmbedding(query, imageUrlsToSearch);

            List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, maxResults);

            List<String> results = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : relevant) {
                results.add(match.embeddingId());
            }
            log.info("向量搜索完成，查询：{}，图片URLs：{}，返回结果数量：{}", query, imageUrls, results.size());
            return results;
        } catch (Exception e) {
            log.error("向量搜索失败，查询：{}，图片URLs：{}", query, imageUrls, e);
            throw new RuntimeException("向量搜索失败", e);
        }
    }

    @Override
    public int getCollectionSize() {
        checkInitialized();
        try {
            // 首先获取集合的UUID
            String collectionId = getCollectionIdByName(collectionName);
            if (collectionId == null) {
                log.warn("无法获取集合的ID，集合名称：{}", collectionName);
                return 0;
            }

            // 使用集合UUID获取集合信息
            String infoUrl = chromaUrl + "/api/v1/collections/" + collectionId + "/count";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(infoUrl))
                    .GET() // 使用GET请求获取计数
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body().trim();
                System.out.println("集合统计响应体: " + responseBody);
                log.info("集合统计响应体: {}", responseBody);

                // 直接解析纯数字格式
                int size = 0;
                try {
                    size = Integer.parseInt(responseBody);
                    log.info("响应体为纯数字，集合大小：{}", size);
                } catch (NumberFormatException e) {
                    log.warn("无法解析集合统计响应中的数字，响应体：{}，错误：{}", responseBody, e.getMessage());
                    return 0;
                }

                log.info("获取集合大小完成，当前大小：{}", size);
                return size;
            } else {
                log.warn("获取集合统计信息失败，状态码: {}，响应: {}", response.statusCode(), response.body());
                return 0;
            }
        } catch (Exception e) {
            log.error("获取集合大小失败", e);
            return 0;
        }
    }

    @Override
    public void clearCollection() {
        checkInitialized();

        HttpClient client = HttpClient.newHttpClient();
        String deleteUrl = chromaUrl + "/api/v1/collections/" + collectionName;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(deleteUrl))
                .DELETE()
                .build();

        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("集合 " + collectionName + " 删除成功！");

                // 删除集合后重新初始化新的
                initializeCollection();
            } else {
                System.err.println("删除失败，状态码：" + response.statusCode() + "，响应：" + response.body());
            }
        } catch (Exception e) {
            System.err.println("请求异常：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 根据集合名称获取集合的 UUID（改进版，使用 Jackson）
     */
    private String getCollectionIdByName(String collectionName) {
        try {
            String listUrl = chromaUrl + "/api/v1/collections";
            log.info("获取集合列表URL: {}", listUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(listUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            log.info("获取集合列表状态码: {}", response.statusCode());
            log.info("获取集合列表响应体: {}", response.body());

            if (response.statusCode() != 200) {
                log.warn("获取集合列表失败，状态码: {}", response.statusCode());
                return null;
            }

            // 使用 Jackson 解析 JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());

            // ChromaDB 返回的是数组
            for (JsonNode node : root) {
                String name = node.get("name").asText();
                if (collectionName.equals(name)) {
                    String id = node.get("id").asText();
                    log.info("找到集合 {} 的ID: {}", collectionName, id);
                    return id;
                }
            }

            log.warn("未找到名称为 {} 的集合", collectionName);
            return null;

        } catch (Exception e) {
            log.error("获取集合ID时发生异常", e);
            return null;
        }
    }

    /**
     * 生成多模态嵌入向量（文本+图像列表）
     * 使用HTTP API方式
     * 
     * @param text      输入文本
     * @param imageUrls 图像数据
     * @return 嵌入向量
     */
    public Embedding generateMultimodalEmbedding(String text, List<String> imageUrls) {
        System.out.println("进入多张处理");
        // 使用HTTP方法处理多张图片
        try {
            Embedding httpResult = generateMultimodalEmbeddingsViaHttp(text, imageUrls);
            if (httpResult != null) {
                log.debug("多图片多模态嵌入生成成功（HTTP方式）");
                return httpResult;
            } else {
                log.error("通过HTTP API生成多图片多模态嵌入失败");
                throw new RuntimeException("通过HTTP API生成多图片多模态嵌入失败");
            }
        } catch (Exception e) {
            log.error("使用HTTP生成多图片多模态嵌入失败: {}", e.getMessage(), e);
            throw new RuntimeException("HTTP请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通过HTTP API生成多模态嵌入向量
     */
    private Embedding generateMultimodalEmbeddingsViaHttp(String text, List<String> imageUrls) {
        try {
            // 构建API请求
            StringBuilder requestBody = new StringBuilder();
            requestBody.append("{");
            requestBody.append("\"model\":\"qwen3-vl-embedding\",");
            requestBody.append("\"input\":{");
            requestBody.append("\"contents\":[{");

            // 添加文本内容
            if (text != null && !text.trim().isEmpty()) {
                requestBody.append("\"text\":\"").append(text.replace("\"", "\\\"")).append("\"");
                // 如果有文本且后面还有有效的图片，则添加逗号
                boolean hasValidImages = imageUrls != null
                        && imageUrls.stream().anyMatch(url -> url != null && !url.trim().isEmpty());
                if (hasValidImages) {
                    requestBody.append(",");
                }
            }

            // 添加图片内容
            if (imageUrls != null && !imageUrls.isEmpty()) {
                boolean firstImage = true;
                for (int i = 0; i < imageUrls.size(); i++) {
                    String imageUrl = imageUrls.get(i);
                    // 只有当图片URL不为空时才添加
                    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                        if (!firstImage) {
                            requestBody.append(",");
                        }
                        requestBody.append("\"image\":\"").append(imageUrl.replace("\"", "\\\"")).append("\"");
                        firstImage = false;
                    }
                }
            }

            requestBody.append("}]");
            requestBody.append("},");
            requestBody.append("\"parameters\":{");
            requestBody.append("\"dimension\":1024,");
            requestBody.append("\"output_type\":\"dense\",");
            requestBody.append("\"fps\":0.5");
            requestBody.append("}}");

            String jsonBody = requestBody.toString();
            log.info("HTTP请求体（正确格式）: {}", jsonBody);
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

                // 计算向量个数
                int numVectors = 0;
                int pos = 0;
                while ((pos = responseBody.indexOf("\"embedding\":[", pos)) != -1) {
                    numVectors++;
                    pos += 13; // 跳过 "\"embedding\":["
                }
                log.info("输出结果中的向量个数: {}", numVectors);

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

                log.warn("无法从HTTP响应中提取嵌入向量");
                return null;
            } else {
                log.warn("HTTP API调用失败，状态码: {}，响应: {}", response.statusCode(), response.body());
                return null;
            }

        } catch (Exception e) {
            log.warn("通过HTTP API生成多图片多模态嵌入向量时发生异常: {}", e.getMessage(), e);
            return null;
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

}