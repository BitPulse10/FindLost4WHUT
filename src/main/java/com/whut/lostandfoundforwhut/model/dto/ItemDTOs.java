package com.whut.lostandfoundforwhut.model.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemDTOs {
    private Integer type; // 0-挂失，1-招领
    private LocalDateTime eventTime;
    private String eventPlace;
    private Integer status; // 0-有效，1-结束
    private String description;
    /**
     * 标签文本，使用 # 分隔
     */
    private String tagText;
    /**
     * 图片ID列表
     */
    private List<Long> imageIds;
}
