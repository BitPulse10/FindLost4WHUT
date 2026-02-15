package com.whut.lostandfoundforwhut.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物品筛选DTO
 * 
 * @author Qoder
 * @date 2026/02/03
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemFilterDTO {

    // 分页参数
    private Integer pageNo = 1;
    private Integer pageSize = 10;

    // 原有过滤参数
    private Integer type; // 0-挂失，1-招领，2-卡证
    private Integer status;

    // 标签列表
    private List<String> tags;

    // 标签筛选模式：false=广泛筛选（命中任一标签），true=精确筛选（必须命中全部标签）
    private Boolean preciseTagMatch = false;

    // 时间段筛选
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private SearchDTO searchDTO;
}
