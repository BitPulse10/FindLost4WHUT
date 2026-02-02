package com.whut.lostandfoundforwhut.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文本嵌入DTO
 */
@Data
@Builder
public class TextEmbeddingDTO {
    /**
     * 文本ID
     */
    private String id;

    /**
     * 文本内容
     */
    private String text;

    /**
     * 元数据
     */
    private Object metadata;
}