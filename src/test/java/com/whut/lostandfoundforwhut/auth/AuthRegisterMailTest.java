package com.whut.lostandfoundforwhut.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.whut.lostandfoundforwhut.mapper.UserMapper;
import com.whut.lostandfoundforwhut.service.IAuthService;
import com.whut.lostandfoundforwhut.service.IRedisService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.StringUtils;

import static org.mockito.Mockito.when;

/**
 * @author DXR
 * @date 2026/02/02
 * @description 注册验证码邮件发送测试（用于本地验证 SMTP 配置）
 */
@SpringBootTest
class AuthRegisterMailTest {

    @Autowired
    private IAuthService authService;

    @MockBean
    private IRedisService redisService;

    @MockBean
    private UserMapper userMapper;

    @Value("${app.mail.test-to:}")
    private String testTo;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Test
    void sendRegisterCode_shouldSendMailWithLocalSmtp() {
        String toEmail = StringUtils.hasText(testTo) ? testTo : mailUsername;
        Assertions.assertTrue(StringUtils.hasText(toEmail),
                "请在 application-dev.yml 配置 app.mail.test-to 或 spring.mail.username 以便发送测试邮件");
        when(redisService.isExists(ArgumentMatchers.anyString())).thenReturn(false);
        when(userMapper.selectOne(ArgumentMatchers.any(LambdaQueryWrapper.class))).thenReturn(null);

        authService.sendRegisterCode(toEmail);
    }
}
