package com.whut.lostandfoundforwhut.common.utils.cos;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.image.Label;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContentRecognizerTest {
    @Mock
    private COS cos;

    @Mock
    private COSClient cosClient;

    @Mock
    private com.qcloud.cos.model.ciModel.image.ImageLabelResponse imageLabelResponse;

    @InjectMocks
    private ContentRecognizer contentRecognizer;

    private final String testKey = "test-image.png";
    private final int testMinConfidence = 60;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(cos.getBucketName()).thenReturn("test-bucket");
        when(cos.getCosClient()).thenReturn(cosClient);
        when(cosClient.getImageLabel(any())).thenReturn(imageLabelResponse);
    }

    private Label mockLabel(String confidence, String name, String firstCategory, String secondCategory) {
        Label label = mock(Label.class);
        when(label.getConfidence()).thenReturn(confidence);
        when(label.getName()).thenReturn(name);
        when(label.getFirstCategory()).thenReturn(firstCategory);
        when(label.getSecondCategory()).thenReturn(secondCategory);
        return label;
    }

    /**
     * 测试 getLabels 方法
     */
    @Test
    void testGetLabels() {
        List<Label> labels = new ArrayList<>();
        labels.add(mockLabel("95", "猫", "动物", "宠物"));
        labels.add(mockLabel("40", "地面", "场景", "户外"));
        when(imageLabelResponse.getRecognitionResult()).thenReturn(labels);

        List<Label> result = contentRecognizer.getLabels(testKey, testMinConfidence);

        assertEquals(1, result.size());
        assertEquals("猫", result.get(0).getName());
        verify(cosClient, times(1)).getImageLabel(any());
    }

    /**
     * 测试 getNames 方法
     */
    @Test
    void testGetNames() {
        List<Label> labels = new ArrayList<>();
        labels.add(mockLabel("80", "钱包", "物品", "日常"));
        labels.add(mockLabel("55", "桌子", "场景", "室内"));
        when(imageLabelResponse.getRecognitionResult()).thenReturn(labels);

        List<String> names = contentRecognizer.getNames(testKey, testMinConfidence);

        assertEquals(1, names.size());
        assertEquals("钱包", names.get(0));
    }

    @Test
    void testGetCategoriesAndNames() {
        List<Label> labels = new ArrayList<>();
        labels.add(mockLabel("90", "钥匙", "物品", "金属"));
        labels.add(mockLabel("90", "钥匙串", "物品", "金属"));
        when(imageLabelResponse.getRecognitionResult()).thenReturn(labels);

        List<String> categoriesAndNames = contentRecognizer.getCategoriesAndNames(testKey, testMinConfidence);

        assertEquals(List.of("物品", "金属", "钥匙", "钥匙串"), categoriesAndNames);
    }
}
