package com.mindease.handler;

import com.mindease.common.exception.*;
import com.mindease.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(BaseException.class)
    public Result<Void> exceptionHandler(BaseException ex) {
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获账号待审核异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(AccountPendingException.class)
    public Result<Void> exceptionHandler(AccountPendingException ex) {
        log.error("账号待审核：{}", ex.getMessage());
        return Result.error(403, ex.getMessage());
    }

    /**
     * 捕获账号被锁定异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(AccountLockedException.class)
    public Result<Void> exceptionHandler(AccountLockedException ex) {
        log.error("账号被锁定：{}", ex.getMessage());
        return Result.error(403, ex.getMessage());
    }

    /**
     * 捕获其他异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> exceptionHandler(Exception ex) {
        log.error("异常信息：{}", ex.getMessage(), ex);
        return Result.error("服务器内部错误");
    }
}

