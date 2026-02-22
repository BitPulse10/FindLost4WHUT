package com.whut.lostandfoundforwhut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.enums.item.ItemStatus;
import com.whut.lostandfoundforwhut.common.enums.item.ItemType;
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
import com.whut.lostandfoundforwhut.model.entity.ImageSearch;
import com.whut.lostandfoundforwhut.model.entity.Item;
import com.whut.lostandfoundforwhut.model.entity.ItemTag;
import com.whut.lostandfoundforwhut.model.entity.Tag;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.model.vo.PageResultVO;
import com.whut.lostandfoundforwhut.service.IImageSearchService;
import com.whut.lostandfoundforwhut.service.IImageService;
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
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
    private static final String VECTOR_SYNC_RETRY_MESSAGE = "向量建库失败，请稍后重试；若仍失败请在个人中心重新编辑后重试";
    private static final String PRIVATE_TAG_NAMESPACE = "__sys_priv__:";
    private static final String PRIVATE_NO_PREFIX = "__sys_priv__:no:";
    private static final String PRIVATE_TAG_INPUT_PREFIX = "priv:";
    private static final String PRIVATE_NO_KEY = "no";
    private static final int PRIVATE_TAG_HASH_LENGTH = 16;
    private static final List<String> PRIVATE_NO_LEGACY_KEYS = List.of("card_no", "id_no", "student_no", "unique_no");

    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final TagMapper tagMapper;
    private final ItemTagMapper itemTagMapper;
    private final ItemImageMapper itemImageMapper;
    private final ImageMapper imageMapper;
    private final IImageService imageService;
    private final IImageSearchService imageSearchService;
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
        validateItemTypeRequired(itemDTO.getType());

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
        try {
            vectorService.addImagesToVectorDatabase(item, imageUrls);
        } catch (Exception e) {
            log.error("新增物品时向量建库失败，物品ID：{}", item.getId(), e);
            throw new AppException(ResponseCode.UN_ERROR.getCode(), VECTOR_SYNC_RETRY_MESSAGE);
        }

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
        List<Long> effectiveImageIds = (newImageIds != null) ? newImageIds : currentImageIds;

        boolean descriptionChanged = isDescriptionChanged(existingItem.getDescription(), newDescription);
        boolean imageChanged = detectImageChange(currentImageIds, newImageIds);
        boolean needVectorUpdate = descriptionChanged || imageChanged;
        boolean statusChangedToClosed = itemDTO.getStatus() != null
                && ItemStatus.CLOSED.getCode().equals(itemDTO.getStatus())
                && !ItemStatus.CLOSED.getCode().equals(existingItem.getStatus());

        // 更新物品基本信息字段
        updateItemFields(existingItem, itemDTO);
        updateItemTags(itemId, itemDTO.getTagText());

        // 结束帖子时，仅清理向量库和相似搜索缓存，保留图片与标签等关联数据
        if (statusChangedToClosed) {
            vectorService.removeFromVectorDatabase(existingItem.getId());
            clearSimilarSearchCache();
        } else {
            // 如果描述或图片发生变化，先更新图片关联
            if (needVectorUpdate) {
                handleImageUpdate(existingItem, currentImageIds, effectiveImageIds);
            }

            // 非关闭状态下，编辑后强制重建向量，避免历史入库失败的帖子无法被图搜命中
            try {
                if (effectiveImageIds != null && !effectiveImageIds.isEmpty()) {
                    List<String> imageUrls = imageMapper.selectUrlsByIds(effectiveImageIds);
                    vectorService.updateVectorDatabase(existingItem, imageUrls);
                    log.info("向量数据库已同步，物品ID：{}", existingItem.getId());
                } else {
                    vectorService.removeFromVectorDatabase(existingItem.getId());
                    log.info("无图片可用于向量重建，已移除向量，物品ID：{}", existingItem.getId());
                }
            } catch (Exception e) {
                log.error("更新物品时向量建库失败，物品ID：{}", existingItem.getId(), e);
                throw new AppException(ResponseCode.UN_ERROR.getCode(), VECTOR_SYNC_RETRY_MESSAGE);
            }

            // 清理相关的Redis缓存
            clearSimilarSearchCache();
        }

        // 更新数据库
        itemMapper.updateById(existingItem);
        log.info("物品更新成功，ID：{}", existingItem.getId());

        return existingItem;
    }

    private void updateItemTags(Long itemId, String tagText) {
        if (tagText == null) {
            return;
        }

        List<String> parsedTags = tagService.parseTagText(tagText);
        boolean hasPrivateTagsInInput = parsedTags.stream()
                .anyMatch(tag -> tag != null && tag.startsWith(PRIVATE_TAG_NAMESPACE));

        if (!hasPrivateTagsInInput) {
            List<String> existingPrivateTags = tagMapper.selectNamesByItemId(itemId).stream()
                    .filter(name -> name != null && name.startsWith(PRIVATE_TAG_NAMESPACE))
                    .toList();
            if (!existingPrivateTags.isEmpty()) {
                LinkedHashSet<String> merged = new LinkedHashSet<>(parsedTags);
                merged.addAll(existingPrivateTags);
                parsedTags = new ArrayList<>(merged);
            }
        }

        tagService.replaceTagsForItem(itemId, parsedTags);
    }

    /**
     * 更新物品的基本信息字段
     */
    private void updateItemFields(Item existingItem, ItemDTO itemDTO) {
        if (itemDTO.getType() != null) {
            validateItemTypeOptional(itemDTO.getType());
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
    private void handleImageUpdate(Item existingItem, List<Long> oldImageIds, List<Long> newImageIds) {
        Long itemId = existingItem.getId();
        List<Long> safeOldImageIds = oldImageIds == null ? new ArrayList<>() : oldImageIds;
        List<Long> safeNewImageIds = newImageIds == null ? new ArrayList<>() : newImageIds;

        Set<Long> newImageSet = new HashSet<>(safeNewImageIds);
        List<Long> removedImageIds = safeOldImageIds.stream()
                .filter(id -> !newImageSet.contains(id))
                .toList();

        // 删除旧的物品-图片关联
        itemImageMapper.deleteByItemId(itemId);
        log.info("已删除物品的旧图片关联，物品ID：{}", itemId);

        // 添加新的物品-图片关联
        if (!safeNewImageIds.isEmpty()) {
            boolean success = itemImageMapper.insertItemImages(itemId, safeNewImageIds);
            if (success) {
                log.info("已添加物品的新图片关联，物品ID：{}，图片ID列表：{}", itemId, safeNewImageIds);
            } else {
                log.warn("物品图片关联添加失败，物品ID：{}，图片ID列表：{}", itemId, safeNewImageIds);
            }
        }

        if (!removedImageIds.isEmpty()) {
            imageService.deleteImagesByIds(removedImageIds);
            log.info("已清理更新中移除的旧图片，物品ID：{}，图片ID列表：{}", itemId, removedImageIds);
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
        Page<Item> page = new Page<>(
                itemFilterDTO.getPageNo(), itemFilterDTO.getPageSize());

        LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<>();

        Integer type = itemFilterDTO.getType();
        if (type != null && type == ItemType.CARD.getCode() && itemFilterDTO.getSearchDTO() != null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "卡证模式不支持图搜");
        }

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

        Integer status = itemFilterDTO.getStatus();
        LocalDateTime startTime = itemFilterDTO.getStartTime();
        LocalDateTime endTime = itemFilterDTO.getEndTime();
        String keyword = itemFilterDTO.getKeyword();
        String eventPlace = itemFilterDTO.getEventPlace();
        List<String> eventPlaceKeywords = StringUtils.hasText(eventPlace)
                ? Arrays.stream(eventPlace.trim().split("\\s+"))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .toList()
                : List.of();
        // 类型筛选
        queryWrapper.eq(type != null, Item::getType, type);

        // 状态筛选
        queryWrapper.eq(status != null, Item::getStatus, status);

        // 时间段筛选
        queryWrapper.ge(startTime != null, Item::getCreatedAt, startTime);
        queryWrapper.le(endTime != null, Item::getCreatedAt, endTime);
        queryWrapper.and(StringUtils.hasText(keyword), wrapper -> wrapper
                .like(Item::getDescription, keyword)
                .or()
                .like(Item::getEventPlace, keyword));
        if (!eventPlaceKeywords.isEmpty()) {
            queryWrapper.and(wrapper -> {
                for (int i = 0; i < eventPlaceKeywords.size(); i++) {
                    wrapper.like(Item::getEventPlace, eventPlaceKeywords.get(i));
                    if (i < eventPlaceKeywords.size() - 1) {
                        wrapper.or();
                    }
                }
            });
        }

        // 标签筛选
        if (itemFilterDTO.getTags() != null && !itemFilterDTO.getTags().isEmpty()) {
            List<String> requestedTags = expandPrivateNoAliases(itemFilterDTO.getTags());
            List<Tag> tags = tagMapper.selectList(
                    new LambdaQueryWrapper<Tag>().in(Tag::getName, requestedTags));

            if (!tags.isEmpty()) {
                List<Long> tagIds = tags.stream()
                        .map(Tag::getId)
                        .collect(Collectors.toList());

                List<Long> itemIds = itemTagMapper.selectList(
                        new LambdaQueryWrapper<ItemTag>().in(ItemTag::getTagId, tagIds))
                        .stream()
                        .collect(Collectors.collectingAndThen(Collectors.toList(), itemTags -> {
                            if (Boolean.TRUE.equals(itemFilterDTO.getPreciseTagMatch())) {
                                Map<Long, Set<Long>> itemToTagSet = new HashMap<>();
                                for (ItemTag itemTag : itemTags) {
                                    itemToTagSet.computeIfAbsent(itemTag.getItemId(), key -> new HashSet<>())
                                            .add(itemTag.getTagId());
                                }
                                return itemToTagSet.entrySet().stream()
                                        .filter(entry -> entry.getValue().containsAll(tagIds))
                                        .map(Map.Entry::getKey)
                                        .collect(Collectors.toList());
                            }
                            return itemTags.stream()
                                    .map(ItemTag::getItemId)
                                    .distinct()
                                    .collect(Collectors.toList());
                        }));

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
                if (mapping.getName() != null && mapping.getName().startsWith(PRIVATE_TAG_NAMESPACE)) {
                    continue;
                }
                tagMap.computeIfAbsent(mapping.getItemId(), key -> new ArrayList<>())
                        .add(mapping.getName());
            }

            for (Item item : records) {
                item.setTags(tagMap.getOrDefault(item.getId(), new ArrayList<>()));
            }
        }

        return PageUtils.toPageResult(page);
    }

    private void validateItemTypeRequired(Integer type) {
        if (type == null || !ItemType.isValid(type)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "物品类型非法，仅支持 0-挂失，1-招领，2-卡证");
        }
    }

    private void validateItemTypeOptional(Integer type) {
        if (type != null && !ItemType.isValid(type)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "物品类型非法，仅支持 0-挂失，1-招领，2-卡证");
        }
    }

    private List<String> expandPrivateNoAliases(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        for (String tag : tags) {
            if (!StringUtils.hasText(tag)) {
                continue;
            }
            String normalized = tag.trim();
            if (normalized.startsWith(PRIVATE_TAG_INPUT_PREFIX)) {
                String transformed = normalizePrivateTagForFilter(normalized);
                if (transformed != null) {
                    expanded.add(transformed);
                    normalized = transformed;
                } else {
                    continue;
                }
            } else {
                expanded.add(normalized);
            }

            if (!normalized.startsWith(PRIVATE_NO_PREFIX)) {
                continue;
            }
            String hash = normalized.substring(PRIVATE_NO_PREFIX.length());
            if (hash.isEmpty()) {
                continue;
            }
            for (String key : PRIVATE_NO_LEGACY_KEYS) {
                expanded.add(PRIVATE_TAG_NAMESPACE + key + ":" + hash);
            }
        }
        return new ArrayList<>(expanded);
    }

    private String normalizePrivateTagForFilter(String rawTag) {
        String normalized = rawTag.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith(PRIVATE_TAG_INPUT_PREFIX)) {
            return null;
        }

        String body = normalized.substring(PRIVATE_TAG_INPUT_PREFIX.length()).trim();
        if (!StringUtils.hasText(body)) {
            return null;
        }

        String[] keyValue;
        if (body.contains("=")) {
            keyValue = body.split("=", 2);
        } else {
            keyValue = body.split(":", 2);
        }
        if (keyValue.length != 2) {
            return null;
        }

        String key = keyValue[0].replaceAll("[^a-z0-9_]", "").trim();
        String value = keyValue[1].trim().toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(key) || !StringUtils.hasText(value)) {
            return null;
        }

        if ("card_no".equals(key) || "id_no".equals(key) || "student_no".equals(key) || "unique_no".equals(key)) {
            key = PRIVATE_NO_KEY;
        }

        String digest = sha256(value);
        if (digest.length() > PRIVATE_TAG_HASH_LENGTH) {
            digest = digest.substring(0, PRIVATE_TAG_HASH_LENGTH);
        }
        return PRIVATE_TAG_NAMESPACE + key + ":" + digest;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "私密标签摘要失败");
        }
    }

    @Override
    @Transactional
    public boolean takeDownItem(Long itemId, Long userId) {
        // 查询物品是否存在且属于当前用户
        Item existingItem = itemMapper.selectById(itemId);
        if (existingItem == null) {
            throw new AppException(ResponseCode.ITEM_NOT_FOUND.getCode(), ResponseCode.ITEM_NOT_FOUND.getInfo());
        }
        if (!existingItem.getUserId().equals(userId)) {
            throw new AppException(ResponseCode.NO_PERMISSION.getCode(), ResponseCode.NO_PERMISSION.getInfo());
        }

        // 先获取关联图片ID，后续用于清理图片关系和图片记录
        List<Long> imageIds = itemImageMapper.getImageIdsByItemId(itemId);

        // 逻辑删除
        existingItem.setIsDeleted(1);
        int rows = itemMapper.updateById(existingItem);
        if (rows <= 0) {
            return false;
        }

        // 清理标签关联（同时清理标签缓存）
        tagService.replaceTagsForItem(itemId, new ArrayList<>());

        // 清理物品-图片关联
        itemImageMapper.deleteByItemId(itemId);

        // 清理图片表记录和存储文件
        if (imageIds != null && !imageIds.isEmpty()) {
            imageService.deleteImagesByIds(imageIds);
        }

        // 从向量数据库中删除物品描述
        vectorService.removeFromVectorDatabase(itemId);
        // 清理相似搜索缓存，避免继续命中已删除物品
        clearSimilarSearchCache();

        return true;
    }

    @Override
    public PageResultVO<Item> listMyItems(Long userId, Integer pageNo, Integer pageSize, Integer type, String keyword) {
        if (userId == null) {
            throw new AppException(ResponseCode.NOT_LOGIN.getCode(), ResponseCode.NOT_LOGIN.getInfo());
        }

        int normalizedPageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        int normalizedPageSize = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 100);

        Page<Item> page = new Page<>(normalizedPageNo, normalizedPageSize);
        LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Item::getUserId, userId);
        queryWrapper.eq(type != null, Item::getType, type);
        queryWrapper.and(StringUtils.hasText(keyword), wrapper -> wrapper
                .like(Item::getDescription, keyword)
                .or()
                .like(Item::getEventPlace, keyword));
        queryWrapper.orderByDesc(Item::getCreatedAt);

        itemMapper.selectPage(page, queryWrapper);

        List<Item> records = page.getRecords();
        if (records != null && !records.isEmpty()) {
            List<Long> itemIds = records.stream().map(Item::getId).distinct().toList();
            List<ItemTagNameDTO> mappings = tagMapper.selectNamesByItemIds(itemIds);
            Map<Long, List<String>> tagMap = new HashMap<>();
            for (ItemTagNameDTO mapping : mappings) {
                if (mapping.getName() != null && mapping.getName().startsWith(PRIVATE_TAG_NAMESPACE)) {
                    continue;
                }
                tagMap.computeIfAbsent(mapping.getItemId(), key -> new ArrayList<>()).add(mapping.getName());
            }
            for (Item item : records) {
                item.setTags(tagMap.getOrDefault(item.getId(), new ArrayList<>()));
            }
        }

        return PageUtils.toPageResult(page);
    }

    @Override
    public PageResultVO<String> listEventPlaces(String keyword, Integer pageNo, Integer pageSize) {
        int normalizedPageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        int normalizedPageSize = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 100);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Item::getEventPlace)
                .isNotNull(Item::getEventPlace)
                .ne(Item::getEventPlace, "")
                .like(StringUtils.hasText(normalizedKeyword), Item::getEventPlace, normalizedKeyword)
                .groupBy(Item::getEventPlace)
                .orderByAsc(Item::getEventPlace);

        List<String> allPlaces = itemMapper.selectObjs(queryWrapper).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        int fromIndex = Math.min((normalizedPageNo - 1) * normalizedPageSize, allPlaces.size());
        int toIndex = Math.min(fromIndex + normalizedPageSize, allPlaces.size());

        PageResultVO<String> result = new PageResultVO<>();
        result.setPageNo(normalizedPageNo);
        result.setPageSize(normalizedPageSize);
        result.setTotal(allPlaces.size());
        result.setRecords(allPlaces.subList(fromIndex, toIndex));
        return result;
    }

    @Override
    public List<Item> searchSimilarItems(String query, List<Long> imageIds, int maxResults) {
        try {
            int normalizedMaxResults = Math.max(1, Math.min(maxResults, 10));
            // 创建 DTO
            SearchDTO searchDTO = new SearchDTO(query, imageIds, normalizedMaxResults);
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
                    List<ImageSearch> imageList = imageSearchService.listByIds(imageIds);
                    imageUrls = imageList.stream()
                            .map(ImageSearch::getUrl)
                            .collect(Collectors.toList());
                }

                // 使用向量数据库搜索相似的物品ID
                List<String> similarItemIds = vectorService.searchInCollection(query, imageUrls, normalizedMaxResults);
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
     * 清理相似搜索缓存
     * 
     *
     */
    private void clearSimilarSearchCache() {
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
                log.info("已清理{}个相似搜索缓存", deletedCount);
            } else {
                log.info("未找到需要清理的相似搜索缓存");
            }

        } catch (Exception e) {
            log.warn("清理相似搜索缓存时出现异常", e);
        }
    }
}
