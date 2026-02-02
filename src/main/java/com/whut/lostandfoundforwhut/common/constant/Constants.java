package com.whut.lostandfoundforwhut.common.constant;

/**
 * @author DXR
 * @date 2026/01/30
 * @description 常量定义
 */
public class Constants {

    /** 逗号分隔 */
    public static final String SPLIT = ",";
    /** 冒号分隔 */
    public static final String COLON = ":";
    /** 空格 */
    public static final String SPACE = " ";
    /** 下划线 */
    public static final String UNDERLINE = "_";

    /**
     * @author DXR
     * @date 2026/01/30
     * @description Redis Key 规则
     */
    public static class RedisKey {
        /** Redis Key 前缀 */
        public static final String PREFIX = "lost_and_found:";
        /** 用户 Token Key 示例：lost_and_found:user:token:1234567890 */
        public static final String USER_TOKEN = PREFIX + "user:token:";
        /** 用户 Profile Key 示例：lost_and_found:user:profile:1234567890 */
        public static final String USER_PROFILE = PREFIX + "user:profile:";
        /** 物品详情 Key 示例：lost_and_found:item:detail:1234567890 */
        public static final String ITEM_DETAIL = PREFIX + "item:detail:";
        /** 物品列表 Key 示例：lost_and_found:item:list:1234567890 */
        public static final String ITEM_LIST = PREFIX + "item:list:";
        /** 物品布隆过滤器 Key 示例：lost_and_found:bloom:item */
        public static final String ITEM_BLOOM = PREFIX + "bloom:item";
        /** 注册验证码 Key 示例：lost_and_found:register:code:test@xx.com */
        public static final String REGISTER_CODE = PREFIX + "register:code:";
        /** 注册验证码发送频率 Key 示例：lost_and_found:register:code:rate:test@xx.com */
        public static final String REGISTER_CODE_RATE = PREFIX + "register:code:rate:";
        /** 找回密码验证码 Key 示例：lost_and_found:password:code:test@xx.com */
        public static final String PASSWORD_RESET_CODE = PREFIX + "password:code:";
        /** 找回密码验证码发送频率 Key 示例：lost_and_found:password:code:rate:test@xx.com */
        public static final String PASSWORD_RESET_CODE_RATE = PREFIX + "password:code:rate:";
        /** 登录失败次数 Key 示例：lost_and_found:login:fail:test@xx.com */
        public static final String LOGIN_FAIL_COUNT = PREFIX + "login:fail:";
        /** 登录锁定 Key 示例：lost_and_found:login:lock:test@xx.com */
        public static final String LOGIN_LOCK = PREFIX + "login:lock:";
        /** 刷新 Token Key 示例：lost_and_found:auth:refresh:xxxxx */
        public static final String REFRESH_TOKEN = PREFIX + "auth:refresh:";
        /** 刷新 Token 通过邮箱 Key 示例：lost_and_found:auth:refresh:by_email:test@xx.com */
        public static final String REFRESH_TOKEN_BY_EMAIL = PREFIX + "auth:refresh:by_email:";
    }
}
