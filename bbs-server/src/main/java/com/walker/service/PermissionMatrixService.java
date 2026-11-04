package com.walker.service;

import java.util.Map;

/**
 * 权限矩阵（只读）。
 *
 * <p>仅展示“代码与权限组配置里的真实矩阵”，不做任何鉴权变更、不落库。
 * 权限点来自 {@code Permission} 枚举，权限组配置来自权限组表，
 * 接口权限来自 Spring 已注册的 {@code @PreAuthorize} 注解。</p>
 */
public interface PermissionMatrixService {

    /** 构建完整矩阵：权限组 → 权限点、权限点 → 权限组、接口 → 注解。 */
    Map<String, Object> getMatrix();
}
