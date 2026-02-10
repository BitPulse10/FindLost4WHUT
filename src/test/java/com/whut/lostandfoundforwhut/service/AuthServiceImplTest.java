package com.whut.lostandfoundforwhut.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.enums.user.UserStatus;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtUtil;
import com.whut.lostandfoundforwhut.mapper.UserMapper;
import com.whut.lostandfoundforwhut.model.dto.UserCreateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserRegisterDTO;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private IRedisService redisService;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private IUserService userService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void sendRegisterCode_shouldRejectBannedUser() {
        ReflectionTestUtils.setField(authService, "mailFrom", "noreply@test.com");
        User existing = new User();
        existing.setStatus(UserStatus.BANNED.getCode());
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        AppException ex = Assertions.assertThrows(AppException.class,
                () -> authService.sendRegisterCode("a@test.com"));

        Assertions.assertEquals(ResponseCode.USER_STATUS_INVALID.getCode(), ex.getCode());
        verify(mailSender, never()).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void register_shouldRejectDeactivatedUserInCooldown() {
        User existing = new User();
        existing.setStatus(UserStatus.DEACTIVATED.getCode());
        existing.setUpdatedAt(LocalDateTime.now().minusDays(5));
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setEmail("b@test.com");
        dto.setPassword("123456");
        dto.setConfirmPassword("123456");
        dto.setCode("1234");

        AppException ex = Assertions.assertThrows(AppException.class,
                () -> authService.register(dto));
        Assertions.assertEquals(ResponseCode.USER_STATUS_INVALID.getCode(), ex.getCode());
        verify(userService, never()).createUser(any(UserCreateDTO.class));
    }

    @Test
    void register_shouldReactivateDeactivatedUserAfterCooldown() {
        User existing = new User();
        existing.setStatus(UserStatus.DEACTIVATED.getCode());
        existing.setUpdatedAt(LocalDateTime.now().minusDays(11));
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setEmail("c@test.com");
        dto.setPassword("123456");
        dto.setConfirmPassword("123456");
        dto.setCode("7777");

        when(redisService.getValue(ArgumentMatchers.contains("register:code:"))).thenReturn("7777");

        User created = new User();
        created.setEmail("c@test.com");
        created.setStatus(UserStatus.NORMAL.getCode());
        when(userService.reactivateUser("c@test.com", "123456", null)).thenReturn(created);

        User result = authService.register(dto);

        Assertions.assertEquals("c@test.com", result.getEmail());
        verify(userService).reactivateUser("c@test.com", "123456", null);
        verify(userService, never()).createUser(any(UserCreateDTO.class));
    }
}
