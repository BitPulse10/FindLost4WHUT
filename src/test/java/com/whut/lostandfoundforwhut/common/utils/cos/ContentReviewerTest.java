package com.whut.lostandfoundforwhut.common.utils.cos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import junit.framework.TestResult;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
class ContentReviewerTest {

    @Autowired
    private ContentReviewer contentReviewer;

    @Test
    void testReviewText() {
        List<String> testTexts = Arrays.asList(
                null,
                "",
                "这是一个包含广告内容的文本",
                "傻逼"
        );

        class TestResult {
            String message;
            boolean equal;
            TestResult(String message, boolean equal) {
                this.message = message;
                this.equal = equal;
            }
        }
        List<TestResult> results = Arrays.asList(
            new TestResult("", true),
            new TestResult("", true),
            new TestResult("", true),
            new TestResult("", false)
        );

        for (int i = 0; i < testTexts.size(); i++) {
            String testText = testTexts.get(i);
            String message = contentReviewer.reviewText(testText);
            
            TestResult test = results.get(i);
            System.out.printf("[%d] \"%s\"\t Review Result: %s\n", i, testText, message);
            if (test.equal) {
                assertEquals(test.message, message);
            } else {
                assertNotEquals(test.message, message);
            }
        }
    }
    
    @Test
    void testReviewImageURL() {
        List<String> testURLs = Arrays.asList(
            "https://images.pexels.com/photos/33416262/pexels-photo-33416262.jpeg"
        );

        class TestResult {
            String message;
            boolean equal;
            TestResult(String message, boolean equal) {
                this.message = message;
                this.equal = equal;
            }
        }
        List<TestResult> results = Arrays.asList(
            new TestResult("", true)
        );

        for (int i = 0; i < testURLs.size(); i++) {
            String testURL = testURLs.get(i);
            String message = contentReviewer.reviewImageURL(testURL);
            
            TestResult test = results.get(i);
            System.out.printf("[%d] \"%s\"\t Review Result: %s\n", i, testURL, message);
            if (test.equal) {
                assertEquals(test.message, message);
            } else {
                assertNotEquals(test.message, message);
            }
        }
    }

    @Test
    void testBatchReviewImageURL() {
        List<String> testURLs = Arrays.asList(
            "https://images.pexels.com/photos/33416262/pexels-photo-33416262.jpeg",
            "https://images.pexels.com/photos/3459967/pexels-photo-3459967.jpeg?_gl=1*5x1b88*_ga*NDQ3MzY5Ni4xNzcwMTAwMzU4*_ga_8JE65Q40S6*czE3NzAxMDAzNTckbzEkZzEkdDE3NzAxMDEwNjgkajU1JGwwJGgw"
        );

        class TestResult {
            String message;
            boolean equal;
            TestResult(String message, boolean equal) {
                this.message = message;
                this.equal = equal;
            }
        }
        List<TestResult> results = Arrays.asList(
            new TestResult("", true),
            new TestResult("", true)
        );

        
        List<String> messages = contentReviewer.batchReviewImageURL(testURLs);
        
        for (int i = 0; i < messages.size(); i++) {
            String message = messages.get(i);
            
            TestResult test = results.get(i);
            System.out.printf("[%d] \"%s\"\t Review Result: %s\n", i, testURLs.get(i), message);
            if (test.equal) {
                assertEquals(test.message, message);
            } else {
                assertNotEquals(test.message, message);
            }
        }
    }

    @Test
    void testReviewImageKey() {
        List<String> testKeys = Arrays.asList(
            "test-image.png"
        );

        class TestResult {
            String message;
            boolean equal;
            TestResult(String message, boolean equal) {
                this.message = message;
                this.equal = equal;
            }
        }
        List<TestResult> results = Arrays.asList(
            new TestResult("", true)
        );

        for (int i = 0; i < testKeys.size(); i++) {
            String testKey = testKeys.get(i);
            String message = contentReviewer.reviewImageKey(testKey);
            
            TestResult test = results.get(i);
            System.out.printf("[%d] \"%s\"\t Review Result: %s\n", i, testKey, message);
            if (test.equal) {
                assertEquals(test.message, message);
            } else {
                assertNotEquals(test.message, message);
            }
        }
    }

    @Test
    void testBatchReviewImageKey() {
        List<String> testKeys = Arrays.asList(
            "test-image.png"
        );

        class TestResult {
            String message;
            boolean equal;
            TestResult(String message, boolean equal) {
                this.message = message;
                this.equal = equal;
            }
        }
        List<TestResult> results = Arrays.asList(
            new TestResult("", true)
        );
        
        List<String> messages = contentReviewer.batchReviewImageKey(testKeys);
        
        for (int i = 0; i < messages.size(); i++) {
            String message = messages.get(i);
            
            TestResult test = results.get(i);
            System.out.printf("[%d] \"%s\"\t Review Result: %s\n", i, testKeys.get(i), message);
            if (test.equal) {
                assertEquals(test.message, message);
            } else {
                assertNotEquals(test.message, message);
            }
        }
    }
}