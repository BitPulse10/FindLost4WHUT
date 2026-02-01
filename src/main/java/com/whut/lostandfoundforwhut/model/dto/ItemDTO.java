package com.whut.lostandfoundforwhut.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author Qoder
 * @date 2026/01/31
 * @description 通用物品 DTO，用于添加和更新操作
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemDTO {
    private Integer type; // 0-挂失，1-招领
    private LocalDateTime eventTime;
    private String eventPlace;
    private Integer status; // 0-有效，1-结束
    private String description;
    private String email; // 用于添加时的邮箱验证
}