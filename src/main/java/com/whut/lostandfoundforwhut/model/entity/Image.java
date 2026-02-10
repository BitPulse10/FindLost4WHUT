package com.whut.lostandfoundforwhut.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author DXR
 * @date 2026/01/30
 * @description 图片实体，对应 images 表
 */
@Data
@TableName("images")
public class Image {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String url;
    
    @TableField(value = "object_key")
    private String objectKey;
}
