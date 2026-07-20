package com.walker.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件上传安全校验工具类
 *
 * 提供文件类型白名单、魔术字节校验、文件大小限制。
 * 后续可改为从数据字典/数据库读取配置。
 */
public class FileValidationUtil {

    private FileValidationUtil() {}

    /** 附件允许的扩展名白名单 */
    private static final Set<String> ALLOWED_ATTACHMENT_EXTENSIONS = new HashSet<>(Arrays.asList(
            // 文档
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv",
            // 图片
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            // 压缩包
            "zip", "rar", "7z", "tar", "gz"
    ));

    /** 图片上传大小限制：10MB */
    public static final long IMAGE_MAX_SIZE = 10 * 1024 * 1024L;

    /** 附件上传大小限制：50MB */
    public static final long ATTACHMENT_MAX_SIZE = 50 * 1024 * 1024L;

    /**
     * 已知图片魔术字节前缀（用于快速否定，不完全精确）
     */
    private static final byte[] MAGIC_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] MAGIC_PNG  = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] MAGIC_GIF  = {0x47, 0x49, 0x46};
    private static final byte[] MAGIC_BMP  = {0x42, 0x4D};
    // WebP: RIFF....WEBP，检查 RIFF 头 + WEBP 尾，需要 12 字节

    /**
     * 校验图片文件：魔术字节 + 扩展名 + 大小
     *
     * @param file          上传文件
     * @param allowedExts   允许的扩展名（小写，如 "jpg","png"），传 null 则只校验魔术字节
     * @param maxSize       最大字节数
     * @return null 校验通过，非 null 为错误消息
     */
    public static String validateImage(MultipartFile file, Set<String> allowedExts, long maxSize) {
        if (file == null || file.isEmpty()) {
            return "文件不能为空";
        }
        if (file.getSize() > maxSize) {
            return "文件大小超过限制（最大 " + (maxSize / (1024 * 1024)) + "MB）";
        }
        // 扩展名校验
        String ext = getExtension(file.getOriginalFilename());
        if (ext == null) {
            return "无法识别文件类型";
        }
        if (allowedExts != null && !allowedExts.contains(ext)) {
            return "不支持的图片格式（支持：" + String.join(", ", allowedExts) + "）";
        }
        // 魔术字节校验
        if (!matchesImageMagicBytes(file)) {
            return "文件内容与图片格式不符，可能已被篡改";
        }
        return null;
    }

    /**
     * 校验附件文件：扩展名白名单 + 大小
     *
     * @param file 上传文件
     * @return null 校验通过，非 null 为错误消息
     */
    public static String validateAttachment(MultipartFile file) {
        return validateAttachment(file, ALLOWED_ATTACHMENT_EXTENSIONS, ATTACHMENT_MAX_SIZE);
    }

    /**
     * 校验附件文件（可自定义白名单和大小）
     */
    public static String validateAttachment(MultipartFile file, Set<String> allowedExts, long maxSize) {
        if (file == null || file.isEmpty()) {
            return "文件不能为空";
        }
        if (file.getSize() > maxSize) {
            return "附件大小超过限制（最大 " + (maxSize / (1024 * 1024)) + "MB）";
        }
        String ext = getExtension(file.getOriginalFilename());
        if (ext == null) {
            return "无法识别文件类型";
        }
        if (allowedExts != null && !allowedExts.contains(ext)) {
            return "不支持的附件格式（支持：" + String.join(", ", allowedExts) + "）";
        }
        return null;
    }

    /**
     * 从文件名提取小写扩展名（不含点）
     */
    public static String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) return null;
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot >= filename.length() - 1) return null;
        return filename.substring(dot + 1).toLowerCase().trim();
    }

    /**
     * 读取文件头与图片魔术字节比对
     */
    private static boolean matchesImageMagicBytes(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header, 0, header.length);
            if (read < 3) return false;

            // JPEG: FF D8 FF
            if (startsWith(header, MAGIC_JPEG)) return true;
            // PNG: 89 50 4E 47
            if (startsWith(header, MAGIC_PNG)) return true;
            // GIF: 47 49 46 38 37 61 (GIF89a/GIF87a)
            if (startsWith(header, MAGIC_GIF)) return true;
            // BMP: 42 4D
            if (startsWith(header, MAGIC_BMP)) return true;
            // WebP: RIFF(4) ....(4) WEBP(4)
            if (read >= 12) {
                byte[] riff = {0x52, 0x49, 0x46, 0x46}; // RIFF
                byte[] webp = {0x57, 0x45, 0x42, 0x50}; // WEBP
                if (startsWith(header, riff) && startsAt(header, 8, webp)) return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        return startsAt(data, 0, prefix);
    }

    private static boolean startsAt(byte[] data, int offset, byte[] target) {
        if (data.length - offset < target.length) return false;
        for (int i = 0; i < target.length; i++) {
            if (data[offset + i] != target[i]) return false;
        }
        return true;
    }
}
