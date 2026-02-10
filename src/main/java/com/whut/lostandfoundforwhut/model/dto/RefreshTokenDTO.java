package com.whut.lostandfoundforwhut.model.dto;

import lombok.Data;

/**
 * @author DXR
 * @date 2026/02/02
 * @description 刷新 Token DTO
 */
@Data
public class RefreshTokenDTO {
    /** refresh token */
    private String refreshToken;
}
