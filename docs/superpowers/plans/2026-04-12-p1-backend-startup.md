# P1: 后端服务启动 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 启动 Docker 基础设施 + 8个 Java 微服务，使所有后端 API 正常返回。

**Architecture:** Docker Compose 启动 PostgreSQL/Redis/Nacos/RabbitMQ/MinIO 基础设施，然后本地按依赖顺序启动8个 Spring Boot 微服务（通过 `mvn spring-boot:run`）。

**Tech Stack:** Docker Compose, Spring Boot, Maven, PostgreSQL, Redis, Nacos

**Spec:** `docs/superpowers/specs/2026-04-12-p1-backend-startup.md`

---

## File Structure

| 操作 | 文件 | 职责 |
|------|------|------|
| 使用 | `docker/docker-compose-full.yml` | 基础设施容器定义 |
| 使用 | `docker/init-db/01-schemas.sql` | 数据库schema初始化 |
| 使用 | `docker/init-db/02-system.sql` | 系统初始数据 |
| 使用 | `maidc-parent/maidc-*/src/main/resources/bootstrap.yml` | 各服务配置 |

---

### Task 1: 启动 Docker 基础设施

**Files:**
- Use: `docker/docker-compose-full.yml`

- [ ] **Step 1: 启动基础设施容器**

```bash
cd E:/pxg_project/docker && docker-compose -f docker-compose-full.yml up -d maidc-postgres maidc-redis maidc-nacos maidc-rabbitmq maidc-minio
```

Expected: 5个容器全部启动，无错误。

- [ ] **Step 2: 等待并验证 PostgreSQL**

```bash
docker exec maidc-postgres pg_isready -U maidc
```

Expected: `accepting connections`

- [ ] **Step 3: 验证 Redis**

```bash
docker exec maidc-redis redis-cli -a maidc123 ping
```

Expected: `PONG`

- [ ] **Step 4: 验证 Nacos（等待启动完成，约60秒）**

```bash
curl -s http://localhost:8848/nacos/v1/console/health/readiness
```

Expected: 返回 `UP` 或 HTTP 200

- [ ] **Step 5: 验证 RabbitMQ**

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:15672
```

Expected: `200`

- [ ] **Step 6: 验证 MinIO**

```bash
curl -s http://localhost:9000/minio/health/live
```

Expected: HTTP 200

- [ ] **Step 7: 检查数据库初始化**

```bash
docker exec maidc-postgres psql -U maidc -d maidc -c "\dn"
```

Expected: 列出 system, cdr, model, audit, rdr 五个 schema

- [ ] **Step 8: 检查初始数据**

```bash
docker exec maidc-postgres psql -U maidc -d maidc -c "SELECT username FROM system.users WHERE username='admin'"
```

Expected: 返回 admin 用户行

---

### Task 2: 启动 Gateway 服务（8080）

**Files:**
- Use: `maidc-parent/maidc-gateway/`

- [ ] **Step 1: 编译并启动 Gateway**

```bash
cd E:/pxg_project/maidc-parent/maidc-gateway && mvn spring-boot:run -DskipTests
```

Expected: 控制台显示 `Started GatewayApplication`，无错误。

- [ ] **Step 2: 验证 Gateway 启动**

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080
```

Expected: 返回 HTTP 状态码（非连接拒绝）

---

### Task 3: 启动 Auth 服务（8081）

**Files:**
- Use: `maidc-parent/maidc-auth/`

- [ ] **Step 1: 编译并启动 Auth**

```bash
cd E:/pxg_project/maidc-parent/maidc-auth && mvn spring-boot:run -DskipTests
```

Expected: 控制台显示 `Started AuthApplication`，DataInitializer 重置 admin 密码为 `Admin@123`。

- [ ] **Step 2: 验证登录 API**

```bash
curl -s -X POST http://localhost:8081/api/v1/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"Admin@123\"}"
```

Expected: `{"code":200,"data":{"token":"xxx","user":{...}}}`

---

### Task 4: 启动 Data 服务（8082）

**Files:**
- Use: `maidc-parent/maidc-data/`

- [ ] **Step 1: 编译并启动 Data**

```bash
cd E:/pxg_project/maidc-parent/maidc-data && mvn spring-boot:run -DskipTests
```

Expected: 控制台显示 `Started DataApplication`

- [ ] **Step 2: 验证患者 API（通过 Gateway）**

先获取 token（从 Task 3），然后：
```bash
curl -s http://localhost:8080/api/v1/cdr/patients?page=1\&size=5 -H "Authorization: Bearer {token}"
```

Expected: `{"code":200,"data":{"content":[],"totalElements":0}}`

---

### Task 5: 启动 Model 服务（8083）

**Files:**
- Use: `maidc-parent/maidc-model/`

- [ ] **Step 1: 编译并启动 Model**

```bash
cd E:/pxg_project/maidc-parent/maidc-model && mvn spring-boot:run -DskipTests
```

Expected: 控制台显示 `Started ModelApplication`

- [ ] **Step 2: 验证模型 API**

```bash
curl -s http://localhost:8080/api/v1/models -H "Authorization: Bearer {token}"
```

Expected: `{"code":200,"data":{...}}`

---

### Task 6: 启动 Task 服务（8084）

- [ ] **Step 1: 编译并启动 Task**

```bash
cd E:/pxg_project/maidc-parent/maidc-task && mvn spring-boot:run -DskipTests
```

Expected: `Started TaskApplication`

---

### Task 7: 启动 Label 服务（8085）

- [ ] **Step 1: 编译并启动 Label**

```bash
cd E:/pxg_project/maidc-parent/maidc-label && mvn spring-boot:run -DskipTests
```

Expected: `Started LabelApplication`

---

### Task 8: 启动 Audit 服务（8086）

- [ ] **Step 1: 编译并启动 Audit**

```bash
cd E:/pxg_project/maidc-parent/maidc-audit && mvn spring-boot:run -DskipTests
```

Expected: `Started AuditApplication`

---

### Task 9: 启动 Msg 服务（8087）

- [ ] **Step 1: 编译并启动 Msg**

```bash
cd E:/pxg_project/maidc-parent/maidc-msg && mvn spring-boot:run -DskipTests
```

Expected: `Started MsgApplication`

---

### Task 10: 端到端验证

- [ ] **Step 1: 通过 Gateway 验证完整登录流程**

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"Admin@123\"}"
```

Expected: `{"code":200,"data":{"token":"xxx"}}`

- [ ] **Step 2: 验证所有服务在 Nacos 注册**

```bash
curl -s "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=maidc-auth" | python -m json.tool
```

对每个服务重复验证（maidc-gateway, maidc-data, maidc-model, maidc-task, maidc-label, maidc-audit, maidc-msg）。

Expected: 每个服务有至少1个健康实例。

- [ ] **Step 3: 验证前端连接**

在浏览器访问 `http://localhost:3000`，用 admin / Admin@123 登录。

Expected: 登录成功，页面正常加载。
