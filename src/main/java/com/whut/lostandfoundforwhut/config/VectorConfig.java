package com.whut.lostandfoundforwhut.config;

import com.whut.lostandfoundforwhut.service.IVectorService;
import com.whut.lostandfoundforwhut.service.impl.VectorServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorConfig {

    /**
     * 向量数据库服务 - 仅在启用时创建完整实现
     *
     * @return
     */
    @Bean
    @ConditionalOnProperty(name = "app.vector-store.enabled", havingValue = "true", matchIfMissing = false)
    public IVectorService vectorServiceEnabled() {
        return new VectorServiceImpl();
    }

    /**
     * 默认禁用的向量服务实现 - 仅在未启用向量服务时使用此实现
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(type = "com.whut.lostandfoundforwhut.service.impl.VectorServiceImpl")
    public IVectorService vectorService() {
        return new IVectorService() {
            @Override
            public void initializeCollection() {
                // 什么都不做
            }

            @Override
            public void addTextToCollection(com.whut.lostandfoundforwhut.model.dto.TextEmbeddingDTO textEmbeddingDTO) {
                // 什么都不做
            }

            @Override
            public java.util.List<String> searchInCollection(String query, int k) {
                // 返回空列表
                return java.util.Collections.emptyList();
            }

            @Override
            public int getCollectionSize() {
                // 返回0
                return 0;
            }

            @Override
            public void deleteFromCollection(String id) {
                // 什么都不要做
            }

            @Override
            public void clearCollection() {
                // 什么都不要做
            }

            @Override
            public java.util.List<String> searchInCollectionByStatus(String query, int k, Integer statusFilter) {
                // 返回空列表
                return java.util.Collections.emptyList();
            }
        };
    }
}