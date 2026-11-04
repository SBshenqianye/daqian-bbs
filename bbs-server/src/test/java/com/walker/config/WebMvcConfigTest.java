package com.walker.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 跨域配置回归测试。
 *
 * <p>浏览器发出的 DELETE 请求会携带 Origin，若 CORS 白名单缺少 DELETE，
 * 请求会在进入权限校验前被直接拒绝（403 Invalid CORS request），
 * 表现为“超级管理员没有权限删除”。</p>
 */
class WebMvcConfigTest {

    @Test
    @DisplayName("CORS 允许 DELETE，避免浏览器删除请求被提前拒绝")
    void corsConfiguration_allowsDelete() {
        List<String> methods = new WebMvcConfig().corsConfiguration().getAllowedMethods();

        assertTrue(methods != null && methods.stream().anyMatch("DELETE"::equalsIgnoreCase),
                "CORS 必须允许 DELETE");
        assertTrue(methods.stream().anyMatch("GET"::equalsIgnoreCase));
        assertTrue(methods.stream().anyMatch("POST"::equalsIgnoreCase));
        assertTrue(methods.stream().anyMatch("PUT"::equalsIgnoreCase));
    }

    @Test
    @DisplayName("CORS 允许携带凭证时不能使用通配源，必须使用 originPattern")
    void corsConfiguration_usesOriginPatternWithCredentials() {
        CorsConfiguration config = new WebMvcConfig().corsConfiguration();

        assertTrue(Boolean.TRUE.equals(config.getAllowCredentials()));
        assertTrue(config.getAllowedOriginPatterns() != null
                && !config.getAllowedOriginPatterns().isEmpty());
    }
}
