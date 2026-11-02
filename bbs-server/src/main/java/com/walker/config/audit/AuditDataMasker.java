package com.walker.config.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 审计日志脱敏与 JSON 序列化。
 *
 * <p>密码、Token、Cookie 等敏感字段统一替换为 {@code ***}；
 * 手机号/身份证号保留后 4 位。落库前必须经过本类。</p>
 */
@Component
public class AuditDataMasker {

    private static final Logger log = LoggerFactory.getLogger(AuditDataMasker.class);

    private static final int MAX_JSON_LENGTH = 64 * 1024;

    /** 归一化后的敏感 key：小写并去掉下划线/短横线。 */
    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
            "password", "oldpassword", "newpassword", "pwd", "passwd",
            "token", "tokenstr", "accesstoken", "refreshtoken", "authorization",
            "cookie", "setcookie", "secret", "clientsecret", "privatekey"
    ));

    /** 归一化后需要保留后 4 位的 key。 */
    private static final Set<String> PARTIAL_MASK_KEYS = new HashSet<>(Arrays.asList(
            "phone", "mobile", "telephone", "idcard", "identitycard", "idno"
    ));

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 转为已脱敏 JSON。任何异常都返回 null，避免影响审计主流程。
     */
    public String toSafeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            JsonNode node = objectMapper.valueToTree(value);
            mask(node);
            String json = objectMapper.writeValueAsString(node);
            return truncate(json, MAX_JSON_LENGTH);
        } catch (Exception e) {
            log.warn("[审计日志] 快照序列化失败，已忽略: {}", e.getMessage());
            return null;
        }
    }

    private void mask(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            List<String> fieldNames = new ArrayList<>();
            Iterator<String> names = object.fieldNames();
            while (names.hasNext()) {
                fieldNames.add(names.next());
            }
            for (String name : fieldNames) {
                String normalized = normalize(name);
                JsonNode child = object.get(name);
                if (SENSITIVE_KEYS.contains(normalized)) {
                    object.put(name, "***");
                } else if (PARTIAL_MASK_KEYS.contains(normalized)) {
                    object.put(name, partialMask(child == null ? null : child.asText()));
                } else {
                    mask(child);
                }
            }
        } else if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            for (JsonNode child : array) {
                mask(child);
            }
        }
    }

    private String partialMask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 4) {
            return "***";
        }
        return "***" + trimmed.substring(trimmed.length() - 4);
    }

    private String normalize(String key) {
        if (key == null) {
            return "";
        }
        return key.toLowerCase().replace("_", "").replace("-", "");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 对外暴露给测试/调用方：脱敏 Map 并返回 JSON。
     */
    public String toSafeJson(Map<String, Object> value) {
        return toSafeJson((Object) value);
    }
}
