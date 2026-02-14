package com.whut.lostandfoundforwhut.service.impl;

import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.mapper.ItemMapper;
import com.whut.lostandfoundforwhut.mapper.TagMapper;
import com.whut.lostandfoundforwhut.model.entity.Item;
import com.whut.lostandfoundforwhut.model.vo.ItemDetailVO;
import com.whut.lostandfoundforwhut.model.vo.UserPublicVO;
import com.whut.lostandfoundforwhut.service.IImageService;
import com.whut.lostandfoundforwhut.service.IItemDetailService;
import com.whut.lostandfoundforwhut.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemDetailServiceImpl implements IItemDetailService {

    private final ItemMapper itemMapper;
    private final TagMapper tagMapper;
    private final IImageService imageService;
    private final IUserService userService;

    @Override
    public ItemDetailVO getItemDetailById(Long itemId) {
        if (itemId == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "物品ID不能为空");
        }

        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new AppException(ResponseCode.ITEM_NOT_FOUND.getCode(), ResponseCode.ITEM_NOT_FOUND.getInfo());
        }

        List<String> tags = tagMapper.selectNamesByItemId(itemId);
        if (tags == null) {
            tags = new ArrayList<>();
        }

        List<String> imageUrls = imageService.getUrlsByItemId(itemId);
        if (imageUrls == null) {
            imageUrls = new ArrayList<>();
        }

        UserPublicVO publisher = userService.getPublicUserById(item.getUserId());
        return ItemDetailVO.from(item, tags, imageUrls, publisher);
    }
}

