# 功能实现记录

## 基本信息

| 项目 | 内容 |
|------|------|
| **日期** | 2026-09-11 |
| **需求号** | 20260911（临时，无 TFS 工作项） |
| **需求标题** | 前端登录页多方案设计对比稿 + 方案A定稿落地（深色科技风） |
| **需求来源** | 用户描述（对比稿产出后评审选定方案A） |

## 阶段一：多方案对比稿

产出 3 套纯静态设计稿（`maidc-portal/design/login/`）：方案A深色科技风 / 方案B简洁浅色风 / 方案C现有基线 + 对比导航 `index.html`。均含完整登录要素、交互态、动效降级、响应式。

## 阶段二：方案A定稿落地

**评审结论**：选定方案A；同时确立**「全站 UI 统一深色科技风」设计约定**（用户要求后续所有页面都考虑科技风，已固化到 `AGENTS.md` Design conventions 章节，后续 AI 会话与开发均可感知）。

## 修改文件清单

### 修改文件

| 文件路径 | 修改说明 |
|----------|----------|
| `maidc-portal/src/views/login/LoginPage.vue` | 按方案A深色科技风重写：数据环/指标卡/扫描线/粒子氛围件 + 玻璃拟态登录卡；保留原认证逻辑（authStore.loginAction、redirect 回跳、message 反馈）；新增记住用户名（localStorage `maidc.login.username` 回填）、密码显隐、Enter 焦点流转/提交、登录 loading 态 |
| `AGENTS.md` | 新增 Design conventions 章节：全站科技风约定 + 规范文档/基准实现路径 |

### 新增文件

| 文件路径 | 说明 |
|----------|------|
| `maidc-portal/docs/design/tech-style-guide.md` | 深色科技风设计规范：色板/几何质感/氛围件/动效/ant-design-vue 深色化映射/反例清单 |
| `maidc-portal/design/login/`（4 个 HTML） | 阶段一设计稿（评审用，可保留作视觉基准） |
| `docs/plans/2026-09-11-login-design-concepts.md` | 实施计划（含两阶段记录） |

### 未改动

- 路由（`/login` 已存在）、auth store、api、`global.css` 全局样式（组件样式全部 scoped）
- `.env.development` 等 env 文件（验证时临时创建的 `.env.development.local` 已删除）

## 技术要点

- 全部样式 scoped + CSS 变量隔离在 `.login-page` 作用域，不污染应用内页面
- 氛围动效（扫描线/粒子/旋转环/呼吸核心）均带 `prefers-reduced-motion` 降级
- 品牌区平台指标（32家/48个/99.99%）为装饰数据，已标注 TODO 接真实统计接口
- 布局断点：≤1080px 隐藏品牌区，≤480px 登录卡紧凑化

## 测试验证

- [x] `npm run type-check`：登录页零错误（仓库存量 30 个错误均在其他模块，非本次引入）
- [x] vite dev + 无头 Edge 截图：落地视觉与设计稿 dark-tech.html 一致
- [x] **docker-compose 部署验证通过**（2026-09-11）：
  - 重建 `docker-portal` 镜像（vite build）并部署，`http://localhost:3000/login` 返回 200，容器内页面截图与设计稿一致（`tmp/login-design-shots/docker-login.png`）
  - 端到端链路探测：POST `/api/v1/auth/login` 经 nginx → gateway → auth 返回后端业务 JSON，链路正常
- 验证截图：`tmp/login-design-shots/`（landing-final / docker-login 等）

## 部署过程中修复的两处基础设施问题（非前端引入）

1. **nacos 无法启动**（阻塞全栈）：`docker-compose-full.yml` 中 `SPRING_DATASOURCE_PLATFORM: embedded` 在 nacos v2.3.2 镜像里会被当作外部数据源开关（仅识别 `mysql`），导致 nacos 尝试连接字面量 `${MYSQL_SERVICE_HOST}` 而退出。已删除该行（不设置时默认即内嵌 derby），并加注释说明。
2. **data/task 启动即退**：`PersonalTaskConsumer` 监听的 RabbitMQ 队列 `label.notify` 在整个仓库中无声明方（followup 功能遗留遗漏），RabbitMQ 中不存在该队列导致 listener fatal。已通过管理 API 手动补建 durable 队列解锁（`PUT /api/queues/:vhost/label.notify`，凭据 maidc）。**遗留：需在代码中补队列声明**（如 maidc-label 服务侧 `Queue` bean 或 listener 改 `queuesToDeclare`），否则 RabbitMQ 数据卷重建后问题复现。

## 登录 500 排查修复（2026-09-11 第二轮）

用户实际登录报 `500 系统内部错误`。逐层排查定位与修复：

1. **直接原因：auth 连不上 Redis**（登录锁定计数用），异常 `Unable to connect to Redis` 被 GlobalExceptionHandler 兜底为 500。根因：redis 容器 `--requirepass maidc123`，而 nacos 共享配置 `maidc-shared.yaml` 默认密码写的是 `maidc_redis`——不匹配。
2. **修复**：修正 `docker/nacos-config/maidc-shared.yaml` redis 默认密码为 `maidc123`，并通过 Nacos OpenAPI 推送到运行中的 nacos，重启后端服务生效。
3. **顺带修复：rabbitmq 默认 vhost** 由 `maidc`（不存在）改为 `/`（实际队列所在），auth 审计日志 MQ 发送失败随之解决。
4. **顺带修复：portal nginx 静态 upstream**：nginx 启动时只解析一次 `gateway` 的 IP，后端容器重启换 IP 后 portal 即 502。改为 `resolver 127.0.0.11 + 变量 upstream` 动态解析（api/ws 两处），已验证 gateway 重启后 portal 无需重启自动恢复。
5. **验证**：POST `/api/v1/auth/login` 返回业务码 4102「用户名或密码错误」（凭据错误时预期行为），证明 db/redis/gateway/auth 全链路正常；登录页 200。admin 账号初始密码注释为 admin123，但实测不符（hash 可能已被修改），用户验证请使用实际账号。

> 注意：seed 注释（02-system.sql）写的 admin123 与库中实际 hash 不符，后续如需重置 admin 密码可参考该文件。

> 注意：seed 注释（02-system.sql）写的 admin123 与库中实际 hash 不符，后续如需重置 admin 密码可参考该文件。

## 「账号已锁定」处理（2026-09-11 第三轮）

- 原因：登录锁定策略（AuthService：连续 5 次失败锁 30 分钟，计数存 redis key `maidc:auth:attempts:{userId}`）。redis 修复前后累积的失败尝试触发了 admin 锁定。
- 处理：删除 redis 失败计数解锁；并核实 **admin 实际密码为 `Admin@123`**（库中 hash `$2a$10$ihi7...` 为历史上重置过的，与 seed 脚本 `$2b$` 注释的 admin123 不同——02-system.sql 的注释具有误导性）。
- 端到端验证：`POST /api/v1/auth/login`（admin/Admin@123）返回 200 + accessToken/refreshToken，登录全链路（页面 → nginx → gateway → auth → postgres/redis）完整打通。
- 解锁命令备忘：`docker exec maidc-redis redis-cli -a maidc123 DEL "maidc:auth:attempts:{userId}"`

## 当前部署状态（2026-09-11）

- 运行中：nacos(healthy) / portal / gateway / auth / model / data / task / label / audit / msg / postgres / redis / rabbitmq / minio
- **未启动：aiworker** —— 宿主机 java.exe（PID 34480，疑似本机 IDE 实例）占用 8090 端口；待用户决定停掉本机进程或改 aiworker 映射端口后 `docker compose up -d aiworker`

## 待处理事项

- [ ] 后端 SSO 未就绪，「医院统一认证（SSO）登录」按钮当前为占位交互
- [ ] 忘记密码/隐私政策/使用条款为占位链接
- [ ] 平台指标接真实接口；字段级校验、失败锁定倒计时、图形验证码位、Caps Lock 提醒待后续增强
- [ ] 全站其余页面按 `tech-style-guide.md` 逐步深色化（另起任务）
- [ ] 代码中补 `label.notify` 队列声明（RabbitMQ 数据卷重建后会复现）
- [ ] aiworker 启动（8090 端口冲突待用户处理）
