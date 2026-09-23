package com.walker.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * #4 特殊标签名称可配置：label_type 白名单归一逻辑单测
 * 不启动 Spring，直接实例化 Controller 反射调用 private normalizeLabelType
 */
class ArticleLabelControllerTest {

    private String invokeNormalize(String labelType) throws Exception {
        ArticleLabelController controller = new ArticleLabelController();
        Method m = ArticleLabelController.class.getDeclaredMethod("normalizeLabelType", String.class);
        m.setAccessible(true);
        return (String) m.invoke(controller, labelType);
    }

    @Test
    @DisplayName("null → normal")
    void null_returnsNormal() throws Exception {
        assertEquals("normal", invokeNormalize(null));
    }

    @Test
    @DisplayName("空串/纯空白 → normal")
    void blank_returnsNormal() throws Exception {
        assertEquals("normal", invokeNormalize(""));
        assertEquals("normal", invokeNormalize("   "));
    }

    @Test
    @DisplayName("合法值原样返回，容忍首尾空白")
    void legalValues_passThrough() throws Exception {
        assertEquals("suggestion", invokeNormalize("suggestion"));
        assertEquals("question", invokeNormalize("question"));
        assertEquals("normal", invokeNormalize("normal"));
        assertEquals("suggestion", invokeNormalize("  suggestion  "));
    }

    @Test
    @DisplayName("非法值回退 normal（大小写敏感，前端须传小写）")
    void illegal_returnsNormal() throws Exception {
        assertEquals("normal", invokeNormalize("SUGGESTION"));
        assertEquals("normal", invokeNormalize("feedback"));
        assertEquals("normal", invokeNormalize("123"));
    }
}
