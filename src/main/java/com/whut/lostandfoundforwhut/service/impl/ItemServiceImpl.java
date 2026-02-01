package com.whut.lostandfoundforwhut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.enums.item.ItemStatus;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.mapper.ItemMapper;
import com.whut.lostandfoundforwhut.mapper.UserMapper;
import com.whut.lostandfoundforwhut.model.dto.ItemDTO;
import com.whut.lostandfoundforwhut.model.dto.PageQueryDTO;
import com.whut.lostandfoundforwhut.model.entity.Item;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.model.vo.PageResultVO;
import com.whut.lostandfoundforwhut.service.IItemService;
import com.whut.lostandfoundforwhut.common.utils.page.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author Qoder
 * @date 2026/01/31
 * @description 物品服务实现类
 */
@Service
@RequiredArgsConstructor
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {

    private final ItemMapper itemMapper;
    private final UserMapper userMapper;

    @Override
    public Item addItem(ItemDTO itemDTO, Long userId) {
        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }

        Item item = new Item();
        item.setUserId(userId);
        item.setType(itemDTO.getType());
        item.setEventTime(itemDTO.getEventTime());
        item.setEventPlace(itemDTO.getEventPlace());
        // 设置默认状态为有效
        item.setStatus(ItemStatus.ACTIVE.getCode());
        item.setDescription(itemDTO.getDescription());
        // 生成物品编码
        item.setItemCode(generateItemCode());

        // 保存到数据库
        itemMapper.insert(item);

        return item;
    }

    @Override
    public Item updateItem(Long itemId, ItemDTO itemDTO, Long userId) {
        // 先查询物品是否存在且属于当前用户
        Item existingItem = itemMapper.selectById(itemId);
        if (existingItem == null) {
            throw new AppException(ResponseCode.ITEM_NOT_FOUND.getCode(), ResponseCode.ITEM_NOT_FOUND.getInfo());
        }
        if (!existingItem.getUserId().equals(userId)) {
            throw new AppException(ResponseCode.NO_PERMISSION.getCode(), ResponseCode.NO_PERMISSION.getInfo());
        }
        if (ItemStatus.CLOSED.getCode().equals(existingItem.getStatus())) {
            throw new AppException(ResponseCode.ITEM_STATUS_INVALID.getCode(), ResponseCode.ITEM_STATUS_INVALID.getInfo());
        }

        // 更新物品信息
        if (itemDTO.getType() != null) {
            existingItem.setType(itemDTO.getType());
        }
        if (itemDTO.getEventTime() != null) {
            existingItem.setEventTime(itemDTO.getEventTime());
        }
        if (itemDTO.getEventPlace() != null) {
            existingItem.setEventPlace(itemDTO.getEventPlace());
        }
        if (itemDTO.getStatus() != null) {
            existingItem.setStatus(itemDTO.getStatus());
        }
        if (itemDTO.getDescription() != null) {
            existingItem.setDescription(itemDTO.getDescription());
        }

        // 更新数据库
        itemMapper.updateById(existingItem);

        return existingItem;
    }

    @Override
    public Item getItemById(Long itemId) {
        return itemMapper.selectById(itemId);
    }

    @Override
    public PageResultVO<Item> filterItems(PageQueryDTO pageQueryDTO, Integer type, Integer status, String keyword) {
        // 创建MyBatis-Plus分页对象
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Item> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pageQueryDTO.getPageNo(), pageQueryDTO.getPageSize());

        LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<>();

        // 类型筛选
        if (type != null) {
            queryWrapper.eq(Item::getType, type);
        }

        // 状态筛选
        if (status != null) {
            queryWrapper.eq(Item::getStatus, status);
        }

        // 关键词搜索（描述字段）
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.like(Item::getDescription, keyword);
        }

        // 按创建时间倒序排列
        queryWrapper.orderByDesc(Item::getCreatedAt);

        // 执行查询
        itemMapper.selectPage(page, queryWrapper);

        // 封装分页结果
        return PageUtils.toPageResult(page);
    }

    @Override
    public boolean takeDownItem(Long itemId, Long userId) {
        // 查询物品是否存在且属于当前用户
        Item existingItem = itemMapper.selectById(itemId);
        if (existingItem == null) {
            throw new AppException(ResponseCode.ITEM_NOT_FOUND.getCode(), ResponseCode.ITEM_NOT_FOUND.getInfo());
        }
        if (!existingItem.getUserId().equals(userId)) {
            throw new AppException(ResponseCode.NO_PERMISSION.getCode(), ResponseCode.NO_PERMISSION.getInfo());
        }

        // 将物品状态设置为CLOSED（1）
        existingItem.setStatus(ItemStatus.CLOSED.getCode());

        // 更新数据库
        int result = itemMapper.updateById(existingItem);
        return result > 0;
    }

    /**
     * 生成物品编码
     */
    private Long generateItemCode() {
        // 使用当前时间戳和随机数组合生成唯一编码
        long timestamp = System.currentTimeMillis();
        int randomNum = (int) (Math.random() * 1000);
        // 组合成一个唯一的Long类型编码
        return timestamp * 1000 + randomNum;
    }
}
