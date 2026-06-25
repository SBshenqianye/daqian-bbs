package com.walker.vo.excel;

import lombok.Data;

import java.util.List;

/**
 * 导入预览结果
 * 解析Excel但不存库，返回预览信息让管理员确认
 */
@Data
public class ImportPreviewVO {

    /** 总行数 */
    private int totalCount;

    /** 组织数量 */
    private int orgCount;

    /** 组织预览 */
    private List<OrgPreview> orgs;

    /** 用户预览 */
    private List<UserPreview> users;

    @Data
    public static class OrgPreview {
        private String orgName;
        private String deptName;
        /** matched/created */
        private String action;
    }

    @Data
    public static class UserPreview {
        private int rowNum;
        private String nickname;
        private String username;
        private String personnelId;
        private String idCard;
        private String orgName;
        private String deptName;
        /** new/update */
        private String action;
        /** 是否可编辑 */
        private boolean editable;
    }
}
