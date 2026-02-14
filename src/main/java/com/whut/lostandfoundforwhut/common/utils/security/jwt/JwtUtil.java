package com.whut.lostandfoundforwhut.common.utils.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * @author DXR
 * @date 2026/01/30
 * @description JWT 工具类，负责签发与校验 Token
 */
@Component
public class JwtUtil {
    private static final int MIN_SECRET_LENGTH = 32;

    /** JWT 密钥（建议至少 32 位） */
    @Value("${app.jwt.secret}")
    private String secret;

    /** 过期时间（毫秒） */
    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    /** 签发者 */
    @Value("${app.jwt.issuer}")
    private String issuer;

    /**
     * 启动时校验 JWT 关键配置，避免使用空密钥或弱密钥
     */
    @PostConstruct
    public void validateConfig() {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT 密钥未配置，请设置 APP_JWT_SECRET 环境变量");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT 密钥长度不足，至少需要 32 个字符");
        }
    }

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 生成 JWT（subject 使用邮箱）
     * @param email 用户邮箱
     * @return Token 字符串
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(email)
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 从 Token 解析邮箱
     * @param token Token
     * @return 邮箱
     */
    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 校验 Token 是否有效
     * @param token Token
     * @return 是否有效
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 获取过期时间（毫秒）
     * @return 过期时间
     */
    public long getExpirationMs() {
        return expirationMs;
    }

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 解析并校验 Claims
     * @param token Token
     * @return Claims
     */
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 生成 HMAC-SHA 密钥
     * @return Key 密钥
     */
    private Key getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
