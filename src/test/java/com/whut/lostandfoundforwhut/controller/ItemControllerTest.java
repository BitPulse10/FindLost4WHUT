package com.whut.lostandfoundforwhut.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtUtil;
import com.whut.lostandfoundforwhut.model.dto.ItemDTO;
import com.whut.lostandfoundforwhut.model.entity.Item;
import com.whut.lostandfoundforwhut.service.IItemService;
import com.whut.lostandfoundforwhut.service.IUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 1. 仅加载 ItemController，排除 MyBatis Plus 自动配置
@SpringBootTest
// 2. 关闭过滤器（避免权限拦截干扰）
@AutoConfigureMockMvc(addFilters = false)
class ItemControllerTest {

    // 核心测试工具：模拟 HTTP 请求
    @Autowired
    private MockMvc mockMvc;

    // JSON 序列化工具（@Import 后可正常注入）
    @Autowired
    private ObjectMapper objectMapper;

    // Mock Controller 直接依赖的 Service
    @MockBean
    private IItemService itemService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private IUserService userService;

    // ==================== 测试添加物品 ====================
    @Test
    void addItem_resolvesUserFromBearerToken() throws Exception {
        // 1. Mock JWT 解析和用户 ID 查询
        when(jwtUtil.getEmail("token")).thenReturn("test@example.com");
        when(userService.getUserIdByEmail("test@example.com")).thenReturn(1L);

        // 2. Mock Service 层添加物品的返回结果
        Item mockItem = new Item();
        mockItem.setId(10L);
        when(itemService.addItem(any(ItemDTO.class), eq(1L))).thenReturn(mockItem);

        // 3. 构造请求参数（避免空 DTO 导致参数校验失败）
        ItemDTO itemDTO = new ItemDTO();
        itemDTO.setDescription("测试物品"); // 补充必要字段，模拟真实请求

        // 4. 执行请求并验证
        mockMvc.perform(post("/api/items/add-item")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDTO)))
                // 验证响应状态码
                .andExpect(status().isOk())
                // 验证响应码（SUCCESS 的 code 通常是 0000 或 200）
                .andExpect(jsonPath("$.code").value(ResponseCode.SUCCESS.getCode()))
                // 验证返回的物品 ID
                .andExpect(jsonPath("$.data.id").value(10));

        // 验证 Service 方法被正确调用
        verify(itemService).addItem(any(ItemDTO.class), eq(1L));
    }

    // ==================== 测试更新物品 ====================
    @Test
    void updateItem_resolvesUserFromBearerToken() throws Exception {
        // 1. Mock 基础依赖
        when(jwtUtil.getEmail("token")).thenReturn("user@example.com");
        when(userService.getUserIdByEmail("user@example.com")).thenReturn(2L);

        // 2. Mock Service 更新结果
        Item mockItem = new Item();
        mockItem.setId(20L);
        when(itemService.updateItem(eq(20L), any(ItemDTO.class), eq(2L))).thenReturn(mockItem);

        // 3. 构造请求参数
        ItemDTO itemDTO = new ItemDTO();
        itemDTO.setDescription("更新后的物品");

        // 4. 执行请求（注意：参数传递方式需与 Controller 一致）
        mockMvc.perform(put("/api/items/update-item")
                        .param("itemId", "20") // 路径参数
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.id").value(20));

        verify(itemService).updateItem(eq(20L), any(ItemDTO.class), eq(2L));
    }

    // ==================== 测试下架物品 ====================
    @Test
    void takeDownItem_resolvesUserFromBearerToken() throws Exception {
        // 1. Mock 基础依赖
        when(jwtUtil.getEmail("token")).thenReturn("owner@example.com");
        when(userService.getUserIdByEmail("owner@example.com")).thenReturn(3L);

        // 2. Mock Service 下架结果
        when(itemService.takeDownItem(eq(30L), eq(3L))).thenReturn(true);

        // 3. 执行请求
        mockMvc.perform(put("/api/items/take-down")
                        .param("itemId", "30")
                        .header("Authorization", "Bearer token")
                        // 无请求体时无需设置 Content-Type
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data").value(true));

        // 验证 Service 方法调用
        verify(itemService).takeDownItem(30L, 3L);
    }
}