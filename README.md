# 面面通 (MianMianTong) — AI 面试助手

基于 Spring Boot + 微信小程序的计算机专业 AI 面试准备平台，集成了 AI 模拟面试、智能刷题、简历优化等功能。

## 功能概览

| 模块 | 功能 | 状态 |
|------|------|------|
| **AI 模拟面试** | 多轮对话、流式回复、自动评分 | 可用 |
| **智能刷题** | 8 大类题库、随机组卷、错题本 | 可用 |
| **简历优化** | 上传解析 → AI 评分 → 深度优化 → 模板导出 | 可用 |
| **在线考试** | 试卷作答、自动判分、成绩统计 | 可用 |

## 技术栈

| 层 | 技术 |
|---|------|
| 后端 | Spring Boot 3.2、MyBatis-Plus、MySQL、Redis |
| 前端 | uni-app (Vue 3 + TypeScript)、微信小程序 |
| AI | DeepSeek API（可切换通义千问） |
| 文档解析 | 阿里云文档智能（大模型版） |
| 文档生成 | Apache POI（Word .docx 导出） |

## 项目结构

```
Uniapp-Projects/
├── mianmiantong-server/          # Spring Boot 后端
│   ├── src/main/java/com/mianmiantong/
│   │   ├── controller/           # REST 控制器
│   │   ├── service/              # 业务逻辑层
│   │   │   ├── ai/               # AI 服务（DeepSeek/Qwen）
│   │   │   ├── document/         # 文档解析/导出/预览
│   │   │   ├── interview/        # 面试逻辑
│   │   │   ├── resume/           # 简历优化
│   │   │   └── user/             # 用户配置
│   │   ├── entity/               # 数据库实体
│   │   ├── mapper/               # MyBatis-Plus 映射
│   │   └── config/               # 配置类
│   └── src/main/resources/
│       ├── application.yml       # 主配置
│       ├── db/migration/         # Flyway 数据库迁移
│       └── META-INF/             # SPI 注册
├── AI-Interview/                 # uni-app 小程序前端
│   ├── pages/
│   │   ├── index/                # 首页
│   │   ├── interview/            # AI 面试
│   │   ├── practice/             # 刷题
│   │   ├── exam/                 # 考试
│   │   ├── resume/               # 简历优化
│   │   └── profile/              # 个人中心
│   ├── components/               # 公共组件
│   └── utils/                    # 工具函数
└── docs/                         # 文档
    ├── screenshots/              # 截图
    └── TODO-优化计划.md           # 待优化清单
```

## 快速开始

### 环境要求

- JDK 17+
- MySQL 8.0
- Redis 7
- Node.js 18+ (用于 uni-app 编译)
- 微信开发者工具

### 1. 启动基础设施

```bash
cd mianmiantong-server
docker-compose up -d  # MySQL + Redis
```

### 2. 配置环境变量

复制 `.env.example` 为 `.env`，填入真实值：

```env
DB_USERNAME=root
DB_PASSWORD=your-password
JWT_SECRET=your-jwt-secret
DEEPSEEK_API_KEY=sk-your-deepseek-key
ALIBABA_CLOUD_ACCESS_KEY_ID=your-aliyun-ak
ALIBABA_CLOUD_ACCESS_KEY_SECRET=your-aliyun-sk
WECHAT_APP_ID=your-wechat-app-id
WECHAT_APP_SECRET=your-wechat-app-secret
```

### 3. 启动后端

```bash
cd mianmiantong-server
mvn clean package -DskipTests
java -jar target/mianmiantong-server-1.0.0.jar
```

### 4. 启动小程序

在 HBuilderX 中打开 `AI-Interview` 目录，运行 → 微信开发者工具。

## API 文档

启动后端后访问 http://localhost:8080/doc.html 查看 Knife4j 接口文档。

### 简历优化 API

![简历 API](docs/screenshots/api-resume.png)

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/resume/upload` | POST | 上传简历文件 |
| `/api/resume/{id}/status` | GET | 轮询文档解析状态 |
| `/api/resume/{id}/analyze` | POST | 触发 AI 快速评分（异步） |
| `/api/resume/{id}/analyze-deep` | POST | 触发深度优化（后台） |
| `/api/resume/{id}/deep-status` | GET | 查询深度优化状态 |
| `/api/resume/{id}/analysis` | GET | 获取分析报告 |
| `/api/resume/{id}/export-word` | GET | 导出 Word 文档 |
| `/api/resume/{id}/preview-html` | GET | HTML 预览 |
| `/api/resume/list` | GET | 简历历史 |
| `/api/resume/template/list` | GET | 模板列表 |
| `/api/resume/template/generate` | GET | 使用模板生成 |

### 模板 API

![模板 API](docs/screenshots/api-template.png)

## 简历优化流程

```
┌─────────────┐     ┌──────────────┐     ┌──────────────┐
│  上传简历    │ ──→ │  阿里云解析   │ ──→ │  AI 快速评分  │
│  (秒级返回)  │     │  (后台异步)   │     │  (~15秒)      │
└─────────────┘     └──────────────┘     └──────┬───────┘
                                                │
                    ┌───────────────────────────┘
                    ▼
┌─────────────────┐     ┌──────────────────────┐
│  查看评分报告    │ ──→ │  深度优化 (后台)       │
│  (五维评分+建议) │     │  逐段对比+优化+追问    │
└─────────────────┘     └──────────┬───────────┘
                                   │
                    ┌──────────────┘
                    ▼
┌─────────────────────────────────────────────┐
│  查看完整报告                                │
│  • Git 风格逐段 diff 对比                     │
│  • 优化后完整简历                            │
│  • 面试追问                                 │
│  • 导出 Word (保留原格式) / 换模板 / 预览      │
└─────────────────────────────────────────────┘
```

## 小程序截图

### 首页 & 刷题
> *在微信开发者工具中运行可查看实际效果*

### 简历优化 - 上传页
> *支持 PDF / Word / 图片上传，JD 目标岗位输入*

### 简历优化 - 评分报告
> *五维评分雷达、缺失关键词、总体建议*

### 简历优化 - 深度优化
> *逐段 Git 风格 diff 对比、优化后简历、模板选择器、Word 导出*

### AI 模拟面试
> *流式对话、自动追问、面试评分报告*

## 待优化

详见 [docs/TODO-优化计划.md](docs/TODO-优化计划.md)

## 许可

本软件仅供**学习研究**使用，**禁止商用**。详见 [LICENSE](LICENSE)。

## 作者

chidaobuchidao
