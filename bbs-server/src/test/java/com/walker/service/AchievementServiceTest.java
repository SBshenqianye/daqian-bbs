package com.walker.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AchievementServiceTest {

    private final AchievementService service = new AchievementService();

    @Test
    @DisplayName("嵌套规则：并集里套交集，编辑后仍可正确评估")
    void nestedAnyOfAll() {
        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("article_count", 10);
        metrics.put("featured_count", 1);
        metrics.put("points_total", 300);

        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("logic", "ANY");
        List<Map<String, Object>> groups = new ArrayList<>();
        groups.add(group("ALL", condition("article_count", ">=", 50)));
        groups.add(group("ALL", condition("featured_count", ">=", 10)));
        groups.add(group("ALL", condition("points_total", ">=", 100)));
        rule.put("groups", groups);

        assertTrue(service.evalRule(rule, metrics));
    }

    @Test
    @DisplayName("嵌套规则：交集不满足时返回 false")
    void nestedAllOfAll() {
        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("article_count", 10);
        metrics.put("reply_count", 5);

        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("logic", "ALL");
        List<Map<String, Object>> groups = new ArrayList<>();
        groups.add(group("ALL", condition("article_count", ">=", 10)));
        groups.add(group("ALL", condition("reply_count", ">=", 50)));
        rule.put("groups", groups);

        assertFalse(service.evalRule(rule, metrics));
    }

    @Test
    @DisplayName("勋章模板：燕尾缺口高度约为主体高度的 18%，符合 15%-20% 规格")
    void medalTemplateNotchRatio() {
        String svg = service.medalTemplate();
        assertTrue(svg.contains("L128 182"), "模板应使用 18% 左右的燕尾缺口");
        assertFalse(svg.contains("L128 170"), "旧模板缺口约 24.7%，应已替换");
    }

    private Map<String, Object> group(String logic, Map<String, Object> condition) {
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("logic", logic);
        group.put("conditions", Arrays.asList(condition));
        return group;
    }

    private Map<String, Object> condition(String metric, String op, int value) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("metric", metric);
        c.put("op", op);
        c.put("value", value);
        return c;
    }
}
