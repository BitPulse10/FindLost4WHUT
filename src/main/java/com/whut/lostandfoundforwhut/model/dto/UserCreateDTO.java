package com.whut.lostandfoundforwhut.model.dto;

import lombok.Data;

/**
 * @author DXR
 * @date 2026/01/31
 * @description 用户创建 DTO
 */
@Data
public class UserCreateDTO {
    /** 邮箱（唯一） */
    private String email;
    /** 明文密码（仅用于创建，入库前加密） */
    private String password;
    /** confirm password (must match password) */
    private String confirmPassword;
    /** 昵称 */
    private String nickname;
}
