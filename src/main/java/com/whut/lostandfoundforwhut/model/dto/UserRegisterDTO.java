package com.whut.lostandfoundforwhut.model.dto;

import lombok.Data;

/**
 * @author DXR
 * @date 2026/02/02
 * @description 用户注册 DTO（含邮箱验证码）
 */
@Data
public class UserRegisterDTO {
    /** 邮箱（唯一） */
    private String email;
    /** 明文密码（仅用于注册，入库前加密） */
    private String password;
    /** confirm password (must match password) */
    private String confirmPassword;
    /** 邮箱验证码（4位） */
    private String code;
    /** 昵称 */
    private String nickname;
}
