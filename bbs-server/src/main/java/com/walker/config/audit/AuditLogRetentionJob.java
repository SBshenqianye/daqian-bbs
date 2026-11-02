package com.walker.config.audit;

import com.walker.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 审计日志保留策略：默认每天 03:30 清理 180 天前的记录。
 *
 * <p>可通过 {@code bbs.audit.*} 配置关闭或调整；清理失败只打 warn。</p>
 */
@Component
public class AuditLogRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(AuditLogRetentionJob.class);

    @Value("${bbs.audit.retention-enabled:true}")
    private boolean enabled;

    @Value("${bbs.audit.retention-days:180}")
    private int retentionDays;

    @Autowired
    private AuditLogService auditLogService;

    @Scheduled(cron = "${bbs.audit.retention-cron:0 30 3 * * ?}")
    public void purge() {
        if (!enabled || retentionDays <= 0) {
            return;
        }
        try {
            long cutoffMillis = System.currentTimeMillis()
                    - retentionDays * 24L * 60L * 60L * 1000L;
            String cutoff = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(cutoffMillis));
            int deleted = auditLogService.purgeBefore(cutoff);
            if (deleted > 0) {
                log.info("[审计日志] 已清理 {} 条早于 {} 的记录", deleted, cutoff);
            }
        } catch (Exception e) {
            log.warn("[审计日志] 定时清理失败，已忽略: {}", e.getMessage());
        }
    }
}
