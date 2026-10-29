package com.walker.config.security;

/**
 * 自审批策略拒绝异常（业务规则，不是权限点）。
 */
public class SelfApprovalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SelfApprovalException(String message) {
        super(message);
    }
}
