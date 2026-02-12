package com.whut.lostandfoundforwhut.model.vo;

import com.whut.lostandfoundforwhut.model.entity.Item;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物品详情视图对象，用于二级面板展示。
 */
@Data
public class ItemDetailVO {
    private Long id;
    private Long userId;
    private Integer type;
    private LocalDateTime eventTime;
    private String eventPlace;
    private Integer status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> tags;
    private List<String> imageUrls;
    private UserPublicVO publisher;

    public static ItemDetailVO from(Item item, List<String> tags, List<String> imageUrls, UserPublicVO publisher) {
        ItemDetailVO vo = new ItemDetailVO();
        vo.setId(item.getId());
        vo.setUserId(item.getUserId());
        vo.setType(item.getType());
        vo.setEventTime(item.getEventTime());
        vo.setEventPlace(item.getEventPlace());
        vo.setStatus(item.getStatus());
        vo.setDescription(item.getDescription());
        vo.setCreatedAt(item.getCreatedAt());
        vo.setUpdatedAt(item.getUpdatedAt());
        vo.setTags(tags);
        vo.setImageUrls(imageUrls);
        vo.setPublisher(publisher);
        return vo;
    }
}
