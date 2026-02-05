package com.whut.lostandfoundforwhut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whut.lostandfoundforwhut.model.entity.Item;

/**
 * @author DXR
 * @date 2026/01/30
 * @description 物品 Mapper 接口
 */
public interface ItemMapper extends BaseMapper<Item> {
    /**
     * 物理删除物品（绕过逻辑删除）
     * @param id 物品ID
     * @return 受影响行数
     */
    int deletePhysicalById(Long id);
}
