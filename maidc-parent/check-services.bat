@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo MAIDC 后端服务启动状态检查
echo ========================================
echo.

echo 正在检查基础服务...
echo.

REM 检查 Nacos
netstat -ano | findstr ":8848" | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo [✓] Nacos      运行中 (端口 8848)
) else (
    echo [✗] Nacos      未运行 - 请先启动 Nacos
)

REM 检查 Redis
netstat -ano | findstr ":6379" | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo [✓] Redis      运行中 (端口 6379)
) else (
    echo [✗] Redis      未运行 - 请先启动 Redis
)

REM 检查 PostgreSQL
netstat -ano | findstr ":5432" | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo [✓] PostgreSQL 运行中 (端口 5432)
) else (
    echo [✗] PostgreSQL 未运行 - 请先启动 PostgreSQL
)

echo.
echo 正在检查 MAIDC 服务...
echo.

REM 检查各个服务端口
set services[0]=8080:Gateway
set services[1]=8081:Auth
set services[2]=8082:Data
set services[3]=8083:Model
set services[4]=8084:Task
set services[5]=8085:Label
set services[6]=8086:Audit
set services[7]=8087:Msg

set running=0
set total=8

for /L %%i in (0,1,7) do (
    for /f "tokens=1,2 delims=:" %%a in ("!services[%%i]!") do (
        set port=%%a
        set name=%%b
        netstat -ano | findstr ":!port!" | findstr "LISTENING" >nul 2>&1
        if !errorlevel! equ 0 (
            echo [✓] !name!     运行中 (端口 !port!)
            set /a running+=1
        ) else (
            echo [ ] !name!     未启动 (端口 !port!)
        )
    )
)

echo.
echo ========================================
echo 状态摘要: !running! / !total! 个服务运行中
echo ========================================

if !running! equ !total! (
    echo.
    echo ✓ 所有服务已成功启动！
    echo.
    echo 访问地址：
    echo   - API网关: http://localhost:8080
    echo   - Nacos控制台: http://localhost:8848/nacos
    echo.
) else if !running! gtr 0 (
    echo.
    echo ⚠ 部分服务未启动，请检查日志窗口
    echo.
) else (
    echo.
    echo ✗ 没有服务运行，请运行 start-all-services.bat
    echo.
)

pause