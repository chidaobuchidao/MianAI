#!/bin/bash
# ==========================================
# 面面通后端 — 阿里云 ECS 一键部署脚本
# 在 ECS 上以 root 执行: bash setup-server.sh
# ==========================================
set -e

APP_DIR="/opt/mianmiantong"
SERVICE_NAME="mianmiantong"

echo "=== [1/6] 安装 JDK 17 ==="
if java -version 2>&1 | grep -q "17\."; then
    echo "JDK 17 已安装，跳过"
else
    # CentOS/RHEL/Alibaba Cloud Linux
    if command -v yum &>/dev/null; then
        yum install -y java-17-openjdk java-17-openjdk-devel
    # Ubuntu/Debian
    elif command -v apt &>/dev/null; then
        apt update && apt install -y openjdk-17-jdk
    else
        echo "无法识别包管理器，请手动安装 JDK 17"
        exit 1
    fi
fi
java -version 2>&1 | head -1

echo ""
echo "=== [2/6] 安装 Redis ==="
if systemctl is-active redis &>/dev/null || systemctl is-active redis-server &>/dev/null; then
    echo "Redis 已运行，跳过"
else
    # 宝塔安装的 Redis 通常在 /www/server/redis/
    if [ -f /www/server/redis/redis.sh ]; then
        /www/server/redis/redis.sh start 2>/dev/null || true
        echo "通过宝塔 Redis 启动"
    elif command -v yum &>/dev/null; then
        yum install -y redis
        systemctl enable redis --now
    elif command -v apt &>/dev/null; then
        apt update && apt install -y redis-server
        systemctl enable redis-server --now
    fi
fi

echo ""
echo "=== [3/6] 创建数据库 ==="
# 从 .env 读取密码
if [ -f "$APP_DIR/.env" ]; then
    source <(grep -E '^DB_' "$APP_DIR/.env" | sed 's/^/export /')
fi
DB_PASS="${DB_PASSWORD:-}"
DB_USER="${DB_USERNAME:-root}"

if [ -n "$DB_PASS" ]; then
    mysql -u"$DB_USER" -p"$DB_PASS" -e "CREATE DATABASE IF NOT EXISTS ai_interview CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || true
else
    mysql -u"$DB_USER" -e "CREATE DATABASE IF NOT EXISTS ai_interview CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || true
fi
echo "数据库 ai_interview 已就绪"

echo ""
echo "=== [4/6] 创建应用目录 ==="
mkdir -p "$APP_DIR"
echo "目录: $APP_DIR"

echo ""
echo "=== [5/6] 配置 systemd 服务 ==="
cat > /etc/systemd/system/${SERVICE_NAME}.service << 'SERVICE_EOF'
[Unit]
Description=面面通 AI模拟面试后端服务
After=network.target mysqld.service redis.service
Wants=mysqld.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/mianmiantong
ExecStart=/usr/bin/java -jar /opt/mianmiantong/mianmiantong.jar --spring.profiles.active=cloud
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=mianmiantong
EnvironmentFile=-/opt/mianmiantong/.env

[Install]
WantedBy=multi-user.target
SERVICE_EOF

# 宝塔 MySQL 服务名可能是 mysqld
if ! systemctl list-unit-files | grep -q "^mysqld.service"; then
    sed -i 's/mysqld.service/mysql.service/g' /etc/systemd/system/${SERVICE_NAME}.service
fi

systemctl daemon-reload
echo "systemd 服务已配置"

echo ""
echo "=== [6/6] 提示 ==="
echo "请将以下文件上传到服务器："
echo "  1. target/mianmiantong.jar     → $APP_DIR/mianmiantong.jar"
echo "  2. .env (用 .env.example 改)    → $APP_DIR/.env"
echo ""
echo "上传完成后执行："
echo "  systemctl enable $SERVICE_NAME --now   # 启动并设为开机自启"
echo "  systemctl status $SERVICE_NAME          # 查看状态"
echo "  journalctl -u $SERVICE_NAME -f          # 查看日志"
echo ""
echo "=== 服务器环境部署完成 ==="
