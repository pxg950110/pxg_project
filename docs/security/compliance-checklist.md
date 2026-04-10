# MAIDC 安全加固 — 等保三级合规检查清单

## 1. 身份鉴别

| 序号 | 检查项 | 实现状态 | 实现方式 |
|------|--------|---------|---------|
| 1.1 | 密码复杂度 | ✅ | PasswordPolicyValidator (8位+, 大小写+数字+特殊字符) |
| 1.2 | 密码加密存储 | ✅ | BCrypt (Spring Security PasswordEncoder) |
| 1.3 | 登录失败处理 | ✅ | 5次失败锁定30分钟 (Redis计数) |
| 1.4 | 会话超时 | ✅ | JWT Access Token 2小时过期 |
| 1.5 | 唯一性鉴别 | ✅ | 用户名+组织ID唯一 |
| 1.6 | 双因素认证预留 | ⏳ | 架构支持扩展 |

## 2. 访问控制

| 序号 | 检查项 | 实现状态 | 实现方式 |
|------|--------|---------|---------|
| 2.1 | 基于角色的访问控制 | ✅ | RBAC (用户→角色→权限树) |
| 2.2 | 最小权限原则 | ✅ | 权限树精细到按钮级别 |
| 2.3 | 接口级鉴权 | ✅ | Gateway AuthFilter + SecurityConfig |
| 2.4 | 数据隔离 | ✅ | orgId 多租户隔离 |

## 3. 安全审计

| 序号 | 检查项 | 实现状态 | 实现方式 |
|------|--------|---------|---------|
| 3.1 | 操作审计日志 | ✅ | @OperLog AOP + maidc-audit 服务 |
| 3.2 | 数据访问日志 | ✅ | DataAccessLogEntity (谁/何时/访问什么) |
| 3.3 | 系统事件日志 | ✅ | SystemEventEntity (登录/登出/异常) |
| 3.4 | 日志保留策略 | ✅ | PostgreSQL 分区表 + 定期归档 |
| 3.5 | 日志防篡改 | ✅ | audit schema 独立权限 |

## 4. 通信安全

| 序号 | 检查项 | 实现状态 | 实现方式 |
|------|--------|---------|---------|
| 4.1 | 传输加密 | ✅ | HTTPS (Nginx TLS 终结) |
| 4.2 | 微服务间加密 | ⏳ | 内网通信，K8s network policy |
| 4.3 | 敏感字段加密 | ✅ | AesEncryptor (AES-256-GCM) |

## 5. 数据安全

| 序号 | 检查项 | 实现状态 | 实现方式 |
|------|--------|---------|---------|
| 5.1 | 数据脱敏 | ✅ | DesensitizeUtils (姓名/身份证/手机/邮箱/地址) |
| 5.2 | 前端脱敏 | ✅ | desensitize.ts 工具函数 |
| 5.3 | 数据库字段加密 | ✅ | @Convert(converter = AesEncryptor.class) |
| 5.4 | 软删除 | ✅ | BaseEntity.isDeleted + @Where + @SQLDelete |
| 5.5 | 数据备份 | ✅ | PostgreSQL pg_dump 定时备份脚本 |

## 6. 输入安全

| 序号 | 检查项 | 实现状态 | 实现方式 |
|------|--------|---------|---------|
| 6.1 | XSS 防护 | ✅ | XssRequestFilter + XssHttpServletRequestWrapper |
| 6.2 | SQL 注入防护 | ✅ | JPA 参数化查询 (无原生SQL拼接) |
| 6.3 | CSRF 防护 | ✅ | JWT Bearer Token (无Cookie认证) |
| 6.4 | 输入校验 | ✅ | @Valid + @Pattern + DTO校验 |
| 6.5 | 文件上传安全 | ✅ | MinIO 预签名URL + 文件类型白名单 |

## 7. 安全管理

| 序号 | 检查项 | 实现状态 | 实现方式 |
|------|--------|---------|---------|
| 7.1 | 安全配置管理 | ✅ | Nacos 配置中心 + 环境变量 |
| 7.2 | 密钥管理 | ✅ | 环境变量注入 (MAIDC_AES_KEY, JWT Secret) |
| 7.3 | 安全漏洞扫描 | ⏳ | OWASP Dependency-Check |
| 7.4 | 安全基线配置 | ⏳ | Docker 镜像安全扫描 |

## 8. 安全组件清单

| 组件 | 位置 | 说明 |
|------|------|------|
| AesEncryptor | common-security/encrypt/ | AES-256-GCM 字段加密 AttributeConverter |
| DesensitizeUtils | common-security/desensitize/ | 6种脱敏策略 |
| XssProtectionUtils | common-security/xss/ | XSS检测 + HTML转义 + 输入净化 |
| XssRequestFilter | common-security/xss/ | Servlet Filter XSS防护 |
| PasswordPolicyValidator | common-security/password/ | 等保三级密码复杂度校验 |
| JwtUtils | common-security/util/ | JWT生成/验证/黑名单 |
| SecurityUtils | common-security/context/ | 用户上下文获取 |
| GlobalExceptionHandler | common-core/exception/ | 统一异常处理 |
| desensitize.ts | frontend/src/utils/ | 前端脱敏工具 |
| security.ts | frontend/src/utils/ | 前端XSS防护 |

---

> 最后更新: 2026-04-11
> 审核人: 待指定
