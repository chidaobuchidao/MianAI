# 面面通 (MianMianTong) — AI 面试助手

基于 Spring Boot + Vue 3 + 微信小程序的计算机专业 AI 面试准备平台，集成 AI 模拟面试、智能题库、简历优化等功能。

## 功能概览

| 模块 | 功能 | 状态 |
|------|------|------|
| **AI 模拟面试** | 多轮对话、流式回复、自动评分、Flash/Pro 模型切换 | 可用 |
| **查看题库** | 8 大类分类浏览、逐题精学、上/下题切换 | 可用 |
| **自由刷题** | 随机组卷 + 按专题刷题、即时判分 | 可用 |
| **错题本** | 错题记录、错误次数统计 | 可用 |
| **简历优化** | 上传解析 → AI 评分 → 深度优化 → Word 导出 | 可用 |

## 技术栈

| 层 | 技术 |
|---|------|
| 后端 | Spring Boot 3.2、MyBatis-Plus、MySQL |
| Web 前端 | Vue 3 + Vite + TypeScript + Vue Router + Pinia |
| 小程序 | uni-app (Vue 3 + TypeScript)、微信小程序 |
| AI | DeepSeek API（可切换通义千问）|
| 文档解析 | 阿里云文档智能 |

## 项目结构

```
MainAI/
├── mianmiantong-server/              # Spring Boot 后端
│   └── src/main/java/com/mianmiantong/
│       ├── controller/               # REST 控制器
│       ├── service/                  # 业务逻辑层
│       ├── entity/                   # 数据库实体
│       ├── mapper/                   # MyBatis-Plus 映射
│       └── config/                   # 配置类（安全/过滤器）
├── AI-Interview/                     # uni-app 小程序
│   ├── pages/
│   │   ├── index/                    # 首页
│   │   ├── interview/                # AI 面试
│   │   ├── question-bank/            # 查看题库
│   │   ├── practice-entry/           # 自由刷题
│   │   ├── wrong-book/               # 错题本
│   │   ├── resume/                   # 简历优化
│   │   └── profile/                  # 个人中心
│   ├── web-app/                      # Vue 3 Web 端
│   │   └── src/
│   │       ├── views/                # 11 个页面
│   │       ├── components/           # 15 个组件
│   │       ├── composables/          # 组合式函数
│   │       ├── utils/                # 工具函数
│   │       └── router/               # 路由配置
│   └── components/                   # 小程序公共组件
└── docs/                             # 文档
    ├── DEVELOPMENT-SUMMARY.md        # 开发总结
    └── superpowers/specs/            # 设计稿
        └── 2026-05-16-mianmian-design.html  # 全页面设计稿
```

## 界面展示

| 首页 | 查看题库 |
|------|----------|
| ![首页](docs/images/homepage.png) | ![查看题库](docs/images/questions.png) |

| 自由刷题 | AI 面试入口 |
|----------|-------------|
| ![自由刷题](docs/images/practice.png) | ![AI面试入口](docs/images/interview.png) |

| 面试聊天（PC 端） | 面试聊天（移动端） |
|-------------------|---------------------|
| ![面试聊天](docs/images/interview-design.png) | ![面试聊天移动端](docs/images/interview-design-mobiephone.png) |

| 简历上传 | 简历评分报告 |
|----------|--------------|
| ![简历上传](docs/images/resume.png) | ![简历报告](docs/images/resumereport.png) |

| 简历深度优化 |
|--------------|
| ![简历深度优化](docs/images/resumedeep.png) |

> 完整 12 页面设计稿（Warm Tech 设计系统）：在浏览器中打开 `docs/superpowers/specs/2026-05-16-mianmian-design.html`

## 快速开始

### 环境要求

- JDK 17+
- MySQL 8.0
- Node.js 18+

### 1. 配置环境变量

复制 `.env.example` 为 `.env`，填入真实值：

```env
DB_USERNAME=root
DB_PASSWORD=your-password
JWT_SECRET=your-jwt-secret
DEEPSEEK_API_KEY=sk-your-deepseek-key
ALIBABA_CLOUD_ACCESS_KEY_ID=your-aliyun-ak
ALIBABA_CLOUD_ACCESS_KEY_SECRET=your-aliyun-sk
```

### 2. 启动后端

```bash
cd mianmiantong-server
mvn spring-boot:run
```

### 3. 启动 Web 前端

```bash
cd AI-Interview/web-app
npm run dev
# 访问 http://localhost:5173
```

### 4. 启动小程序

在 HBuilderX 中打开 `AI-Interview` 目录，运行 → 微信开发者工具。

## API 文档

启动后端后访问 http://localhost:8080/doc.html 查看 Knife4j 接口文档。

### 核心 API

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/auth/login` | POST | 登录 |
| `/api/interview/start` | POST | 开始面试 |
| `/api/interview/list` | GET | 面试历史 |
| `/api/interview/{id}` | GET | 面试详情/报告 |
| `/api/questions` | GET | 题库列表（分页/分类/难度筛选）|
| `/api/questions/{id}` | GET | 题目详情 |
| `/api/questions/random` | GET | 随机抽题 |
| `/api/questions/categories` | GET | 分类列表 |
| `/api/answers` | POST | 提交答案（判分）|
| `/api/resume/upload` | POST | 上传简历 |
| `/api/resume/{id}/analysis` | GET | 获取分析报告 |
| `/api/resume/{id}/analyze` | POST | 触发快速评分 |
| `/api/resume/{id}/analyze-deep` | POST | 深度优化（SSE 流式）|
| `/api/resume/{id}/export-word` | GET | 导出 Word |
| `/api/resume/list` | GET | 简历历史 |
| `/api/user/stats` | GET | 用户统计 |
| `/api/user/ai-config` | GET/PUT | AI 配置 |
| `/api/wrong-questions` | GET | 错题列表 |

## 设计系统

Warm Tech 设计令牌：

| 令牌 | 值 |
|------|-----|
| 背景 | `#F3EFE8` (canvas) / `#FDFCFB` (paper) |
| 强调色 | `#D9750A` (琥珀橙) |
| 暗色 | `#141413` |
| 成功/危险 | `#22C55E` / `#EF4444` |
| 字体 | Inter (UI) / Georgia (标题) / JetBrains Mono (代码) |
| 圆角 | 16px (lg) / 20px (xl) / 100px (full) |

## 许可

本软件仅供**学习研究**使用，**禁止商用**。详见 [LICENSE](LICENSE)。

## 作者

chidaobuchidao
