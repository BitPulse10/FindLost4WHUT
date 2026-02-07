package com.whut.lostandfoundforwhut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.enums.item.ItemStatus;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.mapper.ItemImageMapper;
import com.whut.lostandfoundforwhut.mapper.ItemMapper;
import com.whut.lostandfoundforwhut.mapper.ItemTagMapper;
import com.whut.lostandfoundforwhut.mapper.TagMapper;
import com.whut.lostandfoundforwhut.mapper.UserMapper;
import com.whut.lostandfoundforwhut.model.dto.ItemDTO;
import com.whut.lostandfoundforwhut.model.dto.ItemFilterDTO;
import com.whut.lostandfoundforwhut.model.dto.ItemTagNameDTO;
import com.whut.lostandfoundforwhut.model.entity.Item;
import com.whut.lostandfoundforwhut.model.entity.ItemTag;
import com.whut.lostandfoundforwhut.model.entity.Tag;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.model.vo.PageResultVO;
import com.whut.lostandfoundforwhut.service.IImageService;
import com.whut.lostandfoundforwhut.service.IItemService;
import com.whut.lostandfoundforwhut.service.ITagService;
import com.whut.lostandfoundforwhut.service.IVectorService;
import com.whut.lostandfoundforwhut.common.utils.page.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.whut.lostandfoundforwhut.mapper.ImageMapper;
import com.whut.lostandfoundforwhut.model.entity.Image;

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
    private final ItemImageMapper itemImageMapper;
    private final IImageService imageService;
    private final ImageMapper imageMapper;
    private final ITagService tagService;
    private final IVectorService vectorService;

    @Override
    @Transactional
    public Item addItem(ItemDTO itemDTO, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }

        // 创建物品
        Item item = Item.builder()
                .userId(userId)
                .type(itemDTO.getType())
                .eventTime(itemDTO.getEventTime())
                .eventPlace(itemDTO.getEventPlace())
                .status(ItemStatus.ACTIVE.getCode())
                .description(itemDTO.getDescription())
                .build();

        // 物品保存到数据库
        itemMapper.insert(item);
        log.info("物品创建成功：{}", item.getId());

        // 将物品和图片添加到关联表中
        // List<String> imageUrls;
        // if (itemDTO.getImageIds() != null && !itemDTO.getImageIds().isEmpty()) {
        // boolean imageAssociationSuccess =
        // itemImageMapper.insertItemImages(item.getId(), itemDTO.getImageIds());
        // if (imageAssociationSuccess) {
        // log.info("物品图片关联成功，物品ID：{}，图片数量：{}", item.getId(),
        // itemDTO.getImageIds().size());
        // } else {
        // log.warn("物品图片关联失败，物品ID：{}", item.getId());
        // }

        // 获取图片URL
        // List<Image> images = imageMapper.selectList(
        // new LambdaQueryWrapper<Image>().in(Image::getId, itemDTO.getImageIds()));
        // imageUrls = images.stream()
        // .map(Image::getUrl)
        // .filter(Objects::nonNull)
        // .collect(Collectors.toList());
        // } else {
        // imageUrls = new ArrayList<>();
        // }
        // System.out.println("imageUrls: " + imageUrls);

        // 获取图片的URL
        System.out.println("itemDTO.getImageId(): " + itemDTO.getImageId());
        String imageUrl = imageMapper.selectById(itemDTO.getImageId()).getUrl();

        System.out.println("imageUrl: " + imageUrl);
        // 将物品描述和图片添加到向量数据库
        // vectorService.addToVectorDatabase(item);
        vectorService.addImagesToVectorDatabase(item, imageUrl);

        // 解析并绑定标签
        List<String> tagNames = tagService.parseTagText(itemDTO.getTagText());
        tagService.replaceTagsForItem(item.getId(), tagNames);
        item.setTags(tagService.getTagNamesByItemId(item.getId()));

        return item;
    }

    @Override
    @Transactional
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

        // 处理图片关联
        String updateImageIdStr = itemDTO.getImageId();
        Long updateImageId = null;
        if (updateImageIdStr != null && !updateImageIdStr.isEmpty()) {
            try {
                updateImageId = Long.parseLong(updateImageIdStr);
            } catch (NumberFormatException e) {
                log.warn("图片ID格式错误: {}", updateImageIdStr);
            }
        }

        List<Long> oldImageIds = Optional.ofNullable(itemImageMapper.getImageIdsByItemId(itemId))
                .orElse(new ArrayList<>()); // 获取旧图片实体列表
        List<Long> deleteImageIds = new ArrayList<>();
        List<Long> addImageIds = new ArrayList<>();

        // 如果有旧图片但新图片ID不同，则删除旧图片
        if (!oldImageIds.isEmpty() && (updateImageId == null || !oldImageIds.contains(updateImageId))) {
            deleteImageIds.addAll(oldImageIds);
        }

        // 如果有新图片且与旧图片不同，则添加新图片
        if (updateImageId != null && (oldImageIds.isEmpty() || !oldImageIds.contains(updateImageId))) {
            addImageIds.add(updateImageId);
        }
        // 删除旧关联
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            int rowsAffected = itemImageMapper.deleteItemImages(itemId, deleteImageIds);
            log.info("物品图片关联删除，物品ID：{}，删除图片数量：{}，数据库影响行数：{}", existingItem.getId(), deleteImageIds.size(),
                    rowsAffected);
        }
        // 添加新关联
        if (addImageIds != null && !addImageIds.isEmpty()) {
            boolean success = itemImageMapper.insertItemImages(existingItem.getId(), addImageIds);
            if (success) {
                log.info("物品图片关联成功，物品ID：{}，图片ID：{}", existingItem.getId(), updateImageId);
            } else {
                log.warn("物品图片关联失败，物品ID：{}", existingItem.getId());
            }
        }

        // 更新向量数据库中的物品描述
        vectorService.updateVectorDatabase(existingItem);
        // 仅在传入 tagText 时更新标签
        if (itemDTO.getTagText() != null) {
            List<String> tagNames = tagService.parseTagText(itemDTO.getTagText());
            tagService.replaceTagsForItem(itemId, tagNames);
        }
        existingItem.setTags(tagService.getTagNamesByItemId(itemId));

        // 最后删除旧图片实体
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            imageService.deleteImagesAndFiles(deleteImageIds);
        }

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

        item.setTags(tagService.getTagNamesByItemId(itemId));
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
        List<Item> records = page.getRecords();
        if (records != null && !records.isEmpty()) {
            List<Long> itemIds = records.stream()
                    .map(Item::getId)
                    .distinct()
                    .toList();
            List<ItemTagNameDTO> mappings = tagMapper.selectNamesByItemIds(itemIds);
            Map<Long, List<String>> tagMap = new HashMap<>();
            for (ItemTagNameDTO mapping : mappings) {
                tagMap.computeIfAbsent(mapping.getItemId(), key -> new ArrayList<>())
                        .add(mapping.getName());
            }
            for (Item item : records) {
                item.setTags(tagMap.getOrDefault(item.getId(), new ArrayList<>()));
            }
        }
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
        vectorService.removeFromVectorDatabase(itemId);

        return rows > 0;
    }

    @Override
    public List<Item> filterItemsByStatus(List<Long> itemIds, String status) {
        if (itemIds == null || itemIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建查询条件，只查询指定ID列表中符合状态的物品
        LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Item::getId, itemIds)
                .eq(Item::getStatus, status);

        // 执行查询
        List<Item> filteredItems = itemMapper.selectList(queryWrapper);

        log.info("根据状态筛选物品完成，输入ID数量：{}，筛选结果数量：{}，状态：{}",
                itemIds.size(), filteredItems.size(), status);

        return filteredItems;
    }

    @Override
    public List<Item> searchSimilarItems(String query, int maxResults) {
        try {
            // 使用向量数据库搜索相似的物品ID
            List<String> similarItemIds = vectorService.searchInCollection(query, maxResults);

            // 将向量数据库返回的ID转换为Long类型的物品ID
            List<Long> itemIds = similarItemIds.stream()
                    .filter(id -> id.startsWith("item_")) // 确保是物品ID格式
                    .map(id -> Long.parseLong(id.substring(5))) // 移除 "item_" 前缀并转换为Long
                    .collect(Collectors.toList());

            if (itemIds.isEmpty()) {
                return new ArrayList<>();
            }

            // 根据ID列表查询物品信息
            LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Item::getId, itemIds)
                    .orderByDesc(Item::getCreatedAt); // 按创建时间倒序排列

            List<Item> items = itemMapper.selectList(queryWrapper);

            log.info("搜索相似物品完成，查询：{}，返回结果数量：{}", query, items.size());

            return items;
        } catch (Exception e) {
            log.error("搜索相似物品失败，查询：{}", query, e);
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "搜索相似物品失败：" + e.getMessage());
        }
    }
}
