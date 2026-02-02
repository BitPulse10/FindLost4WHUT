package com.whut.lostandfoundforwhut.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物品元数据DTO，用于向量数据库存储额外信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemMetadata {
    /**
     * 物品状态
     */
    private Integer status;

    /**
     * 标签列表
     */
    private List<String> tags;
}