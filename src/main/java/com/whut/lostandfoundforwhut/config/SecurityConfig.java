package com.whut.lostandfoundforwhut.config;

import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtAuthenticationFilter;
import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


/**
 * @author DXR
 * @date 2026/01/30
 * @description Spring Security 配置，支持 JWT 与开关控制（适配真实业务用户）
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Resource
    private JwtUtil jwtUtil;

    /**
     * 构建安全过滤链，按开关启用/禁用认证
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   @Value("${app.security.enabled:false}") boolean securityEnabled,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        // 基础配置：关闭CSRF，前后端分离场景必备
        http.csrf(AbstractHttpConfigurer::disable);

        if (securityEnabled) {
            // 启用认证时的配置
            http
                    // 无状态会话（JWT不需要Session）
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    // 权限规则配置（适配你的Controller接口）
                    .authorizeHttpRequests(auth -> auth
                            // 放行注册/登录接口（无需认证）
                            .requestMatchers("/api/users").permitAll() // POST /api/users 注册
                            .requestMatchers("/api/auth/**").permitAll() // 其他认证相关接口
                            .requestMatchers("/error").permitAll() // 错误页面
                            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // 放行Swagger文档
                            // 其余所有请求都需要认证
                            .anyRequest().authenticated())
                    // 添加JWT过滤器，在用户名密码过滤器之前执行
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        } else {
            // 禁用认证时，放行所有请求（方便开发调试）
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }

        return http.build();
    }

    /**
     * 创建JWT认证过滤器（适配真实业务的UserDetailsService）
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(UserDetailsService userDetailsService) {
        // 使用实际的 UserDetailsService（从数据库查用户），避免配置类依赖业务 Service
        return new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    }

    /**
     * 密码编码器（BCrypt加密，行业标准）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器（用于登录时的用户名密码认证）
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

}
