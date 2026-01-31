package com.whut.lostandfoundforwhut.config;

import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtAuthenticationFilter;
import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * @author DXR
 * @date 2026/01/30
 * @description Spring Security 配置，支持 JWT 与开关控制
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 构建安全过滤链，按开关启用/禁用认证
     * @param http                    HttpSecurity 配置对象
     * @param jwtAuthenticationFilter JWT 过滤器
     * @return SecurityFilterChain 过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${app.security.enabled:false}") boolean securityEnabled)
            throws Exception {
        // 关闭 CSRF，使用无状态会话（JWT）
        http.csrf(AbstractHttpConfigurer::disable);

        if (securityEnabled) {
            http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/error").permitAll()
                            .anyRequest().authenticated())
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }
        return http.build();
    }

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 创建 JWT 认证过滤器
     * @param jwtUtil            JWT 工具
     * @param userDetailsService 用户详情服务
     * @param securityEnabled    安全开关
     * @return JwtAuthenticationFilter 过滤器
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        // 手动构建过滤器，避免循环依赖
        return new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    }

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 构建内存用户（快速开发使用）
     * @param username        用户名
     * @param password        密码
     * @param passwordEncoder 密码编码器
     * @return UserDetailsService 服务
     */
    @Bean
    public UserDetailsService userDetailsService(
            @Value("${app.security.default-username}") String username,
            @Value("${app.security.default-password}") String password,
            PasswordEncoder passwordEncoder) {
        UserDetails user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 创建密码编码器（BCrypt）
     * @return PasswordEncoder 编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 获取认证管理器
     * @param configuration 认证配置
     * @return AuthenticationManager 管理器
     * @throws Exception 解析异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
