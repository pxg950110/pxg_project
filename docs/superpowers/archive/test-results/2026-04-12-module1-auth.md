# 测试报告 - 认证模块

## 测试环境
- 前端地址: http://localhost:3000 (Vite dev server, 端口非默认5173)
- 后端Auth服务: http://localhost:8081 (maidc-auth Spring Boot)
- 数据库: PostgreSQL 15 (Docker maidc-postgres)
- 缓存: Redis 7 (Docker maidc-redis)
- 测试账号: admin / Admin@123 (DataInitializer默认密码)
- 浏览器: Chrome 146

## 测试时间
- 开始: 2026-04-12 08:21:06
- 结束: 2026-04-12 08:29:22

## 测试结果

| 编号 | 操作 | 状态 | 实际API | 实际响应 | UI结果 | 备注 |
|------|------|------|---------|----------|--------|------|
| 1.1.1 | 页面加载 | PASS | 无 | - | 登录表单正常显示：包含用户名输入框、密码输入框、登录按钮、"记住我"复选框、忘记密码链接、SSO按钮、安全提示 | 标题"欢迎回来"、副标题"请登录您的账户"均正确显示 |
| 1.1.2 | 空表单提交 | PASS | 无(前端校验拦截) | - | 显示黄色警告提示"请输入用户名和密码"，带感叹号图标 | Ant Design message.warning toast |
| 1.1.3 | 只填用户名 | PASS | 无(前端校验拦截) | - | 显示警告提示"请输入用户名和密码" | 提示文案为统一消息"请输入用户名和密码"，未区分"请输入密码" |
| 1.1.4 | 错误凭据 | PASS | POST /api/v1/auth/login | HTTP 200, body: {code:401, message:"未认证或登录已过期", data:null} | 停留在登录页，显示错误toast(自动消失) | API使用HTTP 200 + 业务code 401模式；错误消息为"未认证或登录已过期"而非预期的"用户名或密码错误" |
| 1.1.5 | 正确登录 | PASS | POST /api/v1/auth/login | HTTP 200, body: {code:200, data:{accessToken, refreshToken, user:{id:1, username:"admin", realName:"系统管理员", roles:["admin"]}}} | 跳转到 /dashboard/overview，localStorage存入maidc_access_token和maidc_refresh_token | 密码为Admin@123(非admin123)；token存储在maidc_access_token键下(非默认access_token) |
| 1.1.6 | 密码显隐切换 | FAIL | 无 | - | 密码输入框为纯`<input type="password">`，无眼睛图标/显隐切换按钮 | LoginPage.vue中密码字段未实现visibility toggle功能，缺少切换控件 |
| 1.1.7 | 退出登录 | PASS(部分) | POST /api/v1/auth/logout | HTTP 200, body: {code:200, message:"success", data:null} | token已清除，重定向到/login | 通过JS直接调用logout API测试(因BasicLayout未包裹Dashboard页面，头部用户菜单不可见)。API功能正常，但UI路径不可达 |

## 统计
- 通过: 5/7
- 部分通过: 1/7 (1.1.7 - API功能正常但UI路径不可达)
- 失败: 1/7 (1.1.6 - 密码显隐切换功能缺失)
- 完全通过率: 71.4% (5/7)
- 含部分通过: 85.7% (6/7)

## 发现的问题

### BUG-1: 密码显隐切换功能缺失 (严重度: 低)
- **测试点**: 1.1.6
- **现象**: LoginPage.vue 中密码输入框为 `<input type="password">`，没有提供显隐切换按钮(眼睛图标)
- **预期**: 密码框右侧应有眼睛图标，点击可切换 password/text 显示模式
- **影响**: 用户体验，不影响功能
- **修复建议**: 在密码字段添加 visibility toggle 组件

### BUG-2: BasicLayout 未包裹 Dashboard 页面 (严重度: 高)
- **测试点**: 1.1.7
- **现象**: Dashboard Overview 页面渲染为 `<div class="page-container">`，没有 BasicLayout 包装器（无侧边栏、无头部导航、无用户菜单）
- **原因**: asyncRoutes 动态路由添加时可能未正确嵌套在 BasicLayout 的 children 下，或路由守卫 addRoute 的层级关系有问题
- **影响**: 所有动态路由页面缺少统一布局（侧边栏导航、面包屑、用户头像菜单）
- **修复建议**: 检查 router guards 中 `router.addRoute()` 调用，确保异步路由正确挂载到 BasicLayout 下

### NOTE-1: 登录验证提示文案不精确 (严重度: 低)
- **测试点**: 1.1.3
- **现象**: 只填写用户名时，提示"请输入用户名和密码"，预期为"请输入密码"
- **原因**: LoginPage.vue handleLogin() 方法中统一检查 `!formState.username || !formState.password`，给出统一错误信息
- **建议**: 分开校验，给出精确提示

### NOTE-2: 错误凭据响应消息 (严重度: 低)
- **测试点**: 1.1.4
- **现象**: API 返回 message 为"未认证或登录已过期"，而非更精确的"用户名或密码错误"
- **建议**: 后端 AuthController 登录失败时应返回更明确的错误消息

### NOTE-3: 测试账号密码差异
- **测试点**: 1.1.5
- **说明**: 实际管理员密码为 `Admin@123`（DataInitializer 配置），而非测试计划中的 `admin123`

### NOTE-4: Vite dev server 端口
- **说明**: 前端实际运行在端口 3000，而非 5173。vite.config.ts 可能配置了自定义端口

## 截图
- 登录成功后仪表盘截图: `screenshots/1.1-login.png`
