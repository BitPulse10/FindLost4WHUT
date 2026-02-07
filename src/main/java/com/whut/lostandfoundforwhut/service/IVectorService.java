package com.whut.lostandfoundforwhut.service;

import java.util.List;

import com.whut.lostandfoundforwhut.model.dto.TextEmbeddingDTO;
import com.whut.lostandfoundforwhut.model.entity.Item;

/**
 * 向量数据库服务接口
 */
public interface IVectorService {

    /**
     * 初始化向量数据库集合
     */
    void initializeCollection();

    /**
     * 将物品信息添加到向量数据库
     *
     * @param item 物品实体
     */
    void addToVectorDatabase(Item item);

    /**
     * 添加物品的图片到向量数据库
     *
     * @param item      物品实体
     * @param imageUrls 图片URL列表
     */
    void addImagesToVectorDatabases(Item item, List<String> imageUrls);

    /**
     * 添加物品的单张图片到向量数据库
     *
     * @param item     物品实体
     * @param imageUrl 图片URL
     */
    void addImagesToVectorDatabase(Item item, String imageUrl);

    /**
     * 更新向量数据库中的物品信息
     *
     * @param item 更新后的物品实体
     */
    void updateVectorDatabase(Item item);

    /**
     * 从向量数据库中删除物品信息
     *
     * @param itemId 物品ID
     */
    void removeFromVectorDatabase(Long itemId);

    /**
     * 添加文本到向量数据库
     *
     * @param textEmbeddingDTO 包含ID和文本内容的DTO
     */
    void addTextToCollection(TextEmbeddingDTO textEmbeddingDTO);

    /**
     * 添加文本和对应图片列表到向量数据库
     * 
     * @param query
     * @param imageUrls
     * @param k
     * @return
     */
    // void addTextToCollectionImage(TextEmbeddingDTO textEmbeddingDTO, List<String>
    // imageUrls);

    /**
     * 在向量数据库中搜索相似文本
     *
     * @param query 查询文本
     * @param k     返回最相近的k个结果
     * @return 匹配的ID列表
     */
    List<String> searchInCollection(String query, int maxResults);

    /**
     * 获取集合中的所有条目
     *
     * @return 条目总数
     */
    int getCollectionSize();

    /**
     * 删除集合中的特定条目
     *
     * @param id 要删除的条目的ID
     */
    void deleteFromCollection(String id);

    /**
     * 清空整个集合
     */
    void clearCollection();
}