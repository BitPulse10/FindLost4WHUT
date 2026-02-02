package com.whut.lostandfoundforwhut.common.utils.cos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ContentReviewerTest {

    @Autowired
    private ContentReviewer contentReviewer;

    @Test
    void testReviewTextWithNullText() {
        // 测试空文本参数
        String result = contentReviewer.reviewText(null);
        assertEquals("", result);
    }

    @Test
    void testReviewTextWithEmptyText() {
        // 测试空字符串参数
        String result = contentReviewer.reviewText("");
        assertEquals("", result);
    }

    @Test
    void testReviewTextWithValidText() {
        String testText = "这是一个合法的文本内容";
        String result = contentReviewer.reviewText(testText);
        System.out.println("Valid Text Review Result: " + result);
        assertEquals("", result);
    }

    @Test
    void testReviewTextWithInvalidText() {
        String testText = "傻逼";
        String result = contentReviewer.reviewText(testText);
        System.out.println("Invalid Text Review Result: " + result);
        assertNotEquals("", result);
    }
}