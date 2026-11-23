package com.walker.utils;

import java.util.Locale;
import java.util.Map;

/**
 * 原生 SQL 返回 {@code List<Map<String, Object>>} 时的取值工具。
 * <p>
 * 起因（2026-11-23 生产问题）：PostgreSQL 会把未加引号的列别名折叠成小写，
 * 例如 {@code SELECT a.user_id AS userId} 实际返回的列名是 {@code userid}，
 * 于是 {@code row.get("userId")} 恒为 null —— 功能看着“没数据”，其实 SQL 有数据。
 * MySQL 保留原样，所以同一份代码在开发库正常、在生产 PG 静默失效。
 * <p>
 * 取值时忽略大小写和下划线：{@code userId} / {@code user_id} / {@code USERID} 都能命中。
 * <p>
 * 新增聚合查询优先用 VO 接收（属性匹配大小写不敏感）；只有历史 Map 结果才用本工具。
 */
public final class MapRowUtil {

    private MapRowUtil() {
    }

    /**
     * 按驼峰键名取值，忽略大小写与下划线差异
     */
    public static Object get(Map<String, Object> row, String camelKey) {
        if (row == null || camelKey == null) {
            return null;
        }
        if (row.containsKey(camelKey)) {
            return row.get(camelKey);
        }
        String target = normalize(camelKey);
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && target.equals(normalize(entry.getKey()))) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 取整数值（Number 直接转换，字符串尝试解析，其余/失败返回 null）
     */
    public static Integer getInt(Map<String, Object> row, String camelKey) {
        Object value = get(row, camelKey);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.valueOf(((String) value).trim());
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    /**
     * 取长整数值（聚合 COUNT 在 PG 返回 bigint）
     */
    public static Long getLong(Map<String, Object> row, String camelKey) {
        Object value = get(row, camelKey);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.valueOf(((String) value).trim());
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    private static String normalize(String key) {
        return key.replace("_", "").toLowerCase(Locale.ROOT);
    }
}
