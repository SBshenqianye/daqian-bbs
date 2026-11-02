package com.walker.service;

import java.util.Map;

/**
 * 审计快照解析器：按目标类型读取白名单字段，供 before/after 记录使用。
 */
public interface AuditSnapshotService {

    /**
     * @param targetType 目标类型，例如 ARTICLE / USER / REPORT
     * @param targetId   目标 ID；批量操作可传 ALL
     * @return 白名单字段 Map；无法解析时返回 null
     */
    Map<String, Object> snapshot(String targetType, String targetId);
}
