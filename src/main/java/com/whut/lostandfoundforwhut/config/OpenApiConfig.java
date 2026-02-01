package com.whut.lostandfoundforwhut.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author DXR
 * @date 2026/01/30
 * @description OpenAPI 基础配置（含JWT Token全局认证）
 */
@Configuration
public class OpenApiConfig {

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 构建 OpenAPI 文档信息（新增JWT全局认证）
     * @return OpenAPI 文档对象
     */
    @Bean
    public OpenAPI openAPI() {
        // 1. 定义JWT认证方案
        String securitySchemeName = "Bearer Token";
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP) // HTTP认证类型
                .scheme("bearer") // Bearer模式
                .bearerFormat("JWT") // 令牌格式为JWT
                .in(SecurityScheme.In.HEADER) // 令牌放在请求头
                .name("Authorization"); // 请求头名称

        // 2. 构建OpenAPI对象（保留原有文档信息，新增认证配置）
        return new OpenAPI()
                // 原有基础文档信息
                .info(new Info()
                        .title("LostAndFound API")
                        .version("1.0.0")
                        .description("失物招领系统 API 文档"))
                // 新增：全局安全认证规则（所有接口默认需要这个Token）
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                // 新增：配置认证组件
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, securityScheme));
    }
}