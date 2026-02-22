package com.whut.lostandfoundforwhut.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * @author DXR
 * @date 2026/02/02
 * @description 用户登录 DTO
 */
@Data
public class UserLoginDTO {
    /** 邮箱 */
    private String email;
    /** 密码 */
    private String password;
    /** 腾讯验证码票据 */
    @JsonAlias({"ticket", "captcha_ticket"})
    private String captchaTicket;
    /** 腾讯验证码随机串 */
    @JsonAlias({"randstr", "captcha_randstr"})
    private String captchaRandstr;
}
