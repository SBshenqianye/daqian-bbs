package com.walker.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * PG 别名折叠（userId → userid）导致 Map 取值失败的生产问题回归用例。
 */
class MapRowUtilTest {

    @Test
    @DisplayName("PG 折叠成小写的键也能取到（userid → userId）")
    void get_lowercaseLabel_matches() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("userid", 7);
        row.put("articlecount", 3L);

        assertEquals(7, MapRowUtil.get(row, "userId"));
        assertEquals(3L, MapRowUtil.get(row, "articleCount"));
    }

    @Test
    @DisplayName("下划线与大写键同样能命中")
    void get_underscoreAndUpperCase_matches() {
        Map<String, Object> row = new HashMap<>();
        row.put("USER_ID", 9);

        assertEquals(9, MapRowUtil.get(row, "userId"));
        assertEquals(9, MapRowUtil.get(row, "user_id"));
    }

    @Test
    @DisplayName("原样 camelCase 键优先，不被其他键干扰")
    void get_exactKey_preferred() {
        Map<String, Object> row = new HashMap<>();
        row.put("userId", 1);
        row.put("otherid", 2);

        assertEquals(1, MapRowUtil.get(row, "userId"));
        assertNull(MapRowUtil.get(row, "missingKey"));
        assertNull(MapRowUtil.get(null, "userId"));
    }

    @Test
    @DisplayName("数值转换：Number 与字符串都能取，非法字符串返回 null")
    void getIntAndLong_convert() {
        Map<String, Object> row = new HashMap<>();
        row.put("userid", "12");
        row.put("total", 34L);

        assertEquals(Integer.valueOf(12), MapRowUtil.getInt(row, "userId"));
        assertEquals(Long.valueOf(34L), MapRowUtil.getLong(row, "total"));
        assertNull(MapRowUtil.getInt(row, "missingKey"));
    }
}
