package com.mianmiantong.common;

/**
 * 已登录但无权执行该操作。由 {@code GlobalExceptionHandler} 映射为 HTTP 403。
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
