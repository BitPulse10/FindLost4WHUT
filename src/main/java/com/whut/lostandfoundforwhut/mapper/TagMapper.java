package com.whut.lostandfoundforwhut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whut.lostandfoundforwhut.model.entity.Tag;

/**
 * @author DXR
 * @date 2026/01/30
 * @description 标签 Mapper 接口
 */
public interface TagMapper extends BaseMapper<Tag> {
    /**
     * 根据物品ID获取标签名称列表
     *
     * @param itemId 物品ID
     * @return 标签名称列表
     */
    java.util.List<String> selectNamesByItemId(Long itemId);

    /**
     * 批量获取物品标签名称
     *
     * @param itemIds 物品ID列表
     * @return 物品-标签名称映射列表
     */
    java.util.List<com.whut.lostandfoundforwhut.model.dto.ItemTagNameDTO> selectNamesByItemIds(
            java.util.List<Long> itemIds);
}
