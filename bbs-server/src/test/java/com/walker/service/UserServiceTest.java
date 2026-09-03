package com.walker.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.Arrays;
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

    @BeforeAll
    static void initMybatisPlusCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                User.class
        );
    }

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

    // ========== getUserByUsername ==========

    @Test
    @DisplayName("获取用户 → 用户名为blank/null → 返回null")
    void getUserByUsername_blank_returnsNull() {
        assertNull(userService.getUserByUsername(""));
        assertNull(userService.getUserByUsername(null));
        assertNull(userService.getUserByUsername("   "));
    }

    @Test
    @DisplayName("获取用户 → 有效用户名 → 返回用户")
    void getUserByUsername_valid_returnsUser() {
        User expected = new User();
        expected.setId(5);
        expected.setUsername("ZhangSan");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(expected);

        User result = userService.getUserByUsername("ZhangSan");
        assertNotNull(result);
        assertEquals("ZhangSan", result.getUsername());
        assertEquals(5, result.getId());
    }

    // ========== batchDeleteUsersByUserIds ==========

    @Test
    @DisplayName("批量删除用户 → 调用deleteBatchIds")
    void batchDeleteUsersByUserIds_deletesAll() {
        List<Integer> ids = Arrays.asList(1, 2, 3);
        when(userMapper.deleteBatchIds(ids)).thenReturn(3);

        ResultBean result = userService.batchDeleteUsersByUserIds(ids);
        assertEquals(200, result.getCode());
        verify(userMapper).deleteBatchIds(ids);
    }

    // ========== updateUserAlive: toggle 1→0 ==========

    @Test
    @DisplayName("切换用户状态 → 1变0")
    void updateUserAlive_toggle1to0() {
        User user = new User();
        user.setId(2);
        user.setIsAlive(1);
        when(userMapper.selectById(2)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        ResultBean result = userService.updateUserAliveByUserId(2);
        assertEquals(200, result.getCode());
        verify(userMapper).updateById(argThat(u -> ((User) u).getIsAlive() == 0));
    }

    // ========== adminAddUser: valid user ==========

    @Test
    @DisplayName("新增用户 → 用户名唯一且有人员编号 → 创建成功")
    void adminAddUser_validUser_succeeds() {
        AdminUserAddParam param = new AdminUserAddParam();
        param.setUsername("newuser");
        param.setPersonnelId("P100");
        param.setNickname("新用户");
        param.setOrgNo("5140401");
        param.setUserType("1");

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        ResultBean result = userService.adminAddUser(param);
        assertEquals(200, result.getCode());
        assertEquals("用户创建成功", result.getMessage());
        verify(userMapper).insert(any(User.class));
    }

    // ========== adminUpdateUserDetail: valid update ==========

    @Test
    @DisplayName("管理员更新用户 → 用户存在且昵称非空 → 更新成功")
    void adminUpdateUserDetail_validUpdate_succeeds() {
        AdminUserUpdateParam param = new AdminUserUpdateParam();
        param.setId(1);
        param.setNickname("新昵称");

        User existing = new User();
        existing.setId(1);
        existing.setUsername("olduser");
        when(userMapper.selectById(1)).thenReturn(existing);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        ResultBean result = userService.adminUpdateUserDetail(param);
        assertEquals(200, result.getCode());
        assertEquals("更新成功", result.getMessage());
        verify(userMapper).updateById(argThat(u -> "新昵称".equals(((User) u).getNickname())));
    }

    // ========== adminUpdateUserDetail: password reset ==========

    @Test
    @DisplayName("管理员更新用户 → resetPassword=true → 重置密码")
    void adminUpdateUserDetail_passwordReset_resetsPassword() {
        AdminUserUpdateParam param = new AdminUserUpdateParam();
        param.setId(1);
        param.setResetPassword(true);

        User existing = new User();
        existing.setId(1);
        existing.setUsername("user1");
        when(userMapper.selectById(1)).thenReturn(existing);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        ResultBean result = userService.adminUpdateUserDetail(param);
        assertEquals(200, result.getCode());
        // 验证密码被重置：updateUser 参数中 password 非空且 isFirstLogin=1
        verify(userMapper).updateById(argThat(u -> {
            User uu = (User) u;
            return uu.getPassword() != null && !uu.getPassword().isEmpty()
                    && uu.getIsFirstLogin() != null && uu.getIsFirstLogin() == 1;
        }));
    }

    // ========== modPwd: valid password change ==========

    @Test
    @DisplayName("修改密码 → 正确原密码 + 强新密码 + 不同于旧密码 → 成功")
    void modPwd_validPassword_succeeds() {
        UserModPwdParam param = new UserModPwdParam();
        param.setId(1);
        param.setPassword("OldPass123");
        param.setNewPassword("NewPass456");

        User user = new User();
        user.setId(1);
        user.setPassword("$2a$oldencoded");
        when(userMapper.selectById(1)).thenReturn(user);
        when(passwordEncoder.matches("OldPass123", "$2a$oldencoded")).thenReturn(true);
        when(passwordEncoder.encode("NewPass456")).thenReturn("$2a$newencoded");

        ResultBean result = userService.modPwd(param);
        assertEquals(200, result.getCode());
        assertEquals("修改成功", result.getMessage());
    }

    // ========== login: channel 02 blocked for userType 1 ==========

    @Test
    @DisplayName("登录 → 普通用户(userType=1)通过渠道02登录 → 失败")
    void login_普通用户usesChannel02_returnsError() {
        User user = new User();
        user.setId(1);
        user.setUsername("TESTUSER");
        user.setPassword("$2a$encoded");
        user.setUserType("1"); // 普通用户

        when(userDetailsService.loadUserByUsername("TESTUSER")).thenReturn(user);
        when(passwordEncoder.matches("1234@abcD", "$2a$encoded")).thenReturn(true);

        ResultBean result = userService.login("testuser", "1234@abcD", "02", request);
        assertEquals(500, result.getCode());
        assertEquals("用户名或密码不正确！", result.getMessage());
    }

    // ========== updateUserRole: role 02 ==========

    @Test
    @DisplayName("修改角色 → 角色类型02 → 映射为2，成功")
    void updateUserRole_role02_succeeds() {
        when(userMapper.updateUserRole(1, "2")).thenReturn(1);

        ResultBean result = userService.updateUserRole(1, "02");
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMessage());
        verify(userMapper).updateUserRole(1, "2");
    }
}
