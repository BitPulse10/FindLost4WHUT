package com.whut.lostandfoundforwhut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.lostandfoundforwhut.common.constant.Constants;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.common.utils.page.PageUtils;
import com.whut.lostandfoundforwhut.mapper.ItemTagMapper;
import com.whut.lostandfoundforwhut.mapper.TagMapper;
import com.whut.lostandfoundforwhut.model.entity.ItemTag;
import com.whut.lostandfoundforwhut.model.entity.Tag;
import com.whut.lostandfoundforwhut.model.vo.PageResultVO;
import com.whut.lostandfoundforwhut.service.IRedisService;
import com.whut.lostandfoundforwhut.service.ITagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Codex
 * @date 2026/02/04
 * @description 标签服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements ITagService {

    private static final int MAX_TAG_LENGTH = 20;
    private static final int MAX_PRIVATE_KEY_LENGTH = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int PRIVATE_TAG_HASH_LENGTH = 16;
    private static final String PRIVATE_TAG_INPUT_PREFIX = "priv:";
    private static final String PRIVATE_TAG_NAMESPACE = "__sys_priv__:";
    private static final String PRIVATE_NO_KEY = "no";
    private static final Duration ITEM_TAGS_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration TAG_LIST_CACHE_TTL = Duration.ofMinutes(5);

    private final TagMapper tagMapper;
    private final ItemTagMapper itemTagMapper;
    private final IRedisService redisService;

    @Override
    public PageResultVO<Tag> listTags(String keyword, Integer pageNo, Integer pageSize) {
        int resolvedPageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int resolvedPageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        if (resolvedPageSize > MAX_PAGE_SIZE) {
            resolvedPageSize = MAX_PAGE_SIZE;
        }

        String cacheKey = buildTagListKey(keyword, resolvedPageNo, resolvedPageSize);
        Object cachedResult = redisService.getValue(cacheKey);
        if (cachedResult instanceof PageResultVO<?> cachedPage) {
            @SuppressWarnings("unchecked")
            PageResultVO<Tag> pageResult = (PageResultVO<Tag>) cachedPage;
            return pageResult;
        }

        Page<Tag> page = new Page<>(resolvedPageNo, resolvedPageSize);
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.notLikeRight(Tag::getName, PRIVATE_TAG_NAMESPACE);
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(Tag::getName, keyword.trim());
        }
        queryWrapper.orderByAsc(Tag::getId);
        tagMapper.selectPage(page, queryWrapper);

        PageResultVO<Tag> result = PageUtils.toPageResult(page);
        redisService.setValue(cacheKey, result, TAG_LIST_CACHE_TTL);
        return result;
    }

    @Override
    public List<String> parseTagText(String tagText) {
        if (!StringUtils.hasText(tagText)) {
            return new ArrayList<>();
        }

        String[] parts = tagText.split("#");
        Set<String> normalized = new LinkedHashSet<>();
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            String value = part.trim();
            if (value.isEmpty()) {
                continue;
            }
            String privateTag = normalizePrivateTag(value);
            if (privateTag != null) {
                normalized.add(privateTag);
                continue;
            }
            if (value.length() > MAX_TAG_LENGTH) {
                continue;
            }
            normalized.add(value.toLowerCase(Locale.ROOT));
        }
        return new ArrayList<>(normalized);
    }

    @Override
    public List<Tag> getOrCreateTags(List<String> names) {
        if (names == null || names.isEmpty()) {
            return new ArrayList<>();
        }

        List<Tag> existing = tagMapper.selectList(new LambdaQueryWrapper<Tag>().in(Tag::getName, names));
        Set<String> existingNames = existing.stream().map(Tag::getName).collect(Collectors.toSet());
        boolean hasNewTag = false;

        for (String name : names) {
            if (existingNames.contains(name)) {
                continue;
            }
            Tag tag = new Tag();
            tag.setName(name);
            try {
                tagMapper.insert(tag);
                hasNewTag = true;
            } catch (Exception e) {
                log.warn("创建标签失败，可能已存在，name={}", name, e);
            }
        }

        if (hasNewTag) {
            evictTagListCache();
        }
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>().in(Tag::getName, names));
    }

    @Override
    public void replaceTagsForItem(Long itemId, List<String> names) {
        if (itemId == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "物品ID不能为空");
        }

        itemTagMapper.deleteByItemId(itemId);
        if (names != null && !names.isEmpty()) {
            List<Tag> tags = getOrCreateTags(names);
            if (!tags.isEmpty()) {
                List<ItemTag> relations = tags.stream().map(tag -> {
                    ItemTag itemTag = new ItemTag();
                    itemTag.setItemId(itemId);
                    itemTag.setTagId(tag.getId());
                    return itemTag;
                }).toList();
                itemTagMapper.insertBatch(relations);
            }
        }

        redisService.remove(buildItemTagsKey(itemId));
        evictTagListCache();
    }

    @Override
    public List<String> getTagNamesByItemId(Long itemId) {
        if (itemId == null) {
            return new ArrayList<>();
        }

        String cacheKey = buildItemTagsKey(itemId);
        Object cachedTags = redisService.getValue(cacheKey);
        if (cachedTags instanceof List<?> cachedList) {
            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) cachedList;
            return filterVisibleTags(result);
        }

        List<String> tagNames = filterVisibleTags(tagMapper.selectNamesByItemId(itemId));
        redisService.setValue(cacheKey, tagNames, ITEM_TAGS_CACHE_TTL);
        return tagNames;
    }

    private String normalizePrivateTag(String rawTag) {
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
        if (key.length() > MAX_PRIVATE_KEY_LENGTH) {
            key = key.substring(0, MAX_PRIVATE_KEY_LENGTH);
        }
        String digest = sha256(value);
        if (digest.length() > PRIVATE_TAG_HASH_LENGTH) {
            digest = digest.substring(0, PRIVATE_TAG_HASH_LENGTH);
        }
        return PRIVATE_TAG_NAMESPACE + key + ":" + digest;
    }

    private List<String> filterVisibleTags(List<String> names) {
        if (names == null || names.isEmpty()) {
            return new ArrayList<>();
        }
        return names.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(name -> !name.startsWith(PRIVATE_TAG_NAMESPACE))
                .toList();
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

    private String buildItemTagsKey(Long itemId) {
        return Constants.RedisKey.ITEM_TAGS + itemId;
    }

    private String buildTagListKey(String keyword, int pageNo, int pageSize) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : "all";
        return Constants.RedisKey.TAG_LIST + normalizedKeyword + ":" + pageNo + ":" + pageSize;
    }

    private void evictTagListCache() {
        redisService.removeByPrefix(Constants.RedisKey.TAG_LIST);
    }
}
