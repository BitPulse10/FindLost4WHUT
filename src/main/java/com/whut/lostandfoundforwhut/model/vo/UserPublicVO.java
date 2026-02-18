package com.whut.lostandfoundforwhut.model.vo;

import com.whut.lostandfoundforwhut.model.entity.User;
import lombok.Data;

/**
 * 用户公开信息视图对象（用于列表/详情展示）。
 */
@Data
public class UserPublicVO {
    private Long id;
    private String nickname;
    private String email;

    public static UserPublicVO from(User user) {
        UserPublicVO vo = new UserPublicVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        return vo;
    }
}
