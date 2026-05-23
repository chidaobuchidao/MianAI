# 服务器部署文档

> 最后更新: 2026-05-19 | 部署状态: 生产运行中

## 一、服务器信息

| 项目 | 值 |
|------|-----|
| 类型 | 阿里云 ECS |
| 配置 | 2核 2GB 40GB |
| OS | Alibaba Cloud Linux 3 |
| 公网 IP | 8.148.15.228 |
| 域名 | mianmiantong.top |
| 管理面板 | 宝塔 Linux 面板 |
| SSH | root / 密码 `Lixm2219909857` 端口 22 |
| 宝塔入口 | `http://8.148.15.228:8888` |

## 二、服务器进程架构

```
用户请求 → :80 (Nginx)
              ├── /           → /www/wwwroot/mianmiantong/ (前端静态文件)
              ├── /api/       → 127.0.0.1:8080 (Spring Boot)
              └── /assets/    → 静态资源缓存 30d

Spring Boot (127.0.0.1:8080)
  ├── MySQL  (127.0.0.1:3306)  数据库: ai-interview
  ├── Redis  (127.0.0.1:6379)
  └── Piston (127.0.0.1:2000)  Docker 容器, 代码沙盒执行
```

## 三、各组件详情

### 3.1 MySQL

- 版本: 5.7 (宝塔安装, 路径 `/www/server/mysql/`)
- 密码: root / `2219909857`
- 数据库: `ai-interview`
- 配置文件: `/etc/my.cnf` (已优化为低内存配置, ~40MB RSS)
- 管理: systemctl (宝塔界面也可)

**重要**: 内存优化过的 my.cnf, 不要用宝塔的"性能调整"改回高配置, 否则 OOM。

### 3.2 Redis

- 端口: 6379
- 绑定: 127.0.0.1
- 无密码
- 管理: systemctl

### 3.3 Piston (代码执行引擎)

- 运行方式: Docker 容器 `piston`
- 镜像: `ghcr.io/engineer-man/piston`
- 端口: 2000 (仅 127.0.0.1, 安全组不应对外暴露)
- 已安装运行时: Python 3.10.0, Java 15.0.2, JavaScript 18.15.0, C/C++ 10.2.0, Go 1.16.2
- 包路径: `/opt/piston/packages/`

```bash
# 常用管理命令
docker ps | grep piston              # 查看状态
docker restart piston                # 重启
curl http://127.0.0.1:2000/api/v2/runtimes  # 查看可用运行时
```

### 3.4 Nginx

- 版本: 1.28.3 (宝塔管理)
- 站点配置: `/www/server/panel/vhost/nginx/8.148.15.228.conf`
- 反代配置: `/www/server/panel/vhost/nginx/proxy/8.148.15.228/`
- 前端目录: `/www/wwwroot/mianmiantong/`

### 3.5 Spring Boot (面面通后端)

- JAR 位置: `/opt/mianmiantong/mianmiantong.jar`
- 环境变量: `/opt/mianmiantong/.env`
- 端口: 8080 (绑定 127.0.0.1)
- 进程管理: 宝塔 Java 项目管理器 (或手动 nohup)
- 日志: `/tmp/mianmiantong.log`
- Profile: cloud
- Flyway: 已禁用 (MySQL 5.7 不兼容), 手动执行 SQL

## 四、构建与部署

### 4.1 本地构建

```bash
# 后端 (在项目根目录)
cd mianmiantong-server
mvn clean package -DskipTests
# 产物: target/mianmiantong.jar

# 前端
cd AI-Interview/web-app
npm run build
# 产物: dist/ 目录
```

### 4.2 上传到服务器

通过宝塔 → 文件:

| 本地路径 | 服务器路径 | 操作 |
|----------|-----------|------|
| `target/mianmiantong.jar` | `/opt/mianmiantong/mianmiantong.jar` | 覆盖上传 |
| `dist/` 下所有文件 | `/www/wwwroot/mianmiantong/` | 覆盖上传 |

### 4.3 重启后端

宝塔 → 网站 → Java 项目 → mianmiantong → 重启

或手动:
```bash
pkill -f mianmiantong
cd /opt/mianmiantong
nohup /www/server/java/jdk-17.0.8/bin/java -jar -Xmx1024M -Xms256M \
  mianmiantong.jar \
  --spring.profiles.active=cloud \
  --server.address=127.0.0.1 \
  --spring.flyway.enabled=false \
  > /tmp/mianmiantong.log 2>&1 &
```

### 4.4 数据库变更

如果代码新增了表/字段/数据, 需要在宝塔数据库 `ai-interview` 中手动执行 SQL。

已有的迁移 SQL 在 `mianmiantong-server/src/main/resources/db/migration/`。

## 五、环境变量 (.env)

位于 `/opt/mianmiantong/.env`:

```
DB_HOST=127.0.0.1
DB_PORT=3306
DB_USERNAME=root
DB_PASSWORD=2219909857
DB_NAME=ai-interview
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
JWT_SECRET=mianmiantong-jwt-secret-2026-production
DEEPSEEK_API_KEY=sk-3075900259cd4e8b8bf8a8e3073cd1ef
ALIBABA_CLOUD_ACCESS_KEY_ID=your-alibaba-cloud-access-key-id
ALIBABA_CLOUD_ACCESS_KEY_SECRET=your-alibaba-cloud-access-key-secret
WECHAT_APP_ID=your-wechat-app-id
WECHAT_APP_SECRET=your-wechat-app-secret
PISTON_API_URL=http://127.0.0.1:2000
```

## 六、Nginx 站点配置

宝塔 → 网站 → 8.148.15.228 → 配置文件:

```nginx
server {
    listen 80;
    server_name 8.148.15.228 mianmiantong.top www.mianmiantong.top;
    index index.html index.htm;
    root /www/wwwroot/mianmiantong;

    client_max_body_size 20m;

    # API 反代 (宝塔反向代理管理)
    include /www/server/panel/vhost/nginx/proxy/8.148.15.228/*.conf;

    # Vue Router history 回退
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

宝塔反向代理配置 (自动生成):
- 代理名称: api
- 代理目录: `/api/`
- 目标 URL: `http://127.0.0.1:8080`
- **注意**: 宝塔默认会在 `proxy_pass` 末尾加 `/`, 导致 `/api/` 前缀被截掉。如果 API 报 404, 检查 proxy 配置文件, 确保 `proxy_pass http://127.0.0.1:8080;` **不带末尾斜杠**。

## 七、验证命令

```bash
# 前端 (本机)
curl http://127.0.0.1/

# 前端 (外网)
curl http://8.148.15.228/

# API
curl -H "Authorization: Bearer dev-token-test" http://127.0.0.1/api/questions/random

# Piston
curl -X POST http://127.0.0.1:2000/api/v2/execute \
  -H 'Content-Type: application/json' \
  -d '{"language":"python","version":"3.10.0","files":[{"name":"t.py","content":"print(42)"}]}'

# 查看 Java 日志
tail -50 /tmp/mianmiantong.log
journalctl -u nginx --no-pager -n 20
```

## 八、安全组 (阿里云控制台)

应开放的端口:
- **22** — SSH
- **80** — HTTP (必须)
- **443** — HTTPS (申请 SSL 后)
- **888** — 宝塔面板 (建议限制来源 IP)

应关闭的端口:
- **2000** — Piston (仅内部使用, 不应对外)
- **3306** — MySQL (仅内部使用)
- **6379** — Redis (仅内部使用)

## 九、已知问题与解决方案

| 问题 | 原因 | 解决 |
|------|------|------|
| MySQL 被 OOM Kill | 2GB 内存不足 | 已优化 my.cnf (~40MB RSS), 不要改回默认配置 |
| DNS 间歇性不可用 | DNS 传播需要时间 | 先用 IP `8.148.15.228` 访问, 等几小时 |
| Nginx `server` directive not allowed | 配置放在了错误位置 | 宝塔站点配置文件已经自带 `server {}` 块 |
| Java 项目宝塔显示"未启动" | 手动命令行启动, 宝塔不可见 | 用宝塔 Java 项目管理器启停 |
| API 返回 500 "No static resource" | proxy_pass 末尾 `/` 截掉了 `/api/` 前缀 | 去掉 `proxy_pass http://127.0.0.1:8080/;` 末尾斜杠 |
| 数据库缺少列 (如 `coding_review`, `model`) | 新功能 SQL 未执行 | 手动 ALTER TABLE 补列 |
| Flyway 报错 "MySQL 5.7 no longer supported" | MySQL 5.7 + 新版 Flyway 不兼容 | 启动参数 `--spring.flyway.enabled=false` |

## 十、添加新域名

1. 阿里云 DNS 添加 A 记录 → `8.148.15.228`
2. 宝塔 → 网站 → 站点 → 配置文件 → 在 `server_name` 后面追加新域名
3. 宝塔 → SSL → Let's Encrypt 申请免费证书
