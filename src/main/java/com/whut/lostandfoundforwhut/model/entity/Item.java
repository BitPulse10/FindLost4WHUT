package com.whut.lostandfoundforwhut.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author DXR
 * @date 2026/01/30
 * @description 物品实体，对应 items 表
 */
@Data
@TableName("items")
public class Item {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private Integer type;

    @TableField("event_time")
    private LocalDateTime eventTime;

    @TableField("event_place")
    private String eventPlace;

    private Integer status;

    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;

    private String description;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 标签名称列表，仅用于响应展示
     */
    @TableField(exist = false)
    private List<String> tags;
}
