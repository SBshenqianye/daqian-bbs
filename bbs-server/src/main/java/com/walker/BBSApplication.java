package com.walker;

import com.walker.config.DatabaseInitHelper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

@SpringBootApplication
@MapperScan("com.walker.mapper")
public class BBSApplication {

    public static void main(String[] args) {
        // 第 1 步：在 Spring 启动前初始化数据库（建库 + 建表 + 基础数据）
        String[] dbConfig = loadDataSourceConfig();
        if (dbConfig != null) {
            DatabaseInitHelper.bootstrap(dbConfig[0], dbConfig[1], dbConfig[2]);
        }

        // 第 2 步：启动 Spring Boot
        SpringApplication.run(BBSApplication.class, args);
    }

    /**
     * 从 application.yml / application-{profile}.yml 读取数据源配置
     * @return [url, username, password] 或 null
     */
    @SuppressWarnings("unchecked")
    private static String[] loadDataSourceConfig() {
        try {
            Yaml yaml = new Yaml();

            // 1. 读取 application.yml 获取活跃 profile
            String profile = "dev";
            try (InputStream in = BBSApplication.class.getClassLoader()
                    .getResourceAsStream("application.yml")) {
                if (in != null) {
                    Map<String, Object> root = yaml.load(in);
                    if (root != null) {
                        Map<String, Object> spring = (Map<String, Object>) root.get("spring");
                        if (spring != null) {
                            Object profilesObj = spring.get("profiles");
                            if (profilesObj instanceof Map) {
                                String active = (String) ((Map<?, ?>) profilesObj).get("active");
                                if (active != null && !active.isEmpty()) {
                                    profile = active;
                                }
                            } else if (profilesObj instanceof String) {
                                profile = (String) profilesObj;
                            }
                        }
                    }
                }
            }

            // 2. 读取 application-{profile}.yml
            String profileFile = "application-" + profile + ".yml";
            try (InputStream in = BBSApplication.class.getClassLoader()
                    .getResourceAsStream(profileFile)) {
                if (in == null) {
                    System.err.println("[BBS] 未找到配置文件: " + profileFile);
                    return null;
                }
                Map<String, Object> root = yaml.load(in);
                if (root == null) return null;

                Map<String, Object> spring = (Map<String, Object>) root.get("spring");
                if (spring == null) return null;

                Map<String, Object> datasource = (Map<String, Object>) spring.get("datasource");
                if (datasource == null) return null;

                String url = (String) datasource.get("url");
                String username = (String) datasource.get("username");
                String password = (String) datasource.get("password");

                if (url != null && username != null) {
                    return new String[]{url, username, password};
                }
            }
        } catch (Exception e) {
            System.err.println("[BBS] 读取数据源配置失败: " + e.getMessage());
        }
        return null;
    }
}
