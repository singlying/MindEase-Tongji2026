package com.mindease.common.exception;

/**
 * 用户名已存在异常
 */
public class UsernameAlreadyExistsException extends BaseException {

    public UsernameAlreadyExistsException() {
    }

    public UsernameAlreadyExistsException(String msg) {
        super(msg);
    }
}

