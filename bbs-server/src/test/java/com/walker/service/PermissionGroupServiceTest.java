package com.walker.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walker.config.security.Permission;
import com.walker.mapper.PermissionGroupItemMapper;
import com.walker.mapper.PermissionGroupMapper;
import com.walker.mapper.PermissionPresetMapper;
import com.walker.mapper.UserMapper;
import com.walker.mapper.UserPermissionGroupMapper;
import com.walker.pojo.PermissionGroup;
import com.walker.pojo.PermissionGroupItem;
import com.walker.pojo.PermissionPreset;
import com.walker.pojo.User;
import com.walker.service.impl.PermissionGroupServiceImpl;
import com.walker.vo.ResultBean;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
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

    @BeforeAll
    static void initMybatisPlusCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                User.class
        );
    }

    private PermissionGroup group() {
        PermissionGroup group = new PermissionGroup();
        group.setGroupCode("content_operator");
        group.setGroupName("内容运营");
        group.setScopeType("ADMIN");
        group.setDataScopeType("GLOBAL");
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
    @DisplayName("创建权限组：所有权限点都可直接配置（默认内置组也是普通组）")
    void createGroup_anyPermissionAllowed() {
        PermissionGroup group = group();
        when(permissionGroupMapper.selectCount(any())).thenReturn(0L);
        when(permissionGroupMapper.insert(any(PermissionGroup.class))).thenAnswer(invocation -> {
            PermissionGroup g = invocation.getArgument(0);
            g.setId(1);
            return 1;
        });

        ResultBean result = service.createGroup(group,
                Collections.singletonList(Permission.USER_GRANT_ADMIN.getCode()));

        assertEquals(200, result.getCode());
        verify(permissionGroupItemMapper).insert(argThat(item ->
                Permission.USER_GRANT_ADMIN.getCode().equals(((PermissionGroupItem) item).getPermissionCode())));
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
    @DisplayName("替换权限点：先删后插，全部保存为 grant")
    void replaceItems_success() {
        PermissionGroup group = group();
        group.setId(1);
        when(permissionGroupMapper.selectById(1)).thenReturn(group);

        ResultBean result = service.replaceItems(1,
                Arrays.asList("article:read", "report:review"));

        assertEquals(200, result.getCode());
        verify(permissionGroupItemMapper).delete(any());
        verify(permissionGroupItemMapper, times(2)).insert(argThat(item ->
                "grant".equals(((PermissionGroupItem) item).getEffect())));
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
    @DisplayName("默认内置组也是普通组：可删除、可改编码、可手动加成员")
    void builtInGroup_isEditableAndDeletable() {
        PermissionGroup builtIn = group();
        builtIn.setId(10);
        builtIn.setGroupCode("role_admin");
        when(permissionGroupMapper.selectById(10)).thenReturn(builtIn);
        when(currentUserService.getCurrentUser()).thenReturn(null);

        PermissionGroup codeChange = new PermissionGroup();
        codeChange.setGroupCode("renamed_admin");
        ResultBean codeResult = service.updateGroup(10, codeChange, null);
        assertEquals(200, codeResult.getCode());
        verify(permissionGroupMapper).updateById(argThat(g ->
                "renamed_admin".equals(((PermissionGroup) g).getGroupCode())));

        when(userMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(
                new User().setId(5).setUsername("u5")));
        when(userPermissionGroupMapper.selectCount(any())).thenReturn(0L);
        ResultBean assigned = service.assignUsers(10, Collections.singletonList(5));
        assertEquals(200, assigned.getCode());
        verify(userPermissionGroupMapper).insert(any());

        ResultBean deleted = service.deleteGroup(10);
        assertEquals(200, deleted.getCode());
        verify(permissionGroupMapper).deleteById(10);
    }

    @Test
    @DisplayName("删除最后一个超管组 → 拒绝，避免无人能管理权限")
    void deleteGroup_lastSuperGroup_blocked() {
        PermissionGroup superGroup = group();
        superGroup.setId(10);
        superGroup.setGroupCode("role_super");
        when(permissionGroupMapper.selectById(10)).thenReturn(superGroup);
        when(permissionGroupMapper.selectList(any())).thenReturn(Collections.singletonList(superGroup));
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Arrays.asList(
                item(10, Permission.ADMIN_ACCESS),
                item(10, Permission.PERMISSION_MANAGE),
                item(10, Permission.USER_GRANT_ADMIN)));

        ResultBean result = service.deleteGroup(10);

        assertNotEquals(200, result.getCode());
        verify(permissionGroupMapper, never()).deleteById(10);
    }

    @Test
    @DisplayName("移除最后一个超管组的授予管理员权限 → 拒绝")
    void replaceItems_lastSuperGroup_blocked() {
        PermissionGroup superGroup = group();
        superGroup.setId(10);
        superGroup.setEnabled(1);
        when(permissionGroupMapper.selectById(10)).thenReturn(superGroup);
        when(permissionGroupMapper.selectList(any())).thenReturn(Collections.singletonList(superGroup));
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Arrays.asList(
                item(10, Permission.ADMIN_ACCESS),
                item(10, Permission.PERMISSION_MANAGE),
                item(10, Permission.USER_GRANT_ADMIN)));

        ResultBean result = service.replaceItems(10, Collections.emptyList());

        assertNotEquals(200, result.getCode());
        verify(permissionGroupItemMapper, never()).delete(any());
    }

    @Test
    @DisplayName("移除最后一个超管组的进入后台权限 → 拒绝，避免管理员无法登录")
    void replaceItems_removeAdminAccess_blocked() {
        PermissionGroup superGroup = group();
        superGroup.setId(10);
        superGroup.setEnabled(1);
        when(permissionGroupMapper.selectById(10)).thenReturn(superGroup);
        when(permissionGroupMapper.selectList(any())).thenReturn(Collections.singletonList(superGroup));
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Arrays.asList(
                item(10, Permission.ADMIN_ACCESS),
                item(10, Permission.PERMISSION_MANAGE),
                item(10, Permission.USER_GRANT_ADMIN)));

        ResultBean result = service.replaceItems(10, Arrays.asList(
                Permission.PERMISSION_MANAGE.getCode(),
                Permission.USER_GRANT_ADMIN.getCode()));

        assertNotEquals(200, result.getCode());
        verify(permissionGroupItemMapper, never()).delete(any());
    }

    @Test
    @DisplayName("当前登录账号主组移除关键权限 → 拒绝，避免把自己锁在系统外")
    void updateGroup_ownPrimaryCriticalRemoval_blocked() {
        PermissionGroup superGroup = group();
        superGroup.setId(10);
        superGroup.setEnabled(1);
        when(permissionGroupMapper.selectById(10)).thenReturn(superGroup);
        when(permissionGroupMapper.selectCount(any())).thenReturn(0L);
        when(permissionGroupItemMapper.selectCount(any())).thenReturn(1L);
        User current = new User();
        current.setId(1);
        current.setPrimaryGroupId(10);
        when(currentUserService.getCurrentUser()).thenReturn(current);

        PermissionGroup update = new PermissionGroup();
        update.setGroupCode(superGroup.getGroupCode());
        update.setGroupName(superGroup.getGroupName());
        ResultBean result = service.updateGroup(10, update, Arrays.asList(
                Permission.PERMISSION_MANAGE.getCode(),
                Permission.USER_GRANT_ADMIN.getCode()));

        assertNotEquals(200, result.getCode());
        verify(permissionGroupMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("删除预设方案 → 存在则删除")
    void deletePreset_success() {
        PermissionPreset preset = new PermissionPreset();
        preset.setId(9);
        preset.setPresetName("内容运营预设");
        when(permissionPresetMapper.selectById(9)).thenReturn(preset);

        ResultBean result = service.deletePreset(9);

        assertEquals(200, result.getCode());
        verify(permissionPresetMapper).deleteById(9);
    }

    @Test
    @DisplayName("权限组详情：返回权限点、主组人数与叠加人数")
    void getGroup_returnsPermissionAndCounts() {
        PermissionGroup builtIn = group();
        builtIn.setId(10);
        when(permissionGroupMapper.selectById(10)).thenReturn(builtIn);
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Collections.singletonList(
                item(10, Permission.CONFIG_WRITE)));
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userMapper.selectCount(any())).thenReturn(2L);
        when(userPermissionGroupMapper.selectCount(any())).thenReturn(1L);

        Map<String, Object> detail = service.getGroup(10);

        assertNotNull(detail);
        assertEquals(2L, detail.get("primaryUserCount"));
        assertEquals(1L, detail.get("memberCount"));
        assertEquals(3L, detail.get("userCount"));
        assertTrue(((java.util.List<?>) detail.get("permissionCodes"))
                .contains(Permission.CONFIG_WRITE.getCode()));
    }

    @Test
    @DisplayName("预设应用：覆盖名称/描述/权限点，不再涉及角色基线")
    void applyPreset_appliesDirectPermissions() throws Exception {
        PermissionGroup custom = group();
        custom.setId(1);
        when(permissionGroupMapper.selectById(1)).thenReturn(custom);
        when(permissionGroupMapper.selectCount(any())).thenReturn(0L);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("groupName", "运营组");
        snapshot.put("permissionCodes", Arrays.asList(
                Permission.ARTICLE_READ.getCode(), Permission.REPORT_REVIEW.getCode()));
        PermissionPreset preset = new PermissionPreset();
        preset.setId(100);
        preset.setSnapshotJson(objectMapper.writeValueAsString(snapshot));
        when(permissionPresetMapper.selectById(100)).thenReturn(preset);

        ResultBean result = service.applyPreset(100, 1);

        assertEquals(200, result.getCode());
        verify(permissionGroupMapper).updateById(argThat(g ->
                "运营组".equals(((PermissionGroup) g).getGroupName())));
        verify(permissionGroupItemMapper, times(2)).insert(any(PermissionGroupItem.class));
    }

    @Test
    @DisplayName("有效权限：返回用户与权限点")
    void effective_returnsCodes() {
        User user = new User();
        user.setId(5);
        user.setUsername("tester");
        when(userMapper.selectById(5)).thenReturn(user);
        when(effectivePermissionService.getEffectiveCodes(user))
                .thenReturn(new java.util.LinkedHashSet<>(Arrays.asList("admin:access", "article:read")));
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> data = service.effective(5);

        assertNotNull(data);
        assertEquals(5, data.get("userId"));
        assertTrue(((java.util.List<?>) data.get("permissions")).contains("article:read"));
    }

    @Test
    @DisplayName("删除权限组：主组用户迁移到 role_user，叠加绑定一并解除")
    @SuppressWarnings("unchecked")
    void deleteGroup_migratesPrimaryUsersToRoleUser() {
        PermissionGroup target = group();
        target.setId(10);
        PermissionGroup roleUser = group();
        roleUser.setId(4);
        roleUser.setGroupCode("role_user");
        PermissionGroup admin = group();
        admin.setId(2);
        admin.setGroupCode("role_admin");
        when(permissionGroupMapper.selectById(10)).thenReturn(target);
        when(permissionGroupMapper.selectList(any()))
                .thenReturn(Arrays.asList(target, admin, roleUser));
        // role_admin 同时具备三个关键权限，删除普通组时不会触发“最后一个超管组”保护
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Arrays.asList(
                item(2, Permission.ADMIN_ACCESS),
                item(2, Permission.PERMISSION_MANAGE),
                item(2, Permission.USER_GRANT_ADMIN)));
        when(currentUserService.getCurrentUser()).thenReturn(null);

        ResultBean result = service.deleteGroup(10);

        assertEquals(200, result.getCode());
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>>
                wrapperCaptor = ArgumentCaptor.forClass(
                com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(userMapper).update(isNull(), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getParamNameValuePairs().containsValue(4),
                "原主组用户必须迁移到 role_user，而不是留在空主组");
        verify(permissionGroupMapper).deleteById(10);
    }

    @Test
    @DisplayName("删除权限组：没有 role_user 时回退到非管理类全局组，不能把普通用户升级成管理员")
    @SuppressWarnings("unchecked")
    void deleteGroup_withoutRoleUser_fallsBackToNonAdminGlobalGroup() {
        PermissionGroup target = group();
        target.setId(10);
        PermissionGroup admin = group();
        admin.setId(2);
        admin.setGroupCode("role_admin");
        PermissionGroup front = group();
        front.setId(5);
        front.setGroupCode("content_front");
        PermissionGroup moderator = group();
        moderator.setId(3);
        moderator.setGroupCode("role_moderator");
        moderator.setDataScopeType("BOARD_MODERATOR");
        when(permissionGroupMapper.selectById(10)).thenReturn(target);
        when(permissionGroupMapper.selectList(any()))
                .thenReturn(Arrays.asList(target, admin, moderator, front));
        // 管理类组（admin:access）优先被排除；版主组因数据范围不是 GLOBAL 也被跳过
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Collections.singletonList(
                item(2, Permission.ADMIN_ACCESS)));
        when(currentUserService.getCurrentUser()).thenReturn(null);

        ResultBean result = service.deleteGroup(10);

        assertEquals(200, result.getCode());
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>>
                wrapperCaptor = ArgumentCaptor.forClass(
                com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(userMapper).update(isNull(), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getParamNameValuePairs().containsValue(5),
                "应回退到不具备 admin:access 的全局组");
        assertFalse(wrapperCaptor.getValue().getParamNameValuePairs().containsValue(2),
                "绝不能把被删组用户迁移到管理类组");
    }

    @Test
    @DisplayName("预设列表：返回快照里的来源组、数据范围与权限点，供页面展开详情")
    void listPresets_returnsSnapshotDetails() throws Exception {
        PermissionPreset preset = new PermissionPreset();
        preset.setId(7);
        preset.setPresetCode("preset-content");
        preset.setPresetName("内容运营预设");
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("groupCode", "content_operator");
        snapshot.put("groupName", "内容运营");
        snapshot.put("scopeType", "ALL");
        snapshot.put("dataScopeType", "GLOBAL");
        snapshot.put("permissionCodes", Arrays.asList(
                Permission.ARTICLE_READ.getCode(), Permission.POST_CREATE.getCode()));
        preset.setSnapshotJson(objectMapper.writeValueAsString(snapshot));
        when(permissionPresetMapper.selectList(any())).thenReturn(Collections.singletonList(preset));

        List<Map<String, Object>> rows = service.listPresets();

        assertEquals(1, rows.size());
        Map<String, Object> row = rows.get(0);
        assertEquals("内容运营", row.get("sourceGroupName"));
        assertEquals("content_operator", row.get("sourceGroupCode"));
        assertEquals("ALL", row.get("scopeType"));
        assertEquals("GLOBAL", row.get("dataScopeType"));
        assertEquals(Arrays.asList(Permission.ARTICLE_READ.getCode(), Permission.POST_CREATE.getCode()),
                row.get("permissionCodes"));
    }

    @Test
    @DisplayName("预设列表：历史快照损坏时仍返回基础字段，不影响页面加载")
    void listPresets_malformedSnapshot_returnsBaseFields() {
        PermissionPreset preset = new PermissionPreset();
        preset.setId(8);
        preset.setPresetCode("broken");
        preset.setPresetName("损坏预设");
        preset.setSnapshotJson("{not-json");
        when(permissionPresetMapper.selectList(any())).thenReturn(Collections.singletonList(preset));

        List<Map<String, Object>> rows = service.listPresets();

        assertEquals(1, rows.size());
        assertEquals("broken", rows.get(0).get("presetCode"));
        assertFalse(rows.get(0).containsKey("permissionCodes"),
                "损坏快照不应伪造权限点，也不应抛异常");
    }

    @Test
    @DisplayName("保存预设：编码重复时拒绝，避免覆盖历史快照")
    void createPreset_duplicateCode_rejected() {
        PermissionGroup group = group();
        group.setId(1);
        when(permissionGroupMapper.selectById(1)).thenReturn(group);
        when(permissionPresetMapper.selectCount(any())).thenReturn(1L);

        ResultBean result = service.createPreset("preset-dup", "重复预设", 1);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("已存在"));
        verify(permissionPresetMapper, never()).insert(any(PermissionPreset.class));
    }

    @Test
    @DisplayName("适用端自动标记：只勾前台动作 → FRONT")
    void replaceItems_frontOnly_marksFrontScope() {
        PermissionGroup group = group();
        group.setId(1);
        group.setEnabled(1);
        when(permissionGroupMapper.selectById(1)).thenReturn(group);
        when(permissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());

        ResultBean result = service.replaceItems(1,
                Collections.singletonList(Permission.POST_CREATE.getCode()));

        assertEquals(200, result.getCode());
        verify(permissionGroupMapper).updateById(argThat(g ->
                "FRONT".equals(((PermissionGroup) g).getScopeType())));
    }

    @Test
    @DisplayName("适用端自动标记：管理端 + 前台动作同时勾选 → ALL")
    void replaceItems_adminAndFront_marksAllScope() {
        PermissionGroup group = group();
        group.setId(1);
        group.setEnabled(1);
        when(permissionGroupMapper.selectById(1)).thenReturn(group);
        when(permissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());

        ResultBean result = service.replaceItems(1, Arrays.asList(
                Permission.ARTICLE_READ.getCode(), Permission.POST_CREATE.getCode()));

        assertEquals(200, result.getCode());
        verify(permissionGroupMapper).updateById(argThat(g ->
                "ALL".equals(((PermissionGroup) g).getScopeType())));
    }

    @Test
    @DisplayName("停用当前账号自己的主权限组 → 拒绝，避免登录后立即失去后台入口")
    void updateGroup_disableOwnPrimaryGroup_blocked() {
        PermissionGroup superGroup = group();
        superGroup.setId(10);
        superGroup.setGroupCode("role_super");
        superGroup.setEnabled(1);
        when(permissionGroupMapper.selectById(10)).thenReturn(superGroup);
        when(permissionGroupMapper.selectCount(any())).thenReturn(0L);
        when(permissionGroupItemMapper.selectCount(any())).thenReturn(1L);
        User current = new User();
        current.setId(1);
        current.setPrimaryGroupId(10);
        when(currentUserService.getCurrentUser()).thenReturn(current);

        PermissionGroup update = new PermissionGroup();
        update.setGroupCode("role_super");
        update.setGroupName("超级管理员");
        update.setEnabled(0);
        ResultBean result = service.updateGroup(10, update, null);

        assertNotEquals(200, result.getCode());
        verify(permissionGroupMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("还有其他超管组时：允许收回当前组的权限点，保护逻辑不能过度拦截")
    void replaceItems_otherSuperGroupExists_allowed() {
        PermissionGroup first = group();
        first.setId(10);
        first.setEnabled(1);
        PermissionGroup second = group();
        second.setId(20);
        second.setEnabled(1);
        when(permissionGroupMapper.selectById(10)).thenReturn(first);
        when(permissionGroupMapper.selectList(any())).thenReturn(Arrays.asList(first, second));
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Arrays.asList(
                item(10, Permission.ADMIN_ACCESS),
                item(10, Permission.PERMISSION_MANAGE),
                item(10, Permission.USER_GRANT_ADMIN),
                item(20, Permission.ADMIN_ACCESS),
                item(20, Permission.PERMISSION_MANAGE),
                item(20, Permission.USER_GRANT_ADMIN)));
        when(currentUserService.getCurrentUser()).thenReturn(null);

        ResultBean result = service.replaceItems(10, Collections.emptyList());

        assertEquals(200, result.getCode());
        verify(permissionGroupMapper).updateById(any(PermissionGroup.class));
        verify(permissionGroupItemMapper).delete(any());
    }

    private PermissionGroupItem item(int groupId, Permission permission) {
        PermissionGroupItem item = new PermissionGroupItem();
        item.setGroupId(groupId);
        item.setPermissionCode(permission.getCode());
        item.setEffect("grant");
        return item;
    }
}
