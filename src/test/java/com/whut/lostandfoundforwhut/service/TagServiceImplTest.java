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
import java.util.regex.Pattern;

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
    void parseTagText_privateCardTagShouldHashAndHideRawValue() {
        List<String> result = service.parseTagText("#priv:no=420106200001011234#普通标签#");
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.contains("普通标签"));
        String privateTag = result.stream().filter(v -> v.startsWith("__sys_priv__:no:")).findFirst().orElse("");
        Assertions.assertTrue(Pattern.matches("^__sys_priv__:no:[a-f0-9]{16}$", privateTag));
        Assertions.assertFalse(privateTag.contains("420106200001011234"));
    }

    @Test
    void parseTagText_privateTagShouldSupportColonStyle() {
        List<String> result = service.parseTagText("#priv:no:2023001234#");
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(Pattern.matches("^__sys_priv__:no:[a-f0-9]{16}$", result.get(0)));
    }

    @Test
    void parseTagText_legacyPrivateKeyShouldNormalizeToNo() {
        List<String> result = service.parseTagText("#priv:card_no=420106200001011234#");
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(Pattern.matches("^__sys_priv__:no:[a-f0-9]{16}$", result.get(0)));
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

    @Test
    void getTagNamesByItemId_shouldFilterPrivateTagsWhenReadingDb() {
        Long itemId = 303L;
        when(redisService.getValue(Constants.RedisKey.ITEM_TAGS + itemId)).thenReturn(null);
        when(tagMapper.selectNamesByItemId(itemId)).thenReturn(List.of("公开标签", "__sys_priv__:card_no:abcdef1234567890"));

        List<String> result = service.getTagNamesByItemId(itemId);

        Assertions.assertEquals(List.of("公开标签"), result);
    }

    @Test
    void getTagNamesByItemId_shouldFilterPrivateTagsWhenReadingCache() {
        Long itemId = 404L;
        when(redisService.getValue(Constants.RedisKey.ITEM_TAGS + itemId))
                .thenReturn(List.of("公开标签", "__sys_priv__:student_no:abcdef1234567890"));

        List<String> result = service.getTagNamesByItemId(itemId);

        Assertions.assertEquals(List.of("公开标签"), result);
        verify(tagMapper, never()).selectNamesByItemId(any());
    }
}
