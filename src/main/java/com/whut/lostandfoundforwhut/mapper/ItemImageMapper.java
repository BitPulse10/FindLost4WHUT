package com.whut.lostandfoundforwhut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whut.lostandfoundforwhut.model.entity.Image;
import com.whut.lostandfoundforwhut.model.entity.ItemImage;

import java.util.List;

/**
 * @author DXR
 * @date 2026/01/30
 * @description 物品-图片关联 Mapper 接口
 */
public interface ItemImageMapper extends BaseMapper<ItemImage> {

    /**
     * 根据物品ID获取图片列表
     * 
     * @param itemId 物品ID
     * @return 图片列表
     */
    List<Image> getImagesByItemId(Long itemId);

    /**
     * 根据物品ID获取图片URL列表
     * 
     * @param itemId 物品ID
     * @return 图片URL列表
     */
    List<String> getImageUrlsByItemId(Long itemId);

    /**
     * 根据物品ID获取图片ID列表
     * 
     * @param itemId 物品ID
     * @return 图片ID列表
     */
    List<Long> getImageIdsByItemId(Long itemId);

    /**
     * 批量插入物品-图片关联
     * 
     * @param itemId   物品ID
     * @param imageIds 图片ID列表
     * @return 是否插入成功
     */
    boolean insertItemImages(Long itemId, List<Long> imageIds);

    /**
     * 批量删除物品-图片关联
     * 
     * @param itemId   物品ID
     * @param imageIds 图片ID列表
     * @return 删除的记录数
     */
    int deleteItemImages(Long itemId, List<Long> imageIds);

    /**
     * 根据图片ID删除所有关联关系
     * 
     * @param imageIds 图片ID列表
     * @return 删除的记录数
     */
    int deleteAllItemImagesByImageIds(List<Long> imageIds);
}
