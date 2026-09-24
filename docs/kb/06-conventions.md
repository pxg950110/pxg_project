# 06 研发流程与惯例

## 功能研发全流程（本仓库实际运转口径）

```
过程文件/YYYYMMDD[_名称]/            ← PM 流三件套：01 需求合理性评估（五维打分）→ 02 功能需求 PRD（FR 编号）→ 03 原型说明
        ↓
maidc-portal/src/views/prototype/    ← 高保真可运行原型，注册 constantRoutes（/proto/xxx，不进菜单）
        ↓
docs/plans/yyyy-mm-dd-<slug>.md      ← 实施计划（范围/文件清单/任务拆分/验收标准）
        ↓
编码（feature/disease-kb 等工作分支）→ mvn 单测 + vue-tsc（改动文件零错误口径）
        ↓
docs/feature/yyyy-mm-dd-<slug>.md    ← 功能实现记录（范围/文件清单/验证结果/已知限制）
        ↓
原型目录 + 路由块整体删除（评审通过、正式实现后）
```

现有实例：`过程文件/2026-09-08/`（专病知识库）、`过程文件/20260909/`（CRS 随访）、`过程文件/20260909_首页工作台/`（工作台 v2）。

## 文档体系

| 目录 | 用途 |
|------|------|
| docs/kb/ | 本知识库（调研事实 + 域知识，带 file:line 证据） |
| docs/code-structure.md | 仓库结构权威文档（新人向，2026-09-08 版） |
| docs/superpowers/specs/ | 功能设计 spec（11 份，含 personal-workspace / permission-system / disease-management 等） |
| docs/superpowers/plans/ | 设计→实施计划 |
| docs/plans/、docs/feature/ | 近期迭代的实施计划 / 实现记录（按日期命名） |
| docs/dev/ | 01-architecture ~ 06-dev-setup 开发者指南 |
| docker/init-db/NNN-*.sql | DDL/种子按编号递增；**部署需手动 psql 执行新增脚本** |

## 代码惯例

### 后端（Spring Boot 3 + JPA）
- 实体：`@Where(is_deleted=false)` + `@SQLDelete` 软删 + BaseEntity 审计字段 + jsonb 用 String 列；schema 内无外键，业务层保证。
- 返回统一 `R<T>`；错误 `BusinessException + ErrorCode`；操作日志 `@OperLog(module, operation)`。
- 权限：方法级 `@PreAuthorize("hasPermission('码')")` 或 `@RequirePermission`；新功能用细粒度码（17 号脚本结构）。
- 聚合/统计：native SQL + `safeQuery` 失败降级 0（WorkspaceMetricsRepository 模式）；跨 schema 直查同库可行，跨服务走 Feign 或 MQ。
- 事务边界：complete 类操作"同事务"（量表+治疗+任务 DONE）；best-effort 副作用（AI、事件记录）内部 try/catch 不阻断主流程。

### 前端（Vue 3 + AntD Vue）
- `<script setup lang="ts">`；页面 `PageContainer` 包裹；列表 `SearchForm`+`useTable`；路径别名 `@/`。
- 主题色经 uiStore（localStorage `maidc-primary-color`）+ `--ant-color-primary` 变量，**不要硬编码蓝色值做主题色**（图表等值色 toneColorMap 除外）。
- API 模块按后端服务分文件（src/api/*.ts），类型与后端 VO 同名对齐。
- 类型检查口径：**改动文件零错误**即可交付（全仓存量错误存在于 data-etl/model/system 等模块）。

### 测试口径
- 后端：Mockito 单测（@ExtendWith(MockitoExtension)），服务新增构造依赖时同步补 @Mock（否则 @InjectMocks 注入 null → NPE）。
- 测试命名：`方法_场景_预期`（如 getDashboard_governanceGroup_returnsExclusiveCards）。
- mvn 命令：`mvn -pl maidc-data -am test -Dtest=XXXTest`（宿主机内存受限用小堆）。

### Git
- 分支：feature/<主题>（当前主线 feature/disease-kb）；提交：conventional commits（feat/fix/docs (scope): 描述，中文可）。
- 工作区常有多条并行改动（如患者360 重构、VitalSign），提交时按文件挑拣。

## 部署注意

1. 新增 init-db 脚本需手动执行（无自动 migration，除 maidc-data 的 db/migration 两个数据元脚本）。
2. maidc-task 代码在 maidc-data 模块内（com.maidc.task 包），无独立进程；vite proxy 的 :8084 是逻辑端口约定。
3. 随访提醒 Scheduler 多实例部署前需把内存去重换 Redis（FollowupReminderScheduler.java 注释自认）。
