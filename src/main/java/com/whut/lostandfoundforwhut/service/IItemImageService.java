package com.whut.lostandfoundforwhut.service;

import com.whut.lostandfoundforwhut.model.dto.ItemImageAddDTO;
import com.whut.lostandfoundforwhut.model.entity.Image;

import java.util.List;

/**
 * 物品-图片关联服务接口
 */
public interface IItemImageService {

    /**
     * 根据 itemId 查询所有图片
     * @param itemId 物品ID
     * @return 图片响应DTO列表
     */
    List<Image> getImagesByItemId(Long itemId);

    /**
     * 保存物品和图片的关联关系
     * @param itemId 物品ID
     * @param imageIds 图片ID列表
     * @return 是否保存成功
     */
    boolean saveItemImages(ItemImageAddDTO itemImageAddDTO);
}
