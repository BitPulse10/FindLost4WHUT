package com.whut.lostandfoundforwhut.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * @author DXR
 * @date 2026/01/30
 * @description 全局跨域配置
 */
@Configuration
public class CorsConfig {

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 构建跨域配置源，允许前端跨域访问
     * @return CorsConfigurationSource 跨域配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许的前端域名，开发阶段可放开，生产建议收紧
        // TODO(上线前处理): 当前为开发便利使用通配符，生产环境必须改为明确白名单域名。
        config.setAllowedOriginPatterns(List.of("*"));
        // 允许的请求方法
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // 允许的请求头
        config.setAllowedHeaders(List.of("*"));
        // 暴露给前端的响应头
        config.setExposedHeaders(List.of("Authorization"));
        // 是否允许携带凭证
        config.setAllowCredentials(true);
        // 预检请求缓存时间
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
