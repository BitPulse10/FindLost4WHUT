package com.whut.lostandfoundforwhut.model.dto;

import lombok.Data;

/**
 * @author DXR
 * @date 2026/02/02
 * @description 用户登录 DTO
 */
@Data
public class UserLoginDTO {
    /** 邮箱 */
    private String email;
    /** 密码 */
    private String password;
}
