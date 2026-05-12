# 面面通（MianMianTong）— 系统架构文档

## 1. 项目概述

面面通是一款面向计算机专业学生的 **AI 模拟面试 + 智能刷题** 微信小程序，支持 AI 实时对话面试、在线试卷考试、自由刷题、错题本等功能。

| 属性 | 值 |
|------|-----|
| 项目名称 | 面面通 (MianMianTong) |
| 版本 | 1.0.0 |
| 前端框架 | uni-app (Vue 3 + TypeScript) |
| 后端框架 | Spring Boot 3.2.0 |
| 数据库 | MySQL 8.0 |
| AI 模型 | DeepSeek / Qwen（可切换） |

---

## 2. 技术栈

### 前端（AI-Interview）

| 技术 | 版本 | 用途 |
|------|------|------|
| uni-app | HBuilderX 3.99+ | 跨端小程序框架 |
| Vue 3 | Composition API | 响应式 UI 框架 |
| TypeScript | — | 类型安全 |
| Pinia | 2.3.1 | 状态管理 |
| uView Plus | 3.8.18 | UI 组件库 |
| SCSS | — | 样式预处理 |
| Jest | — | 单元测试框架 |

### 后端（mianmiantong-server）

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.0 | 应用框架 |
| Java | 17 | 运行环境 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| MySQL | 8.0 | 关系数据库 |
| Redis | — | 缓存（已集成待使用） |
| Flyway | — | 数据库迁移 |
| Spring Security | — | 认证授权 |
| JWT (jjwt) | 0.12.3 | 无状态令牌 |
| Knife4j | 4.3.0 | API 文档 |
| Lombok | — | 代码生成 |

---

## 3. 系统架构图

```
┌─────────────────────────────────────────────────────┐
│                  微信小程序客户端                      │
│  ┌───────┐ ┌───────┐ ┌──────┐ ┌──────┐ ┌─────────┐ │
│  │ 首页   │ │ 刷题   │ │错题本 │ │ 我的  │ │ AI面试  │ │
│  │ index  │ │practice│ │wrong │ │profile│ │interview│ │
│  └───┬───┘ └───┬───┘ └──┬───┘ └──┬───┘ └────┬────┘ │
│      │         │        │        │          │       │
│  ┌───┴─────────┴────────┴────────┴──────────┴────┐  │
│  │           utils/request.ts (HTTP + SSE)        │  │
│  │           Pinia store (user.ts)                │  │
│  └────────────────────┬──────────────────────────┘  │
└───────────────────────┼──────────────────────────────┘
                        │  HTTP REST + SSE
                        ▼
┌───────────────────────────────────────────────────────┐
│                  Spring Boot 服务层                     │
│  ┌──────────────────────────────────────────────────┐ │
│  │              Spring Security + JWT                │ │
│  └──────────────────────────────────────────────────┘ │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐ │
│  │AuthCtrl  │ │QuesCtrl  │ │AnsCtrl   │ │ExamCtrl │ │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬────┘ │
│  ┌────┴─────────────┴───────────┴───────────┴─────┐ │
│  │              Service 层                          │ │
│  │  AuthService / QuestionService / AnswerService  │ │
│  │  ExamService / InterviewService / WrongQuestion │ │
│  └────────────────────┬───────────────────────────┘ │
│  ┌────────────────────┴───────────────────────────┐ │
│  │           Mapper 层 (MyBatis-Plus)              │ │
│  └────────────────────┬───────────────────────────┘ │
└───────────────────────┼──────────────────────────────┘
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
    ┌──────────┐ ┌──────────┐ ┌──────────────┐
    │  MySQL   │ │  Redis   │ │ DeepSeek/Qwen │
    │  数据库   │ │  缓存    │ │   AI API     │
    └──────────┘ └──────────┘ └──────────────┘
```

---

## 4. 项目目录结构

### 前端（AI-Interview）

```
AI-Interview/
├── App.vue                    # 应用入口，onLaunch 鉴权
├── main.ts                    # Vue 初始化，挂载 Pinia + uView
├── pages.json                 # 路由、TabBar、分包配置
├── manifest.json              # 小程序 manifest 配置
├── uni.scss                   # 全局 SCSS 变量
├── pages/                     # 页面目录
│   ├── index/index.vue        # 首页（快捷入口 + 分类）
│   ├── practice/practice.vue  # 自由刷题（随机10题）
│   ├── wrong-book/wrong-book.vue  # 错题本
│   ├── profile/profile.vue    # 个人中心 + AI Key 配置
│   ├── login/login.vue        # 微信登录（分包）
│   ├── question/list.vue      # 题库列表（分包）
│   ├── exam/index.vue         # 试卷列表（分包）
│   ├── exam/do.vue            # 答题中（分包）
│   ├── interview/chat.vue     # AI 面试聊天（分包）
│   ├── interview/report.vue   # 面试报告（分包）
│   └── interview/history.vue  # 面试历史（分包）
├── components/                # 公共组件
├── store/user.ts              # Pinia 用户状态管理
├── utils/request.ts           # HTTP 请求封装 + SSE 流式请求
├── common/uni.css             # 全局 CSS
├── static/                    # 静态资源
└── unpackage/                 # 编译产物
```

### 后端（mianmiantong-server）

```
mianmiantong-server/
├── pom.xml
├── docker-compose.yml
└── src/main/
    ├── java/com/mianmiantong/
    │   ├── MianmiantongApplication.java
    │   ├── common/
    │   │   ├── Result.java           # 统一响应体
    │   │   └── JwtUtil.java          # JWT 工具类
    │   ├── config/
    │   │   ├── SecurityConfig.java   # Spring Security 配置
    │   │   ├── JwtAuthFilter.java    # JWT 认证过滤器
    │   │   └── GlobalExceptionHandler.java
    │   ├── controller/
    │   │   ├── AuthController.java   # 登录
    │   │   ├── QuestionController.java # 题库
    │   │   ├── AnswerController.java   # 答题
    │   │   ├── ExamController.java     # 考试
    │   │   ├── WrongQuestionController.java # 错题
    │   │   ├── InterviewController.java    # AI面试
    │   │   └── UserController.java         # 用户
    │   ├── service/
    │   │   ├── AuthService.java
    │   │   ├── QuestionService.java
    │   │   ├── AnswerService.java
    │   │   ├── ExamService.java
    │   │   ├── WrongQuestionService.java
    │   │   ├── InterviewService.java
    │   │   ├── UserAiConfigService.java
    │   │   └── ai/
    │   │       ├── AiService.java          # AI接口
    │   │       ├── DeepSeekAiService.java  # DeepSeek 实现
    │   │       └── QwenAiService.java      # 通义千问 实现
    │   ├── entity/              # 数据库实体
    │   ├── mapper/              # MyBatis-Plus Mapper
    │   └── dto/                 # 请求/响应 DTO
    └── resources/
        ├── application.yml      # 应用配置
        └── db/migration/        # Flyway 数据库迁移
            ├── V1__init.sql
            ├── V2__seed_questions.sql
            └── V3__user_ai_config.sql
```

---

## 5. 核心数据流

### 5.1 登录流程

```
小程序端                    后端
   │                        │
   │── wx.login() 获取code ─│
   │── POST /api/auth/login ─→ AuthController
   │                        │   └→ AuthService.login(code)
   │                        │       ├→ mockWechatLogin(code) → openid
   │                        │       ├→ 查/插 sys_user 表
   │                        │       └→ JwtUtil.generateToken() → JWT
   │←── { token, userId,    ─│
   │       nickname }         │
   │── setStorageSync(token) │
   │── onLaunch: 检查 token ─│
```

### 5.2 AI 面试流程

```
小程序端                             后端                         AI API
   │                                  │                            │
   │─ POST /api/interview/start ──→ InterviewController           │
   │  { position }                   └→ InterviewService.start()  │
   │                                    ├→ 构建 System Prompt      │
   │                                    ├→ AiService.chat() ──────→ DeepSeek/Qwen
   │                                    │←── 第一个问题 ──────────│
   │                                    └→ 创建 InterviewSession   │
   │←── { sessionId, question } ───│                              │
   │                                  │                            │
   │─ POST /{id}/answer/stream ──→   │                            │
   │  { answer }                      └→ answerStream()           │
   │                                    ├→ SseEmitter 建立长连接   │
   │                                    └→ CompletableFuture →     │
   │←── SSE: event:token "文" ───│       AiService.streamChat() ──→ 流式响应
   │←── SSE: event:token "字" ───│         ←── token by token ───│
   │←── SSE: event:finish ──────│       │
   │       { finished, report }         ├→ 解析 JSON 报告         │
   │                                    └→ 更新 InterviewSession   │
```

### 5.3 考试/刷题流程

```
小程序端                        后端
   │                              │
   │─ GET /api/exams ──────────→ ExamController
   │←── 试卷列表 ───────────────│
   │                              │
   │─ POST /api/exams/{id}/start → ExamService.startExam()
   │←── { exam, questions[] } ──│  (题目不含答案)
   │                              │
   │  [倒计时答题...]              │
   │                              │
   │─ POST /api/exams/{id}/submit → ExamService.submit()
   │  { answers[] }              │  ├→ 逐题判分 (judge)
   │                              │  ├→ 写入 answer_record
   │                              │  ├→ 错题自动入 wrong_question
   │←── { totalScore,            │  └→ 返回得分统计
   │      correctCount }         │
```

---

## 6. 认证与安全

- **认证方式**: JWT（无状态），前端存储于 `uni.storage`，每次请求带 `Authorization: Bearer <token>`
- **白名单路径**: `/api/auth/**`, `/doc.html`, `/swagger-ui/**`, `/v3/api-docs/**`
- **开发模式**: `dev-token-` 前缀 token 跳过 Authorization 头（仅开发用）
- **用户 API Key**: 支持用户在个人中心配置自己的 DeepSeek/Qwen API Key，优先使用用户 Key
- **CORS**: 当前允许所有来源（生产环境需收紧）

---

## 7. 部署架构

```
┌──────────────────────────────────────────┐
│              微信小程序客户端               │
└──────────────────┬───────────────────────┘
                   │ HTTPS
                   ▼
┌──────────────────────────────────────────┐
│           Nginx (反向代理)                 │
│  - SSL 终结                               │
│  - 静态资源缓存                            │
│  - /api/* → 后端                           │
└──────────────────┬───────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────┐
│       Spring Boot (docker-compose)        │
│  - 应用容器 :8080                          │
│  - MySQL 容器 :3306                        │
│  - Redis 容器 :6379                        │
└──────────────────────────────────────────┘
```

`docker-compose.yml` 已就绪，包含 MySQL 和 Redis 服务定义。

---

## 8. 已集成的待启用功能

| 功能 | 状态 | 说明 |
|------|------|------|
| Flyway 数据库迁移 | 已集成 / 未启用 | `flyway.enabled: false` |
| Redis 缓存 | 已集成 / 未使用 | 依赖已引入，无缓存代码 |
| WebSocket | 已集成 | `spring-boot-starter-websocket` 已引入 |
| Knife4j API 文档 | 已启用 | 访问 `/doc.html` |
| 微信真实登录 | Mock 阶段 | `AuthService.mockWechatLogin()` |
