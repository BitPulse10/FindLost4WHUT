package com.whut.lostandfoundforwhut.model.dto;

import lombok.Data;

/**
 * @author DXR
 * @date 2026/02/02
 * @description 找回密码验证码发送 DTO
 */
@Data
public class PasswordResetCodeSendDTO {
    /** 邮箱 */
    private String email;
}
