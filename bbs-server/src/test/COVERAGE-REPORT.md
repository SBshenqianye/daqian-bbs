# BBS-Server 代码覆盖率报告

**生成时间**：2026-09-02 14:55  
**工具版本**：JaCoCo 0.8.12  
**测试用例**：154 个（通过 154，跳过 8，失败 0）  
**分析类数**：124 个  

---

## 总体覆盖率

| 指标 | 覆盖/总数 | 覆盖率 |
|------|----------|--------|
| **行覆盖率 (Line)** | 974 / 5,425 | **18.0%** |
| **分支覆盖率 (Branch)** | 407 / 2,669 | **15.2%** |
| **指令覆盖率 (Instruction)** | 4,494 / 27,033 | **16.6%** |
| **方法覆盖率 (Method)** | 153 / 769 | **19.9%** |

---

## 各包覆盖率

| 包 | 行覆盖 | 行覆盖率 | 分支覆盖 | 分支覆盖率 |
|----|--------|---------|----------|-----------|
| com.walker | 0 / 74 | 0.0% | 0 / 62 | 0.0% |
| com.walker.config | 0 / 380 | 0.0% | 0 / 142 | 0.0% |
| com.walker.config.security | 0 / 99 | 0.0% | 0 / 18 | 0.0% |
| com.walker.controller | 0 / 1,602 | 0.0% | 0 / 890 | 0.0% |
| com.walker.pojo | 69 / 106 | 65.1% | 1 / 6 | 16.7% |
| com.walker.service.impl | 802 / 2,876 | **27.9%** | 344 / 1,317 | **26.1%** |
| com.walker.utils | 100 / 275 | **36.4%** | 62 / 234 | **26.5%** |
| com.walker.vo | 3 / 12 | 25.0% | 0 / 0 | — |
| com.walker.vo.param | 0 / 1 | 0.0% | 0 / 0 | — |

---

## 有覆盖的类（行覆盖率 > 0%）详情

| 类 | 行覆盖 | 行覆盖率 | 分支覆盖 | 分支覆盖率 | 状态 |
|----|--------|---------|----------|-----------|------|
| CommentServiceImpl | 73/74 | **98.6%** | 27/40 | 67.5% | ✅ |
| PointsLogServiceImpl | 53/68 | **77.9%** | 25/36 | 69.4% | ✅ |
| NotificationServiceImpl | 49/62 | **79.0%** | 15/26 | 57.7% | ⚠️ |
| ReplyServiceImpl | 45/60 | **75.0%** | 20/36 | 55.6% | ✅ |
| ContentQualityUtil | 70/96 | **72.9%** | 48/76 | 63.2% | ✅ |
| FeaturedRecommendationServiceImpl | 60/98 | **61.2%** | 33/62 | 53.2% | ❌ |
| DictServiceImpl | 8/12 | **66.7%** | — | — | ❌ |
| FeaturedRecommendation (pojo) | 13/19 | **68.4%** | — | — | ❌ |
| ContentQualityUtil.QualityResult | 8/15 | 53.3% | 1/2 | 50.0% | ❌ |
| SystemConfigServiceImpl | 8/15 | 53.3% | — | — | ❌ |
| ModeratorComplaintServiceImpl | 47/105 | **44.8%** | 31/66 | 47.0% | ❌ |
| SensitiveWordServiceImpl | 29/71 | 40.8% | 8/22 | 36.4% | ❌ |
| ViolationServiceImpl | 65/162 | 40.1% | 23/93 | 24.7% | ❌ |
| AppealServiceImpl | 41/111 | 36.9% | 21/54 | 38.9% | ❌ |
| PinyinUtil | 17/52 | 32.7% | 13/60 | 21.7% | ❌ |
| BoardModeratorServiceImpl | 50/181 | 27.6% | 23/66 | 34.8% | ❌ |
| ArticleServiceImpl | 116/532 | 21.8% | 55/256 | 21.5% | ❌ |
| ReportServiceImpl | 52/243 | 21.4% | 20/120 | 16.7% | ❌ |
| SensitiveWordUtil | 5/26 | 19.2% | 0/14 | 0.0% | ❌ |
| SaOrgServiceImpl | 22/121 | 18.2% | 12/98 | 12.2% | ❌ |
| UserServiceImpl | 84/522 | 16.1% | 31/216 | 14.4% | ❌ |
| NotificationCategory (pojo) | 23/25 | **92.0%** | 1/6 | 16.7% | ✅ |
| ModeratorComplaint (pojo) | 16/21 | 76.2% | — | — | ✅ |
| ResultBean | 3/5 | 60.0% | — | — | ❌ |
| User (pojo) | 6/6 | 100.0% | — | — | ✅ |
| 其他 pojo（Appeal, PointsLog, Notification 等） | — | 100.0% | — | — | ✅ |

---

## 0% 覆盖率的类（共 88 个）

### Service 层（21 个）— 需优先补充测试

| 类 | 行数 | 分支数 | 优先级 |
|----|------|--------|--------|
| **OrgImportService** | 105 | 48 | 🔴 高 |
| **CommunityServiceImpl** | 57 | 6 | 🔴 高 |
| **FansServiceImpl** | 53 | 10 | 🔴 高 |
| **LoginLogServiceImpl** | 74 | 22 | 🟡 中 |
| **UserServiceImpl.ImportTask** | 21 | 4 | 🟡 中 |
| **ArticleUserServiceImpl** | 29 | 4 | 🟡 中 |
| **ArticleFileServiceImpl** | 11 | 0 | 🟡 中 |
| **SlideshowServiceImpl** | 20 | 0 | 🟡 中 |
| **CommunityUserServiceImpl** | 19 | 4 | 🟡 中 |
| **InventoryServiceImpl** | 13 | 10 | 🟢 低 |
| **OrgImportService.OrgPair** | 7 | 10 | 🟢 低 |
| **OrgImportService.OrgImportResult** | 2 | 2 | 🟢 低 |
| **ArticleLabelServiceImpl** | 14 | 4 | 🟢 低 |
| **AdminServiceImpl** | 9 | 2 | 🟢 低 |
| **AreaServiceImpl** | 2 | 0 | 🟢 低 |
| **ArticleTypeServiceImpl** | 3 | 0 | 🟢 低 |

### Controller 层（32 个）— 0% 覆盖率

| 类 | 行数 | 分支数 |
|----|------|--------|
| **ArticleController** | 279 | 158 |
| **ReplyController** | 221 | 168 |
| **ReplyListController** | 230 | 100 |
| **UserController** | 218 | 92 |
| **BoardModeratorController** | 57 | 24 |
| **CommentController** | 58 | 12 |
| **ArticleLabelController** | 54 | 40 |
| **CommonController** | 52 | 68 |
| **CommunityController** | 40 | 22 |
| **ArticleFileController** | 39 | 18 |
| **NotificationController** | 39 | 20 |
| **SensitiveWordController** | 38 | 20 |
| **AdminController** | 28 | 12 |
| **SystemConfigController** | 27 | 24 |
| **ReportController** | 24 | 12 |
| **SaOrgController** | 21 | 12 |
| **SlideshowController** | 23 | 12 |
| **AppealController** | 19 | 10 |
| **DictController** | 21 | 16 |
| **ModeratorComplaintController** | 17 | 16 |
| **ViolationController** | 16 | 12 |
| **FeaturedRecommendationController** | 14 | 14 |
| **FansController** | 10 | 4 |
| GlobalExceptionHandler + 内部类 | 25 | 0 |
| 其他小 Controller（7 个） | — | — |

### Config 层（8 个）— 0% 覆盖率

| 类 | 行数 |
|----|------|
| DatabaseInitializer | 216 |
| DatabaseInitHelper | 101 |
| SecurityConfig | 29 |
| JwtTokenUtil | 34 |
| JwtAuthenticationTokenFilter | 16 |
| RestfulAccessDeniedHandler | 10 |
| RestAuthorizationEntryPoint | 10 |
| Swagger2Config | 32 |
| WebMvcConfig | 16 |
| MyBatisConfig | 10 |

### 其他（pojo/vo/bbs）

| 类 | 行数 |
|----|------|
| BBSApplication | 74 |
| FilePathNormalizer | 15 |
| FileValidationUtil | 56 |
| ConstantUtil | 1 |
| ImageUtil | 14 |
| ModeratorRewardCancel (pojo) | 13 |

---

## 覆盖率不足的类（低于 70%）及建议

### 🔴 优先级高（核心业务逻辑，行数多）

1. **ArticleServiceImpl** — 行覆盖率 21.8%，21.5% 分支
   - 建议：补充文章发布、编辑、删除、搜索、分页查询的单元测试
   - 532 行代码，是项目最核心的业务服务

2. **UserServiceImpl** — 行覆盖率 16.1%，14.4% 分支
   - 建议：补充用户注册、登录、信息修改、权限检查的测试
   - 522 行代码，包含复杂的安全逻辑

3. **ReportServiceImpl** — 行覆盖率 21.4%，16.7% 分支
   - 建议：补充举报提交、审核、积分奖励、通知的测试
   - 243 行，已有测试但覆盖不足

4. **ArticleServiceImpl** — 行覆盖率 21.8%
   - 建议：优先测试文章 CRUD 核心方法

5. **ReportServiceImpl** — 行覆盖率 21.4%
   - 建议：补充举报相关测试

6. **SaOrgServiceImpl** — 行覆盖率 18.2%
   - 建议：补充组织机构导入、查询测试

7. **OrgImportService** — 行覆盖率 0%
   - 建议：补充 Excel 导入、数据校验测试

### 🟡 优先级中

8. **BoardModeratorServiceImpl** — 行覆盖率 27.6%
   - 建议：补充版主管理、权限检查测试

9. **ViolationServiceImpl** — 行覆盖率 40.1%
   - 建议：补充违规记录、处罚执行测试

10. **AppealServiceImpl** — 行覆盖率 36.9%
    - 建议：补充申诉提交、审核流程测试

### 🟢 优先级低

11. **Controller 层整体** — 0% 覆盖率（32 个类）
    - 建议：Controller 层通常通过 E2E 测试覆盖，单元测试优先级较低
    - 如果需要单元测试，可用 `@WebMvcTest` + MockMvc 测试接口参数校验和返回值

12. **Config 层** — 0% 覆盖率
    - 建议：配置类通常不需要单元测试，通过集成测试验证即可

---

## 测试执行详情

```
Tests run: 154, Failures: 0, Errors: 0, Skipped: 8
Total time: 26.250 s

测试类明细：
├── AppealServiceTest            9 tests (0 skipped)
├── ArticleServiceTest          15 tests (0 skipped)
├── BoardModeratorServiceTest   14 tests (0 skipped)
├── CommentServiceTest           8 tests (0 skipped)
├── DictServiceTest              6 tests (2 skipped)
├── FeaturedRecommendationServiceTest 11 tests (0 skipped)
├── ModeratorComplaintServiceTest 10 tests (0 skipped)
├── NotificationServiceTest     11 tests (3 skipped)
├── PointsLogServiceTest        15 tests (0 skipped)
├── ReplyServiceTest             5 tests (0 skipped)
├── ReportServiceTest            6 tests (0 skipped)
├── SaOrgServiceTest             5 tests (0 skipped)
├── SensitiveWordServiceTest     7 tests (0 skipped)
├── SystemConfigServiceTest      6 tests (2 skipped)
├── UserServiceTest             19 tests (0 skipped)
└── ViolationServiceTest         7 tests (1 skipped)
```

---

## 建议提升覆盖率的路径

### 短期（目标 30%+）
1. 为 `ArticleServiceImpl` 的核心方法（发布、编辑、删除、搜索）补充 15-20 个测试用例
2. 为 `UserServiceImpl` 的登录、注册、权限方法补充 10 个测试用例
3. 为 `ReportServiceImpl` 补充举报相关边界测试 5 个用例
4. 为 `SaOrgServiceImpl` 补充组织导入测试 5 个用例

### 中期（目标 50%+）
5. 为 `BoardModeratorServiceImpl`、`ViolationServiceImpl`、`AppealServiceImpl` 各补充 5-10 个用例
6. 为 `ModeratorComplaintServiceImpl` 补充投诉相关测试
7. 为 `FeaturedRecommendationServiceImpl` 补充推荐逻辑测试

### 长期（目标 70%+）
8. 为 Controller 层添加 `@WebMvcTest` 单元测试（参数校验、权限拦截）
9. 补充工具类（PinyinUtil、FileValidationUtil）的边界测试
10. 考虑集成测试覆盖 Config 层的启动流程

---

## 文件路径

- **JaCoCo CSV 报告**：`bbs-server/target/site/jacoco/jacoco.csv`
- **JaCoCo HTML 报告**：`bbs-server/target/site/jacoco/index.html`（浏览器打开查看详情）
- **原始执行数据**：`bbs-server/target/jacoco.exec`
