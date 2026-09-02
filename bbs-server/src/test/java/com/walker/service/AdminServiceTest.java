package com.walker.service;

import com.walker.mapper.AdminMapper;
import com.walker.pojo.Admin;
import com.walker.service.impl.AdminServiceImpl;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks
    private AdminServiceImpl adminService;

    @Mock
    private AdminMapper adminMapper;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = adminService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(adminService, adminMapper);
    }

    @Test
    @DisplayName("管理员登录 → 账号密码正确 → 登录成功")
    void login_success() {
        Admin admin = new Admin();
        admin.setId(1);
        admin.setUsername("admin");
        admin.setPassword("123456");
        admin.setPortrait("avatar.png");
        when(adminMapper.selectOne(any())).thenReturn(admin);

        ResultBean result = adminService.login("admin", "123456");

        assertEquals(200, result.getCode());
        Admin returned = (Admin) result.getObj();
        assertEquals("admin", returned.getUsername());
        assertNull(returned.getPassword(), "密码应被清除");
    }

    @Test
    @DisplayName("管理员登录 → 账号密码错误 → 登录失败")
    void login_wrongPassword() {
        when(adminMapper.selectOne(any())).thenReturn(null);

        ResultBean result = adminService.login("admin", "wrong");

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("不正确"));
    }

    @Test
    @DisplayName("管理员登录 → 空用户名 → 登录失败")
    void login_emptyUsername() {
        when(adminMapper.selectOne(any())).thenReturn(null);

        ResultBean result = adminService.login("", "123456");

        assertEquals(500, result.getCode());
    }
}
