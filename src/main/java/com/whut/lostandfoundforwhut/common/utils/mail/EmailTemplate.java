package com.whut.lostandfoundforwhut.common.utils.mail;

/**
 * @author DXR
 * @date 2026/02/02
 * @description 邮件模板工具
 */
public class EmailTemplate {

    private EmailTemplate() {
    }

    /**
     * 构建注册验证码邮件标题
     * @return 邮件标题
     */
    public static String registerCodeSubject() {
        return "注册验证码";
    }

    /**
     * 构建注册验证码邮件内容
     * @param code 4位验证码
     * @param validSeconds 有效期（秒）
     * @return 邮件内容
     */
    public static String registerCodeBody(String code, long validSeconds) {
        return "欢迎使用武理智寻。\n"
                + "您的注册验证码为：" + code + "，有效期 " + validSeconds + " 秒。\n"
                + "如非本人操作请忽略。";
    }

    /**
     * 构建找回密码邮件标题
     * @return 邮件标题
     */
    public static String passwordResetSubject() {
        return "找回密码验证码";
    }

    /**
     * 构建找回密码邮件内容
     * @param code 4位验证码
     * @param validSeconds 有效期（秒）
     * @return 邮件内容
     */
    public static String passwordResetBody(String code, long validSeconds) {
        return "您的找回密码验证码为：" + code + "，有效期 " + validSeconds + " 秒。\n"
                + "如非本人操作请忽略。";
    }
}
