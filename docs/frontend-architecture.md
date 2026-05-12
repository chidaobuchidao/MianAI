# 面面通 — 前端架构文档

> 框架: uni-app (Vue 3 + TypeScript) | 目标平台: 微信小程序 | UI: uView Plus 3.8.18

---

## 1. 页面路由结构

```
┌─────────────────────────────────────────────────┐
│                    TabBar 主包                    │
│  ┌─────────┐ ┌─────────┐ ┌────────┐ ┌────────┐ │
│  │ 首页     │ │ 自由刷题  │ │ 错题本  │ │ 我的    │ │
│  │ /index  │ │/practice│ │/wrong  │ │/profile│ │
│  └────┬────┘ └─────────┘ └────────┘ └───┬────┘ │
└───────┼─────────────────────────────────┼───────┘
        │                                 │
   ┌────┴────────────────────┐    ┌───────┴─────────┐
   │         子包             │    │     子包         │
   │  pages/question/        │    │ pages/interview/ │
   │  ├─ list (题库列表)      │    │ ├─ chat (AI面试)  │
   │                         │    │ ├─ report(报告)   │
   ├─────────────────────────┤    │ └─ history(历史) │
   │  pages/exam/            │    │                  │
   │  ├─ index (试卷列表)     │    └──────────────────┘
   │  └─ do (答题中)          │
   │                         │    ┌──────────────────┐
   └─────────────────────────┘    │ 子包             │
                                  │ pages/login/     │
                                  │ └─ login (登录页) │
                                  └──────────────────┘
```

**路由配置** (`pages.json`):
- 主包 4 个 TabBar 页面（首页、刷题、错题本、我的）
- 4 个分包：login、question、exam、interview
- 使用 `subPackages` 实现代码分包加载，优化首屏速度

---

## 2. 页面清单

| 页面路径 | 功能 | 包类型 |
|---------|------|--------|
| `pages/index/index` | 首页：快捷入口 + 分类导航 | 主包 (Tab) |
| `pages/practice/practice` | 自由刷题：随机10题，即时判分 | 主包 (Tab) |
| `pages/wrong-book/wrong-book` | 错题本：展示/移除错题 | 主包 (Tab) |
| `pages/profile/profile` | 个人中心：统计 + AI Key 配置 + 退出 | 主包 (Tab) |
| `pages/login/login` | 微信一键登录 + 开发模式降级 | 分包 |
| `pages/question/list` | 题库列表：分页 + 分类/难度/题型筛选 | 分包 |
| `pages/exam/index` | 试卷列表：启用中的试卷 | 分包 |
| `pages/exam/do` | 答题中：倒计时 + 上一题/下一题 + 交卷 | 分包 |
| `pages/interview/chat` | AI面试：岗位选择→多轮对话→自动生成报告 | 分包 |
| `pages/interview/report` | 面试报告：评分雷达 + 维度分析 + 对话回放 | 分包 |
| `pages/interview/history` | 面试历史：最近5条记录 | 分包 |

---

## 3. 状态管理 (Pinia)

**唯一 Store: `user.ts`**

```typescript
interface UserStore {
  token: string        // JWT token
  userId: number       // 用户 ID
  nickname: string     // 昵称
  avatarUrl: string    // 头像
  isLogin: boolean     // 登录状态
  login(code)          // 微信登录 → 获取 JWT
  devLogin()           // 开发模式（后端离线时使用）
  logout()             // 退出 → 清 token → 跳转登录页
}
```

状态持久化方式：`token` 通过 `uni.setStorageSync(TOKEN_KEY, token)` 存储本地。

---

## 4. 网络请求层 (`utils/request.ts`)

### 4.1 请求封装

```
request(options)
  ├── 自动拼接 BASE_URL (http://localhost:8080)
  ├── 自动附加 Authorization Bearer Token
  ├── 过滤 null 值参数
  ├── 401 → 自动清除 token → redirect 登录页
  └── 网络失败 → 静默返回空数据（开发模式容错）
```

### 4.2 HTTP 快捷方法

| 方法 | 签名 | 用途 |
|------|------|------|
| `get<T>(url, data?)` | → `Promise<ApiResponse<T>>` | GET 请求 |
| `post<T>(url, data?)` | → `Promise<ApiResponse<T>>` | POST 请求 |
| `put<T>(url, data?)` | → `Promise<ApiResponse<T>>` | PUT 请求 |
| `del<T>(url)` | → `Promise<ApiResponse<T>>` | DELETE 请求 |

### 4.3 流式请求 (SSE)

```
streamRequest(url, data, callbacks)
  ├── 微信环境: wx.request + enableChunked + onChunkReceived
  │   └── 逐行解析 SSE: event:token → onToken / event:finish → onFinish
  └── 非微信环境: 降级为普通 POST
```

### 4.4 开发模式容错

当 `token` 以 `dev-token-` 开头时:
- 不发送 `Authorization` 头
- 请求失败时返回空数据而不报错
- 允许在无后端环境下调试 UI

---

## 5. 组件树

```
App.vue
├── pages/index/index.vue              # 首页
│   └── 无子组件（纯模板渲染）
│
├── pages/login/login.vue              # 登录
│
├── pages/practice/practice.vue        # 刷题
│   ├── start-screen (开始屏)
│   ├── quiz-screen (答题屏)
│   │   ├── progress-bar (进度条)
│   │   ├── quiz-card (题目卡片)
│   │   │   ├── options (选项列表)
│   │   │   ├── fill-input (填空输入)
│   │   │   └── quiz-result (判分结果)
│   │   └── quiz-actions (操作按钮)
│   └── finish-screen (完成屏)
│
├── pages/exam/do.vue                  # 考试
│   ├── top-bar (倒计时栏)
│   ├── q-card (题目卡片)
│   └── bottom-bar (导航栏)
│
├── pages/interview/chat.vue           # AI面试
│   ├── position-screen (岗位选择屏)
│   ├── chat-wrap (聊天屏)
│   │   ├── msg-list (消息列表)
│   │   │   └── msg-bubble (消息气泡 + Markdown渲染)
│   │   └── input-zone (输入区)
│   └── finish-overlay (完成屏)
│
├── pages/interview/report.vue         # 面试报告
│   ├── score-hero (评分环形)
│   ├── card (总评 / 维度 / 建议)
│   └── chat-log-section (对话回放)
│
└── pages/profile/profile.vue          # 个人中心
    ├── head (头像区)
    ├── data-row (统计卡片)
    ├── menu (功能菜单)
    └── modal-card (AI Key 弹窗)
```

---

## 6. AI 面试 Markdown 解析

`chat.vue` 内置了自定义 Markdown → `rich-text` 节点解析器 (`renderMarkdown`):

| 语法 | 渲染 |
|------|------|
| `**text**` | `<strong>` 加粗 |
| `*text*` | `<em>` 斜体 |
| `` `code` `` | `<span>` 代码样式 |
| `1. ` | `<strong>` 列表序号 |
| `\n` | `<br>` 换行 |

自动去除 `[面试结束]` 标记和 JSON 报告块，避免暴露评分数据在聊天界面。

---

## 7. 主题与样式

- **主色调**: `#2B6FF2` (蓝色)
- **背景色**: `#F0F4FF` / `#F5F6FA`
- **全局样式**: 定义于 `App.vue` (`.global-text-primary`, `.global-bg-primary`) 和 `uni.scss`
- **组件样式**: 全部使用 `scoped` SCSS，无全局污染
- **布局**: Flexbox 为主，配合 Grid（首页快捷入口使用 2 列 grid）
- **动画**: CSS `transition` + `@keyframes`（打字动画、进度条过渡、脉冲动画）
