package com.whut.lostandfoundforwhut.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.whut.lostandfoundforwhut.mapper.UserMapper;
import com.whut.lostandfoundforwhut.service.IAuthService;
import com.whut.lostandfoundforwhut.service.IRedisService;
import com.whut.lostandfoundforwhut.service.IVectorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.StringUtils;

import static org.mockito.Mockito.verify;
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

    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private IVectorService vectorService;

    @Value("${app.mail.test-to:}")
    private String testTo;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Test
    void sendRegisterCode_shouldSendMailWithLocalSmtp() {
        String toEmail;
        if (StringUtils.hasText(testTo) && testTo.trim().toLowerCase().endsWith("@whut.edu.cn")) {
            toEmail = testTo.trim().toLowerCase();
        } else {
            toEmail = "autotest@whut.edu.cn";
        }

        when(redisService.isExists(ArgumentMatchers.anyString())).thenReturn(false);
        when(userMapper.selectOne(ArgumentMatchers.any(LambdaQueryWrapper.class))).thenReturn(null);

        authService.sendRegisterCode(toEmail);
        verify(mailSender).send(ArgumentMatchers.any(SimpleMailMessage.class));
    }
}
