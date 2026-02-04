package com.whut.lostandfoundforwhut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.common.utils.page.PageUtils;
import com.whut.lostandfoundforwhut.mapper.ItemTagMapper;
import com.whut.lostandfoundforwhut.mapper.TagMapper;
import com.whut.lostandfoundforwhut.model.entity.ItemTag;
import com.whut.lostandfoundforwhut.model.entity.Tag;
import com.whut.lostandfoundforwhut.model.vo.PageResultVO;
import com.whut.lostandfoundforwhut.service.ITagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
public class TagServiceImpl implements ITagService {

    private static final int MAX_TAG_LENGTH = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final TagMapper tagMapper;
    private final ItemTagMapper itemTagMapper;

    @Override
    public PageResultVO<Tag> listTags(String keyword, Integer pageNo, Integer pageSize) {
        int resolvedPageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int resolvedPageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        if (resolvedPageSize > MAX_PAGE_SIZE) {
            resolvedPageSize = MAX_PAGE_SIZE;
        }

        Page<Tag> page = new Page<>(resolvedPageNo, resolvedPageSize);
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(Tag::getName, keyword.trim());
        }
        queryWrapper.orderByAsc(Tag::getId);
        tagMapper.selectPage(page, queryWrapper);
        return PageUtils.toPageResult(page);
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
            if (value.isEmpty() || value.length() > MAX_TAG_LENGTH) {
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

        List<Tag> existing = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>().in(Tag::getName, names));
        Set<String> existingNames = existing.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        for (String name : names) {
            if (existingNames.contains(name)) {
                continue;
            }
            Tag tag = new Tag();
            tag.setName(name);
            try {
                tagMapper.insert(tag);
            } catch (Exception e) {
                log.warn("创建标签失败，可能已存在：{}", name, e);
            }
        }

        return tagMapper.selectList(new LambdaQueryWrapper<Tag>().in(Tag::getName, names));
    }

    @Override
    public void replaceTagsForItem(Long itemId, List<String> names) {
        if (itemId == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "物品ID不能为空");
        }

        itemTagMapper.deleteByItemId(itemId);
        if (names == null || names.isEmpty()) {
            return;
        }

        List<Tag> tags = getOrCreateTags(names);
        if (tags.isEmpty()) {
            return;
        }

        List<ItemTag> relations = tags.stream()
                .map(tag -> {
                    ItemTag itemTag = new ItemTag();
                    itemTag.setItemId(itemId);
                    itemTag.setTagId(tag.getId());
                    return itemTag;
                })
                .toList();

        itemTagMapper.insertBatch(relations);
    }

    @Override
    public List<String> getTagNamesByItemId(Long itemId) {
        if (itemId == null) {
            return new ArrayList<>();
        }
        return tagMapper.selectNamesByItemId(itemId);
    }
}
