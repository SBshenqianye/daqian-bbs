# 大千智荟 BBS 后端单元测试报告

## 测试概览

| 指标 | 数值 |
|------|------|
| 执行时间 | 2026-09-02 12:03 |
| 测试框架 | JUnit 5 + Mockito 4.x（Spring Boot 2.5.5 自带） |
| 测试类型 | 后端 Service 层单元测试（纯 Mockito，不启动 Spring 容器） |
| 执行命令 | `mvn test "-Dmaven.repo.local=./target/test-repo"` |
| 总用例数 | **154** |
| 通过 | **147** ✅ |
| 跳过（@Disabled） | **7** ⏭️ |
| 失败 | **0** ❌ |
| 构建状态 | **BUILD SUCCESS** ✅ |

---

## 测试覆盖的 Service 类（16 个测试文件）

| # | 测试文件 | 被测 Service | 用例数 | 通过 | 跳过 | 失败 |
|---|---------|-------------|--------|------|------|------|
| 1 | UserServiceTest | UserServiceImpl | 19 | 19 | 0 | 0 |
| 2 | ArticleServiceTest | ArticleServiceImpl | 15 | 15 | 0 | 0 |
| 3 | CommentServiceTest | CommentServiceImpl | 8 | 8 | 0 | 0 |
| 4 | ReplyServiceTest | ReplyServiceImpl | 5 | 5 | 0 | 0 |
| 5 | PointsLogServiceTest | PointsLogServiceImpl | 15 | 15 | 0 | 0 |
| 6 | NotificationServiceTest | NotificationServiceImpl | 11 | 8 | 3 | 0 |
| 7 | AppealServiceTest | AppealServiceImpl | 9 | 9 | 0 | 0 |
| 8 | ViolationServiceTest | ViolationServiceImpl | 7 | 6 | 1 | 0 |
| 9 | SensitiveWordServiceTest | SensitiveWordServiceImpl | 7 | 7 | 0 | 0 |
| 10 | FeaturedRecommendationServiceTest | FeaturedRecommendationServiceImpl | 11 | 11 | 0 | 0 |
| 11 | BoardModeratorServiceTest | BoardModeratorServiceImpl | 14 | 14 | 0 | 0 |
| 12 | ModeratorComplaintServiceTest | ModeratorComplaintServiceImpl | 10 | 10 | 0 | 0 |
| 13 | SaOrgServiceTest | SaOrgServiceImpl | 5 | 5 | 0 | 0 |
| 14 | ReportServiceTest | ReportServiceImpl | 6 | 6 | 0 | 0 |
| 15 | DictServiceTest | DictServiceImpl | 6 | 4 | 2 | 0 |
| 16 | SystemConfigServiceTest | SystemConfigServiceImpl | 6 | 4 | 2 | 0 |

---

## 核心测试场景明细

### 1. UserServiceTest（19 个用例）

| # | 场景 | 结果 |
|---|------|------|
| 1 | 登录成功 → 返回 token | ✅ |
| 2 | 登录密码错误 → 返回错误 | ✅ |
| 3 | 非法渠道登录 → 返回错误 | ✅ |
| 4 | 注册功能已关闭 → 返回错误 | ✅ |
| 5 | 修改密码 → 用户不存在 | ✅ |
| 6 | 修改密码 → 原密码错误 | ✅ |
| 7 | 修改密码 → 新密码强度不足 | ✅ |
| 8 | 修改密码 → 新密码与原密码相同 | ✅ |
| 9 | 修改角色 → 有效角色 → 成功 | ✅ |
| 10 | 修改角色 → 无效角色 → 返回错误 | ✅ |
| 11 | 修改单位 → 用户不存在 | ✅ |
| 12 | 修改单位 → 相同单位 | ✅ |
| 13 | 新增用户 → 用户名已存在 | ✅ |
| 14 | 新增用户 → 人员编号和身份证号都为空 | ✅ |
| 15 | 管理员更新用户 → 用户 ID 为空 | ✅ |
| 16 | 管理员更新用户 → 用户不存在 | ✅ |
| 17 | 删除用户 → 调用 mapper 删除 | ✅ |
| 18 | 获取用户总数 → 返回计数 | ✅ |
| 19 | 切换用户状态 → 0 变 1 | ✅ |

### 2. ArticleServiceTest（15 个用例）

| # | 场景 | 结果 |
|---|------|------|
| 1 | 发帖 → 标题为空 | ✅ |
| 2 | 发帖 → 内容为空 | ✅ |
| 3 | 发帖 → 标题含 HTML 标签 | ✅ |
| 4 | 发帖 → 标签已被禁用 | ✅ |
| 5 | 发帖 → 账号被限制发帖 | ✅ |
| 6 | 发帖 → 正常发布成功 | ✅ |
| 7 | 编辑文章 → 标题为空 | ✅ |
| 8 | 删除文章 → 已审核通过 → 扣回积分 | ✅ |
| 9 | 删除文章 → 精华帖 → 额外扣回精华积分 | ✅ |
| 10 | 删除文章 → 未审核通过 → 不扣积分 | ✅ |
| 11 | 设置精华 → 文章不存在 | ✅ |
| 12 | 设置精华 → 从非精华设为精华 → 加分 | ✅ |
| 13 | 取消精华 → 扣回 | ✅ |
| 14 | 获取文章总数 | ✅ |
| 15 | 批量审核 | ✅ |

### 3. CommentServiceTest（8 个用例）

| # | 场景 | 结果 |
|---|------|------|
| 1 | 评论成功 → 保存记录 | ✅ |
| 2 | 评论 → 自己帖子内评论不计积分 | ✅ |
| 3 | 评论 → 已达 3 次积分上限 | ✅ |
| 4 | 删除评论 → 已审核通过 → 扣回积分 | ✅ |
| 5 | 删除评论 → 未审核通过 → 不扣积分 | ✅ |
| 6 | 热度奖励 → 互动数未达阈值 | ✅ |
| 7 | 热度奖励 → 已发过 → 不重复发 | ✅ |
| 8 | 热度奖励 → 互动数达阈值 → 发奖励并通知 | ✅ |

### 4. PointsLogServiceTest（15 个用例）

| # | 场景 | 结果 |
|---|------|------|
| 1 | 新增积分 → 参数不完整 | ✅ |
| 2 | 新增积分 → 正常保存 | ✅ |
| 3 | 调整积分 → 参数不完整 | ✅ |
| 4 | 调整积分 → 正常加分 | ✅ |
| 5 | 撤销积分 → 记录 ID 为空 | ✅ |
| 6 | 撤销积分 → 记录不存在 | ✅ |
| 7 | 撤销积分 → 已被撤销 | ✅ |
| 8 | 撤销积分 → 撤销记录本身 | ✅ |
| 9 | 撤销积分 → 正常撤销 | ✅ |
| 10 | 统计回帖积分 → 参数为空 | ✅ |
| 11 | 统计回帖积分 → 正常返回 | ✅ |
| 12 | 统计回帖积分 → mapper 返回 null | ✅ |
| 13 | 统计采纳积分 → 参数为空 | ✅ |
| 14 | 统计建议采纳 → 参数为空 | ✅ |
| 15 | 查询积分调整总和 | ✅ |

### 5. BoardModeratorServiceTest（14 个用例）

| # | 场景 | 结果 |
|---|------|------|
| 1 | 任命版主 → 参数不完整 | ✅ |
| 2 | 任命版主 → 已是版主 | ✅ |
| 3 | 任命版主 → 积分不足 300 | ✅ |
| 4 | 任命版主 → 正常任命 | ✅ |
| 5 | 撤销版主 → 参数不完整 | ✅ |
| 6 | 撤销版主 → 不是版主 | ✅ |
| 7 | 撤销版主 → 正常撤销 | ✅ |
| 8 | 判断是否版主 → 是 | ✅ |
| 9 | 判断是否版主 → 否 | ✅ |
| 10 | 月度奖励 → 无有效版主 | ✅ |
| 11 | 取消奖励 → userId 为空 | ✅ |
| 12 | 恢复奖励 → userId 为空 | ✅ |
| 13 | 恢复奖励 → 未被取消 | ✅ |
| 14 | 恢复奖励 → 已被取消 | ✅ |

### 6. FeaturedRecommendationServiceTest（11 个用例）

| # | 场景 | 结果 |
|---|------|------|
| 1 | 推荐精华 → 参数不完整 | ✅ |
| 2 | 推荐精华 → 文章不存在 | ✅ |
| 3 | 推荐精华 → 已是精华帖 | ✅ |
| 4 | 推荐精华 → 已有待审推荐 | ✅ |
| 5 | 推荐精华 → 正常提交 → 通知超管 | ✅ |
| 6 | 审核推荐 → 参数不完整 | ✅ |
| 7 | 审核推荐 → 无效状态 | ✅ |
| 8 | 审核推荐 → 记录不存在 | ✅ |
| 9 | 审核推荐 → 已处理 | ✅ |
| 10 | 审核推荐 → 通过 → 设为精华并加分 | ✅ |
| 11 | 审核推荐 → 拒绝 → 通知推荐人 | ✅ |

### 7. 跳过的用例说明（7 个 @Disabled）

这些用例涉及 MyBatis-Plus 框架内部的 `LambdaUpdateWrapper` 实体元数据解析或 `ServiceImpl.removeById()` 的默认方法调用链，需要 Spring 容器初始化 `TableInfoHelper`，在纯 Mockito 环境下无法模拟。

| 测试文件 | 跳过的用例 | 原因 |
|---------|-----------|------|
| DictServiceTest | removeDictById_succeeds | `ServiceImpl.removeById()` 需要 MyBatis-Plus 上下文 |
| DictServiceTest | removeDictById_fails | 同上 |
| SystemConfigServiceTest | removeConfigById_succeeds | 同上 |
| SystemConfigServiceTest | removeConfigById_fails | 同上 |
| NotificationServiceTest | markRead_byCategory_succeeds | `LambdaUpdateWrapper` 需要实体类元数据 |
| NotificationServiceTest | markRead_byType_succeeds | 同上 |
| NotificationServiceTest | markAllRead_succeeds | 同上 |

> **建议**：后续可通过集成测试（`@MybatisTest` + H2 内存库）覆盖这些场景，或升级 MyBatis-Plus 至支持纯 Mockito 测试的版本。

---

## 测试文件清单

```
bbs-server/src/test/java/com/walker/service/
├── AppealServiceTest.java            (9 用例)
├── ArticleServiceTest.java           (15 用例)
├── BoardModeratorServiceTest.java    (14 用例)
├── CommentServiceTest.java           (8 用例)
├── DictServiceTest.java              (6 用例, 2 skipped)
├── FeaturedRecommendationServiceTest.java  (11 用例)
├── ModeratorComplaintServiceTest.java (10 用例)
├── NotificationServiceTest.java      (11 用例, 3 skipped)
├── PointsLogServiceTest.java         (15 用例)
├── ReplyServiceTest.java             (5 用例)
├── ReportServiceTest.java            (6 用例)
├── SaOrgServiceTest.java             (5 用例)
├── SensitiveWordServiceTest.java     (7 用例)
├── SystemConfigServiceTest.java      (6 用例, 2 skipped)
├── UserServiceTest.java              (19 用例)
├── ViolationServiceTest.java         (7 用例, 1 skipped)
└── DateTest.java                     (已有)
```

---

## 技术要点

### Mock 策略

所有测试使用 `@ExtendWith(MockitoExtension.class)` + `@InjectMocks` + `@Mock`，不启动 Spring 容器。

**关键问题**：MyBatis-Plus `ServiceImpl<M, T>` 的 `baseMapper` 字段由父类声明，子类（如 `DictServiceImpl`）同时声明同类型的 `dictMapper` 字段，导致 `@InjectMocks` 仅注入同名字段，`baseMapper` 为 null → NPE。

**解决方案**：每个测试类添加 `@BeforeEach` 方法，通过反射手动设置 `baseMapper`：

```java
@BeforeEach
void setUp() throws Exception {
    Field baseMapperField = xxxService.getClass().getSuperclass()
        .getDeclaredField("baseMapper");
    baseMapperField.setAccessible(true);
    baseMapperField.set(xxxService, xxxMapper);
}
```

### 关键 Mock 模式

| 场景 | Mock 方式 |
|------|----------|
| 查询返回空 | `when(mapper.selectById(id)).thenReturn(null)` |
| 条件查询计数 | `when(mapper.selectCount(any())).thenReturn(0L)` |
| 保存成功 | `when(mapper.insert(any())).thenReturn(1)` |
| 链式返回不同值 | `.thenReturn(0L).thenReturn(1L)` |
| 验证调用 | `verify(mapper, atLeastOnce()).insert(any())` |
| 验证不调用 | `verify(mapper, never()).deleteById(any())` |
