package com.whut.lostandfoundforwhut.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author DXR
 * @date 2026/01/30
 * @description 业务异常
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 5317680961212299217L;

    /** 异常码 */
    private String code;

    /** 异常信息 */
    private String info;

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 构造函数（仅异常码）
     * @param code 异常码
     */
    public AppException(String code) {
        this.code = code;
    }

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 构造函数（异常码 + 原因）
     * @param code 异常码
     * @param cause 原因
     */
    public AppException(String code, Throwable cause) {
        this.code = code;
        super.initCause(cause);
    }

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 构造函数（异常码 + 描述）
     * @param code 异常码
     * @param message 描述
     */
    public AppException(String code, String message) {
        this.code = code;
        this.info = message;
    }

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 构造函数（异常码 + 描述 + 原因）
     * @param code 异常码
     * @param message 描述
     * @param cause 原因
     */
    public AppException(String code, String message, Throwable cause) {
        this.code = code;
        this.info = message;
        super.initCause(cause);
    }

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 输出异常信息
     * @return 异常字符串
     */
    @Override
    public String toString() {
        return "com.whut.lostandfoundforwhut.common.exception.AppException{" +
                "code='" + code + '\'' +
                ", info='" + info + '\'' +
                '}';
    }

    /**
     * @description 获取异常信息
     * @return 异常信息
     */
    @Override
    public String getMessage() {
        return info;
    }
}
