# 实施计划: 首页工作台 v2 收尾（GOVERNANCE 卡 / 质控卡 / welcome 补全 / FR5 快捷患者检索）

**日期**: 2026-09-11
**需求来源**: `过程文件/20260909_首页工作台/`（PRD FR1 收尾 + FR4 + FR5）；v2 实现记录 `docs/feature/2026-09-09-workspace-v2.md` 的"已知限制"清单
**分支**: feature/disease-kb（沿用 v2 工作分支）
**目标**: 补齐 PRD 中标注"降级/P1"的收尾项，使 4 个角色组的指标卡全部为专属定义，并交付快捷患者检索

## 范围（本轮）

| 项 | PRD | 说明 |
|----|-----|------|
| GOVERNANCE 专属四卡 | FR1 | 平台用户 / 今日审计事件 / 今日权限拒绝 / 活跃告警，替代当前复用 DATA 卡的降级 |
| DATA 组第 4 卡改"待处理质控" | FR1 | 按 PRD 卡片定义，替换"待审批"（审批数保留在 metrics 字段与 RESEARCH 卡） |
| welcome 补机构/科室 | FR4 | welcome 增加 orgName/deptName（可空，逐项降级），前端展示 |
| FR5 快捷患者检索 | FR5（P1） | 欢迎栏检索框 → `/data/cdr/patients?keyword=`；PatientList 透传 query.keyword |

不做（保持既有结论）：FR6 cohortDigest（需队列事件落库前置工程）、FR7/FR8、"开始随访"路由替换（待随访工作台正式路由）。

## 数据源（全部走既有 safeQuery 跨 schema 直查模式，失败降级 0/空）

| 指标 | SQL | 依据 |
|------|-----|------|
| 平台用户 | `SELECT COUNT(*) FROM system.s_user WHERE org_id=:orgId AND is_deleted=false` | 02-system.sql:4-24 |
| 今日审计事件 | `... FROM audit.a_audit_log WHERE org_id=:orgId AND created_at>=CURRENT_DATE` | 06-audit.sql:10-36（索引 idx_a_audit_log_created_at） |
| 今日权限拒绝 | `... FROM audit.a_system_event WHERE org_id=:orgId AND event_type='PERMISSION_DENIED' AND created_at>=CURRENT_DATE` | 18-audit-event-type.sql（PERMISSION_DENIED 枚举） |
| 活跃告警 | `... FROM model.m_alert_record WHERE org_id=:orgId AND status IN ('PENDING','ACKNOWLEDGED')` | 03-model.sql:239-254（无 is_deleted 列，勿加） |
| 待处理质控 | `... FROM cdr.cdr_quarantine_data WHERE org_id=:orgId AND status='PENDING' AND is_deleted=false` | CdrQuarantineDataEntity.java:23,58-59（PENDING/ASSIGNED/PROCESSING/FIXED/DISCARDED/RELEASED，取 PENDING=待处理）；org_id/is_deleted 由 BaseEntity 提供 |

welcome 科室/机构名：复用 `PermissionStore.load(userId)` → `PermissionContext.deptId` → `InstitutionRepository.findById` → `InstitutionEntity.name`（与 `DeptScopeService.deptFilterName` 同链路，DeptScopeService.java:37-50）；orgName 同表按 orgId 查（查不到即为空）。全部 try/catch 降级，welcome 永不阻塞工作台。

## 需要修改的文件

### 后端（maidc-parent）

| 文件 | 修改 |
|------|------|
| `task/repository/WorkspaceMetricsRepository.java` | +5 个计数方法（safeQuery 模式） |
| `task/vo/WorkspaceDashboardVO.java` | WelcomeInfo +orgName/deptName（可空） |
| `task/service/WorkspaceService.java` | +governanceCards；dataCards 第 4 卡改质控；buildWelcome 补机构/科室（注入 PermissionStore/InstitutionRepository，降级安全） |
| `task/service/WorkspaceServiceTest.java` | +governance 用例、DATA 第 4 卡断言更新、welcome 科室解析用例 |

### 前端（maidc-portal）

| 文件 | 修改 |
|------|------|
| `src/api/workspace.ts` | WelcomeInfo +orgName?/deptName? |
| `src/views/dashboard/workspace/WelcomeSection.vue` | +机构·科室行；+快捷患者检索框（hasPermission('cdr:read') 控制），回车跳患者列表 |
| `src/views/dashboard/workspace/WorkspaceView.vue` | 透传 orgName/deptName |
| `src/views/data-cdr/PatientList.vue` | onMounted 读 route.query.keyword → setSearchParams + formState 回填 |

icon 复用现有映射（icons.ts 已含 user/file-search/shield/alert），无新增。

## 实施步骤

### Task 1 后端指标计数
- [ ] WorkspaceMetricsRepository +5 方法

### Task 2 VO 与服务
- [ ] WelcomeInfo +orgName/deptName
- [ ] governanceCards / dataCards 质控卡 / buildWelcome

### Task 3 测试
- [ ] 新增/更新 WorkspaceServiceTest 用例
- [ ] `mvn -pl maidc-data -am test -Dtest=WorkspaceServiceTest`

### Task 4 前端
- [ ] api/workspace.ts、WelcomeSection、WorkspaceView、PatientList
- [ ] `npm run type-check`（vue-tsc）

### Task 5 记录
- [ ] docs/feature/2026-09-11-workspace-v2-finalize.md

## 验收标准

1. GOVERNANCE 组返回 4 张专属卡（user_count/audit_today/perm_denied_today/active_alerts）
2. DATA 组第 4 卡为 quality_pending（待处理质控）
3. welcome 含 orgName/deptName（无科室用户为 null，不报错）
4. 前端欢迎栏出现检索框（cdr:read 用户），回车后患者列表带关键词过滤
5. mvn 测试与 vue-tsc 通过
