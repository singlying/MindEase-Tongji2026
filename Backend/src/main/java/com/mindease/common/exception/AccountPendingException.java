package com.mindease.common.exception;

/**
 * 账号待审核异常
 */
public class AccountPendingException extends BaseException {

    public AccountPendingException() {
    }

    public AccountPendingException(String msg) {
        super(msg);
    }
}

