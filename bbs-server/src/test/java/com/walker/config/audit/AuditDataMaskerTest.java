package com.walker.config.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审计脱敏测试：密码、Token 等敏感字段不得落库。
 */
class AuditDataMaskerTest {

    private AuditDataMasker masker;

    @BeforeEach
    void setUp() throws Exception {
        masker = new AuditDataMasker();
        Field field = AuditDataMasker.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(masker, new ObjectMapper());
    }

    @Test
    @DisplayName("密码、Token、嵌套密码全部脱敏")
    void passwordAndTokenAreMasked() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("newPassword", "NestedSecret!");
        nested.put("normal", "keep-me");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", "tester");
        data.put("password", "PlainPassword123");
        data.put("oldPassword", "OldPassword123");
        data.put("token", "jwt-token-value");
        data.put("tokenStr", "another-token");
        data.put("nested", nested);

        String json = masker.toSafeJson(data);

        assertNotNull(json);
        assertFalse(json.contains("PlainPassword123"));
        assertFalse(json.contains("OldPassword123"));
        assertFalse(json.contains("jwt-token-value"));
        assertFalse(json.contains("another-token"));
        assertFalse(json.contains("NestedSecret!"));
        assertTrue(json.contains("\"username\":\"tester\""));
        assertTrue(json.contains("keep-me"));
        assertTrue(json.contains("***"));
    }

    @Test
    @DisplayName("手机号/身份证号保留后 4 位")
    void phoneAndIdCardKeepLastFour() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("phone", "13800138000");
        data.put("idCard", "110101199001011234");

        String json = masker.toSafeJson(data);

        assertNotNull(json);
        assertFalse(json.contains("13800138000"));
        assertFalse(json.contains("110101199001011234"));
        assertTrue(json.contains("***8000"));
        assertTrue(json.contains("***1234"));
    }

    @Test
    @DisplayName("null 与序列化异常不影响调用方")
    void nullAndUnserializableAreSafe() {
        assertNull(masker.toSafeJson((Object) null));

        Object bad = new Object() {
            public String getValue() {
                throw new IllegalStateException("boom");
            }
        };
        assertDoesNotThrow(() -> masker.toSafeJson(bad));
    }
}
