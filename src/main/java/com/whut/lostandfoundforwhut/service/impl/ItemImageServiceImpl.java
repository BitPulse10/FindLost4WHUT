package com.whut.lostandfoundforwhut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.mapper.ImageMapper;
import com.whut.lostandfoundforwhut.mapper.ItemImageMapper;
import com.whut.lostandfoundforwhut.model.dto.ItemImageAddDTO;
import com.whut.lostandfoundforwhut.model.entity.Image;
import com.whut.lostandfoundforwhut.model.entity.ItemImage;
import com.whut.lostandfoundforwhut.service.IItemImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ItemImageServiceImpl extends ServiceImpl<ItemImageMapper, ItemImage> implements IItemImageService {

    @Autowired
    private ImageMapper imageMapper;

    @Override
    public List<Image> getImagesByItemId(Long itemId) {
        QueryWrapper<ItemImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("item_id", itemId);
        List<ItemImage> itemImages = baseMapper.selectList(queryWrapper);

        List<Image> images = new ArrayList<>();
        for (ItemImage itemImage : itemImages) {
            Image image = imageMapper.selectById(itemImage.getImageId());
            if (image != null) {
                images.add(image);
            }
        }

        return images;
    }

    @Override
    @Transactional
    public boolean saveItemImages(ItemImageAddDTO itemImageAddDTO) {
        Long itemId = itemImageAddDTO.getItemId();
        List<Long> imageIds = itemImageAddDTO.getImageIds();
        if (itemId == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "物品ID不能为空");
        }

        if (imageIds == null || imageIds.isEmpty()) {
            return true;
        }

        List<ItemImage> itemImages = new ArrayList<>();
        for (Long imageId : imageIds) {
            ItemImage itemImage = new ItemImage();
            itemImage.setItemId(itemId);
            itemImage.setImageId(imageId);
            itemImages.add(itemImage);
        }
        
        return saveBatch(itemImages);
    }
}
