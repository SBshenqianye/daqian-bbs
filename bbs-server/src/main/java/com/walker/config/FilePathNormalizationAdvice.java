package com.walker.config;

import com.walker.annotation.NormalizeFilePath;
import com.walker.utils.FilePathNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 统一文件路径归一化 Advice。
 * <p>
 * 拦截所有 {@code @ControllerAdvice} 范围内的 Controller 响应，
 * 递归扫描含 {@link NormalizeFilePath} 注解的字段并自动执行路径归一化。
 * </p>
 *
 * <pre>
 * 工作流程：
 *   1. supports() 始终返回 true（对所有 Controller 响应生效）
 *   2. beforeBodyWrite() 对返回值递归扫描
 *   3. 对 POJO 反射遍历字段，找到带 @NormalizeFilePath 的 String 字段
 *   4. embedded=true  → normalizeEmbeddedUrls（全文替换 /files/→/bbs-server/files/）
 *   5. embedded=false → normalizeFieldUrl（单路径加 context-path 前缀）
 * </pre>
 */
@ControllerAdvice
public class FilePathNormalizationAdvice implements ResponseBodyAdvice<Object> {

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        // 对所有响应生效
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body == null || contextPath == null || contextPath.isEmpty()) {
            return body;
        }
        normalize(body, Collections.newSetFromMap(new IdentityHashMap<>()));
        return body;
    }

    /**
     * 递归扫描并归一化对象中的文件路径字段。
     */
    @SuppressWarnings("unchecked")
    private void normalize(Object obj, Set<Object> visited) {
        if (obj == null) return;
        if (!visited.add(obj)) return; // 防止循环引用

        // Collection → 每个元素递归
        if (obj instanceof Collection) {
            for (Object item : (Collection<Object>) obj) {
                normalize(item, visited);
            }
            return;
        }

        // Map → values 递归
        if (obj instanceof Map) {
            for (Object val : ((Map<Object, Object>) obj).values()) {
                normalize(val, visited);
            }
            return;
        }

        // 基本类型、枚举、数组 → 跳过
        if (isSimpleType(obj.getClass())) return;

        // POJO → 反射遍历字段
        Class<?> clazz = obj.getClass();
        // 处理代理/增强类（如 MyBatis/Spring 代理）
        while (clazz != null && clazz.getName().startsWith("com.walker")) {
            for (Field field : clazz.getDeclaredFields()) {
                NormalizeFilePath ann = field.getAnnotation(NormalizeFilePath.class);
                if (ann == null) continue;
                if (field.getType() != String.class) continue;

                field.setAccessible(true);
                try {
                    String value = (String) field.get(obj);
                    if (value == null || value.isEmpty()) continue;

                    String normalized;
                    if (ann.embedded()) {
                        normalized = FilePathNormalizer.normalizeEmbeddedUrls(value, contextPath);
                    } else {
                        normalized = FilePathNormalizer.normalizeFieldUrl(value, contextPath);
                    }

                    if (!normalized.equals(value)) {
                        field.set(obj, normalized);
                    }
                } catch (IllegalAccessException e) {
                    // 忽略无法访问的字段
                }
            }
            clazz = clazz.getSuperclass();
        }

        // 继续递归嵌套的 com.walker 对象（VO 中引用其他 VO）
        // 但为避免重复扫描已访问对象，visited 会阻止循环
        clazz = obj.getClass();
        while (clazz != null && clazz.getName().startsWith("com.walker")) {
            for (Field field : clazz.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (fieldType.getName().startsWith("com.walker")
                        && !fieldType.isEnum()
                        && !isSimpleType(fieldType)) {
                    field.setAccessible(true);
                    try {
                        normalize(field.get(obj), visited);
                    } catch (IllegalAccessException e) {
                        // 忽略
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * 判断是否为简单类型（不递归扫描其内部字段）。
     */
    private boolean isSimpleType(Class<?> clazz) {
        if (clazz.isPrimitive()) return true;
        if (clazz.isEnum()) return true;
        if (clazz.getName().startsWith("java.lang")) return true;
        if (clazz.getName().startsWith("java.math")) return true;
        if (clazz.getName().startsWith("java.time")) return true;
        if (clazz.getName().startsWith("java.util.Date")) return true;
        // 数组类型（byte[]、String[] 等）
        if (clazz.isArray()) return true;
        return false;
    }
}
