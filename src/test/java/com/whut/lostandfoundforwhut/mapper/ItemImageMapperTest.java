package com.whut.lostandfoundforwhut.mapper;

import com.whut.lostandfoundforwhut.model.entity.Image;
import com.whut.lostandfoundforwhut.model.entity.ItemImage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ItemImageMapperTest {
    @Autowired
    private ItemImageMapper itemImageMapper;

    @Test
    void testGetImagesByItemId() {
        // 测试获取物品的图片列表
        Long itemId = 1L;
        List<Image> images = itemImageMapper.getImagesByItemId(itemId);
        
        // 验证结果
        assertNotNull(images);
        // 可以根据实际数据添加更多断言
    }

    @Test
    void testGetImageUrlsByItemId() {
        // 测试获取物品的图片URL列表
        Long itemId = 1L;
        List<String> urls = itemImageMapper.getImageUrlsByItemId(itemId);
        
        // 验证结果
        assertNotNull(urls);
        // 可以根据实际数据添加更多断言
    }

    @Test
    void testInsertItemImages() {
        // 准备测试数据
        Long itemId = 1L;
        List<Long> itemImages = Arrays.asList(11L, 12L);
        
        // 执行批量插入
        boolean result = itemImageMapper.insertItemImages(itemId, itemImages);
        
        // 验证结果
        assertTrue(result);
        
        // 验证数据是否插入成功
        List<Image> insertedImages = itemImageMapper.getImagesByItemId(itemId);
        assertNotNull(insertedImages);
        assertTrue(insertedImages.size() >= 2);
    }
}
