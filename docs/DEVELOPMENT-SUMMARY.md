# Mianmian (面面通) 开发总结文档

> 最后更新: 2026-05-16 | 分支: main | 提交: 18faac8 / 5c3c2b6

---

## 一、项目结构

```
E:\My_Projects\IntervVault\
├── AI-Interview/                  # 小程序 (uni-app + Vue3)
│   ├── pages/                     # 业务页面
│   │   ├── index/                 # 首页
│   │   ├── interview/             # 面试对话(chat) + 历史(history) + 报告(report)
│   │   ├── resume/                # 简历上传(upload) + 历史(history) + 报告(report)
│   │   ├── exam/                  # 试卷列表(index) + 答题(do)
│   │   ├── practice/              # 自由刷题
│   │   ├── wrong-book/            # 错题本
│   │   ├── profile/               # 个人中心
│   │   ├── login/                 # 登录
│   │   └── question/              # 题目列表(list)
│   ├── components/                # 公共组件
│   ├── styles/                    # tokens.css, animations.css, tokens.scss
│   ├── utils/request.ts           # API 请求工具
│   ├── uni.scss                   # uView Plus + uni-app 主题变量
│   └── pages.json                 # 路由配置
│
├── AI-Interview/web-app/          # Web 端 (Vue3 + Vite + TypeScript)
│   ├── src/
│   │   ├── components/            # 14个组件 (见下文)
│   │   ├── views/                 # 11个页面 (见下文)
│   │   ├── composables/           # useScrollReveal.ts
│   │   ├── router/index.ts        # Vue Router (含 beforeEach 鉴权守卫)
│   │   ├── stores/user.ts         # Pinia 用户状态
│   │   ├── utils/request.ts       # API 客户端 (get/post/postForm/put/del)
│   │   └── styles/                # tokens.css, animations.css, global.css
│   ├── vite.config.ts             # 代理 /api → localhost:8080
│   └── package.json               # vue3, pinia, vue-router, marked, gsap, three, ogl
│
├── mianmiantong-server/           # 后端 (Spring Boot 3.2 + MyBatis Plus + MySQL)
│   └── src/main/java/com/mianmiantong/
│       ├── config/                # SecurityConfig, JwtAuthFilter
│       ├── controller/            # 7个Controller (见下文)
│       ├── service/               # 业务服务层
│       ├── dto/                   # 数据传输对象
│       ├── entity/                # 数据库实体
│       └── mapper/                # MyBatis Mapper
│
└── docs/                          # 文档
    ├── DEVELOPMENT-SUMMARY.md     # 本文件
    └── superpowers/specs/         # 设计稿 HTML
        ├── 2026-05-15-mianmian-full-design.html
        └── 2026-05-16-mianmian-design.html
```

---

## 二、Web 端组件清单 (14个)

| 组件 | 文件 | 功能 |
|------|------|------|
| `GlareCard` | components/GlareCard.vue | 鼠标追踪眩光悬浮卡片，对角扫光+径向发光+3D倾斜 |
| `ParticleBg` | components/ParticleBg.vue | WebGL 粒子背景 (OGL库)，鼠标跟随+3D旋转 |
| `GridScan` | components/GridScan.vue | Three.js Shader 3D网格扫描动画，用于评分背景 |
| `PixelCard` | components/PixelCard.vue | 圆点矩阵卡片，悬停鼠标扩散效果 |
| `Folder` | components/Folder.vue | 拟真文件夹开合动画，纸片展开+磁吸 |
| `CodeBlock` | components/CodeBlock.vue | macOS风格代码块 (三色圆点窗口装饰) |
| `VoicePanel` | components/VoicePanel.vue | 语音录音面板，波形跳动动画 |
| `ScoreBadge` | components/ScoreBadge.vue | 分数徽章 (绿/黄/橙/红色自适应) |
| `DiagnosticCard` | components/DiagnosticCard.vue | 诊断建议卡片，红/黄/蓝指示器 |
| `SkeletonBar` | components/SkeletonBar.vue | 骨架屏加载条 |
| `TopicChip` | components/TopicChip.vue | 话题标签胶囊 |
| `ScrollReveal` | components/ScrollReveal.vue | GSAP ScrollTrigger 滚动入场动画 |
| `SplitText` | components/SplitText.vue | GSAP 逐字动画 |
| `DecryptedText` | components/DecryptedText.vue | 乱码解密文字动画 |
| `UnifiedDiff` | components/UnifiedDiff.vue | LCS算法代码Diff对比 (GitHub风格) |

---

## 三、Web 端页面清单 (11个)

| 页面 | 路由 | 关键功能 |
|------|------|----------|
| `HomeView` | `/` | Hero+暗色大卡片+功能入口网格+Hot Topics+ParticleBg粒子背景 |
| `LoginView` | `/login` | 微信登录按钮+dev-token开发模式 |
| `InterviewView` | `/interview` | 岗位选择→SSE流式对话→胶囊输入(键盘/语音切换)→Markdown渲染→AI结束自动跳转报告 |
| `InterviewReportView` | `/interview/report?id=` | 环形进度条+维度卡片+提升建议 |
| `InterviewHistoryView` | `/interview/history` | 面试历史列表(进行中/已完成状态) |
| `ResumeUploadView` | `/resume/upload` | PDF/WORD上传+GlareCard+Folder历史入口 |
| `ResumeReportView` | `/resume/report?resumeId=` | 暗色GridScan评分区+维度进度条+缺失关键词+深度优化(SSE流式)+Diff对比+面试追问 |
| `ResumeHistoryView` | `/resume/history` | 解析状态标签+Folder动画 |
| `ExamView` + `ExamDoView` | `/exam`, `/exam/do` | 试卷列表+答题(ABCD选项+进度条) |
| `PracticeView` | `/practice` | 分类标签筛选+题目列表 |
| `WrongBookView` | `/wrong-book` | 错题列表(错误次数标签) |
| `ProfileView` | `/profile` | 头像+统计卡片+菜单+AI Key配置弹窗 |

---

## 四、后端 API 端点

### Auth (认证)
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 `{code, nickname, avatarUrl}` → `{token, userId, nickname, avatarUrl}` |

### Interview (面试)
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/interview/start` | 开始面试 `{position, resumeId?, model?}` → session |
| POST | `/api/interview/{id}/answer` | 提交回答 |
| POST | `/api/interview/{id}/answer/stream` | SSE流式回答 (event:token / event:finish) |
| POST | `/api/interview/{id}/end` | 结束面试 |
| GET | `/api/interview/list` | 面试历史 |
| GET | `/api/interview/{id}` | 面试详情(含dimensions JSON字符串) |

### Resume (简历)
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/resume/upload` | 上传简历 `multipart: file, jobDescription, position` → `{resumeId, fileName, parseStatus}` |
| GET | `/api/resume/list` | 简历历史(含position字段) |
| GET | `/api/resume/{id}/analysis` | 获取分析报告 |
| POST | `/api/resume/{id}/analyze` | 触发快速评分 |
| POST | `/api/resume/{id}/analyze-deep` | SSE深度优化 |
| GET | `/api/resume/{id}/deep-status` | 深度优化状态+重试次数 |
| GET | `/api/resume/{id}/export-word` | 导出Word |
| GET | `/api/resume/{id}/preview-html` | HTML预览 |
| DELETE | `/api/resume/{id}` | 删除简历 |

### Questions (题库)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/questions` | 题目列表 `?categoryId=&difficulty=&type=&page=&size=` |
| GET | `/api/questions/random` | 随机抽题 |
| GET | `/api/questions/categories` | 分类列表 |

### User (用户)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/stats` | 统计数据 `{practiceCount, interviewCount, wrongCount}` |
| GET | `/api/user/ai-config` | AI配置 |
| PUT | `/api/user/ai-config` | 保存AI配置 `{provider, apiKey, model}` |

### Other
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 (dev-token- 前缀=userId 1) |
| GET | `/api/resume/template/list` | 模板列表 |

---

## 五、关键架构决策

### 1. 前后端响应格式
后端统一返回 `{code: 200, message: "success", data: {...}}`。
前端 `request.ts` 的 `ApiResponse<T>` 接口匹配 `{code, message, data}`。
**注意**: 用 `res.code === 200` 判断成功，**不是** `res.success`。

### 2. SSE 事件格式
```
event:token
data:文本片段

event:finish
data:{"finished":true,"report":{"score":5,...}}
```
前端解析：`buffer.split('\n\n')` → 逐行 `event:`/`data:` 分离 → 流结束后 `processSSE()` flush 残余 buffer。

### 3. 开发模式认证
`JwtAuthFilter` 支持 `dev-token-` 前缀 token → 自动认证为 userId=1。
前端路由守卫 `beforeEach` 自动生成 `dev-token-{timestamp}` 若无 token。

### 4. Web 端端口
- Vite 开发服务器: `http://localhost:5173`
- 代理 `/api` → `http://localhost:8080`
- Spring Boot: `http://localhost:8080`
- 构建: `npx vite build` → `dist/`

### 5. 小程序端
- HBuilderX 项目，uView Plus UI 框架
- 使用 `uni.uploadFile` 上传文件
- `uni.getStorageSync('mianmiantong_token')` 获取 token

### 6. ResumeHistoryDto
**重要**: 新增 `position` 字段。SQL 查询 `r.position AS position`。
若修改此 DTO，需同步更新小程序端的 `interface ResumeItem`。

---

## 六、设计令牌 (Warm Tech)

```css
--bg-canvas: #F3EFE8;     /* 外部大背景 */
--bg-paper: #FDFCFB;      /* 卡片主背景 (纸张白) */
--bg-surface: #F7F7F5;    /* 次背景 */
--bg-dark: #141413;       /* 暗色卡片/按钮 */

--text-main: #141413;     /* 主文本 */
--text-muted: #4A4A4A;    /* 次文本 */
--text-light: #888888;    /* 辅助文本 */

--accent: #D9750A;        /* 琥珀橙 */
--color-success: #22C55E;
--color-danger: #EF4444;

--font-sans: 'Inter', -apple-system, 'PingFang SC', sans-serif;
--font-serif: 'Georgia', serif;
--font-mono: 'JetBrains Mono', monospace;

--radius-lg: 16px;
--radius-xl: 20px;
--radius-full: 100px;

--shadow-sm: 0 1px 2px rgba(0,0,0,0.02);
--shadow-md: 0 4px 12px rgba(0,0,0,0.06);
--shadow-lg: 0 16px 32px rgba(0,0,0,0.10);
```

---

## 七、待开发功能

- [ ] 代码块语法高亮 (当前marked.js渲染基本代码块，无完整syntax highlighting)
- [ ] PC端全功能代码编辑器 (Monaco/CodeMirror)
- [ ] PC端面板拖拽调节宽度
- [ ] 语音识别集成
- [ ] 微信OAuth真实登录流程

---

## 八、启动命令

```bash
# 后端
cd mianmiantong-server
mvn spring-boot:run                # 开发模式
java -jar target/mianmiantong.jar  # 生产模式

# Web前端
cd AI-Interview/web-app
npm run dev     # 开发服务器 → http://localhost:5173
npm run build   # 生产构建 → dist/

# 小程序
# 用 HBuilderX 打开 AI-Interview/ 目录，运行到微信开发者工具
```
