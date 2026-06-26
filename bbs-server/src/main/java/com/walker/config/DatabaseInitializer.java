package com.walker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 数据库初始化后置组件（暂无逻辑，保留用于后续扩展）
 */
@Component
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @SuppressWarnings("unused")
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        // 数据库和基础数据已由 DatabaseInitHelper.bootstrap() 在 Spring 启动前完成
        log.debug("DatabaseInitializer 已就绪");
    }
}
