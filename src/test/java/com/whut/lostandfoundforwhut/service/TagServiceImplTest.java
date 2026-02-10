package com.whut.lostandfoundforwhut.service;

import com.whut.lostandfoundforwhut.common.constant.Constants;
import com.whut.lostandfoundforwhut.mapper.ItemTagMapper;
import com.whut.lostandfoundforwhut.mapper.TagMapper;
import com.whut.lostandfoundforwhut.service.impl.TagServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Codex
 * @date 2026/02/04
 * @description 标签解析与缓存单元测试
 */
@ExtendWith(MockitoExtension.class)
public class TagServiceImplTest {

    @Mock
    private TagMapper tagMapper;

    @Mock
    private ItemTagMapper itemTagMapper;

    @Mock
    private IRedisService redisService;

    @InjectMocks
    private TagServiceImpl service;

    @Test
    void parseTagText_shouldNormalizeAndDeduplicate() {
        List<String> result = service.parseTagText(" #A#b##A#abcdefghijklmnopqrstu# ");
        Assertions.assertEquals(List.of("a", "b"), result);
    }

    @Test
    void parseTagText_emptyShouldReturnEmptyList() {
        List<String> result = service.parseTagText("   ");
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getTagNamesByItemId_returnsFromCache() {
        Long itemId = 101L;
        String cacheKey = Constants.RedisKey.ITEM_TAGS + itemId;
        List<String> cached = List.of("card", "blue");
        when(redisService.getValue(cacheKey)).thenReturn(cached);

        List<String> result = service.getTagNamesByItemId(itemId);

        Assertions.assertEquals(cached, result);
        verify(tagMapper, never()).selectNamesByItemId(any());
    }

    @Test
    void getTagNamesByItemId_readsDbAndBackfillsCache() {
        Long itemId = 202L;
        String cacheKey = Constants.RedisKey.ITEM_TAGS + itemId;
        when(redisService.getValue(cacheKey)).thenReturn(null);
        when(tagMapper.selectNamesByItemId(itemId)).thenReturn(List.of("wallet"));

        List<String> result = service.getTagNamesByItemId(itemId);

        Assertions.assertEquals(List.of("wallet"), result);
        verify(redisService).setValue(eq(cacheKey), eq(List.of("wallet")), any(Duration.class));
    }
}
