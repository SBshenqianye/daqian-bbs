package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.SaOrgMapper;
import com.walker.mapper.UserMapper;
import com.walker.pojo.SaOrg;
import com.walker.pojo.User;
import com.walker.service.impl.SaOrgServiceImpl;
import com.walker.service.impl.UserServiceImpl;
import com.walker.vo.ResultBean;
import com.walker.vo.param.AdminUserAddParam;
import com.walker.vo.param.AdminUserUpdateParam;
import com.walker.vo.param.UserModOrgNoParam;
import com.walker.vo.param.UserModPwdParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SaOrgMapper saOrgMapper;

    @Mock
    private SaOrgServiceImpl saOrgService;

    @Mock
    private com.walker.config.security.JwtTokenUtil jwtTokenUtil;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = userService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(userService, userMapper);
        // Set @Value fields that Mockito can't inject
        Field tokenHeadField = UserServiceImpl.class.getDeclaredField("tokenHead");
        tokenHeadField.setAccessible(true);
        tokenHeadField.set(userService, "Bearer ");
        // Set orgImportService to avoid NPE
        Field orgImportField = UserServiceImpl.class.getDeclaredField("orgImportService");
        orgImportField.setAccessible(true);
        orgImportField.set(userService, mock(com.walker.service.impl.OrgImportService.class));
    }

    // ========== login ==========

    @Test
    @DisplayName("登录成功 → 返回token")
    void login_validCredentials_returnsToken() {
        User user = new User();
        user.setId(1);
        user.setUsername("TESTUSER");
        user.setPassword("$2a$encoded");
        user.setUserType("1");
        user.setIsFirstLogin(0);

        when(userDetailsService.loadUserByUsername("TESTUSER")).thenReturn(user);
        when(passwordEncoder.matches("1234@abcD", "$2a$encoded")).thenReturn(true);
        when(saOrgService.resolveDisplayOrgName(any(), any())).thenReturn("内江市公司");

        ResultBean result = userService.login("testuser", "1234@abcD", "01", request);
        assertEquals(200, result.getCode());
        assertNotNull(result.getObj());
    }

    @Test
    @DisplayName("登录密码错误 → 返回错误")
    void login_wrongPassword_returnsError() {
        User user = new User();
        user.setId(1);
        user.setUsername("TESTUSER");
        user.setPassword("$2a$encoded");
        user.setUserType("1");

        when(userDetailsService.loadUserByUsername("TESTUSER")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "$2a$encoded")).thenReturn(false);

        ResultBean result = userService.login("testuser", "wrong", "01", request);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("非法渠道登录 → 返回错误")
    void login_invalidChannel_returnsError() {
        User user = new User();
        user.setId(1);
        user.setUsername("TESTUSER");
        user.setPassword("$2a$encoded");
        user.setUserType("1");

        when(userDetailsService.loadUserByUsername("TESTUSER")).thenReturn(user);
        when(passwordEncoder.matches("1234", "$2a$encoded")).thenReturn(true);

        ResultBean result = userService.login("testuser", "1234", "03", request);
        assertEquals(500, result.getCode());
        assertEquals("非法渠道登录！", result.getMessage());
    }

    // ========== register ==========

    @Test
    @DisplayName("注册功能已关闭 → 返回错误")
    void register_alwaysReturnsClosed() {
        ResultBean result = userService.register("user", "pass", "13800000000", "51404", "昵称", request);
        assertEquals(500, result.getCode());
    }

    // ========== modPwd ==========

    @Test
    @DisplayName("修改密码 → 用户不存在 → 返回错误")
    void modPwd_userNotFound_returnsError() {
        UserModPwdParam param = new UserModPwdParam();
        param.setId(999);
        param.setPassword("old");
        param.setNewPassword("NewPass123");

        when(userMapper.selectById(999)).thenReturn(null);

        ResultBean result = userService.modPwd(param);
        assertEquals(500, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    @DisplayName("修改密码 → 原密码错误 → 返回错误")
    void modPwd_wrongOldPassword_returnsError() {
        UserModPwdParam param = new UserModPwdParam();
        param.setId(1);
        param.setPassword("wrong");
        param.setNewPassword("NewPass123");

        User user = new User();
        user.setId(1);
        user.setPassword("$2a$old");
        when(userMapper.selectById(1)).thenReturn(user);
        when(passwordEncoder.matches("wrong", "$2a$old")).thenReturn(false);

        ResultBean result = userService.modPwd(param);
        assertEquals(500, result.getCode());
        assertEquals("密码不正确", result.getMessage());
    }

    @Test
    @DisplayName("修改密码 → 新密码强度不足 → 返回错误")
    void modPwd_weakPassword_returnsError() {
        UserModPwdParam param = new UserModPwdParam();
        param.setId(1);
        param.setPassword("old");
        param.setNewPassword("weak");

        User user = new User();
        user.setId(1);
        user.setPassword("$2a$old");
        when(userMapper.selectById(1)).thenReturn(user);
        when(passwordEncoder.matches("old", "$2a$old")).thenReturn(true);

        ResultBean result = userService.modPwd(param);
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("密码强度不足"));
    }

    @Test
    @DisplayName("修改密码 → 新密码与原密码相同 → 返回错误")
    void modPwd_samePassword_returnsError() {
        UserModPwdParam param = new UserModPwdParam();
        param.setId(1);
        param.setPassword("NewPass123");
        param.setNewPassword("NewPass123");

        User user = new User();
        user.setId(1);
        user.setPassword("$2a$old");
        when(userMapper.selectById(1)).thenReturn(user);
        when(passwordEncoder.matches("NewPass123", "$2a$old")).thenReturn(true);

        ResultBean result = userService.modPwd(param);
        assertEquals(500, result.getCode());
        assertEquals("新密码不能与原密码相同", result.getMessage());
    }

    // ========== updateUserRole ==========

    @Test
    @DisplayName("修改角色 → 有效角色 → 成功")
    void updateUserRole_validRole_success() {
        when(userMapper.updateUserRole(1, "1")).thenReturn(1);
        ResultBean result = userService.updateUserRole(1, "01");
        assertEquals(200, result.getCode());
    }

    @Test
    @DisplayName("修改角色 → 无效角色 → 返回错误")
    void updateUserRole_invalidRole_returnsError() {
        ResultBean result = userService.updateUserRole(1, "99");
        assertEquals(500, result.getCode());
        assertEquals("权限越界", result.getMessage());
    }

    // ========== modOrgNo ==========

    @Test
    @DisplayName("修改单位 → 用户不存在 → 返回错误")
    void modOrgNo_userNotFound_returnsError() {
        UserModOrgNoParam param = new UserModOrgNoParam();
        param.setId(999);
        param.setOrgNo("5140401");

        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

        ResultBean result = userService.modOrgNo(param);
        assertEquals(500, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    @DisplayName("修改单位 → 相同单位 → 返回错误")
    void modOrgNo_sameOrg_returnsError() {
        UserModOrgNoParam param = new UserModOrgNoParam();
        param.setId(1);
        param.setOrgNo("5140401");

        User user = new User();
        user.setId(1);
        user.setOrgNo("5140401");
        user.setNickname("内江市公司-张三");

        List<User> users = new ArrayList<>();
        users.add(user);
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(users);

        ResultBean result = userService.modOrgNo(param);
        assertEquals(500, result.getCode());
        assertEquals("用户单位不能与原单位相同", result.getMessage());
    }

    // ========== adminAddUser ==========

    @Test
    @DisplayName("新增用户 → 用户名已存在 → 返回错误")
    void adminAddUser_usernameExists_returnsError() {
        AdminUserAddParam param = new AdminUserAddParam();
        param.setUsername("existing");
        param.setPersonnelId("P001");

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        ResultBean result = userService.adminAddUser(param);
        assertEquals(500, result.getCode());
        assertEquals("用户名已存在", result.getMessage());
    }

    @Test
    @DisplayName("新增用户 → 人员编号和身份证号都为空 → 返回错误")
    void adminAddUser_noPersonnelIdNoIdCard_returnsError() {
        AdminUserAddParam param = new AdminUserAddParam();
        param.setUsername("newuser");
        param.setPersonnelId("");
        param.setIdCard("");

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        ResultBean result = userService.adminAddUser(param);
        assertEquals(500, result.getCode());
        assertEquals("人员编号和身份证号至少填一个", result.getMessage());
    }

    // ========== adminUpdateUserDetail ==========

    @Test
    @DisplayName("管理员更新用户 → 用户ID为空 → 返回错误")
    void adminUpdateUserDetail_noId_returnsError() {
        AdminUserUpdateParam param = new AdminUserUpdateParam();
        param.setId(null);

        ResultBean result = userService.adminUpdateUserDetail(param);
        assertEquals(500, result.getCode());
        assertEquals("用户ID不能为空", result.getMessage());
    }

    @Test
    @DisplayName("管理员更新用户 → 用户不存在 → 返回错误")
    void adminUpdateUserDetail_userNotFound_returnsError() {
        AdminUserUpdateParam param = new AdminUserUpdateParam();
        param.setId(999);

        when(userMapper.selectById(999)).thenReturn(null);

        ResultBean result = userService.adminUpdateUserDetail(param);
        assertEquals(500, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    // ========== deleteUserByUserId ==========

    @Test
    @DisplayName("删除用户 → 调用mapper删除")
    void deleteUserByUserId_callsMapper() {
        when(userMapper.deleteById(1)).thenReturn(1);
        ResultBean result = userService.deleteUserByUserId(1);
        assertEquals(200, result.getCode());
        verify(userMapper).deleteById(1);
    }

    // ========== getUserCount ==========

    @Test
    @DisplayName("获取用户总数 → 返回计数")
    void getUserCount_returnsCount() {
        when(userMapper.selectCount(null)).thenReturn(42L);
        ResultBean result = userService.getUserCount();
        assertEquals(200, result.getCode());
        assertEquals(42L, result.getObj());
    }

    // ========== updateUserAliveByUserId ==========

    @Test
    @DisplayName("切换用户状态 → 0变1")
    void updateUserAlive_toggle0to1() {
        User user = new User();
        user.setId(1);
        user.setIsAlive(0);
        when(userMapper.selectById(1)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        ResultBean result = userService.updateUserAliveByUserId(1);
        assertEquals(200, result.getCode());
        verify(userMapper).updateById(argThat(u -> ((User) u).getIsAlive() == 1));
    }
}
