package com.whut.lostandfoundforwhut.common.exception;

import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author DXR
 * @date 2026/01/30
 * @description 全局异常处理，统一返回结构
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 处理自定义业务异常
     * @param e 业务异常
     * @return 统一失败结果
     */
    @ExceptionHandler(AppException.class)
    public Result<Void> handleAppException(AppException e) {
        log.error("AppException", e);
        return Result.fail(e.getCode(), e.getInfo());
    }

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 处理参数校验异常
     * @param e 校验异常
     * @return 统一失败结果
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidation(Exception e) {
        log.error("Validation exception", e);
        return Result.fail(ResponseCode.ILLEGAL_PARAMETER);
    }

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 处理未捕获异常
     * @param e 未知异常
     * @return 统一失败结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return Result.fail(ResponseCode.UN_ERROR);
    }
}
