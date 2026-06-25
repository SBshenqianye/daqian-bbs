package com.walker.vo.excel;

import lombok.Data;

import java.util.List;

/**
 * Excel 导入结果
 */
@Data
public class ImportResultVO {

    /** 总行数 */
    private int totalCount;

    /** 用户导入成功数 */
    private int userSuccessCount;

    /** 用户导入失败数 */
    private int userFailCount;

    /** 新建组织数 */
    private int orgCreatedCount;

    /** 覆盖更新数 */
    private int userUpdatedCount;

    /** 新增用户数 */
    private int userNewCount;

    /** 行详情列表 */
    private List<RowResult> details;

    @Data
    public static class RowResult {
        private int rowNum;
        /** 是否成功 */
        private boolean success;
        private String nickname;
        private String username;
        private String personnelId;
        private String orgName;
        /** 操作类型: 新增/覆盖/跳过/失败 */
        private String action;
        private String message;
    }
}
