package com.whut.lostandfoundforwhut.model.dto;

import lombok.Data;

/**
 * @author DXR
 * @date 2026/02/02
 * @description 找回密码 DTO
 */
@Data
public class PasswordResetDTO {
    /** ?? */
    private String email;
    /** 邮箱 */
    private String password;
    /** confirm password */
    private String confirmPassword;
    /** 邮箱验证码（4位） */
    private String code;
}
