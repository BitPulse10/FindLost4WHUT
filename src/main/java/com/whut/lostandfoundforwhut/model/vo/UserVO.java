package com.whut.lostandfoundforwhut.model.vo;

import com.whut.lostandfoundforwhut.model.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author DXR
 * @date 2026/01/31
 * @description 用户视图对象（对外返回）
 */
@Data
public class UserVO {
    private Long id;
    private String email;
    private String nickname;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 登录后返回的 Token（可空） */
    private String token;

    /**
     * @author DXR
     * @date 2026/01/31
     * @description 将用户实体转换为视图对象，并可携带登录 Token
     * @param user  用户实体
     * @param token 登录后返回的 Token（可为空）
     * @return 用户视图对象
     */
    public static UserVO from(User user, String token) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        vo.setToken(token);
        return vo;
    }
}
