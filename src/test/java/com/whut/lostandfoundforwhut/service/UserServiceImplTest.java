package com.whut.lostandfoundforwhut.service;

import com.whut.lostandfoundforwhut.common.constant.Constants;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtUtil;
import com.whut.lostandfoundforwhut.mapper.UserMapper;
import com.whut.lostandfoundforwhut.model.dto.UserNicknameUpdateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserPasswordUpdateDTO;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private IRedisService redisService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void updatePassword_throwsWhenConfirmMismatch() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);

        UserPasswordUpdateDTO dto = new UserPasswordUpdateDTO();
        dto.setPassword("pass1");
        dto.setConfirmPassword("pass2");

        AppException ex = assertThrows(AppException.class, () -> userService.updatePassword(1L, dto));
        assertEquals(ResponseCode.ILLEGAL_PARAMETER.getCode(), ex.getCode());
    }

    @Test
    void updateNickname_throwsWhenNicknameBlank() {
        User user = new User();
        user.setId(2L);
        when(userMapper.selectById(2L)).thenReturn(user);

        UserNicknameUpdateDTO dto = new UserNicknameUpdateDTO();
        dto.setNickname(" ");

        AppException ex = assertThrows(AppException.class, () -> userService.updateNickname(2L, dto));
        assertEquals(ResponseCode.ILLEGAL_PARAMETER.getCode(), ex.getCode());
    }

    @Test
    void getUserIdByEmail_returnsFromCache() {
        String email = "cache@example.com";
        String cacheKey = Constants.RedisKey.USER_ID_BY_EMAIL + email;
        when(redisService.getValue(cacheKey)).thenReturn(1001L);

        Long userId = userService.getUserIdByEmail(email);

        assertEquals(1001L, userId);
        verify(userMapper, never()).selectOne(any());
    }

    @Test
    void getUserIdByEmail_readsDbAndBackfillsCache() {
        String email = "db@example.com";
        String userIdKey = Constants.RedisKey.USER_ID_BY_EMAIL + email;
        when(redisService.getValue(userIdKey)).thenReturn(null);

        User user = new User();
        user.setId(2002L);
        user.setEmail(email);
        user.setNickname("n1");
        when(userMapper.selectOne(any())).thenReturn(user);

        Long userId = userService.getUserIdByEmail(email);

        assertEquals(2002L, userId);
        verify(redisService).setValue(eq(Constants.RedisKey.USER_PROFILE_BY_EMAIL + email), eq(user), any(Duration.class));
        verify(redisService).setValue(eq(userIdKey), eq(2002L), any(Duration.class));
        verify(redisService).setValue(eq(Constants.RedisKey.USER_EMAIL_BY_ID + 2002L), eq(email), any(Duration.class));
    }

    @Test
    void updateNickname_evictsUserCacheAfterDbUpdate() {
        User user = new User();
        user.setId(12L);
        user.setEmail("evict@example.com");
        when(userMapper.selectById(12L)).thenReturn(user);

        UserNicknameUpdateDTO dto = new UserNicknameUpdateDTO();
        dto.setNickname("new_name");

        userService.updateNickname(12L, dto);

        verify(userMapper, times(1)).updateById(user);
        verify(redisService).remove(Constants.RedisKey.USER_PROFILE_BY_EMAIL + "evict@example.com");
        verify(redisService).remove(Constants.RedisKey.USER_ID_BY_EMAIL + "evict@example.com");
        verify(redisService).remove(Constants.RedisKey.USER_EMAIL_BY_ID + 12L);
    }

    @Test
    void getNicknameByToken_throwsBusinessExceptionWhenJwtParseFails() {
        String token = "Bearer bad-token";
        when(jwtUtil.isTokenValid("bad-token")).thenReturn(true);
        when(jwtUtil.getEmail("bad-token")).thenThrow(new RuntimeException("jwt parse error"));

        AppException ex = assertThrows(AppException.class, () -> userService.getNicknameByToken(token));
        assertEquals(ResponseCode.NOT_LOGIN.getCode(), ex.getCode());
        assertEquals("Token无效或解析失败", ex.getInfo());
    }
}
