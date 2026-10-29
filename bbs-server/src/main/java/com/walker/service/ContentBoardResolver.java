package com.walker.service;

/**
 * 内容所属板块解析：用于举报/违规等“只拿到 targetType + targetId”的场景。
 */
public interface ContentBoardResolver {

    /**
     * 解析内容所属标签（板块）ID。
     * 支持 article/comment/reply；内容不存在或无法解析时返回 null。
     */
    Integer resolveLabelId(String targetType, Integer targetId);
}
