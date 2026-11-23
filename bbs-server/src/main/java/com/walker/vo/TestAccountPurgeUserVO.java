package com.walker.vo;

import lombok.Data;

/**
 * 清理测试账号帖子：按账号汇总的预览行
 * <p>
 * 用 VO 而不是 Map 接收：PostgreSQL 会把未加引号的列别名折叠成小写
 * （<code>AS articleCount</code> 返回的列名是 <code>articlecount</code>），
 * Map 结果会因此取不到键；VO 走属性名的大小写不敏感匹配，MySQL/PostgreSQL 都正常。
 */
@Data
public class TestAccountPurgeUserVO {
    /**
     * 用户 ID
     */
    private Integer userId;
    /**
     * 账号
     */
    private String username;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 该账号未删除的帖子数
     */
    private Long articleCount;
}
