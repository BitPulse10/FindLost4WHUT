package com.whut.lostandfoundforwhut.config;

import com.whut.lostandfoundforwhut.service.IVectorService;
import com.whut.lostandfoundforwhut.service.impl.VectorServiceImpl;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 向量数据库配置类
 */
@Slf4j
@Configuration
public class VectorConfig {

    /**
     * 向量数据库服务 Bean
     * 直接创建实际的服务实现
     */
    @Bean(name = "vectorService")
    @Primary
    public IVectorService vectorService() {
        log.info("向量数据库功能已启用，正在创建 VectorServiceImpl");
        return new VectorServiceImpl();
    }
}