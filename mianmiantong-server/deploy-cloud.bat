@echo off
chcp 65001 >nul
echo === 面面通后端 - 阿里云 ECS 部署工具 ===
echo.

set SERVER_IP=8.148.15.228
set SERVER_DIR=/opt/mianmiantong

echo [1/3] 编译项目...
call mvn clean package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo 编译失败！
    exit /b 1
)
echo 编译成功！

echo.
echo [2/3] 上传 JAR 到服务器...
scp target\mianmiantong.jar root@%SERVER_IP%:%SERVER_DIR%/mianmiantong.jar
if %ERRORLEVEL% neq 0 (
    echo 上传失败！请检查服务器 IP 和 SSH 连接
    exit /b 1
)
echo JAR 上传成功！

echo.
echo [3/3] 重启服务...
ssh root@%SERVER_IP% "systemctl restart mianmiantong && systemctl status mianmiantong --no-pager"
if %ERRORLEVEL% neq 0 (
    echo 服务重启失败！请检查服务器配置
    exit /b 1
)

echo.
echo === 部署完成 ===
echo 查看日志: ssh root@%SERVER_IP% "journalctl -u mianmiantong -f"
pause
