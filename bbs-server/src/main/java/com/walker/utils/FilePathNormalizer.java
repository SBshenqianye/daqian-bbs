package com.walker.utils;

/**
 * 文件路径归一化工具类
 * <p>
 * 将旧格式 /files/xxx 统一为 {contextPath}/files/xxx，
 * 对已含 context-path 的路径自动去重，新旧数据均能正确处理。
 * </p>
 */
public class FilePathNormalizer {

    /**
     * 归一化嵌入在文本内容中的文件路径（如 markdown / HTML 中的 /files/xxx）
     * <pre>
     *   旧数据: /files/common/upload/xxx → /bbs-server/files/common/upload/xxx
     *   新数据: /bbs-server/files/xxx → 不变（自动去重）
     * </pre>
     *
     * @param content     文章内容（markdown 或 HTML）
     * @param contextPath 部署上下文路径，如 /bbs-server
     * @return 归一化后的内容
     */
    public static String normalizeEmbeddedUrls(String content, String contextPath) {
        if (content == null || contextPath == null || contextPath.isEmpty()) {
            return content;
        }
        // 先去除可能已有的 context-path 前缀，再统一加上
        return content.replace(contextPath + "/files/", "/files/")
                      .replace("/files/", contextPath + "/files/");
    }

    /**
     * 归一化单个文件路径字段
     * <pre>
     *   旧数据: /files/User/id_1/portrait/xxx → /bbs-server/files/User/id_1/portrait/xxx
     *   新数据: /bbs-server/files/xxx → 不变
     *   其他格式: 原样返回
     * </pre>
     *
     * @param path        文件路径
     * @param contextPath 部署上下文路径，如 /bbs-server
     * @return 归一化后的路径
     */
    public static String normalizeFieldUrl(String path, String contextPath) {
        if (path == null || contextPath == null || contextPath.isEmpty()) {
            return path;
        }
        if (path.startsWith(contextPath)) {
            return path; // 已含 context-path，无需处理
        }
        if (path.startsWith("/files/")) {
            return contextPath + path;
        }
        return path;
    }

    /**
     * 拼接文件系统存储路径，保证 base 与 child 之间恰有一个斜杠
     * <pre>
     *   base=/data/bbs/bbsUpload,   child=common/upload/x.png → /data/bbs/bbsUpload/common/upload/x.png
     *   base=/data/bbs/bbsUpload/,  child=common/upload/x.png → /data/bbs/bbsUpload/common/upload/x.png
     * </pre>
     * 防止 storage.path 尾斜杠缺失时拼出 bbsUploadcommon 这类错误目录（2026-08 生产故障：
     * 生产 .env 的 BBS_UPLOAD_DIR 无尾斜杠，而 WSL 部署脚本会补斜杠，导致两环境行为不一致）。
     *
     * @param base  storage.path（可能带或不带尾斜杠，如 /data/bbs/bbsUpload 或 /data/bbs/bbsUpload/）
     * @param child 相对子路径（如 common/upload/2026-08-06/xxx.png）
     * @return base 与 child 拼接后、中间恰有一个斜杠的完整路径
     */
    public static String joinStoragePath(String base, String child) {
        if (base == null || child == null) {
            return base + child;
        }
        return base.replaceAll("[/\\\\]+$", "") + "/" + child;
    }
}
