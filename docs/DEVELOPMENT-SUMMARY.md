# 面面通 (Mianmian) 开发总结

> 最后更新: 2026-05-19

## 一、项目结构

```
IntervVault/
├── mianmiantong-server/              # Spring Boot 3.2 后端 (Java 17)
│   ├── controller/                   # 10 个 REST 控制器
│   │   ├── auth/AuthController.java  # 登录/注册
│   │   ├── interview/InterviewController.java  # AI 面试核心
│   │   ├── resume/ResumeController.java       # 简历优化
│   │   ├── user/UserController.java           # 用户统计/配额/AI配置
│   │   ├── admin/AdminController.java         # 管理后台
│   │   ├── CodingController.java              # Piston 代码执行
│   │   ├── QuestionController.java            # 题库
│   │   ├── ExamController.java                # 模拟考试
│   │   ├── AnswerController.java              # 单题判分
│   │   ├── WrongQuestionController.java       # 错题本
│   │   └── TemplateController.java            # 简历模板
│   ├── service/                      # 业务层
│   ├── dto/                          # 请求/响应 DTO
│   ├── entity/                       # 12 个 JPA 实体
│   ├── mapper/                       # 12 个 MyBatis-Plus Mapper
│   ├── config/                       # Security / JWT / CORS / 异常处理
│   └── common/                       # Result 信封 / JwtUtil
├── AI-Interview/                     # 前端
│   ├── pages/                        # 微信小程序页面 (uni-app)
│   ├── web-app/                      # Web 端 (Vue 3 + TypeScript + Vite)
│   │   └── src/
│   │       ├── views/                # 15 页面
│   │       ├── components/           # 17 组件
│   │       ├── composables/          # 3 composables
│   │       ├── stores/               # Pinia user store
│   │       ├── router/               # 路由 (含权限守卫)
│   │       ├── utils/                # request / sanitize / positionIcons
│   │       └── styles/               # tokens.css / global.css
│   ├── components/                   # 小程序公共组件
│   ├── manifest.json                 # uni-app 配置
│   └── pages.json                    # 小程序页面路由
└── docs/                             # 文档
    ├── DEVELOPMENT-SUMMARY.md        # 本文件
    ├── SERVER-SETUP.md               # 服务器配置指南
    ├── algorithm_problems_seed.sql   # 算法题库种子数据
    └── superpowers/specs/            # 设计规格文档
```

## 二、技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2 |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.0 |
| 认证 | Spring Security + JWT (jjwt) | 0.12.3 |
| 数据库迁移 | Flyway | V1~V9 |
| API 文档 | Knife4j (OpenAPI 3) | `/doc.html` |
| AI 提供商 | DeepSeek / Qwen (可配置切换) | — |
| 简历解析 | 阿里云 Document AI | — |
| 代码执行 | Piston (Docker) | 5 语言 |
| Web 前端 | Vue 3 + TypeScript + Vite | 3.5 / 6.0 / 8.0 |
| 状态管理 | Pinia | 3.0 |
| 路由 | Vue Router | 4.6 |
| 代码编辑器 | CodeMirror 6 | — |
| 动画 | GSAP + ScrollTrigger | — |
| 小程序 | uni-app (Vue 3) + uview-plus | mp-weixin |

## 三、API 端点全览

### 认证 `/api/auth`
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/login` | 微信 mock 登录 | 否 |
| POST | `/api/auth/login/password` | 用户名密码登录 | 否 |
| POST | `/api/auth/register` | 注册新账户 | 否 |

### AI 面试 `/api/interview`
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/interview/start` | 开始面试 (position/model/resumeId) | JWT |
| POST | `/api/interview/{id}/answer` | 提交文本回答 | JWT |
| POST | `/api/interview/{id}/answer/stream` | SSE 流式回答 (含 code/codeLang/codeFile) | JWT |
| POST | `/api/interview/{id}/end` | 结束面试并生成报告 | JWT |
| GET | `/api/interview/list` | 面试历史列表 | JWT |
| GET | `/api/interview/{id}` | 面试详情 (含报告) | JWT |

### 题库 `/api/questions`
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/questions` | 分页列表 (category/difficulty/keyword) | JWT |
| GET | `/api/questions/random` | 随机抽题 | JWT |
| GET | `/api/questions/{id}` | 题目详情 | JWT |
| GET | `/api/questions/categories` | 分类列表 | JWT |

### 考试 `/api/exams`
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/exams` | 考试列表 | JWT |
| POST | `/api/exams/{id}/start` | 开始考试 | JWT |
| POST | `/api/exams/{id}/submit` | 提交考试 | JWT |

### 答题 `/api/answers`
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/answers` | 提交答案 + AI 判分 | JWT |

### 错题本 `/api/wrong-questions`
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/wrong-questions` | 错题列表 | JWT |
| DELETE | `/api/wrong-questions/{id}` | 移除错题 | JWT |

### 简历 `/api/resume`
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/resume/upload` | 上传简历 (multipart + jobDescription + model) | JWT |
| GET | `/api/resume/{id}/status` | 解析状态 | JWT |
| POST | `/api/resume/{id}/analyze` | 触发 AI 分析 | JWT |
| POST | `/api/resume/{id}/analyze-deep` | SSE 深度优化 | JWT |
| POST | `/api/resume/{id}/retry-deep` | SSE 重试深度优化 | JWT |
| GET | `/api/resume/{id}/retry-deep` | 查询重试状态 | JWT |
| GET | `/api/resume/{id}/deep-status` | 深度优化状态 | JWT |
| GET | `/api/resume/{id}/analysis` | 分析报告 | JWT |
| GET | `/api/resume/list` | 简历历史 | JWT |
| GET | `/api/resume/{id}/export-word` | Word 导出 | JWT |
| GET | `/api/resume/{id}/preview-html` | HTML 预览 (?token=) | JWT |
| POST | `/api/resume/{id}/retry-parse` | 重试解析 | JWT |
| DELETE | `/api/resume/{id}` | 删除简历 | JWT |

### 简历模板 `/api/resume/template`
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/resume/template/list` | 模板列表 | JWT |
| GET | `/api/resume/template/generate` | 生成模板 | JWT |

### 代码执行 `/api/coding`
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/coding/run` | Piston 代码执行 (5 语言) | JWT |

### 用户 `/api/user`
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/user/stats` | 统计数据 (刷题数/面试数/错题数) | JWT |
| GET | `/api/user/ai-config` | 获取 AI 配置 | JWT |
| PUT | `/api/user/ai-config` | 保存 AI 配置 (provider/apiKey/model) | JWT |
| GET | `/api/user/quota` | 配额信息 (hasApiKey/isAdmin/quotaRemaining) | JWT |

### 管理后台 `/api/admin` (需 role=1)
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/admin/status` | 系统状态 (总用户/总面试/Key配置) | JWT+Admin |
| GET | `/api/admin/users` | 用户列表 (含统计) | JWT+Admin |
| GET | `/api/admin/sessions` | 最近面试记录 | JWT+Admin |
| POST | `/api/admin/set-quota` | 设置用户剩余配额 | JWT+Admin |
| POST | `/api/admin/set-limit` | 设置用户每日配额上限 | JWT+Admin |
| POST | `/api/admin/toggle-admin` | 切换管理员角色 | JWT+Admin |
| POST | `/api/admin/delete-user` | 删除用户 | JWT+Admin |
| POST | `/api/admin/clear-sessions` | 清空面试记录 | JWT+Admin |
| POST | `/api/admin/clear-all` | 清空全部数据 | JWT+Admin |

### 认证方式

- Web 端：`Authorization: Bearer <token>` header
- 小程序：`?token=<token>` URL 查询参数（`downloadFile` 不支持自定义 header）
- JwtAuthFilter 同时支持两种方式
- JWT payload 包含：`sub`(userId)、`role`(0/1)、`openid`、`exp`

### 响应格式

统一信封：`{ code: number, message: string, data: T }`

## 四、功能完成度

| 模块 | Web | 小程序 | 说明 |
|------|-----|--------|------|
| 用户认证 | ✅ | ✅ | 密码登录/注册 + 微信 mock |
| AI 模拟面试 | ✅ | ✅ | SSE 流式 + 5 阶段进度 + 模型切换 |
| 笔试编程 | ✅ | — | CodeMirror 6 + Piston 执行 + AI 审查 |
| 题库浏览 | ✅ | ✅ | 8 分类 + 分页 + 随机抽题 |
| 自由刷题 | ✅ | ✅ | 随机组卷 + 即时判分 |
| 错题本 | ✅ | ✅ | 记录 + 统计 + 移除 |
| 简历优化 | ✅ | ✅ | 上传→解析→评分→深度优化→导出 |
| API Key 配置 | ✅ | — | DeepSeek/千问 双选项卡 |
| 管理后台 | ✅ | — | 用户管理 + 配额设置 + 数据清理 |
| 配额系统 | ✅ | — | 日配额 + API Key 无限 + 管理员无限 |
| 权限控制 | ✅ | — | 三层防护 (菜单/路由/API) |

## 五、配额与权限架构 (2026-05-19 重构)

### 配额模型

```
用户类型:
├── 管理员 (role=1)        → 无限配额
├── 已配置 API Key         → 无限配额
└── 普通用户               → 日配额 (默认 10 次/天)
    ├── Flash 模型         → 消耗 1 次
    └── Pro 模型           → 消耗 2 次
```

### 前端状态流

```
登录 → LoginView.loginSuccess()
  → JWT payload 解码 role → userStore.setAdmin()
  → localStorage('isAdmin')

各功能页面:
  → useQuota.fetchQuota() → /api/user/quota
    → 缓存 30s
    → 自动同步 userStore.setAdmin()
  → useQuota.checkQuota(needed, msg?)
    → 检查 isAdmin / hasApiKey / quotaRemaining
```

### 管理员访问防护 (纵深防御)

```
第1层: ProfileView 菜单   v-if="quota?.isAdmin"      → 隐藏入口
第2层: Router 守卫        requiresAdmin meta          → 重定向 /profile
第3层: AdminView mount    !userStore.isAdmin          → router.replace
第4层: 后端               requireAdmin()              → 403
```

### 涉及文件

| 文件 | 职责 |
|------|------|
| `stores/user.ts` | isAdmin 存储 + localStorage 持久化 |
| `router/index.ts` | requiresAdmin 路由元信息 + beforeEach 守卫 |
| `composables/useQuota.ts` | 配额缓存 + 检查 + isAdmin 同步 |
| `views/LoginView.vue` | JWT payload 解码 → setAdmin() |
| `views/ProfileView.vue` | 配额展示 + setAdmin() 同步 |
| `views/AdminView.vue` | mount 时 isAdmin 二次校验 |
| `views/InterviewView.vue` | 面试前配额检查 |
| `views/ResumeUploadView.vue` | 上传前配额检查 |
| `views/ResumeReportView.vue` | 分析/深度优化/重试 配额检查 |
| `utils/request.ts` | 401 → clearUser + 跳转登录页 |

## 六、笔试编程系统

### 架构

```
InterviewView.vue (PC 三栏布局)
├── Sidebar (面试进度)
├── Chat (对话流)
└── CodePanel (右侧, 可拖拽)
    ├── CodeEditor (CodeMirror 6, 5 语言下拉)
    ├── [Run Code] → POST /api/coding/run → Piston → 结果面板
    └── [提交审查] → SSE → AI 评审 → 笔试报告
```

### SSE 标记体系

| 标记 | 方向 | AI 调用 | 说明 |
|------|------|---------|------|
| `[笔试邀请]` | AI→前端 | 面试 AI | AI 邀请候选人编程 |
| `[进入编程环节]` | 前端→后端 | — | 用户接受，触发题库出题 |
| `[编程题目]` | 后端→前端 | 题库随机选题 | 含 templates/testCases |
| `[笔试结束]` | 后端→前端 | 独立代码审查 AI | 5 维度评分 JSON |
| `[面试结束]` | AI→前端 | 面试 AI 或 end() | 面试报告 JSON |

### 题库表 algorithm_problem

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| title | VARCHAR(100) | 题目标题 |
| description | TEXT | 题目描述（含概念解释） |
| difficulty | VARCHAR(10) | easy/medium/hard |
| starter_code | JSON | `{"java":"...","python":"..."}` |
| test_cases | JSON | `[{"input":"...","expected":"..."}]` |
| solution_code | JSON | 参考答案（仅后端） |
| category | VARCHAR(50) | array/string/linkedlist/tree/dp/math/stack/hash |

### 10 道种子题

| # | 题目 | 分类 | 难度 |
|---|------|------|------|
| 1 | 两数之和 | Array | Easy |
| 2 | 两数相加 | Linked List | Medium |
| 3 | 无重复字符的最长子串 | String | Medium |
| 4 | 最长回文子串 | String | Medium |
| 5 | Z 字形变换 | String | Medium |
| 6 | 整数反转 | Math | Easy |
| 7 | 字符串转换整数 atoi | String | Medium |
| 8 | 回文数 | Math | Easy |
| 9 | 正则表达式匹配 | DP | Hard |
| 10 | 盛最多水的容器 | Array | Medium |

### 面试与笔试分离机制

```
用户选择 [进入编程] 或 [跳过编程]
  → 面试立即结束 → end() API 生成面试报告 (async)
  → 进入编程: 题库出题 → 写代码 → 提交审查
    → 独立 AI 调用 (仅评审代码)
    → [笔试结束] JSON → 笔试报告
  → 自动跳转报告页 (面试 + 笔试 双 Tab)
```

- 面试报告基于 Q&A 对话，**不含代码审查内容**
- 笔试报告独立评分，来自代码审查 AI
- `sessionStorage` 关键 Key：`interviewScore/Feedback/Dims/Suggestion` + `interviewCodingReview`

## 七、前端架构

### 页面路由 (15 个)

| 路由 | 页面 | 认证 | 管理员 |
|------|------|------|--------|
| `/` | HomeView | 否 | — |
| `/login` | LoginView | 否 | — |
| `/interview` | InterviewView | JWT | — |
| `/interview/report` | InterviewReportView | JWT | — |
| `/interview/history` | InterviewHistoryView | JWT | — |
| `/resume/upload` | ResumeUploadView | JWT | — |
| `/resume/report` | ResumeReportView | JWT | — |
| `/resume/history` | ResumeHistoryView | JWT | — |
| `/questions` | QuestionsView | JWT | — |
| `/questions/:id` | QuestionDetailView | JWT | — |
| `/practice` | PracticeView | JWT | — |
| `/practice/do` | PracticeDoView | JWT | — |
| `/wrong-book` | WrongBookView | JWT | — |
| `/profile` | ProfileView | JWT | — |
| `/admin` | AdminView | JWT | ✅ |

### Composable 层

| Composable | 用途 |
|------------|------|
| `useInterviewStream.ts` | SSE 流解析：标记检测 ([面试结束]/[编程题目]/[笔试邀请]/[笔试结束])、渐进渲染、ReportData/CodingReview 构建 |
| `useQuota.ts` | 配额缓存 (30s TTL)、`fetchQuota()` + `checkQuota(needed, msg?)`、自动同步 isAdmin |
| `useScrollReveal.ts` | GSAP ScrollTrigger 滚动揭示动画 |

### Store 层

| Store | 字段 |
|-------|------|
| `user` | userId, nickname, avatarUrl, token, isAdmin, isLoggedIn |

### Utils 层

| Util | 用途 |
|------|------|
| `request.ts` | fetch 封装 (get/post/put/del/postForm)、Bearer token 注入、401 → clearUser + 跳转登录 |
| `sanitize.ts` | `extractBalancedJson`、`fixJsonString`、`sanitizeEndMarker`、`scoreToVerdict` |
| `positionIcons.ts` | 面试岗位 SVG 图标 |

## 八、服务器

| 项目 | 值 |
|------|-----|
| 类型 | 阿里云 ECS 2核2G 40GB |
| OS | Alibaba Cloud Linux 3 |
| IP | 8.148.15.228 |
| Piston | Docker, 端口 2000 |
| 面板 | 宝塔 |
| Java | Tomcat 10.1.18 (修复 SSE 并发 Bug) |

详见 [SERVER-SETUP.md](SERVER-SETUP.md)

## 九、开发环境

```bash
# 前端
cd AI-Interview/web-app
npm install
npm run dev          # http://localhost:5173 (Vite proxy → :8080)

# 后端
cd mianmiantong-server
# 需要 MySQL 运行中，数据库 ai-interview
# 环境变量: DB_USERNAME, DB_PASSWORD, JWT_SECRET, DEEPSEEK_API_KEY 等
mvn spring-boot:run  # http://localhost:8080

# 数据库种子数据
mysql -u root ai-interview < docs/algorithm_problems_seed.sql
```

## 十、小程序兼容性约束

Web 前端改动需遵循以下规则，确保不影响小程序：

1. **API 响应格式** — 不改动 `{ code, message, data }` 信封结构
2. **JWT 认证** — 不修改 `?token=` 查询参数支持（小程序 downloadFile 依赖）
3. **API 路径/参数** — 不删除或重命名已有字段，只追加
4. **小程序文件** — 不改动 `AI-Interview/pages/`、`AI-Interview/components/` (uni-app)
5. **后端接口** — 新增字段用 `@JsonInclude(NON_NULL)` 或确保向后兼容

## 十一、后续迭代计划

### P1 — 下一周期

- [ ] 题库 test_cases 校验：Run Code 自动构造 stdin 并比对 expected
- [ ] 代码模板与题库联动：切换语言从 DB 拉取完整模板
- [ ] 小程序端笔试适配（只读代码展示 + AI 审查入口）
- [ ] 题库扩充 (10→30 题)
- [ ] 面试历史分页加载 (当前一次性加载全部)

### P2 — 中期

- [ ] 前端部署到服务器 (nginx 静态文件)
- [ ] 语音识别集成 (面试回答语音输入)
- [ ] 微信 OAuth 真实登录 (替代 dev-token mock)
- [ ] 小程序真机测试与审核上线
- [ ] 面试回放功能 (消息历史完整回放)
- [ ] 简历模板自定义 (用户可编辑模板样式)
- [ ] 数据看板 (个人学习统计 + 趋势图表)

### P3 — 长期

- [ ] 国际化 i18n (中/英)
- [ ] 多轮对话记忆优化 (上下文压缩/摘要)
- [ ] WebSocket 替代 SSE (双向实时通信)
- [ ] 代码执行沙箱升级 (gVisor/firecracker 替代 Piston)
- [ ] 社区题库 (用户投稿 + 审核机制)
- [ ] 面试房间 (多人协同面试模拟)
- [ ] 移动端 PWA 支持
- [ ] 服务器端 `ORDER BY RAND()` → `COUNT + OFFSET` 重构
- [ ] 线程池重构 (`CompletableFuture` → 专用 `ExecutorService`)

## 十二、架构关键决策

### ADR-001: 面试与笔试分离
用户选择进入/跳过编程时，面试立即结束。笔试完全独立，使用独立的 AI 调用评审代码。面试报告不含代码审查内容。

### ADR-002: 配额前端三层防护
菜单可见性 → 路由守卫 → 组件 mount 检查 → 后端 API 鉴权。每层独立，纵深防御。

### ADR-003: isAdmin 双路同步
JWT payload (登录时) + `/api/user/quota` (运行时) 两路设置 `userStore.isAdmin`，确保状态一致。

### ADR-004: SSE 标记体系
AI 输出中使用结构化标记 (`[面试结束]`/`[编程题目]`/`[笔试结束]` + JSON payload)，前端 `tryParseMarker<T>` 泛型工厂统一解析。

### ADR-005: Web/小程序共享 API
Web 前端和小程序共享同一套后端 API。Web 端改动只追加字段，不删改已有结构。认证兼容 `Bearer` header 和 `?token=` query param。
