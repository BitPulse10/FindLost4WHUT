package com.whut.lostandfoundforwhut.config;

import com.whut.lostandfoundforwhut.model.dto.TextEmbeddingDTO;
import com.whut.lostandfoundforwhut.model.entity.Item;
import com.whut.lostandfoundforwhut.service.IVectorService;
import com.whut.lostandfoundforwhut.service.impl.VectorServiceImpl;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 向量数据库配置类
 * 通过 app.vector-store.enabled 配置项控制是否启用向量数据库功能
 */
@Slf4j
@Configuration
public class VectorConfig {

    @Value("${app.vector-store.enabled:true}")
    private boolean vectorStoreEnabled;

    /**
     * 向量数据库服务 Bean
     * 当 app.vector-store.enabled=true 时创建实际的服务实现
     * 当 app.vector-store.enabled=false 或未配置时创建空实现
     */
    @Bean(name = "vectorService")
    @Primary
    public IVectorService vectorService() {
        if (vectorStoreEnabled) {
            log.info("向量数据库功能已启用，正在创建 VectorServiceImpl");
            return new VectorServiceImpl();
        } else {
            log.info("向量数据库功能已禁用，使用空实现");
            return new DisabledVectorService();
        }
    }

    /**
     * 禁用状态下的向量服务实现类
     * 所有方法都是空操作或返回默认值
     */
    private static class DisabledVectorService implements IVectorService {

        @Override
        public void initializeCollection() {
            // 空实现 - 不做任何操作
        }

        @Override
        public void addTextToCollection(TextEmbeddingDTO textEmbeddingDTO) {
            // 空实现 - 不做任何操作
        }

        @Override
        public java.util.List<String> searchInCollection(String query, int maxResults) {
            // 返回空列表
            return java.util.Collections.emptyList();
        }

        @Override
        public int getCollectionSize() {
            // 返回 0
            return 0;
        }

        @Override
        public void deleteFromCollection(String id) {
            // 空实现 - 不做任何操作
        }

        @Override
        public void clearCollection() {
            // 空实现 - 不做任何操作
        }

        @Override
        public void addToVectorDatabase(Item item) {
            // 空实现 - 不做任何操作
        }

        @Override
        public void addImagesToVectorDatabases(Item item, List<String> imageUrls) {
            // 空实现 - 不做任何操作
        }

        @Override
        public void addImagesToVectorDatabase(Item item, String imageUrl) {
            // 空实现 - 不做任何操作
        }

        @Override
        public void updateVectorDatabase(Item item) {
            // 空实现 - 不做任何操作
        }

        @Override
        public void removeFromVectorDatabase(Long itemId) {
            // 空实现 - 不做任何操作
        }
    }
}