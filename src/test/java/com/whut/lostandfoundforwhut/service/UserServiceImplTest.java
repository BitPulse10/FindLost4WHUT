package com.whut.lostandfoundforwhut.service;

import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

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
}
