package com.whut.lostandfoundforwhut.security;

import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author DXR
 * @date 2026/01/30
 * @description JWT 工具测试
 */
@SpringBootTest(classes = JwtUtil.class)
@TestPropertySource(properties = {
        "app.jwt.secret=ChangeThisToA32CharOrLongerSecretKey!",
        "app.jwt.expiration-ms=86400000",
        "app.jwt.issuer=lostAndFoundForWhut"
})
class JwtUtilTests {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 生成并解析 Token
     */
    @Test
    void shouldGenerateAndParseToken() {
        String token = jwtUtil.generateToken("test@example.com");
        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("test@example.com", jwtUtil.getEmail(token));
    }

    /**
     * @author DXR
     * @date 2026/01/31
     * @description 生成指定邮箱的Token并打印到控制台
     */
    @Test
    void generateTokenForSpecificEmail() {
        String email = "123@qq.com";
        String token = jwtUtil.generateToken(email);
        System.out.println("Generated Token for " + email + ": " + token);
        /*
         * eyJhbGciOiJIUzI1NiJ9.
         * eyJzdWIiOiIxMjNAcXEuY29tIiwiaXNzIjoibG9zdEFuZEZvdW5kRm9yV2h1dCIsImlhdCI6MTc2OTg0NTMzNCwiZXhwIjoxNzY5OTMxNzM0fQ
         * .nsP5qWXOBD-YkoUlCCyf5KPOomLiAwKySTOlpmhFEjw
         */
        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals(email, jwtUtil.getEmail(token));
    }
}