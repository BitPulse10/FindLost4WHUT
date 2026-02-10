package com.whut.lostandfoundforwhut.common.utils.cos;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.image.ImageLabelRequest;
import com.qcloud.cos.model.ciModel.image.ImageLabelResponse;
import com.qcloud.cos.model.ciModel.image.Label;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class ContentRecognizerTest {
    
    @Autowired
    private ContentRecognizer contentRecognizer;

    private String testKey = "test-image.png";
    private int testMinConfidence = 60;

    /**
     * 测试 getLabels 方法
     */
    @Test
    void testGetLabels() {
        // 调用 getLabels 方法
        List<Label> result = contentRecognizer.getLabels(testKey, testMinConfidence);
        for (Label label : result) {
            System.out.println(label.toString());
        }
    }

    /**
     * 测试 getNames 方法
     */
    @Test
    void testGetNames() {
        // 调用 getNames 方法
        List<String> names = contentRecognizer.getNames(testKey, testMinConfidence);
        for (String name : names) {
            System.out.println(name);
        }
    }

    @Test
    void testGetCategoriesAndNames() {
        // 调用 getCategoriesAndNames 方法
        List<String> categoriesAndNames = contentRecognizer.getCategoriesAndNames(testKey, testMinConfidence);  
        for (String categoryOrName : categoriesAndNames) {
            System.out.println(categoryOrName);
        }
    }
}
