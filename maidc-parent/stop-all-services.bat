@echo off
chcp 65001 >nul
echo ========================================
echo MAIDC 后端服务停止脚本
echo ========================================
echo.

echo 正在停止所有 MAIDC 服务...
taskkill /FI "WINDOWTITLE eq MAIDC-*" /F

echo.
echo 所有服务已停止
pause