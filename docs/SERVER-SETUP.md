# 服务器配置指南

> 服务器: 阿里云 ECS 2核2G 40GB | OS: Alibaba Cloud Linux 3 | IP: 8.148.15.228

## 一、Piston 代码执行引擎

### 部署

```bash
# 1. 安装 Docker
yum install -y yum-utils
yum-config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
yum install -y docker-ce docker-ce-cli containerd.io
systemctl start docker && systemctl enable docker

# 2. 启用 cgroup v2（Alibaba Cloud Linux 3 需手动配置）
grubby --update-kernel=ALL --args="systemd.unified_cgroup_hierarchy=1"
reboot

# 3. 启动 Piston 容器（挂载持久化卷）
docker run -d -p 2000:2000 --restart=always --name piston \
  -v /opt/piston/packages:/piston/packages \
  --tmpfs /piston/jobs:exec,size=256M \
  --privileged \
  ghcr.io/engineer-man/piston
```

### 语言包安装

包文件从 GitHub Releases 下载（本地下载后上传）：

| 语言 | Piston 名称 | 版本 | 文件大小 |
|------|-------------|------|----------|
| Python | `python` | 3.10.0 | 169MB |
| Java | `java` | 15.0.2 | 187MB |
| JavaScript | `node` | 18.15.0 | 43MB |
| C/C++ | `gcc` | 10.2.0 | 690MB |
| Go | `go` | 1.16.2 | 123MB |

```bash
# 上传包到 /root/piston_pkgs/，然后：
for d in python/3.10.0 java/15.0.2 node/18.15.0 gcc/10.2.0 go/1.16.2; do
  mkdir -p /opt/piston/packages/$d
  tar xzf /root/piston_pkgs/${d%/*}-${d#*/}.pkg.tar.gz -C /opt/piston/packages/$d
  touch /opt/piston/packages/$d/.ppman-installed  # 安装标记
done

# 修复 run 脚本为绝对路径
# 例如: echo '#!/bin/bash\n/piston/packages/python/3.10.0/bin/python3.10 "$@"' > /opt/piston/packages/python/3.10.0/run

chown -R 1000:1000 /opt/piston/packages/
docker restart piston
```

### 防火墙

```bash
# 阿里云安全组：入方向 TCP 2000/2000, 来源 0.0.0.0/0
# 宝塔面板 → 安全 → 防火墙 → 添加 TCP 2000
firewall-cmd --add-port=2000/tcp --permanent && firewall-cmd --reload
```

### 验证

```bash
# 检查运行状态
docker ps | grep piston
# 检查安装的运行时
curl -s http://localhost:2000/api/v2/runtimes | python3 -c 'import json,sys; print(len(json.load(sys.stdin)), "runtimes")'
# 测试执行
curl -X POST http://localhost:2000/api/v2/execute -H 'Content-Type: application/json' \
  -d '{"language":"python","version":"3.10.0","files":[{"name":"t.py","content":"print(42)"}]}'
```

## 二、后端部署

### 项目路径

```
/opt/mianmiantong/
├── mianmiantong.jar           # Spring Boot jar
├── .env                        # 环境变量
├── application-cloud.yml       # 生产配置
├── mianmiantong.service        # systemd 服务
└── nginx-mianmiantong.conf     # Nginx 反代
```

### 环境变量 (.env)

```env
DB_HOST=127.0.0.1
DB_PORT=3306
DB_USERNAME=root
DB_PASSWORD=your-db-password
JWT_SECRET=your-jwt-secret
DEEPSEEK_API_KEY=sk-your-key
PISTON_API_URL=http://127.0.0.1:2000  # 同机部署用本地回环
```

### systemd 服务

```ini
[Unit]
Description=MianmianTong Server
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/mianmiantong
ExecStart=/usr/bin/java -jar /opt/mianmiantong/mianmiantong.jar --spring.config.location=/opt/mianmiantong/application-cloud.yml
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### Nginx 配置

```nginx
server {
    listen 80;
    server_name _;
    # Web 前端静态文件
    root /opt/mianmiantong/web-app/dist;
    index index.html;
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

### 部署命令

```bash
# 构建
cd mianmiantong-server && mvn clean package -DskipTests

# 上传
scp target/mianmiantong.jar root@8.148.15.228:/opt/mianmiantong/

# 启动
systemctl restart mianmiantong
systemctl status mianmiantong
```

## 三、开发环境 vs 生产环境

| 配置 | 开发环境 | 生产环境 |
|------|----------|----------|
| 后端位置 | Windows 本地 | 服务器 |
| Piston URL | `http://8.148.15.228:2000` | `http://127.0.0.1:2000` |
| 数据库 | 本地 MySQL | 服务器 MySQL |
| 前端 | `npm run dev` | Nginx 静态文件 |
| 端口 | 5173/8080 | 80 |
