package com.whut.lostandfoundforwhut.service;

import java.util.List;

import com.whut.lostandfoundforwhut.model.dto.TextEmbeddingDTO;

/**
 * 向量数据库服务接口
 */
public interface IVectorService {

    /**
     * 初始化向量数据库集合
     */
    void initializeCollection();

    /**
     * 添加文本到向量数据库
     *
     * @param textEmbeddingDTO 包含ID和文本内容的DTO
     */
    void addTextToCollection(TextEmbeddingDTO textEmbeddingDTO);

    /**
     * 在向量数据库中搜索相似文本
     *
     * @param query 查询文本
     * @param k     返回最相近的k个结果
     * @return 匹配的ID列表
     */
    List<String> searchInCollection(String query, int k);

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

    /**
     * 根据查询文本和状态筛选搜索相似文本
     *
     * @param query        查询文本
     * @param k            返回最相近的k个结果
     * @param statusFilter 状态筛选条件（例如：ACTIVE=0, CLOSED=1）
     * @return 匹配的ID列表
     */
    List<String> searchInCollectionByStatus(String query, int k, Integer statusFilter);
}