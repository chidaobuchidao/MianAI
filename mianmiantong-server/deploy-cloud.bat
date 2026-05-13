@echo off
echo === 面面通后端 - 云托管部署脚本 ===

echo [1/3] 编译项目...
call mvn clean package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo 编译失败！
    exit /b 1
)
echo 编译成功！

echo [2/3] 构建 Docker 镜像...
docker build -t mianmiantong-server:latest .
if %ERRORLEVEL% neq 0 (
    echo 镜像构建失败！
    exit /b 1
)
echo 镜像构建成功！

echo [3/3] 本地测试启动（可选）...
echo 运行: docker run -p 8080:80 ^
  -e DB_USERNAME=root ^
  -e DB_PASSWORD=你的密码 ^
  -e CLOUD_MYSQL_HOST=你的云MySQL地址 ^
  -e DEEPSEEK_API_KEY=你的key ^
  -e JWT_SECRET=你的secret ^
  -e ALIBABA_CLOUD_ACCESS_KEY_ID=你的key ^
  -e ALIBABA_CLOUD_ACCESS_KEY_SECRET=你的secret ^
  mianmiantong-server:latest
echo.
echo === 镜像构建完成 ===
echo.
echo 下一步: 将镜像推送至腾讯云容器镜像仓库:
echo   docker tag mianmiantong-server:latest ccr.ccs.tencentyun.com/你的仓库/mianmiantong:latest
echo   docker push ccr.ccs.tencentyun.com/你的仓库/mianmiantong:latest
echo.
echo 然后在云托管管理后台选择「镜像部署」即可
pause
