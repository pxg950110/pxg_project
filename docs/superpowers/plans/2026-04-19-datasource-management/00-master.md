# 数据源管理增强 - 总计划

> **执行顺序：** Plan 1 → Plan 2 → Plan 3，每个 Plan 独立可测试

| Plan | 内容 | 文件 |
|------|------|------|
| 01-ddl | DDL + 种子数据 | `01-ddl.md` |
| 02-backend | 后端 Entity/Repo/Service/Controller + 连接测试策略 | `02-backend.md` |
| 03-frontend | 前端动态表单 + 列表重构 + 健康监控 | `03-frontend.md` |

## 依赖关系

```
01-ddl (数据库表)
  └→ 02-backend (后端 CRUD + 测试)
       └→ 03-frontend (前端界面)
```
