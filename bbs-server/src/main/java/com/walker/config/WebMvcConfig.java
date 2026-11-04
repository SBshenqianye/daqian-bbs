package com.walker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${storage.path}")
    private String path;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 确保 path 有且仅有一个尾部斜杠，避免双斜杠问题
        String normalizedPath = path.replaceAll("[/\\\\]+$", "") + "/";
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + normalizedPath);
    }

    @Bean
    public CorsFilter corsFilter() {
        // 配置跨域规则生效的路径（包括/files/**和所有接口）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration());
        return new CorsFilter(source);
    }

    /** 提取为包级方法，便于回归测试校验允许的请求方法。 */
    CorsConfiguration corsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许的源（内网环境可直接放行所有，也可指定前端内网IP）
        // 方式1：放行所有源（内网推荐）
        config.addAllowedOriginPattern("*");
        // 方式2：指定具体前端源（更安全，如内网前端地址）
        // config.addAllowedOrigin("http://192.168.1.100:3000");

        // 允许携带Cookie（如需登录态访问附件则必须）
        config.setAllowCredentials(true);
        // 允许的请求方法（GET/POST/PUT/DELETE 等；缺 DELETE 会让浏览器的删除请求被 CORS 直接拒绝）
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("PATCH");
        config.addAllowedMethod("OPTIONS");
        // 允许的请求头
        config.addAllowedHeader("*");
        // 跨域预检请求的有效期（秒），避免频繁预检
        config.setMaxAge(3600L);
        return config;
    }

}

