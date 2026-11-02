package com.walker.config.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要记录审计日志的 Controller 方法。
 *
 * <p>目标 ID 使用 SpEL 从方法参数中取值，例如：
 * {@code #articleParam.articleId}、{@code #params['reportId']}。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** 业务模块，例如 ARTICLE、REPORT、USER。 */
    String module();

    /** 操作编码，例如 ARTICLE_AUDIT、REPORT_REVIEW。 */
    String action();

    /** 目标类型，例如 ARTICLE、USER；为空表示不记录目标。 */
    String targetType() default "";

    /** 目标 ID 的 SpEL 表达式；支持字面量如 {@code 'ALL'}。 */
    String targetId() default "";

    /** 是否在方法执行前后抓取白名单快照。 */
    boolean snapshot() default true;
}
