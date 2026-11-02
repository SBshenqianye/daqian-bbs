package com.walker.service;

import java.util.Map;

/**
 * 权限矩阵（只读）。
 *
 * <p>Phase A：仅展示“代码里的真实矩阵”，不做任何鉴权变更、不落库。
 * 角色权限来自 {@code AccountRole}/{@code Permission} 枚举，
 * 接口权限来自 Spring 已注册的 {@code @PreAuthorize} 注解。</p>
 */
public interface PermissionMatrixService {

    /** 构建完整矩阵：角色 → 权限点、权限点 → 角色、接口 → 注解。 */
    Map<String, Object> getMatrix();
}
