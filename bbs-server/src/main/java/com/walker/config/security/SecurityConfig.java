package com.walker.config.security;


import com.walker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security 配置类
 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserService userService;

    @Autowired
    private RestfulAccessDeniedHandler restfulAccessDeniedHandler;

    @Autowired
    private RestAuthorizationEntryPoint restAuthorizationEntryPoint;


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception{
        auth.userDetailsService(userDetailsService()).passwordEncoder(passwordEncoder());
    }

    @Override
    public void configure(WebSecurity web) throws Exception{
        web.ignoring().antMatchers(
//                "/register",
//                "/login",
//                "/logout",
//                "/css/**",
//                "/index.html",
//                "/favicon.ico",
//                "/doc.html",
//                "/webjars/**",
//                "/swagger-resources/**",
//                "/v2/api-docs/**"
        );
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception{

        http.csrf()
                .disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                // 注意：antMatchers 顺序敏感，具体路径必须放在通配符之前
                // 用户信息接口必须登录：未认证时 principal 为 null，UserController.getUserInfo 会 NPE（500）
                // 改为认证要求后，未登录由 RestAuthorizationEntryPoint 统一返回 401
                .antMatchers("/common/user/info")
                .authenticated()
                // 旧版管理员登录接口保留兼容（前端实际使用 /common/login），必须放在 /admin/** 之前
                .antMatchers("/admin/login")
                .permitAll()
                // 管理端接口统一要求管理员/超级管理员角色；含未挂在 /admin 前缀下的历史管理接口
                .antMatchers(
                        "/admin/**",
                        "/reply/admin/**",
                        "/getAllUser",
                        "/updateUserRole",
                        "/saOrg/**",
                        "/sensitiveWord/addSensitiveWord",
                        "/sensitiveWord/delSensitiveWord",
                        "/sensitiveWord/importExcel",
                        "/common/saOrg/batchUpdateRanking",
                        "/common/saOrg/batchUpdateDisplay"
                )
                .hasAnyRole("ADMIN", "SUPER_ADMIN")
                .antMatchers("/common/**", "/files/**")
                .permitAll() // common（登录/注册等）和文件放行
                .anyRequest()
                .authenticated()
                .and()
                .headers()
                .cacheControl();
        //添加jwt 登录授权过滤器
        http.addFilterBefore(JwtAuthenticationTokenFilter(),
                UsernamePasswordAuthenticationFilter.class);
        //添加自定义未授权和未登录结果返回
        http.exceptionHandling()
                .accessDeniedHandler(restfulAccessDeniedHandler)
                .authenticationEntryPoint(restAuthorizationEntryPoint);
    }


    @Override
    @Bean
    public UserDetailsService userDetailsService(){
        return username -> userService.getUserByUsername(username);
    }


    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationTokenFilter JwtAuthenticationTokenFilter(){
        return new JwtAuthenticationTokenFilter();
    }
}
