package com.whut.lostandfoundforwhut.model.dto;

import lombok.Data;

/**
 * @author DXR
 * @date 2026/02/02
 * @description 用户修改密码 DTO（验证码）
 */
@Data
public class UserPasswordUpdateByCodeDTO {
    /** 新密码 */
    private String password;
    /** confirm password */
    private String confirmPassword;
    /** 邮箱验证码（4位） */
    private String code;
}
