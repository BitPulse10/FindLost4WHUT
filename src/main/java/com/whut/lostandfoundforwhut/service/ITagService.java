package com.whut.lostandfoundforwhut.service;

import com.whut.lostandfoundforwhut.model.entity.Tag;
import com.whut.lostandfoundforwhut.model.vo.PageResultVO;

import java.util.List;

/**
 * @author Codex
 * @date 2026/02/04
 * @description 标签服务接口
 */
public interface ITagService {
    /**
     * 标签查询（支持关键字）
     *
     * @param keyword 关键字（可为空）
     * @param pageNo  页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    PageResultVO<Tag> listTags(String keyword, Integer pageNo, Integer pageSize);

    /**
     * 解析标签文本
     *
     * @param tagText 标签文本
     * @return 规范化后的标签名称列表
     */
    List<String> parseTagText(String tagText);

    /**
     * 获取或创建标签
     *
     * @param names 标签名称列表
     * @return 标签实体列表
     */
    List<Tag> getOrCreateTags(List<String> names);

    /**
     * 替换物品标签关联
     *
     * @param itemId 物品ID
     * @param names 标签名称列表
     */
    void replaceTagsForItem(Long itemId, List<String> names);

    /**
     * 获取物品标签名称列表
     *
     * @param itemId 物品ID
     * @return 标签名称列表
     */
    List<String> getTagNamesByItemId(Long itemId);
}
