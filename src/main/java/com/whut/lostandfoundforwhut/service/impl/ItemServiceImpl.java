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
import com.whut.lostandfoundforwhut.model.dto.SearchDTO;
import com.whut.lostandfoundforwhut.model.entity.Item;
import com.whut.lostandfoundforwhut.model.entity.ItemTag;
import com.whut.lostandfoundforwhut.model.entity.Tag;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.model.vo.PageResultVO;
import com.whut.lostandfoundforwhut.service.IItemService;
import com.whut.lostandfoundforwhut.service.ITagService;
import com.whut.lostandfoundforwhut.service.IVectorService;
import com.whut.lostandfoundforwhut.common.utils.page.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.whut.lostandfoundforwhut.mapper.ImageMapper;

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
    private final ImageMapper imageMapper;
    private final ITagService tagService;
    private final IVectorService vectorService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

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
        save(item);
        log.info("物品添加数据库成功：{}", item.getId());

        // 将物品和图片添加到关联表中
        List<Long> imageIds = itemDTO.getImageIds();
        boolean success = itemImageMapper.insertItemImages(item.getId(), imageIds);
        if (!success) {
            log.warn("物品图片关联失败，物品ID：{}", item.getId());
        }

        // 获取图片的URL
        List<String> imageUrls = imageMapper.selectUrlsByIds(imageIds);

        // 将物品描述和图片添加到向量数据库
        vectorService.addImagesToVectorDatabase(item, imageUrls);

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

        // 获取当前物品的图片ID列表用于比较
        List<Long> currentImageIds = itemImageMapper.getImageIdsByItemId(itemId);
        // 检查是否有变化需要触发向量库更新
        String newDescription = itemDTO.getDescription();
        List<Long> newImageIds = itemDTO.getImageIds();

        boolean descriptionChanged = isDescriptionChanged(existingItem.getDescription(), newDescription);
        boolean imageChanged = detectImageChange(currentImageIds, newImageIds);
        boolean needVectorUpdate = descriptionChanged || imageChanged;

        // 更新物品基本信息字段
        updateItemFields(existingItem, itemDTO);

        // 如果描述或图片发生变化，处理图片关联和向量库更新
        if (needVectorUpdate) {
            handleImageUpdate(existingItem, newImageIds);

            // 更新向量数据库
            if (newImageIds != null && !newImageIds.isEmpty()) {
                List<String> imageUrls = imageMapper.selectUrlsByIds(newImageIds);
                vectorService.updateVectorDatabase(existingItem, imageUrls);
                log.info("向量数据库已更新，物品ID：{}", existingItem.getId());
            }

            // 清理相关的Redis缓存
            clearSimilarSearchCache(existingItem.getId());
        }

        // 更新数据库
        itemMapper.updateById(existingItem);
        log.info("物品更新成功，ID：{}", existingItem.getId());

        return existingItem;
    }

    /**
     * 更新物品的基本信息字段
     */
    private void updateItemFields(Item existingItem, ItemDTO itemDTO) {
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
    }

    /**
     * 检查描述是否发生变化
     */
    private boolean isDescriptionChanged(String oldDescription, String newDescription) {
        if (oldDescription == null) {
            return newDescription != null;
        }
        return !oldDescription.equals(newDescription);
    }

    /**
     * 检测图片是否发生变化
     */
    private boolean detectImageChange(List<Long> oldImageIds, List<Long> newImageIds) {
        // 如果新图片ID列表为null，但原图片ID列表不为空，则视为有变化
        if (newImageIds == null) {
            return oldImageIds != null && !oldImageIds.isEmpty();
        }
        // 如果原图片ID列表为null，但新图片ID列表不为空，则视为有变化
        if (oldImageIds == null) {
            return !newImageIds.isEmpty();
        }

        // 比较两个列表是否相同（不考虑顺序）
        // 首先比较大小，不同则直接返回true
        if (oldImageIds.size() != newImageIds.size()) {
            return true;
        }

        // 使用HashSet提高查找效率
        Set<Long> oldImageSet = new HashSet<>(oldImageIds);
        for (Long newId : newImageIds) {
            if (!oldImageSet.contains(newId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 处理图片更新：删除旧关联，添加新关联
     */
    private void handleImageUpdate(Item existingItem, List<Long> newImageIds) {
        Long itemId = existingItem.getId();

        // 删除旧的物品-图片关联
        itemImageMapper.deleteByItemId(itemId);
        log.info("已删除物品的旧图片关联，物品ID：{}", itemId);

        // 添加新的物品-图片关联
        if (newImageIds != null && !newImageIds.isEmpty()) {
            boolean success = itemImageMapper.insertItemImages(itemId, newImageIds);
            if (success) {
                log.info("已添加物品的新图片关联，物品ID：{}，图片ID列表：{}", itemId, newImageIds);
            } else {
                log.warn("物品图片关联添加失败，物品ID：{}，图片ID列表：{}", itemId, newImageIds);
            }
        }
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
        if (itemFilterDTO.getPageSize() > 100) {
            itemFilterDTO.setPageSize(100);
        }

        // 创建MyBatis-Plus分页对象
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Item> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                itemFilterDTO.getPageNo(), itemFilterDTO.getPageSize());

        LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<>();

        // 从 Redis 获取相似搜索的物品ID列表
        if (itemFilterDTO.getSearchDTO() != null) {
            String redisKey = itemFilterDTO.getSearchDTO().toRedisKey();
            String cachedIds = redisTemplate.opsForValue().get(redisKey);
            List<Long> similarItemIds = new ArrayList<>();

            if (cachedIds != null && !cachedIds.isEmpty()) {
                similarItemIds = parseItemIds(cachedIds);
                if (!similarItemIds.isEmpty()) {
                    log.info("使用Redis缓存的相似搜索结果进行过滤，物品数量：{}", similarItemIds.size());
                }
            } else {
                // 如果 Redis 中没有缓存，执行相似物品查询
                log.info("Redis中未找到相似搜索缓存，执行相似物品查询，键：{}", redisKey);

                // 执行相似物品搜索
                List<Item> similarItems = searchSimilarItems(
                        itemFilterDTO.getSearchDTO().getQuery(),
                        itemFilterDTO.getSearchDTO().getImageIds(),
                        itemFilterDTO.getSearchDTO().getMaxResults());

                // 提取物品ID
                similarItemIds = similarItems.stream()
                        .map(Item::getId)
                        .collect(Collectors.toList());

                log.info("执行相似物品查询完成，获取到{}个相似物品", similarItemIds.size());

                // 将结果缓存到Redis
                if (!similarItemIds.isEmpty()) {
                    String idsJson = similarItemIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","));
                    redisTemplate.opsForValue().set(redisKey, idsJson, 1, TimeUnit.HOURS);
                    log.info("相似物品ID已缓存到Redis，键：{}，数量：{}", redisKey, similarItemIds.size());
                }
            }

            // 应用相似物品ID过滤条件
            if (!similarItemIds.isEmpty()) {
                queryWrapper.in(Item::getId, similarItemIds);
            } else {
                // 如果没有相似物品，返回空结果
                queryWrapper.eq(Item::getId, -1L);
            }
        }

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
            List<Tag> tags = tagMapper.selectList(
                    new LambdaQueryWrapper<Tag>().in(Tag::getName, itemFilterDTO.getTags()));

            if (!tags.isEmpty()) {
                List<Long> tagIds = tags.stream()
                        .map(Tag::getId)
                        .collect(Collectors.toList());

                List<Long> itemIds = itemTagMapper.selectList(
                        new LambdaQueryWrapper<ItemTag>().in(ItemTag::getTagId, tagIds))
                        .stream()
                        .map(ItemTag::getItemId)
                        .distinct()
                        .collect(Collectors.toList());

                if (!itemIds.isEmpty()) {
                    queryWrapper.in(Item::getId, itemIds);
                } else {
                    queryWrapper.eq(Item::getId, -1L);
                }
            } else {
                queryWrapper.eq(Item::getId, -1L);
            }
        }

        // 按创建时间倒序排列
        queryWrapper.orderByDesc(Item::getCreatedAt);

        // 执行查询
        itemMapper.selectPage(page, queryWrapper);

        // 封装分页结果（填充标签）
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
    public List<Item> searchSimilarItems(String query, List<Long> imageIds, int maxResults) {
        try {
            // 创建 DTO
            SearchDTO searchDTO = new SearchDTO(query, imageIds, maxResults);
            String redisKey = searchDTO.toRedisKey();

            // 尝试从 Redis 获取缓存
            String cachedIds = redisTemplate.opsForValue().get(redisKey);
            List<Long> itemIds;

            if (cachedIds != null) {
                // 缓存命中
                log.info("从 Redis 缓存获取相似物品ID，键：{}", redisKey);
                itemIds = parseItemIds(cachedIds);
            } else {
                // 缓存未命中，执行向量搜索
                log.info("Redis 缓存未命中，执行向量搜索，键：{}", redisKey);

                // 处理查询文本为空的情况
                if (query == null) {
                    query = "";
                }

                // 获取图片Url列表
                List<String> imageUrls = new ArrayList<>();
                if (imageIds != null && !imageIds.isEmpty()) {
                    imageUrls = imageMapper.selectUrlsByIds(imageIds);
                }

                // 使用向量数据库搜索相似的物品ID
                List<String> similarItemIds = vectorService.searchInCollection(query, imageUrls, maxResults);
                log.info("向量服务返回的ID列表: {}", similarItemIds);
                log.info("向量服务返回ID数量: {}", similarItemIds.size());

                // 将向量数据库返回的ID转换为Long类型的物品ID
                itemIds = similarItemIds.stream()
                        .filter(id -> id.startsWith("item_"))
                        .map(id -> Long.parseLong(id.substring(5)))
                        .collect(Collectors.toList());

                // 存入 Redis，设置过期时间（例如 1 小时）
                if (!itemIds.isEmpty()) {
                    String idsJson = itemIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","));
                    redisTemplate.opsForValue().set(redisKey, idsJson, 1, TimeUnit.HOURS);
                    log.info("相似物品ID已缓存到 Redis，键：{}，数量：{}", redisKey, itemIds.size());
                }
            }

            if (itemIds.isEmpty()) {
                return new ArrayList<>();
            }

            // 根据ID列表查询物品信息
            LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Item::getId, itemIds)
                    .orderByDesc(Item::getCreatedAt);

            List<Item> items = itemMapper.selectList(queryWrapper);
            log.info("搜索相似物品完成，查询：{}，返回结果数量：{}", query, items.size());
            return items;
        } catch (Exception e) {
            log.error("搜索相似物品失败，查询：{}", query, e);
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "搜索相似物品失败：" + e.getMessage());
        }
    }

    /**
     * 解析 Redis 中存储的 ID 字符串
     */
    private List<Long> parseItemIds(String idsStr) {
        if (idsStr == null || idsStr.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(idsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    /**
     * 清理与指定物品相关的相似搜索缓存
     * 
     * @param itemId 物品ID
     */
    private void clearSimilarSearchCache(Long itemId) {
        try {
            Set<String> keysToDelete = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions()
                    .match("similar:search:*")
                    .count(100)
                    .build();

            // 使用SCAN命令遍历匹配的键
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keysToDelete.add(cursor.next());
                }
            }

            if (!keysToDelete.isEmpty()) {
                Long deletedCount = redisTemplate.delete(keysToDelete);
                log.info("已清理{}个相似搜索缓存，涉及物品ID：{}", deletedCount, itemId);
            } else {
                log.info("未找到需要清理的相似搜索缓存，物品ID：{}", itemId);
            }

        } catch (Exception e) {
            log.warn("清理相似搜索缓存时出现异常，物品ID：{}", itemId, e);
        }
    }
}
