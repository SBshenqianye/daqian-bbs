package com.walker.service;

import com.walker.pojo.AuditLog;

import java.util.Map;

/**
 * 审计日志服务。
 */
public interface AuditLogService {

    /**
     * 写入一条审计日志。实现必须吞掉异常，不能影响业务主流程。
     */
    void record(AuditLog log);

    /**
     * 分页查询审计日志。
     */
    Map<String, Object> list(Integer page, Integer size,
                             String module, String action,
                             Integer operatorId, String operatorName,
                             String result, String startTime, String endTime,
                             String targetType, String targetId);

    /**
     * 清理早于指定时间的审计日志（保留策略）。
     *
     * @param cutoff 截止时间 yyyy-MM-dd HH:mm:ss（小于该时间的记录会被删除）
     * @return 删除条数
     */
    int purgeBefore(String cutoff);
}
