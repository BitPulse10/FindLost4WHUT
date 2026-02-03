package com.whut.lostandfoundforwhut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.enums.item.ItemStatus;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.mapper.ItemMapper;
import com.whut.lostandfoundforwhut.mapper.ItemTagMapper;
import com.whut.lostandfoundforwhut.mapper.TagMapper;
import com.whut.lostandfoundforwhut.mapper.UserMapper;
import com.whut.lostandfoundforwhut.model.dto.ItemDTO;
import com.whut.lostandfoundforwhut.model.dto.ItemFilterDTO;
import com.whut.lostandfoundforwhut.model.dto.ItemMetadata;
import com.whut.lostandfoundforwhut.model.entity.Item;
import com.whut.lostandfoundforwhut.model.entity.ItemTag;
import com.whut.lostandfoundforwhut.model.entity.Tag;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.model.vo.PageResultVO;
import com.whut.lostandfoundforwhut.service.IItemService;
import com.whut.lostandfoundforwhut.service.IVectorService;
import com.whut.lostandfoundforwhut.model.dto.TextEmbeddingDTO;
import com.whut.lostandfoundforwhut.common.utils.page.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Qoder
 * @date 2026/01/31
 * @description 物品服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {

    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final TagMapper tagMapper;
    private final ItemTagMapper itemTagMapper;
    private final IVectorService vectorService;

    @Override
    @Transactional
    public Item addItem(ItemDTO itemDTO, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }

        Item item = new Item();
        item.setUserId(userId);
        item.setType(itemDTO.getType());
        item.setEventTime(itemDTO.getEventTime());
        item.setEventPlace(itemDTO.getEventPlace());
        item.setStatus(ItemStatus.ACTIVE.getCode());
        item.setDescription(itemDTO.getDescription());

        // 保存到数据库
        int rowsAffected = itemMapper.insert(item);
        log.info("数据库影响行数：{}，物品创建成功：{}", rowsAffected, item.getId());

        // 将物品描述添加到向量数据库
        addToVectorDatabase(item);

        return item;
    }

    @Override
    public Item updateItem(Long itemId, ItemDTO itemDTO, Long userId) {
        // 查询物品是否存在
        Item existingItem = itemMapper.selectById(itemId);
        if (existingItem == null) {
            throw new AppException(ResponseCode.ITEM_NOT_FOUND.getCode(), ResponseCode.ITEM_NOT_FOUND.getInfo());
        }
        if (!existingItem.getUserId().equals(userId)) {
            throw new AppException(ResponseCode.NO_PERMISSION.getCode(), ResponseCode.NO_PERMISSION.getInfo());
        }
        if (ItemStatus.CLOSED.getCode().equals(existingItem.getStatus())) {
            throw new AppException(ResponseCode.ITEM_STATUS_INVALID.getCode(),
                    ResponseCode.ITEM_STATUS_INVALID.getInfo());
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

        // 更新向量数据库中的物品描述
        updateVectorDatabase(existingItem);

        return existingItem;
    }

    @Override
    public Item getItemById(Long itemId) {
        if (itemId == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "物品ID不能为空");
        }

        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            log.warn("尝试获取不存在的物品，ID：{}", itemId);
            throw new AppException(ResponseCode.ITEM_NOT_FOUND.getCode(), ResponseCode.ITEM_NOT_FOUND.getInfo());
        }

        return item;
    }

    @Override
    public PageResultVO<Item> filterItems(ItemFilterDTO itemFilterDTO) {
        // 验证分页参数
        if (itemFilterDTO.getPageNo() == null || itemFilterDTO.getPageNo() < 1) {
            itemFilterDTO.setPageNo(1);
        }
        if (itemFilterDTO.getPageSize() == null || itemFilterDTO.getPageSize() < 1) {
            itemFilterDTO.setPageSize(10);
        }
        if (itemFilterDTO.getPageSize() > 100) { // 限制每页最大数量
            itemFilterDTO.setPageSize(100);
        }

        // 创建MyBatis-Plus分页对象
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Item> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                itemFilterDTO.getPageNo(), itemFilterDTO.getPageSize());

        LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<>();

        // 类型筛选
        if (itemFilterDTO.getType() != null) {
            queryWrapper.eq(Item::getType, itemFilterDTO.getType());
        }

        // 状态筛选
        if (itemFilterDTO.getStatus() != null) {
            queryWrapper.eq(Item::getStatus, itemFilterDTO.getStatus());
        }

        // 时间段筛选
        if (itemFilterDTO.getStartTime() != null) {
            queryWrapper.ge(Item::getCreatedAt, itemFilterDTO.getStartTime());
        }
        if (itemFilterDTO.getEndTime() != null) {
            queryWrapper.le(Item::getCreatedAt, itemFilterDTO.getEndTime());
        }

        // 标签筛选
        if (itemFilterDTO.getTags() != null && !itemFilterDTO.getTags().isEmpty()) {
            // 先查找匹配的标签ID
            List<Tag> tags = tagMapper.selectList(
                    new LambdaQueryWrapper<Tag>().in(Tag::getName, itemFilterDTO.getTags()));

            if (!tags.isEmpty()) {
                List<Long> tagIds = tags.stream()
                        .map(Tag::getId)
                        .collect(Collectors.toList());

                // 然后查找这些标签对应的物品ID
                List<Long> itemIds = itemTagMapper.selectList(
                        new LambdaQueryWrapper<ItemTag>().in(ItemTag::getTagId, tagIds)).stream()
                        .map(ItemTag::getItemId)
                        .distinct()
                        .collect(Collectors.toList());

                if (!itemIds.isEmpty()) {
                    queryWrapper.in(Item::getId, itemIds);
                } else {
                    // 如果没有找到匹配标签的物品，则返回空结果
                    queryWrapper.eq(Item::getId, -1L); // 一个不可能存在的ID
                }
            } else {
                // 如果没有找到匹配的标签，则返回空结果
                queryWrapper.eq(Item::getId, -1L); // 一个不可能存在的ID
            }
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
        if (ItemStatus.CLOSED.getCode().equals(existingItem.getStatus())) {
            throw new AppException(ResponseCode.ITEM_STATUS_INVALID.getCode(),
                    ResponseCode.ITEM_STATUS_INVALID.getInfo());
        }

        // 更新物品状态为关闭而不是物理删除
        existingItem.setStatus(ItemStatus.CLOSED.getCode());
        int rows = itemMapper.updateById(existingItem);

        // 从向量数据库中删除物品描述
        removeFromVectorDatabase(itemId);

        return rows > 0;
    }

    /**
     * 将物品信息添加到向量数据库
     *
     * @param item 物品实体
     */
    private void addToVectorDatabase(Item item) {
        try {
            String itemDescription = item.getDescription();
            // 创建包含物品状态和标签的元数据对象
            ItemMetadata itemMetadata = ItemMetadata.builder()
                    .status(item.getStatus())
                    .tags(List.of()) // 暂时空标签列表，后续可扩展
                    .build();
            TextEmbeddingDTO textEmbeddingDTO = TextEmbeddingDTO.builder()
                    .id("item_" + item.getId())
                    .text(itemDescription)
                    .metadata(itemMetadata)
                    .build();
            vectorService.addTextToCollection(textEmbeddingDTO);
            log.info("物品信息已添加到向量数据库，ID：{}", item.getId());
        } catch (Exception e) {
            log.error("添加到向量数据库时发生异常，物品ID：{}", item.getId(), e);
            // 这里不抛出异常，因为向量数据库的失败不应影响主业务流程
        }
    }

    /**
     * 更新向量数据库中的物品信息
     *
     * @param item 更新后的物品实体
     */
    private void updateVectorDatabase(Item item) {
        try {
            String itemDescription = item.getDescription() != null ? item.getDescription() : "未提供描述";
            // 先删除旧的向量数据
            vectorService.deleteFromCollection("item_" + item.getId());

            // 创建包含物品状态和标签的元数据对象
            ItemMetadata itemMetadata = ItemMetadata.builder()
                    .status(item.getStatus())
                    .tags(List.of()) // 暂时空标签列表，后续可扩展
                    .build();
            TextEmbeddingDTO textEmbeddingDTO = TextEmbeddingDTO.builder()
                    .id("item_" + item.getId())
                    .text(itemDescription)
                    .metadata(itemMetadata)
                    .build();
            vectorService.addTextToCollection(textEmbeddingDTO);
            log.info("向量数据库中物品信息已更新，ID：{}", item.getId());
        } catch (Exception e) {
            log.error("更新向量数据库时发生异常，物品ID：{}", item.getId(), e);
            // 这里不抛出异常，因为向量数据库的失败不应影响主业务流程
        }
    }

    /**
     * 从向量数据库中删除物品信息
     *
     * @param itemId 物品ID
     */
    private void removeFromVectorDatabase(Long itemId) {
        try {
            vectorService.deleteFromCollection("item_" + itemId);
            log.info("向量数据库中物品信息已删除，ID：{}", itemId);
        } catch (Exception e) {
            log.error("删除向量数据库条目时发生异常，物品ID：{}", itemId, e);
            // 这里不抛出异常，因为向量数据库的失败不应影响主业务流程
        }
    }
}