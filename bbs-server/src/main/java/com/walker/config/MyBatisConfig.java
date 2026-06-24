package com.walker.config;

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * MyBatis 配置：注册 DatabaseIdProvider，使 Mapper XML 中可使用 _databaseId 动态切换 SQL
 * MySQL → "mysql", PostgreSQL → "postgresql"
 */
@Configuration
public class MyBatisConfig {

    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.put("MySQL", "mysql");
        properties.put("PostgreSQL", "postgresql");
        provider.setProperties(properties);
        return provider;
    }
}
