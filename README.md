# MAIDC — 医疗AI数据中心平台

> **Medical AI Data Center** — 医疗AI模型全生命周期管理平台

## 项目简介

MAIDC 是面向医疗行业的AI模型管理与数据治理平台，覆盖从数据采集、模型开发、评估审批、部署推理到持续监控的全生命周期。

### 核心能力

- **模型全生命周期管理** — 注册、版本管理、评估、审批、部署、路由、推理
- **临床数据治理 (CDR)** — 患者数据采集、数据质量、ETL转换、数据脱敏
- **科研数据管理 (RDR)** — 研究项目、队列管理、数据集版本、特征字典
- **智能标注** — AI辅助预标注、多人协作标注、质量控制
- **安全合规** — 等保三级、RBAC权限、审计日志、数据加密脱敏
- **可观测性** — Prometheus + Grafana + ELK 全链路监控

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端 | Java + Spring Boot | 17 + 3.2.5 |
| 微服务 | Spring Cloud Alibaba | 2023.0.1 |
| 数据库 | PostgreSQL | 15 |
| 缓存 | Redis | 7 |
| 消息队列 | RabbitMQ | 3.12 |
| 对象存储 | MinIO | RELEASE.2024-02 |
| 注册配置中心 | Nacos | 2.3 |
| AI Worker | Python + FastAPI + Celery | 3.11 |
| 前端 | Vue 3 + TypeScript + Ant Design Vue | 3.4 + 5 + 4.2 |
| 监控 | Prometheus + Grafana + ELK | - |
| 网关 | Spring Cloud Gateway | 4.1.x |

## 架构概览

```
┌─────────────────────────────────────────────────────────┐
│                     Nginx / CDN                          │
├─────────────────────────────────────────────────────────┤
│                   Vue.js Frontend (:3000)                │
├─────────────────────────────────────────────────────────┤
│              Spring Cloud Gateway (:8080)                │
│         Auth / RateLimit / Trace / Logging              │
├───────┬───────┬───────┬───────┬───────┬────────┬───────┤
│ Auth  │ Model │ Data  │ Task  │ Label │ Audit  │  Msg  │
│ :8081 │ :8083 │ :8082 │ :8084 │ :8085 │ :8086  │ :8087 │
├───────┴───────┴───────┴───────┴───────┴────────┴───────┤
│              AI Worker (FastAPI + Celery) :8088          │
├─────────────────────────────────────────────────────────┤
│  PostgreSQL  │  Redis  │  MinIO  │  RabbitMQ  │  Nacos  │
│   :5432      │  :6379  │  :9000  │  :5672     │  :8848  │
└─────────────────────────────────────────────────────────┘
```

## 快速启动

### 前置条件

- Docker 20.10+ & Docker Compose v2
- JDK 17+
- Node.js 18+ & pnpm 8+
- 8GB+ 内存

### 1. 启动基础设施

```bash
# 启动 PostgreSQL, Redis, MinIO, RabbitMQ, Nacos
docker compose -f docker/docker-compose-infra.yml up -d

# 初始化数据库
docker exec -i maidc-postgres psql -U maidc -d maidc < docker/init-db/01-schemas.sql
docker exec -i maidc-postgres psql -U maidc -d maidc < docker/init-db/02-system.sql
docker exec -i maidc-postgres psql -U maidc -d maidc < docker/init-db/03-model.sql
docker exec -i maidc-postgres psql -U maidc -d maidc < docker/init-db/04-cdr.sql
docker exec -i maidc-postgres psql -U maidc -d maidc < docker/init-db/05-rdr.sql
docker exec -i maidc-postgres psql -U maidc -d maidc < docker/init-db/06-audit.sql

# 初始化 MinIO buckets
bash docker/init-minio.sh

# 导入 Nacos 配置
bash docker/init-nacos.sh
```

### 2. 启动后端服务

```bash
cd maidc-parent
mvn clean package -DskipTests

# 启动各服务（按顺序）
java -jar maidc-gateway/target/maidc-gateway.jar &
java -jar maidc-auth/target/maidc-auth.jar &
java -jar maidc-data/target/maidc-data.jar &
java -jar maidc-model/target/maidc-model.jar &
java -jar maidc-task/target/maidc-task.jar &
java -jar maidc-label/target/maidc-label.jar &
java -jar maidc-audit/target/maidc-audit.jar &
java -jar maidc-msg/target/maidc-msg.jar &
```

### 3. 启动前端

```bash
cd maidc-portal
pnpm install
pnpm dev
# 访问 http://localhost:3000
```

### 4. 一键启动（Docker Compose）

```bash
# 全部服务一键启动（包含监控）
docker compose -f docker/docker-compose-full.yml up -d

# 仅启动监控组件
docker compose -f docker/docker-compose-monitoring.yml up -d
```

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 超级管理员 | admin | admin123 |

> 生产环境请立即修改默认密码

## 项目结构

```
maidc-parent/                # Maven 父工程
├── common/                  # 公共模块
│   ├── common-core/         # 统一响应体、异常处理、错误码
│   ├── common-redis/        # Redis 缓存服务、分布式锁、缓存策略
│   ├── common-minio/        # MinIO 文件存储
│   ├── common-mq/           # RabbitMQ 消息基类
│   ├── common-log/          # 操作日志 AOP
│   ├── common-security/     # JWT、安全工具、XSS防护、加密脱敏
│   └── common-jpa/          # JPA审计、JSONB转换、软删除基类
├── maidc-gateway/           # API网关 (:8080)
├── maidc-auth/              # 认证服务 (:8081)
├── maidc-data/              # 数据服务 (:8082)
├── maidc-model/             # 模型服务 (:8083)
├── maidc-task/              # 任务调度 (:8084)
├── maidc-label/             # 标注服务 (:8085)
├── maidc-audit/             # 审计服务 (:8086)
├── maidc-msg/               # 消息服务 (:8087)
└── maidc-aiworker/          # AI推理Worker (Python)

maidc-portal/                # Vue.js 前端
├── src/
│   ├── api/                 # API 接口层
│   ├── assets/              # 静态资源
│   ├── components/          # 29个公共组件
│   ├── hooks/               # 4个 Composition Hooks
│   ├── layouts/             # 布局组件
│   ├── router/              # 路由配置
│   ├── stores/              # Pinia 状态管理
│   ├── utils/               # 工具函数
│   └── views/               # ~64个页面视图

docker/                      # Docker 部署文件
monitoring/                  # 监控配置
docs/                        # 项目文档
scripts/                     # 脚本工具
```

## API 文档

服务启动后访问：
- Gateway API Docs: `http://localhost:8080/swagger-ui.html`
- 各服务独立文档: `http://localhost:808{1-7}/swagger-ui.html`

## 监控

| 服务 | 地址 |
|------|------|
| Grafana | http://localhost:3001 (admin/admin) |
| Prometheus | http://localhost:9090 |
| Kibana | http://localhost:5601 |
| RabbitMQ Console | http://localhost:15672 (guest/guest) |
| MinIO Console | http://localhost:9001 (minioadmin/minioadmin) |
| Nacos Console | http://localhost:8848/nacos (nacos/nacos) |

## 测试

```bash
# 端到端测试（全流程）
bash scripts/e2e-test.sh

# 集成测试（F1-F8 全部8个流程）
bash scripts/e2e-tests.sh

# 性能测试
bash scripts/performance-test.sh
```

## 文档

| 文档 | 位置 |
|------|------|
| 架构设计 | `docs/dev/01-architecture.md` |
| API契约 | `docs/dev/02-api-contract.md` |
| 前端规范 | `docs/dev/03-frontend-guide.md` |
| 后端规范 | `docs/dev/04-backend-guide.md` |
| 流程图 | `docs/dev/05-flow-diagrams.md` |
| 开发环境搭建 | `docs/dev/06-dev-setup.md` |
| 安全合规检查清单 | `docs/security/compliance-checklist.md` |
| 研发计划 | `docs/superpowers/plans/2026-04-11-maidc-development-plan.md` |

## License

Proprietary — Internal Use Only
