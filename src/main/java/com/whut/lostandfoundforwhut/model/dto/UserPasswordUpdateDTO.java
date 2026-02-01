package com.whut.lostandfoundforwhut.model.dto;

import lombok.Data;

/**
 * @author DXR
 * @date 2026/01/31
 * @description User password update DTO
 */
@Data
public class UserPasswordUpdateDTO {
    /** new password */
    private String password;
    /** confirm password (must match password) */
    private String confirmPassword;
}
