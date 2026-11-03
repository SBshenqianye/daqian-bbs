package com.walker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walker.config.security.Permission;
import com.walker.mapper.PermissionGroupItemMapper;
import com.walker.mapper.PermissionGroupMapper;
import com.walker.mapper.PermissionPresetMapper;
import com.walker.mapper.UserMapper;
import com.walker.mapper.UserPermissionGroupMapper;
import com.walker.pojo.PermissionGroup;
import com.walker.pojo.PermissionGroupItem;
import com.walker.pojo.User;
import com.walker.service.impl.PermissionGroupServiceImpl;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionGroupServiceTest {

    @InjectMocks
    private PermissionGroupServiceImpl service;

    @Mock
    private PermissionGroupMapper permissionGroupMapper;

    @Mock
    private PermissionGroupItemMapper permissionGroupItemMapper;

    @Mock
    private UserPermissionGroupMapper userPermissionGroupMapper;

    @Mock
    private PermissionPresetMapper permissionPresetMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private EffectivePermissionService effectivePermissionService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private PermissionGroup group() {
        PermissionGroup group = new PermissionGroup();
        group.setGroupCode("content_operator");
        group.setGroupName("内容运营");
        group.setScopeType("ADMIN");
        group.setEnabled(1);
        return group;
    }

    @Test
    @DisplayName("创建权限组：写入组和权限点")
    void createGroup_success() {
        PermissionGroup group = group();
        when(permissionGroupMapper.selectCount(any())).thenReturn(0L);
        when(permissionGroupMapper.insert(any(PermissionGroup.class))).thenAnswer(invocation -> {
            PermissionGroup g = invocation.getArgument(0);
            g.setId(1);
            return 1;
        });
        when(currentUserService.getCurrentUser()).thenReturn(null);

        ResultBean result = service.createGroup(group,
                Arrays.asList("article:read", "article:review"));

        assertEquals(200, result.getCode());
        assertEquals(1, result.getObj());
        verify(permissionGroupMapper).insert(any(PermissionGroup.class));
        verify(permissionGroupItemMapper, times(2)).insert(any(PermissionGroupItem.class));
    }

    @Test
    @DisplayName("创建权限组：禁止授予 user:grant:admin")
    void createGroup_forbiddenGrant() {
        PermissionGroup group = group();
        when(permissionGroupMapper.selectCount(any())).thenReturn(0L);

        ResultBean result = service.createGroup(group,
                Collections.singletonList(Permission.USER_GRANT_ADMIN.getCode()));

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("不能授予"));
        verify(permissionGroupMapper, never()).insert(any(PermissionGroup.class));
    }

    @Test
    @DisplayName("创建权限组：编码重复拒绝")
    void createGroup_duplicateCode() {
        when(permissionGroupMapper.selectCount(any())).thenReturn(1L);

        ResultBean result = service.createGroup(group(),
                Collections.singletonList("article:read"));

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("已存在"));
    }

    @Test
    @DisplayName("替换权限点：先删后插")
    void replaceItems_success() {
        PermissionGroup group = group();
        group.setId(1);
        when(permissionGroupMapper.selectById(1)).thenReturn(group);

        ResultBean result = service.replaceItems(1,
                Arrays.asList("article:read", "report:review"));

        assertEquals(200, result.getCode());
        verify(permissionGroupItemMapper).delete(any());
        verify(permissionGroupItemMapper, times(2)).insert(any(PermissionGroupItem.class));
    }

    @Test
    @DisplayName("用户加入权限组：无效用户拒绝")
    void assignUsers_invalidUser() {
        PermissionGroup group = group();
        group.setId(1);
        when(permissionGroupMapper.selectById(1)).thenReturn(group);
        when(userMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());

        ResultBean result = service.assignUsers(1, Collections.singletonList(999));

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("无效用户"));
    }

    @Test
    @DisplayName("系统角色：禁止删除、禁止修改编码/baseRole、禁止手动加成员")
    void systemGroup_immutableIdentityAndNoManualMembers() {
        PermissionGroup system = group();
        system.setId(10);
        system.setBaseRole("ADMIN");
        when(permissionGroupMapper.selectById(10)).thenReturn(system);

        ResultBean deleted = service.deleteGroup(10);
        assertEquals(500, deleted.getCode());
        assertTrue(deleted.getMessage().contains("不可删除"));
        verify(permissionGroupMapper, never()).deleteById(any());

        PermissionGroup codeChange = group();
        codeChange.setGroupCode("hacked_code");
        ResultBean codeResult = service.updateGroup(10, codeChange, null);
        assertEquals(500, codeResult.getCode());
        assertTrue(codeResult.getMessage().contains("编码不可修改"));

        PermissionGroup roleChange = group();
        roleChange.setBaseRole("SUPER_ADMIN");
        ResultBean roleResult = service.updateGroup(10, roleChange, null);
        assertEquals(500, roleResult.getCode());
        assertTrue(roleResult.getMessage().contains("基线不可修改"));

        ResultBean assigned = service.assignUsers(10, Collections.singletonList(5));
        assertEquals(500, assigned.getCode());
        assertTrue(assigned.getMessage().contains("自动应用"));
        verify(userPermissionGroupMapper, never()).insert(any());
    }

    @Test
    @DisplayName("系统角色：允许修改名称/描述/权限，并返回自动应用信息")
    void systemGroup_allowsPermissionUpdateAndSummary() {
        PermissionGroup system = group();
        system.setId(10);
        system.setBaseRole("ADMIN");
        when(permissionGroupMapper.selectById(10)).thenReturn(system);
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userMapper.selectCount(any())).thenReturn(2L, 1L);

        PermissionGroup rename = new PermissionGroup();
        rename.setGroupName("管理员角色");
        ResultBean updated = service.updateGroup(10, rename, null);
        assertEquals(200, updated.getCode());
        verify(permissionGroupMapper).updateById(argThat(g -> "管理员角色".equals(((PermissionGroup) g).getGroupName())));

        java.util.Map<String, Object> detail = service.getGroup(10);
        assertNotNull(detail);
        assertEquals(Boolean.TRUE, detail.get("system"));
        assertEquals(Boolean.TRUE, detail.get("autoApply"));
        assertEquals(3L, detail.get("roleUserCount"));
        assertEquals(3L, detail.get("userCount"));
    }

    @Test
    @DisplayName("系统角色：不能通过创建接口新增")
    void createGroup_systemRoleRejected() {
        PermissionGroup system = group();
        system.setBaseRole("ADMIN");

        ResultBean result = service.createGroup(system, Collections.emptyList());

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("系统预置"));
        verify(permissionGroupMapper, never()).insert(any(PermissionGroup.class));
    }

    @Test
    @DisplayName("有效权限：返回用户与权限点")
    void effective_returnsCodes() {
        User user = new User();
        user.setId(5);
        user.setUsername("tester");
        user.setUserType("2");
        when(userMapper.selectById(5)).thenReturn(user);
        when(effectivePermissionService.getEffectiveCodes(user))
                .thenReturn(new java.util.LinkedHashSet<>(Arrays.asList("admin:access", "article:read")));
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());

        java.util.Map<String, Object> data = service.effective(5);

        assertNotNull(data);
        assertEquals(5, data.get("userId"));
        assertTrue(((java.util.List<?>) data.get("permissions")).contains("article:read"));
    }
}
