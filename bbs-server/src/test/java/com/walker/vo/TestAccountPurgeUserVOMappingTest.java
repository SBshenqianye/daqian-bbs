package com.walker.vo;

import org.apache.ibatis.reflection.MetaClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 回归：PostgreSQL 会把未加引号的列别名折叠成小写（articleCount → articlecount），
 * 预览查询必须用 VO 接收，靠 MyBatis 属性名大小写不敏感匹配，而不是 Map。
 */
class TestAccountPurgeUserVOMappingTest {

    @Test
    @DisplayName("PG 折叠后的小写列名能映射到驼峰属性")
    void pgFoldedColumnLabels_mapToCamelCaseProperties() {
        MetaClass meta = MetaClass.forClass(TestAccountPurgeUserVO.class,
                new org.apache.ibatis.reflection.DefaultReflectorFactory());

        assertEquals("userId", meta.findProperty("userid"));
        assertEquals("articleCount", meta.findProperty("articlecount"));
        assertEquals("username", meta.findProperty("username"));
        assertEquals("nickname", meta.findProperty("nickname"));
    }
}
