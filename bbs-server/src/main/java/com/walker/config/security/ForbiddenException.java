package com.walker.config.security;

/**
 * 权限不足异常。由全局异常处理器转换为 ResultBean(code=403)。
 */
public class ForbiddenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super(message);
    }
}
