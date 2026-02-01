package com.whut.lostandfoundforwhut.model.dto;

import lombok.Data;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class) // 启用蛇形命名策略
public class ItemImageAddDTO {
    private Long itemId;
    private List<Long> imageIds;
}