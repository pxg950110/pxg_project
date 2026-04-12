# P1: 后端服务启动

## 目标
启动 Docker 基础设施 + 8个 Java 微服务，使所有后端 API 返回正常（非500）。

## 启动步骤

### 第一步：Docker 基础设施
```bash
docker-compose up -d postgres redis nacos rabbitmq minio
```

等待所有服务健康检查通过（约60秒，Nacos最慢）。

验证：
- PostgreSQL: `docker exec maidc-postgres pg_isready`
- Redis: `docker exec maidc-redis redis-cli -a maidc_redis ping`
- Nacos: `curl http://localhost:8848/nacos/v1/console/health/readiness`
- RabbitMQ: `curl http://localhost:15672` (管理界面)
- MinIO: `curl http://localhost:9000/minio/health/live`

### 第二步：Java 微服务启动顺序
1. Gateway (8080) — `cd maidc-parent/maidc-gateway && mvn spring-boot:run`
2. Auth (8081) — `cd maidc-parent/maidc-auth && mvn spring-boot:run`
3. Data (8082) — `cd maidc-parent/maidc-data && mvn spring-boot:run`
4. Model (8083) — `cd maidc-parent/maidc-model && mvn spring-boot:run`
5. Task (8084) — `cd maidc-parent/maidc-task && mvn spring-boot:run`
6. Label (8085) — `cd maidc-parent/maidc-label && mvn spring-boot:run`
7. Audit (8086) — `cd maidc-parent/maidc-audit && mvn spring-boot:run`
8. Msg (8087) — `cd maidc-parent/maidc-msg && mvn spring-boot:run`

### 第三步：验证
```bash
# 登录测试
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}'

# 用户列表
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer {token}"
```

## 关键配置
- Nacos共享配置: maidc-shared.yaml（数据库/Redis连接池等）
- JPA DDL: update（自动建表）
- 数据库: 5个schema（system, cdr, model, audit, rdr）
- 初始数据: docker/init-db/02-system.sql（admin用户+角色+权限+字典）

## 成功标准
- 所有5个Docker容器运行中
- 所有8个Java服务在Nacos注册
- `POST /api/v1/auth/login` 返回200 + token
- `GET /api/v1/users` 返回200 + 用户列表
