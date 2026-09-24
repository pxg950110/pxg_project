# MAIDC 后端服务启动指南

## 前置依赖

启动 MAIDC 服务前，请确保以下基础服务已运行：

| 服务 | 端口 | 说明 |
|------|------|------|
| Nacos | 8848 | 服务注册与配置中心 |
| Redis | 6379 | 缓存服务 (密码: maidc_redis) |
| PostgreSQL | 5432 | 数据库服务 |

## 快速启动

### Windows 用户

1. **启动所有服务**
   ```cmd
   start-all-services.bat
   ```
   - 每个服务将在独立的命令行窗口中运行
   - 首次启动需要下载依赖，可能需要几分钟

2. **检查服务状态**
   ```cmd
   check-services.bat
   ```

3. **停止所有服务**
   ```cmd
   stop-all-services.bat
   ```

### Linux/Mac 用户

```bash
# 在项目根目录执行
cd maidc-parent
mvn spring-boot:run -pl maidc-gateway -Dspring-boot.run.profiles=dev &
mvn spring-boot:run -pl maidc-auth -Dspring-boot.run.profiles=dev &
mvn spring-boot:run -pl maidc-data -Dspring-boot.run.profiles=dev &
mvn spring-boot:run -pl maidc-model -Dspring-boot.run.profiles=dev &
mvn spring-boot:run -pl maidc-task -Dspring-boot.run.profiles=dev &
mvn spring-boot:run -pl maidc-label -Dspring-boot.run.profiles=dev &
mvn spring-boot:run -pl maidc-audit -Dspring-boot.run.profiles=dev &
mvn spring-boot:run -pl maidc-msg -Dspring-boot.run.profiles=dev &
```

## 服务端口映射

| 服务 | 端口 | 说明 |
|------|------|------|
| maidc-gateway | 8080 | API网关 (统一入口) |
| maidc-auth | 8081 | 认证服务 |
| maidc-data | 8082 | 数据服务 |
| maidc-model | 8083 | 模型服务 |
| maidc-task | 8084 | 任务服务 |
| maidc-label | 8085 | 标注服务 |
| maidc-audit | 8086 | 审计服务 |
| maidc-msg | 8087 | 消息服务 |

## 访问地址

- **API网关**: http://localhost:8080
- **Nacos控制台**: http://localhost:8848/nacos (用户名/密码: nacos/nacos)
- **Swagger文档**: http://localhost:8080/swagger-ui.html (如果启用)

## 服务启动顺序

推荐启动顺序：
1. 基础服务 (Nacos → Redis → PostgreSQL)
2. Gateway (8080)
3. 其他微服务 (可并行启动)

## 故障排查

### 服务无法启动

1. **检查端口占用**
   ```cmd
   netstat -ano | findstr "8080"
   ```

2. **检查日志**
   - 查看各服务窗口的错误信息
   - 日志文件位置: `maidc-parent/logs/`

3. **检查Nacos连接**
   - 确保Nacos已启动: http://localhost:8848/nacos
   - 检查服务是否注册成功

### 常见问题

**Q: 服务启动后立即退出**
- 检查数据库连接配置
- 检查Redis连接配置
- 查看日志文件中的错误信息

**Q: Gateway无法路由到其他服务**
- 确保目标服务已启动并注册到Nacos
- 在Nacos控制台查看服务列表

**Q: 认证失败**
- 检查JWT配置是否正确
- 确保Redis连接正常

## 开发环境配置

配置文件位置: `maidc-parent/maidc-*/src/main/resources/application-dev.yml`

关键配置项：
- 数据库连接
- Redis连接
- Nacos地址
- JWT密钥

## 生产环境部署

生产环境请使用:
- Docker容器化部署
- Kubernetes编排
- 使用 `application-prod.yml` 配置文件
