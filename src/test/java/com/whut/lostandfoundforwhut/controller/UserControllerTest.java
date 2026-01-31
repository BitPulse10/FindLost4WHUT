package com.whut.lostandfoundforwhut.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtUtil;
import com.whut.lostandfoundforwhut.mapper.UserMapper;
import com.whut.lostandfoundforwhut.model.dto.UserNicknameUpdateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserPasswordUpdateDTO;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.service.IUserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // 关闭过滤器，手动Mock SecurityContext
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IUserService userService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private JwtUtil jwtUtil;

    // 新增：Mock SecurityContext相关对象（解决空指针核心）
    @MockBean
    private SecurityContext securityContext;

    @MockBean
    private Authentication authentication;

    @MockBean
    private UserDetails userDetails;

    // 测试前置处理：初始化SecurityContext（每个测试方法执行前调用）
    @Test
    void setup() {
        // 让SecurityContextHolder返回Mock的securityContext
        SecurityContextHolder.setContext(securityContext);
        // Mock authentication不为null
        when(securityContext.getAuthentication()).thenReturn(authentication);
        // Mock getPrincipal返回UserDetails
        when(authentication.getPrincipal()).thenReturn(userDetails);
        // Mock UserDetails的getUsername返回测试用的邮箱
        when(userDetails.getUsername()).thenReturn("login@example.com");
    }

    // ==================== updatePassword 测试方法 ====================
    @Test
    void updatePassword_returnsNoPermissionWhenEmailMismatch() throws Exception {
        // 先执行前置初始化
        setup();

        // 1. Mock Service 方法（无需再Mock JWT，因为现在从SecurityContext获取）
        User loginUser = new User();
        loginUser.setId(1L);
        loginUser.setEmail("login@example.com");
        when(userService.getUserById(1L)).thenReturn(loginUser);

        User targetUser = new User();
        targetUser.setId(2L);
        targetUser.setEmail("other@example.com");
        when(userService.getUserById(2L)).thenReturn(targetUser);

        // 2. 构造请求参数
        UserPasswordUpdateDTO dto = new UserPasswordUpdateDTO();
        dto.setPassword("newPassword123");
        dto.setConfirmPassword("newPassword123");

        // 3. 执行请求并验证
        mockMvc.perform(put("/api/users/2/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.NO_PERMISSION.getCode()))
                .andExpect(jsonPath("$.info").value(ResponseCode.NO_PERMISSION.getInfo()));

        verify(userService, never()).updatePassword(anyLong(), any(UserPasswordUpdateDTO.class));
    }

    // ==================== updateNickname 测试方法 ====================
    @Test
    void updateNickname_returnsIllegalParameterWhenNicknameBlank() throws Exception {
        // 先执行前置初始化（解决空指针）
        setup();

        // 1. Mock Service 方法
        User loginUser = new User();
        loginUser.setId(1L);
        loginUser.setEmail("login@example.com");
        when(userService.getUserById(1L)).thenReturn(loginUser);

        // 2. Mock 异常抛出（确保抛出的是AppException）
        doThrow(new AppException(
                ResponseCode.ILLEGAL_PARAMETER.getCode(),
                ResponseCode.ILLEGAL_PARAMETER.getInfo()
        )).when(userService).updateNickname(eq(1L), any(UserNicknameUpdateDTO.class));

        // 3. 构造请求参数（空昵称）
        UserNicknameUpdateDTO dto = new UserNicknameUpdateDTO();
        dto.setNickname(" "); // 空昵称触发非法参数

        // 4. 执行请求并验证
        mockMvc.perform(put("/api/users/1/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.ILLEGAL_PARAMETER.getCode()))
                .andExpect(jsonPath("$.info").value(ResponseCode.ILLEGAL_PARAMETER.getInfo()));

        verify(userService, times(1)).updateNickname(1L, dto);
    }
}