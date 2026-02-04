package com.whut.lostandfoundforwhut.service;

import com.whut.lostandfoundforwhut.service.impl.TagServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Codex
 * @date 2026/02/04
 * @description 标签解析单元测试
 */
public class TagServiceImplTest {

    @Test
    void parseTagText_shouldNormalizeAndDeduplicate() {
        TagServiceImpl service = new TagServiceImpl(null, null);
        List<String> result = service.parseTagText(" #A#b##A#abcdefghijklmnopqrstu# ");
        Assertions.assertEquals(List.of("a", "b"), result);
    }

    @Test
    void parseTagText_emptyShouldReturnEmptyList() {
        TagServiceImpl service = new TagServiceImpl(null, null);
        List<String> result = service.parseTagText("   ");
        Assertions.assertTrue(result.isEmpty());
    }
}
