package com.whut.lostandfoundforwhut.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * @author DXR
 * @date 2026/02/02
 * @description 注册邮箱验证码发送 DTO
 */
@Data
public class RegisterCodeSendDTO {
    /** 邮箱 */
    private String email;
    /** 腾讯验证码票据 */
    @JsonAlias({"ticket", "captcha_ticket"})
    private String captchaTicket;
    /** 腾讯验证码随机串 */
    @JsonAlias({"randstr", "captcha_randstr"})
    private String captchaRandstr;
}
