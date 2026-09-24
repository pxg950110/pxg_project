# 重构记录：数据字典与主数据管理（masterdata）

## 基本信息

| 项目 | 内容 |
|------|------|
| 日期 | 2026-09-15 |
| 分支 | feature/disease-kb |
| 范围 | maidc-data masterdata 模块（后端）+ maidc-portal 主数据/字典页面（前端）+ init-db |

## 调研结论（重构依据）

- **P0 断裂**：WS/T 303 标准三页（概念域/值域/数据元概念）请求双重前缀 `/api/v1/api/v1/...` 且后端无 concept-domains Controller → 整组不可用；DictManage.vue 为孤儿组件（未注册路由）且调用不存在的字典 CUD 接口；主入口 MasterDataManagement.vue 标准体系树永为空（TODO）。
- **P0 鉴权失效**：合并单体 SecurityConfig `prePostEnabled=false`，全部 `@PreAuthorize` 为死代码；权限码 `masterdata:read/create/update/delete` 未在 SQL 注册；PUT/DELETE 错挂 `masterdata:create`。
- **P0 模型分裂**：WS/T303 七表由 Hibernate 隐式建在 cdr schema（flyway 脚本是死文件）；五个医学字典表（d_*）不在任何 init-db 脚本。
- **P2 坏味道**：DictController 直连 Repository；@Async 自调用失效（本次记录、未改动）。

## 改动清单

### 后端（maidc-data）
| 文件 | 说明 |
|------|------|
| controller/ConceptDomainController.java（新增） | 概念域 CRUD + value-meanings 子资源（GET/POST 列表与新增、PUT/DELETE 按值含义 id），路径对齐前端 dataElementStandard.ts |
| service/ConceptDomainService.java（新增） | 概念域与值含义业务（分页/keyword/domainType 检索、级联逻辑删除、唯一码校验、orgId/createdBy 兜底） |
| controller/DictController.java（重写） | `/api/v1/system` 下字典类型 CRUD + 条目管理（类型即实体：POST/PUT/DELETE /dict-types、GET/POST /dict-types/{id}/items、PUT/DELETE /dict-items/{id}） |
| service/DictService.java（新增） | 字典类型/条目业务（唯一码、级联逻辑删除、创建字段兜底），替代 Controller 直连 Repository |
| entity/DictTypeEntity.java + repository/DictTypeRepository.java（新增） | `system.s_dict_type` 字典类型表（类型编码唯一；条目经 s_dict.dict_type 关联） |
| 21 个 masterdata 控制器 | `@PreAuthorize` → `@RequirePermission`（common-security 既有体系，不依赖 prePostEnabled），PUT/DELETE 粒度由 create 修正为 update/delete（EtlFieldMappingController 误匹配已还原） |
| 七个 WS/T303 实体 | @Table 统一补 `schema = "masterdata"`（concept_domain/value_meaning/value_domain/permissible_value/data_element_concept/object_class/property） |

### 数据库
| 文件 | 说明 |
|------|------|
| docker/init-db/23-masterdata-standard.sql（新增） | 七表 DDL（masterdata schema，幂等）+ V2026.06 样例数据（ON CONFLICT 防重，剔除指向已废弃 data_element 死表的语句）+ 权限码种子（masterdata:read/create/update/delete、system:dict:manage，授 admin/data_admin 全量、researcher/doctor/auditor 只读）+ s_dict_type 表 |
| 存量迁移（已执行） | cdr 下七表 `ALTER TABLE ... SET SCHEMA masterdata`（数据随迁）；s_dict.created_at/created_by 列默认值核对 |

### 前端（maidc-portal）
| 文件 | 说明 |
|------|------|
| src/api/dataElementStandard.ts | 修复 13 处双重前缀（`/api/v1/api/v1/...` → `/masterdata/...`） |
| src/api/data.ts | 字典 API 重写并做字段映射层（typeCode/typeName ↔ code/name；dictCode/dictLabel/dictValue/isEnabled/sortOrder ↔ code/name/value/sort_order/status），DictManage.vue 零改动适配新契约 |
| src/router/asyncRoutes.ts | 注册「数据字典」路由（/system/dict-manage，meta.permission=system:dict:manage）；masterdata 路由组补 meta.permission=masterdata:read |
| src/views/masterdata/MasterDataManagement.vue | 标准体系树接入真实数据（概念域 + 编码体系），替换 TODO 空实现 |

## 验证结果

- [x] maidc-app 启动正常（含全部重构代码）
- [x] 概念域：列表（5 条 WS/T303 样例）✓、创建（CD_TEST_REFACTOR）✓、值含义列表 ✓
- [x] 字典：类型创建/列表/删除（级联逻辑删除条目）✓、条目创建/列表 ✓、重复编码 400 校验 ✓
- [x] 权限码：`/users/me` 返回 admin 含 `masterdata:dict:manage` 等 ✓；`@RequirePermission` 由 PermissionAspect 执行（401/403 + 越权审计事件）
- [x] 前端：主数据管理树加载真实概念域（CD_BLOOD_TYPE/CD_GENDER/CD_WEIGHT 等 6 条）✓、「数据字典」菜单与页面渲染 ✓、概念域列表页双重前缀修复后可访问 ✓

## 遗留事项

- [ ] IDE 竞态教训：本地 IDE（ECJ 类语言服务）会向 target/classes 写入「Unresolved compilation」错误类并被 Maven 打包 → app 镜像改为从 `maidc-parent/build/maidc-app.jar`（构建后校验过的稳定副本）取件；建议在 IDE 中关闭自动构建或配置 Lombok/MapStruct 注解处理
- [ ] P2：MasterDataImportService @Async 自调用失效（现为同步执行，功能可用）；dictionary 五套 CRUD 模板化合并；MasterDataManagement 值集视图/表单（ValueSetFormModal 等 5 组件）TODO 未接 API；d_* 五字典表纳入 init-db 正式 DDL
- [ ] 代码提交（git-merge）与 PR
