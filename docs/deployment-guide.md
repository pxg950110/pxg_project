# MAIDC 部署手册

## 环境要求

### 硬件最低配置

| 组件 | CPU | 内存 | 磁盘 |
|------|-----|------|------|
| 应用服务器 | 8核 | 16GB | 100GB SSD |
| 数据库服务器 | 8核 | 32GB | 500GB SSD |
| AI推理服务器 | 8核+GPU | 32GB | 200GB SSD |

### 软件环境

| 软件 | 版本 | 说明 |
|------|------|------|
| Docker | 20.10+ | 容器运行时 |
| Docker Compose | v2.20+ | 容器编排 |
| JDK | 17 | Java运行时 |
| PostgreSQL | 15 | 主数据库 |
| Redis | 7 | 缓存 |
| MinIO | latest | 对象存储 |
| RabbitMQ | 3.12 | 消息队列 |
| Nacos | 2.3 | 注册配置中心 |
| Nginx | 1.24+ | 反向代理 |
| Node.js | 18+ | 前端构建 |
| pnpm | 8+ | 前端包管理 |

## Docker Compose 部署（推荐）

### Step 1: 准备配置文件

```bash
# 克隆项目
git clone <repo-url> /opt/maidc
cd /opt/maidc

# 复制环境变量模板
cp .env.example .env
```

编辑 `.env` 文件，配置以下变量：

```env
# 数据库
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=maidc
POSTGRES_USER=maidc
POSTGRES_PASSWORD=<change-me>

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=<change-me>

# MinIO
MINIO_ENDPOINT=minio:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=<change-me>

# JWT
MAIDC_JWT_SECRET=<generate-a-256-bit-secret>
MAIDC_AES_KEY=<generate-a-32-byte-key-base64>

# RabbitMQ
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USER=maidc
RABBITMQ_PASSWORD=<change-me>
```

### Step 2: 启动基础设施

```bash
docker compose -f docker/docker-compose-infra.yml up -d

# 等待服务就绪
sleep 30

# 初始化数据库
for f in docker/init-db/*.sql; do
  docker exec -i maidc-postgres psql -U maidc -d maidc < "$f"
done

# 初始化 MinIO
bash docker/init-minio.sh

# 初始化 Nacos
bash docker/init-nacos.sh
```

### Step 3: 构建后端服务

```bash
cd maidc-parent
mvn clean package -DskipTests -Pprod
cd ..
```

### Step 4: 启动全部服务

```bash
docker compose -f docker/docker-compose-full.yml up -d

# 检查服务状态
docker compose -f docker/docker-compose-full.yml ps
```

### Step 5: 构建部署前端

```bash
cd maidc-portal
pnpm install
pnpm build

# 将 dist 目录部署到 Nginx
# 参见 docker/nginx/nginx.conf
```

### Step 6: 启动监控

```bash
docker compose -f docker/docker-compose-monitoring.yml up -d
```

### Step 7: 验证

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 运行E2E测试
bash scripts/e2e-test.sh
```

## Kubernetes 部署（生产环境）

### 前置条件

- Kubernetes 1.28+
- Helm 3.12+
- Ingress Controller (nginx-ingress)
- cert-manager (TLS)
- StorageClass (SSD)

### 部署步骤

```bash
# 1. 创建命名空间
kubectl create namespace maidc

# 2. 创建 Secrets
kubectl create secret generic maidc-secrets \
  --from-literal=POSTGRES_PASSWORD=<password> \
  --from-literal=REDIS_PASSWORD=<password> \
  --from-literal=JWT_SECRET=<secret> \
  --from-literal=AES_KEY=<key> \
  -n maidc

# 3. 部署基础设施（使用 Helm Charts）
helm install postgresql bitnami/postgresql -n maidc -f helm/postgresql-values.yaml
helm install redis bitnami/redis -n maidc -f helm/redis-values.yaml
helm install minio bitnami/minio -n maidc -f helm/minio-values.yaml
helm install rabbitmq bitnami/rabbitmq -n maidc -f helm/rabbitmq-values.yaml

# 4. 部署应用服务
kubectl apply -f k8s/ -n maidc

# 5. 配置 Ingress
kubectl apply -f k8s/ingress.yaml -n maidc
```

### 资源配额建议

| 服务 | CPU Request | CPU Limit | Memory Request | Memory Limit |
|------|-------------|-----------|----------------|--------------|
| gateway | 500m | 1000m | 512Mi | 1Gi |
| auth | 250m | 500m | 256Mi | 512Mi |
| model | 500m | 2000m | 512Mi | 2Gi |
| data | 500m | 1000m | 512Mi | 1Gi |
| task | 250m | 500m | 256Mi | 512Mi |
| label | 250m | 500m | 256Mi | 512Mi |
| audit | 250m | 500m | 256Mi | 512Mi |
| msg | 250m | 500m | 256Mi | 512Mi |
| aiworker | 1000m | 4000m | 2Gi | 8Gi |
| portal | 100m | 200m | 64Mi | 128Mi |

## 数据库维护

### 备份

```bash
# 全量备份
docker exec maidc-postgres pg_dump -U maidc -Fc maidc > backup_$(date +%Y%m%d).dump

# 恢复
docker exec -i maidc-postgres pg_restore -U maidc -d maidc < backup.dump
```

### 分区维护

```bash
# 启用 pg_partman 自动分区
psql -U maidc -d maidc -f docs/sql/pg_partman-partition.sql

# 手动触发分区维护
psql -U maidc -d maidc -c "CALL partman.run_maintenance_proc();"
```

## 安全配置

生产环境部署前务必完成以下配置：

1. **修改默认密码** — admin 用户密码
2. **设置 JWT 密钥** — 环境变量 `MAIDC_JWT_SECRET`
3. **设置 AES 密钥** — 环境变量 `MAIDC_AES_KEY`
4. **启用 HTTPS** — 配置 TLS 证书
5. **配置安全头** — Nginx 已预设安全响应头
6. **网络隔离** — 数据库/Redis/MinIO 不暴露公网

详见 `docs/security/compliance-checklist.md`

## 故障排查

| 问题 | 排查命令 |
|------|---------|
| 服务未启动 | `docker compose logs <service>` |
| 数据库连接失败 | `docker exec maidc-postgres pg_isready` |
| Redis连接失败 | `docker exec maidc-redis redis-cli ping` |
| 网关路由异常 | 检查 Nacos 服务注册状态 |
| 前端白屏 | 检查 Nginx 配置 `try_files` |

## 升级步骤

```bash
# 1. 备份数据库
docker exec maidc-postgres pg_dump -U maidc -Fc maidc > pre-upgrade.dump

# 2. 拉取新版本
git pull origin main

# 3. 数据库迁移（如有）
docker exec -i maidc-postgres psql -U maidc -d maidc < docker/init-db/migration.sql

# 4. 重新构建
cd maidc-parent && mvn clean package -DskipTests -Pprod && cd ..

# 5. 滚动重启
docker compose -f docker/docker-compose-full.yml up -d --no-deps --build maidc-model
# 逐个服务更新...
```
