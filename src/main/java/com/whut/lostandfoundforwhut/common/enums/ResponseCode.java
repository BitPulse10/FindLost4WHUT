package com.whut.lostandfoundforwhut.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author DXR
 * @date 2026/01/30
 * @description 响应状态码枚举类
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ResponseCode {

    SUCCESS("0000", "成功"),
    UN_ERROR("0001", "未知错误"),
    ILLEGAL_PARAMETER("0002", "非法参数"),
    NOT_LOGIN("0003", "未登录"),
    NO_PERMISSION("0004", "无权限"),
    RESOURCE_NOT_FOUND("0005", "资源不存在"),
    DUPLICATE_OPERATION("0006", "重复操作"),

    USER_NOT_FOUND("USR_001", "用户不存在"),
    USER_STATUS_INVALID("USR_002", "用户状态无效"),
    USER_EMAIL_EXISTS("USR_003", "邮箱已存在"),
    USER_EMAIL_CODE_INVALID("USR_004", "邮箱验证码无效"),
    USER_EMAIL_CODE_EXPIRED("USR_005", "邮箱验证码已过期"),
    USER_EMAIL_CODE_RATE_LIMIT("USR_006", "验证码发送过于频繁"),
    USER_PASSWORD_ERROR("USR_007", "Email or password error"),
    USER_LOGIN_LOCKED("USR_008", "Too many failed attempts, try again in 5 minutes"),
    USER_REFRESH_TOKEN_INVALID("USR_009", "Refresh token invalid"),
    USER_PASSWORD_CODE_INVALID("USR_010", "Password reset code invalid"),
    USER_PASSWORD_CODE_EXPIRED("USR_011", "Password reset code expired"),
    USER_PASSWORD_CODE_RATE_LIMIT("USR_012", "Password reset code sent too frequently"),

    MAIL_CONFIG_INVALID("MAIL_001", "邮箱配置无效"),
    MAIL_SEND_FAILED("MAIL_002", "邮件发送失败"),

    ITEM_NOT_FOUND("ITEM_001", "物品不存在"),
    ITEM_STATUS_INVALID("ITEM_002", "物品状态无效"),
    ITEM_CLAIM_CONFLICT("ITEM_003", "物品认领冲突"),
    ;

    private String code;
    private String info;
}

