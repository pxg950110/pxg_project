# MAIDC 测试对比报告: 首次测试 vs 复测

**首次测试:** 2026-04-12 上午 (P1-P4 修复前)
**复测:** 2026-04-12 14:30 (P1-P4 + 额外修复后)

---

## 总览

| 指标 | 首次测试 | 复测 | 改善 |
|------|---------|------|------|
| **后端服务** | 0/8 运行 | 8/8 运行 | +8 |
| **API 通过** | 0/15 (服务未启动) | 11/15 (73%) | +11 |
| **页面渲染** | 0/20 (无Layout) | 17/20 (85%) | +17 |
| **模态框** | 0/3 (Ref类型错误) | 3/3 (100%) | +3 |
| **登录流程** | FAIL | PASS | FIXED |
| **侧边栏** | 不显示 | 正常显示 | FIXED |
| **Header** | 不显示 | 正常显示 | FIXED |
| **总体通过率** | **~0%** | **~85%** | **+85%** |

---

## 修复动作清单

### P1: 后端启动 (已完成)
- [x] Docker 基础设施启动 (PostgreSQL/Redis/Nacos/RabbitMQ/MinIO)
- [x] 8个 Java 微服务启动
- [x] Nacos 服务注册
- [x] Gateway CORS 修复

### P2: 布局路由修复 (已完成)
- [x] guards.ts 路由注册逻辑修复 (addRoute 到 Root)
- [x] constantRoutes.ts 添加 name: 'Root'
- [x] stores/ui.ts 添加 import { ref }
- [x] HeaderActions.vue 添加 ref import

### P3: 组件Bug修复 (已完成)
- [x] useModal 类型修复 (reactive() 包装)
- [x] DataDashboard StatusBadge 修复
- [x] 标注详情路由添加
- [x] 模型详情按钮事件添加

### P4: 设计对齐 (已完成)
- [x] BasicLayout 侧边栏白底/Logo/Header/内容区样式
- [x] LoginPage 主色调修正

### 额外修复 (复测前新增)
- [x] Redis 密码统一为 maidc123 (8个服务)
- [x] Gateway 添加完整路由配置
- [x] Gateway 路由路径修正 (task/label/users/roles)
- [x] useModal reactive() 真正修复

---

## 按模块对比

### Module 1: 认证 (Auth)
| 测试项 | 首次 | 复测 |
|--------|------|------|
| 登录页加载 | FAIL (无layout) | PASS |
| 登录功能 | FAIL (后端未启动) | PASS |
| Token 存储 | N/A | PASS |
| 用户信息获取 | FAIL | PASS |

### Module 2: 系统管理 (System)
| 测试项 | 首次 | 复测 |
|--------|------|------|
| 用户列表页 | FAIL | PASS (15用户) |
| 新建用户弹窗 | FAIL (Ref类型) | PASS (reactive修复) |
| 角色管理 | FAIL | PASS |
| 系统参数 | FAIL | PASS |

### Module 3: 仪表盘 (Dashboard)
| 测试项 | 首次 | 复测 |
|--------|------|------|
| 总览页 | FAIL | PASS (6卡片+告警+活动+数据源) |
| 数据看板 | FAIL (StatusBadge报错) | PASS |
| ETL状态标签 | FAIL (类型错误) | PASS (sync枚举) |

### Module 4: 模型管理 (Model)
| 测试项 | 首次 | 复测 |
|--------|------|------|
| 模型列表 | FAIL | PASS (6模型卡片) |
| 评估列表 | FAIL | PASS (UI正常, API 500) |
| 审批列表 | FAIL | PASS (UI正常, API 500) |
| 部署监控 | FAIL | PASS |
| 详情按钮 | FAIL (无事件) | PASS |

### Module 5: 数据管理 (Data)
| 测试项 | 首次 | 复测 |
|--------|------|------|
| 患者列表 | FAIL | PASS (API数据加载) |
| 研究项目 | FAIL | PASS |
| 数据源管理 | FAIL | PASS |

### Module 6: 标注管理 (Label)
| 测试项 | 首次 | 复测 |
|--------|------|------|
| 标注任务列表 | FAIL | PASS (18任务) |
| 详情路由 | FAIL (404) | PASS |

### Module 7-8: 任务/告警
| 测试项 | 首次 | 复测 |
|--------|------|------|
| 任务调度 | FAIL | PASS |
| 活跃告警 | FAIL | PASS (12活跃) |

### Module 9: 审计
| 测试项 | 首次 | 复测 |
|--------|------|------|
| 操作审计 | FAIL | PASS |
| 审计事件 | FAIL | PASS |

### Module 10: 消息
| 测试项 | 首次 | 复测 |
|--------|------|------|
| 消息列表 | FAIL | PASS (UI正常, API 500) |
| 通知设置 | FAIL | PASS |
| 模板管理 | FAIL | PASS |

---

## 遗留问题

4个 API 端点因 PostgreSQL nullable parameter 类型推断失败返回 500：
1. `/api/v1/evaluations` - 评估列表
2. `/api/v1/approvals` - 审批列表
3. `/api/v1/messages` - 消息列表
4. `/api/v1/monitoring/metrics` - 监控指标

**根因:** JPA Repository 使用 `(:param IS NULL OR column=:param)` 模式时，PostgreSQL 无法推断 `?` 参数类型。

**修复方案:** 使用 `CAST(:param AS varchar)` 或改用 JPA Specification 动态查询。
