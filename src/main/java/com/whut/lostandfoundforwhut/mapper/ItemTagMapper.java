package com.whut.lostandfoundforwhut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whut.lostandfoundforwhut.model.entity.ItemTag;

/**
 * @author DXR
 * @date 2026/01/30
 * @description 物品-标签关联 Mapper 接口
 */
public interface ItemTagMapper extends BaseMapper<ItemTag> {
    /**
     * 根据物品ID删除关联
     *
     * @param itemId 物品ID
     * @return 影响行数
     */
    int deleteByItemId(Long itemId);

    /**
     * 批量插入物品-标签关联
     *
     * @param items 关联列表
     * @return 影响行数
     */
    int insertBatch(java.util.List<ItemTag> items);
}
