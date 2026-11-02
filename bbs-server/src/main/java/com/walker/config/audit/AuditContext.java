package com.walker.config.audit;

import java.util.Map;

/**
 * 审计上下文：用于业务代码补充切面无法自动推断的信息（如动态 action、创建后的目标 ID）。
 *
 * <p>切面在 finally 中统一清理，避免线程池复用导致串数据。</p>
 */
public final class AuditContext {

    private static final ThreadLocal<Holder> HOLDER = new ThreadLocal<>();

    private AuditContext() {
    }

    public static void setAction(String action) {
        holder().action = action;
    }

    public static void setTargetId(String targetId) {
        holder().targetId = targetId;
    }

    public static void setBefore(Map<String, Object> before) {
        holder().before = before;
    }

    public static void setAfter(Map<String, Object> after) {
        holder().after = after;
    }

    static String getAction() {
        Holder holder = HOLDER.get();
        return holder == null ? null : holder.action;
    }

    static String getTargetId() {
        Holder holder = HOLDER.get();
        return holder == null ? null : holder.targetId;
    }

    static Map<String, Object> getBefore() {
        Holder holder = HOLDER.get();
        return holder == null ? null : holder.before;
    }

    static Map<String, Object> getAfter() {
        Holder holder = HOLDER.get();
        return holder == null ? null : holder.after;
    }

    public static void clear() {
        HOLDER.remove();
    }

    private static Holder holder() {
        Holder holder = HOLDER.get();
        if (holder == null) {
            holder = new Holder();
            HOLDER.set(holder);
        }
        return holder;
    }

    private static class Holder {
        private String action;
        private String targetId;
        private Map<String, Object> before;
        private Map<String, Object> after;
    }
}
