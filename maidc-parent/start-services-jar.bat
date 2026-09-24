@echo off
chcp 65001 >nul
echo ========================================
echo MAIDC 后端服务启动脚本 (JAR模式)
echo ========================================
echo.

echo 前置依赖检查：
echo   - Nacos (localhost:8848)
echo   - Redis (localhost:6379)
echo   - PostgreSQL (localhost:5432)
echo.

echo 正在并行启动所有服务...
echo 每个服务将在独立的命令行窗口中运行
echo.

REM 并行启动所有服务 (使用JAR文件)
start "MAIDC-Gateway-8080" cmd /k "cd /d %~dp0maidc-gateway\target && title MAIDC-Gateway-8080 && java -jar -Dspring.profiles.active=dev maidc-gateway-1.0.0-SNAPSHOT.jar"
start "MAIDC-Auth-8081" cmd /k "cd /d %~dp0maidc-auth\target && title MAIDC-Auth-8081 && java -jar -Dspring.profiles.active=dev maidc-auth-1.0.0-SNAPSHOT.jar"
start "MAIDC-Data-8082" cmd /k "cd /d %~dp0maidc-data\target && title MAIDC-Data-8082 && java -jar -Dspring.profiles.active=dev maidc-data-1.0.0-SNAPSHOT.jar"
start "MAIDC-Model-8083" cmd /k "cd /d %~dp0maidc-model\target && title MAIDC-Model-8083 && java -jar -Dspring.profiles.active=dev maidc-model-1.0.0-SNAPSHOT.jar"
start "MAIDC-Task-8084" cmd /k "cd /d %~dp0maidc-task\target && title MAIDC-Task-8084 && java -jar -Dspring.profiles.active=dev maidc-task-1.0.0-SNAPSHOT.jar"
start "MAIDC-Label-8085" cmd /k "cd /d %~dp0maidc-label\target && title MAIDC-Label-8085 && java -jar -Dspring.profiles.active=dev maidc-label-1.0.0-SNAPSHOT.jar"
start "MAIDC-Audit-8086" cmd /k "cd /d %~dp0maidc-audit\target && title MAIDC-Audit-8086 && java -jar -Dspring.profiles.active=dev maidc-audit-1.0.0-SNAPSHOT.jar"
start "MAIDC-Msg-8087" cmd /k "cd /d %~dp0maidc-msg\target && title MAIDC-Msg-8087 && java -jar -Dspring.profiles.active=dev maidc-msg-1.0.0-SNAPSHOT.jar"

echo.
echo ========================================
echo 所有服务启动命令已发送！
echo ========================================
echo.
echo 服务端口：
echo   - Gateway: 8080 (API网关)
echo   - Auth:    8081 (认证服务)
echo   - Data:    8082 (数据服务)
echo   - Model:   8083 (模型服务)
echo   - Task:    8084 (任务服务)
echo   - Label:   8085 (标注服务)
echo   - Audit:   8086 (审计服务)
echo   - Msg:     8087 (消息服务)
echo.
echo 访问地址：http://localhost:8080
echo Nacos控制台：http://localhost:8848/nacos (nacos/nacos)
echo.
echo 提示：
echo   1. 每个服务在独立窗口运行，请查看日志确认启动成功
echo   2. 使用 check-services.bat 检查服务状态
echo   3. 使用 stop-all-services.bat 停止所有服务
echo.
pause