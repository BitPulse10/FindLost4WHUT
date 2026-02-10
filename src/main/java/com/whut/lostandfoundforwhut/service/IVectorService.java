package com.whut.lostandfoundforwhut.service;

import java.util.List;

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
    void updateVectorDatabase(Item item, String imageUrl);

    /**
     * 从向量数据库中删除物品信息
     *
     * @param itemId 物品ID
     */
    void removeFromVectorDatabase(Long itemId);

    /**
     * 在向量数据库中搜索相似文本
     *
     * @param query      查询文本
     * @param imageUrl   图片URL
     * @param maxResults 返回最相近的k个结果
     * @return 匹配的ID列表
     */
    List<String> searchInCollection(String query, String imageUrl, int maxResults);

    /**
     * 获取集合中的所有条目
     *
     * @return 条目总数
     */
    int getCollectionSize();

    /**
     * 清空整个集合
     */
    void clearCollection();
}