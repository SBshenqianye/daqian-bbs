package com.walker.config.security;


import com.walker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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

    @Autowired
    private AdminAccessFilter adminAccessFilter;

    @Autowired
    private FirstLoginPasswordFilter firstLoginPasswordFilter;


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
                // 管理端接口统一要求已认证；实际 admin:access 由 AdminAccessFilter + 方法级权限点校验
                .antMatchers(
                        "/admin/**",
                        "/reply/admin/**",
                        "/getAllUser",
                        "/updateUserRole",
                        "/saOrg/**",
                        "/sensitiveWord/**",
                        "/common/saOrg/batchUpdateRanking",
                        "/common/saOrg/batchUpdateDisplay"
                )
                .authenticated()
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
        // 管理路径入口校验：JWT 之后，确保旧 token 也按当前有效权限重新判断
        http.addFilterAfter(adminAccessFilter, JwtAuthenticationTokenFilter.class);
        // 管理端首次登录强制改密：入口校验之后、业务接口之前
        http.addFilterAfter(firstLoginPasswordFilter, AdminAccessFilter.class);
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

    /**
     * AdminAccessFilter 已加入 Spring Security 过滤链，关闭 Servlet 容器的自动注册，避免重复执行。
     */
    @Bean
    public FilterRegistrationBean<AdminAccessFilter> adminAccessFilterRegistration() {
        FilterRegistrationBean<AdminAccessFilter> registration =
                new FilterRegistrationBean<>(adminAccessFilter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * FirstLoginPasswordFilter 已加入 Spring Security 过滤链，关闭自动注册。
     */
    @Bean
    public FilterRegistrationBean<FirstLoginPasswordFilter> firstLoginPasswordFilterRegistration() {
        FilterRegistrationBean<FirstLoginPasswordFilter> registration =
                new FilterRegistrationBean<>(firstLoginPasswordFilter);
        registration.setEnabled(false);
        return registration;
    }
}
