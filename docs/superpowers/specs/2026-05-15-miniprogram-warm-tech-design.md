# 小程序端 Warm Tech 设计优化方案

> 本文档供小程序开发 agent 使用。Web 端独立开发，两套前端共享 `mianmiantong-server` 后端 API。

## 设计参考

设计稿：`E:\My_Projects\Test_Lixm\2.html`

### 设计系统令牌

已在 `AI-Interview/styles/tokens.css` 和 `AI-Interview/uni.scss` 中完成配置，直接复用即可：

```scss
// 背景色
$bg-canvas: #F3EFE8;    // 外部大背景
$bg-paper: #FDFCFB;     // 卡片/界面主背景 (纸张白)
$bg-surface: #F7F7F5;    // 次背景
$bg-dark: #141413;       // 深色卡片/按钮背景

// 文本色
$text-main: #141413;     // 主文本
$text-muted: #4A4A4A;    // 次文本
$text-light: #888888;    // 辅助文本

// 强调色
$accent: #D9750A;        // 琥珀橙
$color-success: #22C55E;
$color-danger: #EF4444;

// 边框
$border-light: rgba(0, 0, 0, 0.06);
$border-medium: rgba(0, 0, 0, 0.10);

// 阴影（移动端悬停无效，仅微阴影）
$shadow-sm: 0 1px 2px rgba(0,0,0,0.02);
$shadow-md: 0 4px 12px rgba(0,0,0,0.06);
$shadow-lg: 0 16px 32px rgba(0,0,0,0.10);

// 字体
--font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif;
--font-serif: 'Georgia', serif;
--font-mono: 'JetBrains Mono', 'Fira Code', monospace;
```

### 动画（已存在于 animations.css）

- `blink` — 光标闪烁
- `shimmer` — 骨架屏光影扫过
- `wave` — 语音波形跳动
- `btn-press` — 按钮按压反馈 (scale 0.97)

### 小程序限制注意事项

- **不支持 `backdrop-filter`** — 毛玻璃效果降级为纯色背景
- **不支持 `mix-blend-mode`** — 图片叠加降级为 opacity
- **hover 无效** — 所有交互用 `:active` 或 `@touchstart`/`@touchend`
- **`<rich-text>` 渲染 Markdown** — 代码块高亮用简单的 `<pre>` + 单色背景
- **字体回退** — Inter / Georgia / JetBrains Mono 不可用，回退到系统字体
- **暗色代码块** — 用 `background: #141413; color: #E4E4E4` 模拟，无需真实语法高亮

---

## 逐页优化清单

### 1. `pages/index/index.vue` — 首页 (已完成大部分)

**已完成：**
- Header（品牌 + 头像）
- Hero 区域（标签 + 标题 + 副标题 + 暗色大卡片）
- 功能卡片网格（2×2）
- Hot Topics + 题目分类标签
- CSS 纹理背景

**待优化：**
- 功能卡片添加 `@touchstart`/`@touchend` 按压反馈（已用 btn-press，确认生效）
- Hot Topics 第一个标签默认 active 状态
- 暗色大卡片图片加载失败时的 fallback 背景

**注意：不要引入 backdrop-filter，用纯色替代。**

---

### 2. `pages/interview/chat.vue` — AI 面试对话页 (高优先级)

**当前状态：** 基础聊天 UI，emoji 头像，简单输入框。

**设计稿要求：**
```
┌──────────────────────────┐
│  ←  高级工程师面试  [结束] │  ← 顶部栏，半透明毛玻璃 → 降级为 bg-paper
│      🟢 AI 面试官在线      │
├──────────────────────────┤
│                          │
│  [🤖] Interviewer        │  ← AI 消息：小头像 + 角色名 + 文本
│  概括得很准。接下来看...    │
│                          │
│  ┌────────────────────┐  │
│  │ ● ● ●  AQS.java    │  │  ← 暗色代码块 (bg-dark)
│  │────────────────────│  │
│  │ public final void  │  │
│  │   acquire(int arg) │  │
│  └────────────────────┘  │
│                          │
│        ┌──────────────┐  │
│        │ 用户消息气泡  │  │  ← 右侧对齐，bg-surface
│        └──────────────┘  │
│                          │
│  [🤖] 正在分析...        │  ← 骨架屏状态
│  ████████░░░░            │
│                          │
├──────────────────────────┤
│  [⌨]  Tap to speak...  │  ← 语音面板 (无 backdrop-filter)
│              [🎤 发送]   │
└──────────────────────────┘
```

**优化要点：**

1. **AI 消息气泡**
   - 左侧 AI 头像（圆角矩形 `border-radius: 8px`，不要圆形）
   - 角色名小字 `font-size: 13px; font-weight: 500`
   - 消息文本 `font-size: 15px; line-height: 1.6; color: var(--text-muted)`

2. **暗色代码块**
   ```html
   <view class="code-block">
     <view class="code-header">
       <view class="code-dots">
         <view class="dot red" /><view class="dot yellow" /><view class="dot green" />
       </view>
       <text class="code-filename">AQS.java</text>
     </view>
     <scroll-view class="code-body" scroll-x>
       <text class="code-text">{{ codeSnippet }}</text>
     </scroll-view>
   </view>
   ```
   - 样式：`background: #141413; border-radius: 12px; padding: 16px;`
   - 字体：`font-family: monospace; font-size: 12px; line-height: 1.6; color: #E4E4E4;`
   - 窗口装饰圆点：红 `#ED6A5E`、黄 `#F4BF4F`、绿 `#61C554`，直径 10px

3. **用户消息气泡**
   - 右对齐 `justify-content: flex-end`
   - `background: bg-surface; border-radius: 16px 16px 4px 16px;`

4. **骨架屏加载状态**
   - 复用 `.skeleton-bar` 类
   - 添加小字 "正在分析你的回答..."

5. **底部语音面板**
   ```html
   <view class="voice-panel">
     <view class="voice-bar">
       <button class="voice-btn-keyboard">⌨</button>
       <text class="voice-hint">轻触说话...</text>
       <button class="voice-btn-mic">🎤</button>
     </view>
   </view>
   ```
   - 样式：白色圆角 24px 卡片，`box-shadow: shadow-lg`
   - 暂停/发送按钮：暗色圆形 `background: bg-dark`

6. **录音中状态**
   - 暗色底板 `background: bg-dark; border-radius: 24px;`
   - 中间 5 条波形动画（`wave-bar` 已存在）
   - 左取消 / 右发送按钮

---

### 3. `pages/resume/report.vue` — 简历诊断报告 (高优先级)

**设计稿要求：**
```
┌──────────────────────────┐
│  ←  诊断结果              │  ← 顶部栏
├──────────────────────────┤
│  ┌──────┐               │
│  │  78  │  项目深度不足   │  ← 分数 + 评分描述
│  │ /100 │  你的 Java...   │     (绿色卡片 #F0FDF4)
│  └──────┘               │
│                          │
│  ● 高优修改建议           │
│  ┌────────────────────┐  │
│  │ ⚠️ 缺乏量化指标     │  │  ← 诊断卡片
│  │ 原句："优化了..."    │  │
│  │ ┌────────────────┐ │  │
│  │ │ AI 建议重写为：  │ │  │  ← 绿色左边框重写建议
│  │ │ "通过引入..."    │ │  │
│  │ └────────────────┘ │  │
│  └────────────────────┘  │
│                          │
│  [⬇ 导出优化后的 PDF]    │  ← 暗色导出按钮
└──────────────────────────┘
```

**优化要点：**

1. **分数徽章**
   ```html
   <view class="score-badge">
     <text class="score-num">78</text>
     <text class="score-max">/100</text>
   </view>
   ```
   - 样式：`width: 80px; height: 80px; border-radius: 24px; background: #F0FDF4; border: 1px solid #BBF7D0;`
   - 数字：`font-size: 28px; font-weight: 700; color: #16A34A;`

2. **诊断卡片**
   - 红色圆点指示器：`width: 8px; height: 8px; background: color-danger; border-radius: 50%;`
   - 标题 `font-size: 14px; font-weight: 500;`
   - 原文展示 `background: bg-surface; border-radius: 8px; padding: 12px;`
   - AI 重写建议：左边框绿色 `border-left: 3px solid color-success;`

3. **导出按钮**
   - 全宽暗色按钮：`background: bg-dark; color: #FFF; border-radius: 16px; padding: 16px;`
   - 下载图标 + 文字居中

---

### 4. `pages/resume/upload.vue` — 简历上传页

**优化要点：**
- 上传区域：大虚线框 → 改为卡片式 `border-radius: 16px; border: 1px dashed border-medium;`
- 文件图标放大，居中
- 上传按钮：暗色全宽按钮
- 最近上传列表：卡片式，swipe-delete 保留

---

### 5. `pages/profile/profile.vue` — 个人中心

**优化要点：**
- 头像改为大号（80×80），圆角 24px
- 数据统计卡片：3 列等宽，白色卡片
- 菜单项：去除 emoji 图标 → 改用简洁文字 + 箭头，或保留 uni-icons
- 退出按钮：红色文字 `color-danger`

---

### 6. `pages/login/login.vue` — 登录页

**优化要点：**
- 大号 "Mianmian." 品牌文字（Georgia 字体）
- 副标题 "AI 模拟面试平台"
- 微信一键登录按钮：暗色圆角全宽
- 底部留白

---

### 7. `pages/exam/index.vue` + `pages/exam/do.vue` — 在线试卷

**优化要点：**
- 试卷卡片 → 白底圆角卡片 + 阴影
- 答题页：选项按钮用设计稿样式（border + rounded-full）
- 进度条用 accent 色

---

### 8. `pages/practice/practice.vue` — 自由刷题

**优化要点：**
- 分类标签用 chip 样式（与首页 Hot Topics 一致）
- 题目卡片：白底 + 圆角

---

### 9. `pages/wrong-book/wrong-book.vue` — 错题本

**优化要点：**
- 空状态：大号图标 + "还没有错题" 文字
- 错题卡片：白底 + 红色小标记

---

### 10. `pages/interview/history.vue` + `pages/interview/report.vue`

**优化要点：**
- 历史列表：卡片式，状态标签（完成/进行中）
- 报告页：分数 + 维度雷达图（如已有）或条形图

---

## 全局优化

### uni.scss
- uView Plus 主题色已配置为 Warm Tech 色板 ✅
- uni-app 内置变量已配置 ✅

### App.vue
- 全局字体设置
- 页面背景色统一为 `bg-canvas`
- 全局 `-webkit-font-smoothing: antialiased`

### 新的公共组件 (可选，放在 `components/`)

1. **`DiagnosticCard.vue`** — 诊断建议卡片（简历报告用）
2. **`CodeBlockMini.vue`** — 小程序端代码块（无语法高亮，仅暗色背景）

---

## 小程序 vs Web 端的差异处理

| 特性 | 小程序 (uni-app) | Web (Vue 3 + Vite) |
|------|------------------|--------------------|
| 代码块 | 纯暗色背景 `<pre>` | Monaco/syntax-highlighted |
| 毛玻璃 | 降级为纯色 | `backdrop-filter` 完整支持 |
| 图片混合 | `opacity` 模拟 | `mix-blend-mode: luminosity` |
| 粒子背景 | CSS 纹理（已实现） | Canvas 粒子 |
| 语音面板 | 基础波形 CSS | 完整波形 + 录音可视化 |
| PC 三栏 | ❌ 不支持 | ✅ 响应式三栏 |
| 眩光悬浮 | ❌ 无 hover | ✅ GlareCard |

---

## 执行顺序建议

1. 首页收尾（Hot Topics active 状态、按钮反馈）
2. 面试对话页（chat.vue）— 最复杂，优先
3. 简历报告页（report.vue）
4. 简历上传页（upload.vue）
5. 个人中心（profile.vue）
6. 登录页（login.vue）
7. 其余页面（exam, practice, wrong-book, history）
