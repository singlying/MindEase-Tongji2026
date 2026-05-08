package com.mindease.common.exception;

/**
 * 权限不足异常
 */
public class PermissionDeniedException extends BaseException {

    public PermissionDeniedException() {
        super();
    }

    public PermissionDeniedException(String msg) {
        super(msg);
    }
}

