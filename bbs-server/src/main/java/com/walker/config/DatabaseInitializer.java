package com.walker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 数据库初始化后置组件
 * <p>
 * 数据库和表结构已在 {@link DatabaseInitHelper#bootstrap} 中完成。
 * 本组件仅在 Spring 启动后确保超级管理员密码与配置一致。
 * </p>
 */
@Component
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${bbs.init.super-admin.username:asiayak}")
    private String superAdminUsername;

    @Value("${bbs.init.super-admin.password:1234@abcD}")
    private String superAdminPassword;

    @PostConstruct
    public void init() {
        try {
            String encodedPassword = passwordEncoder.encode(superAdminPassword);
            int updated = jdbcTemplate.update(
                    "UPDATE bbs_user SET password = ?, is_first_login = 0 WHERE username = ? AND is_delete = 0",
                    encodedPassword, superAdminUsername
            );
            if (updated > 0) {
                log.info("超级管理员 [{}] 密码已更新", superAdminUsername);
            } else {
                log.warn("超级管理员 [{}] 不存在或已删除，跳过密码更新", superAdminUsername);
            }
        } catch (Exception e) {
            log.warn("更新超级管理员密码失败: {}", e.getMessage());
        }
    }
}
