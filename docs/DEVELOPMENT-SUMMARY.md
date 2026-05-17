# Mianmian (面面通) 开发总结

> 最后更新: 2026-05-17

## 一、项目结构

```
IntervVault/
├── mianmiantong-server/          # Spring Boot 3.2 后端
│   ├── controller/               # 8个 REST 控制器
│   ├── service/                  # 业务层 (ai/coding/document/interview/resume/user)
│   ├── dto/                      # 数据传输对象
│   ├── entity/                   # 数据库实体
│   └── mapper/                   # MyBatis-Plus 映射
├── AI-Interview/                 # uni-app 小程序 + Vue 3 Web 端
│   ├── pages/                    # 小程序页面
│   ├── web-app/                  # Web 端 (Vue 3 + Vite)
│   │   └── src/
│   │       ├── views/            # 11 页面
│   │       ├── components/       # 16 组件 (含 CodeEditor)
│   │       ├── composables/      # 组合式函数
│   │       └── utils/            # 工具函数
│   └── components/               # 小程序公共组件
└── docs/                         # 文档
    ├── DEVELOPMENT-SUMMARY.md    # 本文件
    ├── SERVER-SETUP.md           # 服务器配置指南
    └── superpowers/specs/        # 设计稿与规格文档
```

## 二、功能完成度

| 模块 | 状态 | 说明 |
|------|------|------|
| AI 模拟面试（Web） | ✅ 可用 | SSE 流式对话, 5 阶段进度, 模型切换, 代码编辑器 |
| AI 模拟面试（小程序） | ✅ 可用 | 流式对话 |
| 查看题库 | ✅ 可用 | 分类浏览, 逐题精学, 上/下切换 |
| 自由刷题 | ✅ 可用 | 随机组卷, 按专题刷题, 即时判分 |
| 错题本 | ✅ 可用 | 错题记录与统计 |
| 简历优化 | ✅ 可用 | 上传→解析→评分→深度优化→导出 |
| API Key 配置 | ✅ 可用 | DeepSeek/千问选项卡 |
| Piston 代码执行 | ✅ 可用 | Python/Java/JS/C++/Go, 服务器部署 |
| 笔试编程环节 | ⏳ 进行中 | 出题+编辑器已完成, Run Code 待接入前端 |
| 微信 OAuth 登录 | ⏳ 待做 | 目前 dev-token 模式 |

## 三、API 端点

### 新增
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/coding/run` | Piston 代码执行 |

### 已有
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 |
| POST | `/api/interview/start` | 开始面试 |
| POST | `/api/interview/{id}/answer/stream` | SSE 流式回答 |
| GET | `/api/interview/list` | 面试历史 |
| GET | `/api/questions` | 题库列表(分页/分类) |
| GET | `/api/questions/{id}` | 题目详情 |
| GET | `/api/questions/random` | 随机抽题 |
| POST | `/api/answers` | 提交答案判分 |
| POST | `/api/resume/upload` | 上传简历 |
| GET | `/api/resume/{id}/analysis` | 分析报告 |
| POST | `/api/resume/{id}/analyze-deep` | SSE 深度优化 |
| GET | `/api/wrong-questions` | 错题列表 |
| GET/PUT | `/api/user/ai-config` | AI 配置 |

## 四、服务器

| 项目 | 值 |
|------|-----|
| 类型 | 阿里云 ECS 2核2G 40GB |
| OS | Alibaba Cloud Linux 3 |
| IP | 8.148.15.228 |
| Piston | Docker, 端口 2000, 5 语言 |
| 面板 | 宝塔 |

详见 [SERVER-SETUP.md](SERVER-SETUP.md)

## 五、后续开发方向

### 笔试编程系统

| 优先级 | 任务 |
|--------|------|
| P0 | 前端 [Run Code] 按钮 + 结果面板 |
| P0 | 算法题库表 + 种子数据 (10-20 题) |
| P1 | AI 从题库选题（替代自由生成） |
| P1 | 提交代码给 AI 审查（SSE 流） |
| P2 | 代码编辑器拖拽调节宽度 |

### 部署

| 优先级 | 任务 |
|--------|------|
| P0 | 后端 + 前端部署到服务器 (nginx + systemd) |
| P1 | 小程序真机测试与审核 |

### 增强

| 优先级 | 任务 |
|--------|------|
| P2 | 语音识别集成 |
| P2 | 微信 OAuth 真实登录 |
