# MAIDC 项目知识库（Knowledge Base）

> 由 2026-09-11 的全仓调研（3 轮）与首页工作台 v2 开发实践沉淀而成。
> 基准：分支 `feature/disease-kb`，HEAD `2880873`（feat(workspace): role-based workspace v2）+ 当日工作区改动。
> **维护约定**：事实均带 `文件:行号` 证据；代码变动后请同步更新对应条目与"基准"信息。

## 目录

| 文件 | 内容 | 适用场景 |
|------|------|----------|
| [01-project-overview.md](01-project-overview.md) | 项目定位、微服务拓扑、端口、数据分层、init-db 脚本清单 | 新人上手 / 跨服务定位 |
| [02-frontend.md](02-frontend.md) | maidc-portal 技术栈、路由表、视图清单、权限前端、API 层、布局与主题 | 前端开发 / 页面定位 |
| [03-backend-security.md](03-backend-security.md) | Controller 清单、8 角色权限模型、DataScope、权限码、脱敏豁免 | 后端开发 / 权限对接 |
| [04-workspace-dashboard.md](04-workspace-dashboard.md) | 首页工作台专题：v1→v2 演进、接口契约、角色组卡片、数据源 SQL、FR1-FR8 状态 | 工作台迭代 / 需求评审 |
| [05-domain-followup-kb.md](05-domain-followup-kb.md) | CRS 随访试点（6 表/7 Controller/状态机/结局统计）+ 专病知识库（空间/条目/FTS/QA） | 专病域开发 |
| [06-conventions.md](06-conventions.md) | 研发流程（过程文件→原型→plan→feature）、验证口径、git/文档惯例 | 所有开发任务 |

## 快速定位（高频问题）

| 问题 | 去处 |
|------|------|
| 登录后落到哪个页面？为什么？ | 02 §路由（`/` → `/dashboard` → `/dashboard/workspace`） |
| 某角色能看到什么？DEPT scope 怎么工作？ | 03 §角色清单 / §DataScope |
| 工作台某张卡的数据来自哪张表？ | 04 §数据源 SQL 清单 |
| 加一张指标卡要改哪些文件？ | 04 §迭代清单（Repository 计数 → cards 构建 → icons.ts → 前端组件） |
| 随访任务的提醒/升级逻辑？ | 05 §提醒调度 |
| 原型页面放哪、什么时候删？ | 06 §原型生命周期 |
