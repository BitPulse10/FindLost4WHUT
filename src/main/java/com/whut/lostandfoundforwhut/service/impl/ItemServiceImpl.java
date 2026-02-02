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
import com.whut.lostandfoundforwhut.model.dto.ItemFilter;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import java.util.List;
import java.util.stream.Collectors;

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
        System.out.println("数据库影响行数：" + rowsAffected);
        System.out.println("物品创建成功：" + item);

        // 将物品描述添加到向量数据库
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
            System.out.println("物品信息已添加到向量数据库，ID：" + item.getId());
        } catch (Exception e) {
            System.out.println("添加到向量数据库时发生异常：" + e.getMessage());
        }

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
        try {
            String itemDescription = existingItem.getDescription() != null ? existingItem.getDescription() : "未提供描述";
            vectorService.deleteFromCollection("item_" + itemId);
            // 创建包含物品状态和标签的元数据对象
            ItemMetadata itemMetadata = ItemMetadata.builder()
                    .status(existingItem.getStatus())
                    .tags(List.of()) // 暂时空标签列表，后续可扩展
                    .build();
            TextEmbeddingDTO textEmbeddingDTO = TextEmbeddingDTO.builder()
                    .id("item_" + existingItem.getId())
                    .text(itemDescription)
                    .metadata(itemMetadata)
                    .build();
            vectorService.addTextToCollection(textEmbeddingDTO);
            System.out.println("向量数据库中物品信息已更新，ID：" + existingItem.getId());
        } catch (Exception e) {
            System.out.println("更新向量数据库时发生异常：" + e.getMessage());
        }

        return existingItem;
    }

    @Override
    public Item getItemById(Long itemId) {
        return itemMapper.selectById(itemId);
    }

    @Override
    public PageResultVO<Item> filterItems(ItemFilter ItemFilterDTO) {
        // 创建MyBatis-Plus分页对象
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Item> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                ItemFilterDTO.getPageNo(), ItemFilterDTO.getPageSize());

        LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<>();

        // 类型筛选
        if (ItemFilterDTO.getType() != null) {
            queryWrapper.eq(Item::getType, ItemFilterDTO.getType());
        }

        // 状态筛选
        if (ItemFilterDTO.getStatus() != null) {
            queryWrapper.eq(Item::getStatus, ItemFilterDTO.getStatus());
        }

        // 时间段筛选
        if (ItemFilterDTO.getStartTime() != null) {
            queryWrapper.ge(Item::getCreatedAt, ItemFilterDTO.getStartTime());
        }
        if (ItemFilterDTO.getEndTime() != null) {
            queryWrapper.le(Item::getCreatedAt, ItemFilterDTO.getEndTime());
        }

        // 标签筛选
        if (ItemFilterDTO.getTags() != null && !ItemFilterDTO.getTags().isEmpty()) {
            // 先查找匹配的标签ID
            List<Tag> tags = tagMapper.selectList(
                    new LambdaQueryWrapper<Tag>().in(Tag::getName, ItemFilterDTO.getTags()));

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
        try {
            vectorService.deleteFromCollection("item_" + itemId);
            System.out.println("向量数据库中物品信息已删除，ID：" + itemId);
        } catch (Exception e) {
            System.out.println("删除向量数据库条目时发生异常：" + e.getMessage());
        }

        return rows > 0;
    }

    /**
     * 通过向量搜索查找相似物品
     * 
     * @param query        查询文本
     * @param maxResults   最大返回结果数
     * @param statusFilter 状态筛选条件
     * @return 相似物品列表
     */
    public List<Item> searchSimilarItems(String query, int maxResults, Integer statusFilter) {
        // 使用向量数据库搜索相似物品ID
        List<String> similarItemIds = vectorService.searchInCollectionByStatus(query, maxResults, statusFilter);

        // 提取ID并转换为Long类型
        List<Long> itemIds = similarItemIds.stream()
                .filter(id -> id.startsWith("item_"))
                .map(id -> Long.parseLong(id.substring(5))) // 移除"item_"前缀
                .collect(Collectors.toList());

        if (itemIds.isEmpty()) {
            return List.of();
        }

        // 根据ID列表查询物品信息
        LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Item::getId, itemIds);

        // 如果有状态筛选，添加状态条件
        if (statusFilter != null) {
            queryWrapper.eq(Item::getStatus, statusFilter);
        }

        List<Item> items = itemMapper.selectList(queryWrapper);

        // 按照相似度排序（即按照返回的ID顺序）
        return items.stream()
                .sorted((i1, i2) -> Integer.compare(itemIds.indexOf(i1.getId()), itemIds.indexOf(i2.getId())))
                .collect(Collectors.toList());
    }
}