package com.walker.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要自动归一化的文件路径字段。
 * <p>
 * 配合 {@link com.walker.config.FilePathNormalizationAdvice} 使用，
 * 在 Controller 返回响应时自动对标记的字段执行路径归一化。
 * </p>
 *
 * <pre>
 * 用法：
 *   &#64;NormalizeFilePath                    // 单路径字段 → normalizeFieldUrl
 *   &#64;NormalizeFilePath(embedded = true)   // 嵌入内容 → normalizeEmbeddedUrls
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NormalizeFilePath {

    /**
     * 是否是嵌入在 Markdown/HTML 内容中的路径。
     * true  → 调用 {@code normalizeEmbeddedUrls}（全文替换）
     * false → 调用 {@code normalizeFieldUrl}（单路径加前缀）
     */
    boolean embedded() default false;
}
