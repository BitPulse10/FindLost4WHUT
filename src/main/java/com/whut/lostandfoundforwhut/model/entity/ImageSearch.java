package com.whut.lostandfoundforwhut.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;

import lombok.Data;

/**
 * 图搜临时图片实体类
 */
@Data
public class ImageSearch {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "url")
    private String url;
    
    @TableField(value = "object_key")
    private String objectKey;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(value = "expire_time", fill = FieldFill.INSERT)
    private LocalDateTime expireTime;
}
