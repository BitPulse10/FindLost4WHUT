package com.whut.lostandfoundforwhut.model.vo;

import com.whut.lostandfoundforwhut.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author DXR
 * @date 2026/02/02
 * @description 登录结果
 */
@Data
@AllArgsConstructor
public class AuthLoginResult {
    private User user;
    private String token;
    private String refreshToken;
}
