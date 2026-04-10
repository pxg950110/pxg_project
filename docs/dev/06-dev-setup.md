# MAIDC 开发环境搭建指南

> **版本**: v1.0
> **日期**: 2026-04-10
> **适用**: Phase 1 开发团队

---

## 目录

1. [基础环境要求](#1-基础环境要求)
2. [基础设施一键启动](#2-基础设施一键启动docker-compose)
3. [数据库初始化](#3-数据库初始化)
4. [MinIO 初始化](#4-minio-初始化)
5. [后端项目启动](#5-后端项目启动)
6. [前端项目启动](#6-前端项目启动)
7. [AI Worker 启动](#7-ai-worker-启动)
8. [常见问题 FAQ](#8-常见问题-faq)

---

## 1. 基础环境要求

| 工具 | 版本要求 | 用途 | 安装方式 |
|------|---------|------|---------|
| JDK | 17 (OpenJDK) | 后端运行时 | [Eclipse Temurin](https://adoptium.net/) |
| Maven | 3.9+ | Java构建 | `choco install maven` / `brew install maven` |
| Node.js | 18+ | 前端运行时 | [nvm](https://github.com/nvm-sh/nvm) |
| pnpm | 8+ | 前端包管理 | `npm install -g pnpm` |
| Python | 3.10+ | AI Worker | [pyenv](https://github.com/pyenv/pyenv) |
| Poetry | 1.7+ | Python依赖管理 | `pip install poetry` |
| Docker Desktop | 最新版 | 基础设施 | [Docker官网](https://www.docker.com/products/docker-desktop) |
| Git | 2.40+ | 版本控制 | + Git LFS（模型文件） |
| IDE | - | IntelliJ IDEA / VS Code / PyCharm | - |

**Docker Desktop 资源配置建议**：
- CPU: 6核+
- 内存: 8GB+
- 磁盘: 50GB+

---

## 2. 基础设施一键启动（Docker Compose）

### 2.1 docker-compose-infra.yml

在项目根目录创建 `docker/docker-compose-infra.yml`：

```yaml
version: '3.8'

services:
  # ==================== PostgreSQL ====================
  postgres:
    image: postgres:15
    container_name: maidc-postgres
    environment:
      POSTGRES_DB: maidc
      POSTGRES_USER: maidc
      POSTGRES_PASSWORD: maidc123
      POSTGRES_INITDB_ARGS: "--encoding=UTF-8 --lc-collate=C --lc-ctype=C"
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./init-db:/docker-entrypoint-initdb.d  # 自动执行SQL初始化
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U maidc -d maidc"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  # ==================== Redis ====================
  redis:
    image: redis:7-alpine
    container_name: maidc-redis
    command: redis-server --requirepass maidc_redis --appendonly yes
    ports:
      - "6379:6379"
    volumes:
      - redisdata:/data
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "maidc_redis", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  # ==================== MinIO ====================
  minio:
    image: minio/minio:latest
    container_name: maidc-minio
    environment:
      MINIO_ROOT_USER: maidc
      MINIO_ROOT_PASSWORD: maidc12345
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"   # API端口
      - "9001:9001"   # 控制台端口
    volumes:
      - miniodata:/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  # ==================== RabbitMQ ====================
  rabbitmq:
    image: rabbitmq:3.12-management
    container_name: maidc-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: maidc
      RABBITMQ_DEFAULT_PASS: maidc123
      RABBITMQ_DEFAULT_VHOST: maidc
    ports:
      - "5672:5672"    # AMQP端口
      - "15672:15672"  # 管理界面
    volumes:
      - rabbitmqdata:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "check_port_connectivity"]
      interval: 15s
      timeout: 10s
      retries: 5
    restart: unless-stopped

  # ==================== Nacos ====================
  nacos:
    image: nacos/nacos-server:v2.3.0
    container_name: maidc-nacos
    environment:
      MODE: standalone
      SPRING_DATASOURCE_PLATFORM: embedded
      NACOS_AUTH_ENABLE: "true"
      NACOS_AUTH_TOKEN: "SecretKey012345678901234567890123456789012345678901234567890123456789"
      NACOS_AUTH_IDENTITY_KEY: serverIdentity
      NACOS_AUTH_IDENTITY_VALUE: security
    ports:
      - "8848:8848"
      - "9848:9848"
    healthcheck:
      test: ["CMD-SHELL", "curl -sf http://localhost:8848/nacos/actuator/health || exit 1"]
      interval: 15s
      timeout: 10s
      retries: 10
    restart: unless-stopped

volumes:
  pgdata:
  redisdata:
  miniodata:
  rabbitmqdata:
```

### 2.2 启动命令

```bash
# 进入docker目录
cd docker

# 启动所有基础设施（后台运行）
docker compose -f docker-compose-infra.yml up -d

# 查看状态
docker compose -f docker-compose-infra.yml ps

# 查看日志
docker compose -f docker-compose-infra.yml logs -f postgres

# 停止所有服务
docker compose -f docker-compose-infra.yml down

# 停止并清除数据（慎用）
docker compose -f docker-compose-infra.yml down -v
```

### 2.3 验证基础设施

| 服务 | 验证方式 |
|------|---------|
| PostgreSQL | `psql -h localhost -U maidc -d maidc` (密码: maidc123) |
| Redis | `redis-cli -h localhost -a maidc_redis ping` → PONG |
| MinIO | 浏览器访问 http://localhost:9001 (maidc/maidc12345) |
| RabbitMQ | 浏览器访问 http://localhost:15672 (maidc/maidc123) |
| Nacos | 浏览器访问 http://localhost:8848/nacos (nacos/nacos) |

---

## 3. 数据库初始化

### 3.1 初始化脚本

SQL脚本位于 `docker/init-db/` 目录，按文件名排序自动执行：

| 顺序 | 文件名 | 内容 |
|------|--------|------|
| 1 | `01-schemas.sql` | 创建5个Schema + 扩展 |
| 2 | `02-system.sql` | 系统表 + 初始数据 |
| 3 | `03-model.sql` | 模型管理表(11张) |
| 4 | `04-cdr.sql` | CDR表(28张) |
| 5 | `05-rdr.sql` | RDR表(19张) |
| 6 | `06-audit.sql` | 审计表(3张) + 分区 |

> 挂载到 `/docker-entrypoint-initdb.d` 后 PostgreSQL 首次启动时自动执行。

### 3.2 01-schemas.sql

```sql
-- MAIDC Schema 初始化
-- 创建5个业务Schema

-- 启用必要扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 创建Schema
CREATE SCHEMA IF NOT EXISTS system;
CREATE SCHEMA IF NOT EXISTS cdr;
CREATE SCHEMA IF NOT EXISTS rdr;
CREATE SCHEMA IF NOT EXISTS model;
CREATE SCHEMA IF NOT EXISTS audit;

-- Schema注释
COMMENT ON SCHEMA system IS '系统管理（用户/角色/权限/字典/配置）';
COMMENT ON SCHEMA cdr IS '临床数据仓库（患者/就诊/检验/影像等）';
COMMENT ON SCHEMA rdr IS '研究数据仓库（项目/队列/数据集/ETL）';
COMMENT ON SCHEMA model IS '模型管理（注册/版本/评估/部署/监控）';
COMMENT ON SCHEMA audit IS '审计日志（操作/数据访问/系统事件）';

-- 授权
GRANT USAGE ON SCHEMA system, cdr, rdr, model, audit TO maidc;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA system, cdr, rdr, model, audit TO maidc;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA system, cdr, rdr, model, audit TO maidc;
ALTER DEFAULT PRIVILEGES IN SCHEMA system, cdr, rdr, model, audit GRANT ALL PRIVILEGES ON TABLES TO maidc;
ALTER DEFAULT PRIVILEGES IN SCHEMA system, cdr, rdr, model, audit GRANT ALL PRIVILEGES ON SEQUENCES TO maidc;
```

### 3.3 02-system.sql（核心初始化数据）

```sql
-- ==================== system.s_user ====================
CREATE TABLE system.s_user (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(64)     NOT NULL,
    password_hash   VARCHAR(256)    NOT NULL,
    real_name       VARCHAR(64)     NOT NULL,
    email           VARCHAR(128),
    phone           VARCHAR(32),
    avatar_url      VARCHAR(256),
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    last_login_at   TIMESTAMP,
    last_login_ip   VARCHAR(45),
    password_changed_at TIMESTAMP,
    must_change_pwd BOOLEAN         NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(64)     NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT uk_username UNIQUE (org_id, username)
);

-- ==================== system.s_role ====================
CREATE TABLE system.s_role (
    id              BIGSERIAL       PRIMARY KEY,
    role_code       VARCHAR(32)     NOT NULL,
    role_name       VARCHAR(64)     NOT NULL,
    description     TEXT,
    is_system       BOOLEAN         NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(64)     NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT uk_role_code UNIQUE (org_id, role_code)
);

-- ==================== system.s_permission ====================
CREATE TABLE system.s_permission (
    id              BIGSERIAL       PRIMARY KEY,
    permission_code VARCHAR(64)     NOT NULL,
    permission_name VARCHAR(128)    NOT NULL,
    resource_type   VARCHAR(32)     NOT NULL,
    resource_key    VARCHAR(128)    NOT NULL,
    action          VARCHAR(32)     NOT NULL,
    parent_id       BIGINT          REFERENCES system.s_permission(id),
    sort_order      INT             NOT NULL DEFAULT 0,
    created_by      VARCHAR(64)     NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT uk_perm_code UNIQUE (org_id, permission_code)
);

-- ==================== system.s_user_role ====================
CREATE TABLE system.s_user_role (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES system.s_user(id),
    role_id         BIGINT          NOT NULL REFERENCES system.s_role(id),
    granted_by      BIGINT          NOT NULL DEFAULT 0,
    granted_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP,
    org_id          BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

-- ==================== system.s_role_permission ====================
CREATE TABLE system.s_role_permission (
    id              BIGSERIAL       PRIMARY KEY,
    role_id         BIGINT          NOT NULL REFERENCES system.s_role(id),
    permission_id   BIGINT          NOT NULL REFERENCES system.s_permission(id),
    org_id          BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT uk_role_perm UNIQUE (role_id, permission_id)
);

-- ==================== system.s_dict ====================
CREATE TABLE system.s_dict (
    id              BIGSERIAL       PRIMARY KEY,
    dict_type       VARCHAR(32)     NOT NULL,
    dict_code       VARCHAR(32)     NOT NULL,
    dict_label      VARCHAR(128)    NOT NULL,
    dict_value      VARCHAR(256),
    sort_order      INT             NOT NULL DEFAULT 0,
    parent_code     VARCHAR(32),
    is_enabled      BOOLEAN         NOT NULL DEFAULT TRUE,
    remark          VARCHAR(256),
    created_by      VARCHAR(64)     NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT uk_dict UNIQUE (org_id, dict_type, dict_code)
);

-- ==================== system.s_config ====================
CREATE TABLE system.s_config (
    id              BIGSERIAL       PRIMARY KEY,
    config_key      VARCHAR(128)    NOT NULL,
    config_value    TEXT            NOT NULL,
    config_type     VARCHAR(16)     NOT NULL DEFAULT 'STRING',
    description     VARCHAR(256),
    is_encrypted    BOOLEAN         NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(64)     NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id          BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT uk_config_key UNIQUE (org_id, config_key)
);

-- ==================== 初始数据 ====================

-- 超级管理员（密码: admin123，BCrypt加密）
INSERT INTO system.s_user (username, password_hash, real_name, status, created_by, org_id)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'ACTIVE', 'system', 0);

-- 系统内置角色（6个）
INSERT INTO system.s_role (role_code, role_name, description, is_system, created_by, org_id) VALUES
('PLATFORM_ADMIN', '平台管理员', '系统运维与权限管理', TRUE, 'system', 0),
('DATA_ADMIN',     '数据管理员', '临床/科研数据治理', TRUE, 'system', 0),
('RESEARCHER',     '研究员',     '科研项目负责人',   TRUE, 'system', 0),
('AI_ENGINEER',    'AI工程师',   '模型开发与部署',   TRUE, 'system', 0),
('CLINICAL_DOCTOR','临床医生',   'AI辅助诊断使用者', TRUE, 'system', 0),
('AUDITOR',        '审计员',     '合规审查',         TRUE, 'system', 0);

-- admin用户赋予平台管理员角色
INSERT INTO system.s_user_role (user_id, role_id, granted_by, org_id)
SELECT u.id, r.id, 0, 0
FROM system.s_user u, system.s_role r
WHERE u.username = 'admin' AND r.role_code = 'PLATFORM_ADMIN';

-- 基础数据字典
INSERT INTO system.s_dict (dict_type, dict_code, dict_label, dict_value, sort_order, created_by, org_id) VALUES
('model_type',      'IMAGING',       '影像AI模型',   NULL, 1, 'system', 0),
('model_type',      'NLP',           'NLP文本模型',  NULL, 2, 'system', 0),
('model_type',      'GENOMIC',       '基因组学模型', NULL, 3, 'system', 0),
('model_type',      'STRUCTURED',    '结构化数据模型',NULL, 4, 'system', 0),
('model_type',      'MULTIMODAL',    '多模态模型',   NULL, 5, 'system', 0),
('model_status',    'DRAFT',         '草稿',         NULL, 1, 'system', 0),
('model_status',    'REGISTERED',    '已注册',       NULL, 2, 'system', 0),
('model_status',    'PUBLISHED',     '已发布',       NULL, 3, 'system', 0),
('model_status',    'DEPRECATED',    '已废弃',       NULL, 4, 'system', 0),
('framework',       'PYTORCH',       'PyTorch',      NULL, 1, 'system', 0),
('framework',       'TENSORFLOW',    'TensorFlow',   NULL, 2, 'system', 0),
('framework',       'SKLEARN',       'scikit-learn', NULL, 3, 'system', 0),
('framework',       'XGBOOST',       'XGBoost',      NULL, 4, 'system', 0),
('framework',       'ONNX',          'ONNX',         NULL, 5, 'system', 0),
('task_type',       'CLASSIFICATION', '分类',        NULL, 1, 'system', 0),
('task_type',       'SEGMENTATION',   '分割',        NULL, 2, 'system', 0),
('task_type',       'OBJECT_DETECTION','目标检测',    NULL, 3, 'system', 0),
('task_type',       'NAMED_ENTITY',   '命名实体识别', NULL, 4, 'system', 0),
('task_type',       'REGRESSION',     '回归',        NULL, 5, 'system', 0),
('encounter_type',  'OUTPATIENT',     '门诊',        NULL, 1, 'system', 0),
('encounter_type',  'INPATIENT',      '住院',        NULL, 2, 'system', 0),
('encounter_type',  'EMERGENCY',      '急诊',        NULL, 3, 'system', 0),
('gender',          'M',             '男',           NULL, 1, 'system', 0),
('gender',          'F',             '女',           NULL, 2, 'system', 0),
('gender',          'O',             '其他',         NULL, 3, 'system', 0);

-- 系统配置
INSERT INTO system.s_config (config_key, config_value, config_type, description, created_by, org_id) VALUES
('jwt.secret',         'maidc-jwt-secret-key-2026-change-in-production', 'STRING', 'JWT签名密钥', 'system', 0),
('jwt.expiration',     '86400',    'NUMBER', 'JWT过期时间(秒)', 'system', 0),
('jwt.refresh.expiration','604800','NUMBER', 'Refresh Token过期时间(秒)', 'system', 0),
('password.minLength', '8',        'NUMBER', '密码最小长度', 'system', 0),
('login.maxRetry',     '5',        'NUMBER', '登录最大重试次数', 'system', 0),
('login.lockDuration', '1800',     'NUMBER', '账户锁定时长(秒)', 'system', 0),
('file.maxSize',       '524288000','NUMBER', '文件上传最大字节数(500MB)', 'system', 0);
```

> 其他Schema建表SQL参照 PRD 文档 `docs/superpowers/specs/2026-04-08-maidc-design.md` 中的完整DDL。

---

## 4. MinIO 初始化

### 4.1 创建Bucket

通过MinIO控制台 (http://localhost:9001) 或 mc 命令行创建：

```bash
# 安装mc客户端
# Windows: choco install minio-client
# Mac: brew install minio/stable/mc

# 配置别名
mc alias set maidc http://localhost:9000 maidc maidc12345

# 创建4个Bucket
mc mb maidc/maidc-models
mc mb maidc/maidc-dicom
mc mb maidc/maidc-datasets
mc mb maidc/maidc-docs

# 验证
mc ls maidc
```

### 4.2 Bucket用途

| Bucket | 用途 | 访问策略 |
|--------|------|---------|
| `maidc-models` | 模型文件(.pt/.onnx/.pkl) | 私有，服务端签名URL |
| `maidc-dicom` | DICOM影像文件 | 私有，服务端签名URL |
| `maidc-datasets` | 数据集文件(CSV/Parquet) | 私有，服务端签名URL |
| `maidc-docs` | 审批材料/报告等附件 | 私有，服务端签名URL |

### 4.3 文件路径规范

```
maidc-models/
  └── {model_id}/
      └── {version_no}/
          ├── model.pt              # 模型文件
          └── config.yaml           # 配置文件

maidc-dicom/
  └── {yyyy}/{MM}/{dd}/
      └── {accession_no}/
          └── *.dcm                 # DICOM文件

maidc-datasets/
  └── {dataset_id}/
      └── {version_no}/
          └── data.parquet          # 数据文件

maidc-docs/
  └── approval/
      └── {approval_id}/
          └── {filename}            # 审批材料
```

---

## 5. 后端项目启动

### 5.1 编译

```bash
# 克隆项目
git clone <repo-url> maidc
cd maidc

# 编译（跳过测试）
mvn clean install -DskipTests

# 首次编译会下载依赖，约3-5分钟
```

### 5.2 Nacos 配置

1. 访问 Nacos 控制台：http://localhost:8848/nacos （用户名/密码：nacos/nacos）
2. 创建命名空间 `maidc-dev`
3. 导入各服务配置，以 `maidc-model` 为例：

**Data ID**: `maidc-model-dev.yaml`

```yaml
server:
  port: 8083

spring:
  application:
    name: maidc-model
  datasource:
    url: jdbc:postgresql://localhost:5432/maidc?currentSchema=model
    username: maidc
    password: maidc123
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
    properties:
      hibernate:
        default_schema: model
        format_sql: true
  rabbitmq:
    host: localhost
    port: 5672
    username: maidc
    password: maidc123
    virtual-host: maidc
  data:
    redis:
      host: localhost
      port: 6379
      password: maidc_redis

minio:
  endpoint: http://localhost:9000
  access-key: maidc
  secret-key: maidc12345
  bucket-models: maidc-models

jwt:
  secret: maidc-jwt-secret-key-2026-change-in-production
  expiration: 86400
```

### 5.3 启动顺序

Phase 1 需启动的服务：

```bash
# 1. 网关（依赖Nacos）
cd maidc-gateway && mvn spring-boot:run

# 2. 认证服务（依赖Redis+PostgreSQL）
cd maidc-auth && mvn spring-boot:run

# 3. 模型管理服务（依赖PostgreSQL+Redis+MinIO+RabbitMQ）
cd maidc-model && mvn spring-boot:run
```

也可通过IDE直接运行各服务的 `Application` 主类。

### 5.4 验证

```bash
# 检查Gateway健康
curl http://localhost:8080/actuator/health

# 检查Nacos注册
# 访问 http://localhost:8848/nacos → 服务管理 → 服务列表
# 应看到 maidc-gateway, maidc-auth, maidc-model 三个服务

# 测试登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

---

## 6. 前端项目启动

### 6.1 安装依赖

```bash
cd maidc-portal

# 使用pnpm安装
pnpm install
```

### 6.2 环境配置

创建 `.env.development`：

```env
# API基础地址
VITE_API_BASE_URL=http://localhost:8080

# 应用标题
VITE_APP_TITLE=MAIDC 医疗AI数据中心

# 是否开启Mock
VITE_USE_MOCK=false
```

### 6.3 Vite 代理配置

`vite.config.ts` 中配置API代理：

```typescript
export default defineConfig({
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 不需要rewrite，后端路径已包含/api前缀
      },
    },
  },
});
```

### 6.4 启动开发服务器

```bash
pnpm dev

# 输出：
# VITE v5.x.x  ready in xxx ms
# ➜  Local:   http://localhost:3000/
```

访问 http://localhost:3000 ，使用 admin/admin123 登录。

---

## 7. AI Worker 启动

### 7.1 安装依赖

```bash
cd maidc-aiworker

# 使用Poetry安装依赖
poetry install
```

`pyproject.toml` 核心依赖：

```toml
[tool.poetry.dependencies]
python = "^3.10"
fastapi = "^0.110.0"
uvicorn = {extras = ["standard"], version = "^0.27.0"}
celery = {extras = ["rabbitmq"], version = "^5.3.0"}
redis = "^5.0.0"
pydantic = "^2.6.0"
sqlalchemy = "^2.0.0"
psycopg2-binary = "^2.9.0"
minio = "^7.2.0"
httpx = "^0.27.0"

[tool.poetry.group.dev.dependencies]
pytest = "^8.0.0"
pytest-asyncio = "^0.23.0"
```

### 7.2 环境配置

创建 `.env` 文件：

```env
# RabbitMQ
RABBITMQ_URL=amqp://maidc:maidc123@localhost:5672/maidc

# PostgreSQL（只读连接）
DATABASE_URL=postgresql://maidc:maidc123@localhost:5432/maidc

# Redis
REDIS_URL=redis://:maidc_redis@localhost:6379/0

# MinIO
MINIO_ENDPOINT=localhost:9000
MINIO_ACCESS_KEY=maidc
MINIO_SECRET_KEY=maidc12345
MINIO_SECURE=false

# 服务配置
WORKER_CONCURRENCY=4
LOG_LEVEL=INFO
```

### 7.3 启动服务

```bash
# 启动FastAPI推理服务（同步推理API）
poetry run uvicorn app.main:app --host 0.0.0.0 --port 8090 --reload

# 启动Celery Worker（异步任务执行）
# 高优先级：推理+评估
poetry run celery -A app.celery_app worker \
  -Q inference,evaluation \
  --concurrency=2 \
  --loglevel=info \
  -n worker-gpu@%h

# 低优先级：预处理+批量推理
poetry run celery -A app.celery_app worker \
  -Q preprocessing,batch_inference \
  --concurrency=4 \
  --loglevel=info \
  -n worker-cpu@%h
```

### 7.4 验证

```bash
# FastAPI健康检查
curl http://localhost:8090/health

# FastAPI文档
# 浏览器访问 http://localhost:8090/docs

# Celery状态检查
poetry run celery -A app.celery_app inspect active
poetry run celery -A app.celery_app inspect stats
```

---

## 8. 常见问题 FAQ

### PostgreSQL

**Q: 连接被拒绝**
```
FATAL: no pg_hba.conf entry for host
```
A: 修改 `pg_hba.conf`，添加：
```
host    maidc    maidc    0.0.0.0/0    md5
```

**Q: Schema不存在**
A: 确认 `init-db/01-schemas.sql` 已执行。手动执行：
```bash
psql -h localhost -U maidc -d maidc -f docker/init-db/01-schemas.sql
```

### Nacos

**Q: 服务注册不上**
A: 检查：
1. Nacos 是否启动（http://localhost:8848/nacos）
2. 应用配置的 `spring.cloud.nacos.discovery.server-addr` 是否正确
3. 命名空间是否匹配

**Q: 配置读取不到**
A: 确保 Data ID 格式为 `{服务名}-{profile}.{后缀}`，如 `maidc-model-dev.yaml`

### RabbitMQ

**Q: vhost 不存在**
A: 手动创建vhost并授权：
```bash
docker exec maidc-rabbitmq rabbitmqctl add_vhost maidc
docker exec maidc-rabbitmq rabbitmqctl set_permissions -p maidc maidc ".*" ".*" ".*"
```

### MinIO

**Q: Bucket访问被拒绝**
A: MinIO Bucket默认私有，需要通过服务端签名URL访问，不要设置公开策略。

### Maven

**Q: 编译失败 - 依赖下载超时**
A: 配置国内Maven镜像（`~/.m2/settings.xml`）：
```xml
<mirror>
  <id>aliyun</id>
  <mirrorOf>central</mirrorOf>
  <url>https://maven.aliyun.com/repository/central</url>
</mirror>
```

### Docker

**Q: 容器内存不足**
A: Docker Desktop → Settings → Resources → Memory 调整至 8GB+

---

> **文档结束** — MAIDC 开发环境搭建指南 v1.0
