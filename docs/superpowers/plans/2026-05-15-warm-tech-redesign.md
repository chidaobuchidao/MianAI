# Warm Tech UI 重设计 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 AI-Interview uni-app 项目从 AI 模板风重设计为 Warm Tech 风格，适配移动端+PC 端响应式，保留所有微交互动画

**Architecture:** 先建立设计系统基础设施（CSS tokens、动画、SVG 图标组件），然后逐页重写样式。修改方式：替换全局色彩变量 + 逐页重写 `<style>` 块 + 用 SVG 图标替代 emoji。不做组件库引入，不改变现有数据流和路由。

**Tech Stack:** uni-app + Vue 3 + Pinia + SCSS，响应式用 CSS 媒体查询

---

## File Structure

```
AI-Interview/
├── styles/                          # 新增：设计系统
│   ├── tokens.css                   # CSS 自定义属性（色彩、字体、间距、阴影）
│   └── animations.css               # 关键帧动画（blink/shimmer/wave）
├── components/
│   └── MianIcon.vue                 # 新增：SVG 图标组件（替代 emoji）
├── uni.scss                         # 修改：更新 uView 主题色 + SCSS 变量
├── common/uni.css                   # 修改：更新 .theme-light 变量
├── pages.json                       # 修改：导航栏、tabBar 配色
├── pages/index/index.vue            # 修改：首页
├── pages/interview/chat.vue         # 修改：AI面试对话
├── pages/profile/profile.vue        # 修改：个人中心
├── pages/login/login.vue            # 修改：登录页
├── pages/resume/upload.vue          # 修改：简历上传
├── pages/resume/report.vue          # 修改：简历报告
├── pages/resume/history.vue         # 修改：简历历史
├── pages/interview/report.vue       # 修改：面试报告
├── pages/interview/history.vue      # 修改：面试历史
├── pages/question/list.vue          # 修改：题库列表
├── pages/exam/index.vue             # 修改：试卷列表
├── pages/exam/do.vue                # 修改：答题页
├── pages/practice/practice.vue      # 修改：自由刷题
└── pages/wrong-book/wrong-book.vue  # 修改：错题本
```

---

### Task 1: 创建设计系统基础设施

**Files:**
- Create: `styles/tokens.css`
- Create: `styles/animations.css`
- Create: `components/MianIcon.vue`

- [ ] **Step 1: 创建 CSS 设计 Token**

```css
/* styles/tokens.css */
:root {
  /* 背景 */
  --bg-canvas: #F3EFE8;
  --bg-paper: #FDFCFB;
  --bg-surface: #F7F7F5;
  --bg-dark: #141413;

  /* 文本 */
  --text-main: #141413;
  --text-muted: #4A4A4A;
  --text-light: #888888;

  /* 强调 */
  --accent: #D9750A;
  --accent-hover: #B96508;
  --color-success: #22C55E;
  --color-danger: #EF4444;

  /* 边框 */
  --border-light: rgba(0, 0, 0, 0.06);
  --border-medium: rgba(0, 0, 0, 0.10);

  /* 阴影 */
  --shadow-sm: 0 1px 2px rgba(0,0,0,0.02);
  --shadow-md: 0 4px 12px rgba(0,0,0,0.06);
  --shadow-lg: 0 16px 32px rgba(0,0,0,0.10);
  --shadow-xl: 0 24px 48px rgba(0,0,0,0.12);

  /* 圆角 */
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;
  --radius-full: 100px;

  /* 字体 */
  --font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif;
  --font-serif: 'Georgia', serif;
  --font-mono: 'JetBrains Mono', 'Fira Code', monospace;
}

/* H5 端加载 Google Fonts */
/* #ifdef H5 */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap');
/* #endif */
```

- [ ] **Step 2: 创建动画关键帧**

```css
/* styles/animations.css */
/* 光标闪烁 */
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
.animate-blink { animation: blink 1s infinite; }

/* 骨架屏光影 */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
.skeleton-bar {
  background: linear-gradient(90deg, #F0F0F0 25%, #E4E4E4 50%, #F0F0F0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite linear;
  border-radius: 6px;
}

/* 语音波形 */
@keyframes wave {
  0%, 100% { transform: scaleY(0.5); }
  50% { transform: scaleY(1); }
}
.wave-bar {
  width: 3px;
  background: var(--color-success);
  border-radius: 2px;
  transform-origin: center;
  animation: wave 1s infinite ease-in-out;
}
.wave-bar:nth-child(1) { height: 16px; animation-delay: 0.0s; }
.wave-bar:nth-child(2) { height: 24px; animation-delay: 0.2s; }
.wave-bar:nth-child(3) { height: 12px; animation-delay: 0.4s; }
.wave-bar:nth-child(4) { height: 20px; animation-delay: 0.1s; }
.wave-bar:nth-child(5) { height: 14px; animation-delay: 0.3s; }

/* 按钮按压 */
.btn-press {
  transition: transform 0.15s;
}
.btn-press:active {
  transform: scale(0.97);
}

/* PC 端卡片 hover */
@media (min-width: 1025px) {
  .card-hover {
    transition: transform 0.2s, box-shadow 0.2s;
  }
  .card-hover:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-lg);
  }
}
```

- [ ] **Step 3: 创建 MianIcon SVG 图标组件**

```vue
<!-- components/MianIcon.vue -->
<template>
  <view class="mian-icon" :style="{ width: size + 'rpx', height: size + 'rpx' }">
    <image v-if="name" :src="'/static/icons/' + name + '.svg'" mode="aspectFit" style="width: 100%; height: 100%;" />
  </view>
</template>
<script setup lang="ts">
defineProps<{ name: string; size?: number }>();
</script>
<style scoped>
.mian-icon { display: inline-flex; align-items: center; justify-content: center; }
</style>
```

先占位，SVG 图标文件在后续任务中按需创建。

- [ ] **Step 4: 提交**

```bash
git add styles/tokens.css styles/animations.css components/MianIcon.vue
git commit -m "feat: add design system foundation (tokens, animations, icon component)"
```

---

### Task 2: 更新全局配置

**Files:**
- Modify: `uni.scss`
- Modify: `common/uni.css`
- Modify: `pages.json`

- [ ] **Step 1: 更新 uni.scss 主题变量**

替换 `uni.scss` 中 uView 和 uni-app 的颜色变量：

```scss
/* uni.scss — Warm Tech 主题 */

/* uView Plus 主题变量 */
$u-primary: #D9750A;
$u-success: #22C55E;
$u-warning: #D9750A;
$u-error: #EF4444;
$u-info: #888888;

@import "uview-plus/theme.scss";
@import "uview-plus/index.scss";

/* uni-app 内置变量 */
/* 行为相关颜色 */
$uni-color-primary: #D9750A;
$uni-color-success: #22C55E;
$uni-color-warning: #D9750A;
$uni-color-error: #EF4444;

/* 文字基本颜色 */
$uni-text-color: #141413;
$uni-text-color-inverse: #FDFCFB;
$uni-text-color-grey: #888888;
$uni-text-color-placeholder: #888888;
$uni-text-color-disable: #c0c0c0;

/* 背景颜色 */
$uni-bg-color: #FDFCFB;
$uni-bg-color-grey: #F7F7F5;
$uni-bg-color-hover: #F3EFE8;
$uni-bg-color-mask: rgba(0, 0, 0, 0.4);

/* 边框颜色 */
$uni-border-color: rgba(0, 0, 0, 0.10);

/* 尺寸变量 */
$uni-font-size-sm: 12px;
$uni-font-size-base: 14px;
$uni-font-size-lg: 16px;

$uni-img-size-sm: 20px;
$uni-img-size-base: 26px;
$uni-img-size-lg: 40px;

/* Border Radius */
$uni-border-radius-sm: 4px;
$uni-border-radius-base: 8px;
$uni-border-radius-lg: 12px;
$uni-border-radius-circle: 50%;

/* 间距 */
$uni-spacing-row-sm: 5px;
$uni-spacing-row-base: 10px;
$uni-spacing-row-lg: 15px;
$uni-spacing-col-sm: 4px;
$uni-spacing-col-base: 8px;
$uni-spacing-col-lg: 12px;

$uni-opacity-disabled: 0.3;

$uni-color-title: #141413;
$uni-font-size-title: 20px;
$uni-color-subtitle: #4A4A4A;
$uni-font-size-subtitle: 26px;
$uni-color-paragraph: #4A4A4A;
$uni-font-size-paragraph: 15px;
```

- [ ] **Step 2: 更新 common/uni.css 主题变量**

```css
/* common/uni.css */
.theme-light {
  --list-background-color: #FDFCFB;
  --background-color: #F7F7F5;
  --active-color: #D9750A;
  --active-background-color: #F3EFE8;
  --text-color: #141413;
  --border-color: rgba(0, 0, 0, .06);
}

/* ... 其余不变 ... */
```

- [ ] **Step 3: 更新 pages.json 全局样式**

```json
"globalStyle": {
    "navigationBarTextStyle": "white",
    "navigationBarTitleText": "面面通",
    "navigationBarBackgroundColor": "#141413",
    "backgroundColor": "#F3EFE8"
},
"tabBar": {
    "color": "#888888",
    "selectedColor": "#D9750A",
    "backgroundColor": "#FDFCFB",
    "borderStyle": "black",
    ...
}
```

- [ ] **Step 4: 提交**

```bash
git add uni.scss common/uni.css pages.json
git commit -m "feat: apply Warm Tech palette to global config"
```

---

### Task 3: 重设计首页 index.vue

**Files:**
- Modify: `pages/index/index.vue`

- [ ] **Step 1: 创建 SVG 图标文件**

在 `static/icons/` 下创建内联 SVG 的 `.svg` 文件用于首页图标。先用简单 SVG path（Heroicons 风格），后续可替换。

创建 `static/icons/interview.svg`:
```svg
<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
```

创建 `static/icons/doc.svg`、`static/icons/target.svg`、`static/icons/chart.svg`、`static/icons/file.svg`、`static/icons/folder.svg` 类似操作。

- [ ] **Step 2: 重写 index.vue `<style>` 块**

完整替换现有 SCSS，使用设计 token + 响应式布局：

```scss
<style lang="scss" scoped>
// 移动端默认
.home { background: var(--bg-canvas); min-height: 100vh; padding-bottom: 40rpx; }

// Header
.header { display: flex; justify-content: space-between; align-items: center; padding: 24rpx 28rpx 16rpx; }
.brand { font-family: var(--font-serif); font-size: 36rpx; font-weight: 600; letter-spacing: -0.5px; color: var(--text-main); }
.avatar { width: 64rpx; height: 64rpx; border-radius: 50%; border: 1px solid var(--border-light); }
.avatar-txt { font-size: 28rpx; font-weight: 700; color: var(--text-main); }

// Hero
.hero { padding: 0 28rpx 32rpx; }
.hero-title { font-family: var(--font-serif); font-size: 56rpx; line-height: 1.2; color: var(--text-main); letter-spacing: -1px; margin-bottom: 16rpx; }
.hero-sub { font-size: 28rpx; color: var(--text-muted); line-height: 1.6; margin-bottom: 32rpx; }

// 深色 Hero Card
.hero-card {
  position: relative; border-radius: var(--radius-xl); overflow: hidden; box-shadow: var(--shadow-md); background: var(--bg-dark);
  padding: 40rpx 32rpx; display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24rpx;
}
.hero-card-title { font-size: 36rpx; font-weight: 500; color: #fff; display: block; margin-bottom: 8rpx; }
.hero-card-desc { font-size: 24rpx; color: rgba(255,255,255,0.5); display: block; }
.hero-card-arrow { width: 56rpx; height: 56rpx; background: var(--accent); border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }

// 功能列表卡片
.func-list { display: flex; flex-direction: column; gap: 16rpx; padding: 0 28rpx; margin-bottom: 32rpx; }
.func-item {
  display: flex; align-items: center; gap: 24rpx; padding: 28rpx 24rpx;
  background: var(--bg-paper); border: 1px solid var(--border-light);
  border-radius: var(--radius-lg); box-shadow: var(--shadow-sm);
}
.func-item:active { background: var(--bg-surface); }
.func-icon-wrap { width: 64rpx; height: 64rpx; background: var(--bg-surface); border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.func-info { flex: 1; }
.func-title { font-size: 28rpx; font-weight: 500; color: var(--text-main); display: block; }
.func-desc { font-size: 24rpx; color: var(--text-light); display: block; margin-top: 4rpx; }
.func-arrow { color: #ccc; font-size: 32rpx; }

// 分类标签
.section { padding: 0 28rpx; }
.section-label { font-size: 24rpx; font-weight: 600; color: var(--text-light); text-transform: uppercase; letter-spacing: 2rpx; margin-bottom: 16rpx; display: block; }
.tag-list { display: flex; flex-wrap: wrap; gap: 12rpx; }
.tag {
  border: 1px solid var(--border-light); padding: 12rpx 24rpx; border-radius: var(--radius-full);
  font-size: 26rpx; color: var(--text-main); background: var(--bg-paper);
}
.tag:active { background: var(--bg-surface); border-color: var(--text-main); }

// 响应式：PC 端
@media (min-width: 1025px) {
  .home { display: flex; flex-direction: column; align-items: center; }
  .header, .hero, .func-list, .section { width: 100%; max-width: 800px; padding-left: 0; padding-right: 0; }
  .func-list { flex-direction: row; flex-wrap: wrap; gap: 20rpx; }
  .func-item { width: calc(50% - 10rpx); }
}
</style>
```

- [ ] **Step 3: 重写 index.vue `<template>` 块**

移除所有 `<DeepStatusBar />` 冗余实例，用 SVG 替换 emoji，用新结构替代 Hero:

```vue
<template>
  <view class="home">
    <!-- Header -->
    <view class="header">
      <text class="brand">Mianmian.</text>
      <view class="avatar" v-if="!userStore.avatarUrl" @click="goProfile">
        <text class="avatar-txt">{{ (userStore.nickname || '?')[0] }}</text>
      </view>
      <image class="avatar" v-else :src="userStore.avatarUrl" mode="aspectFill" @click="goProfile" />
    </view>

    <!-- Hero -->
    <view class="hero">
      <text class="hero-title">为每一次面试\n做好准备。</text>
      <text class="hero-sub">AI 模拟面试 + 智能刷题，专为计算机学生打造。</text>

      <!-- Dark CTA Card -->
      <view class="hero-card" @click="goInterview">
        <view>
          <text class="hero-card-title">开始 AI 面试</text>
          <text class="hero-card-desc">语音对话 · 代码编辑 · 深度追问</text>
        </view>
        <view class="hero-card-arrow">
          <text style="color:#fff;font-size:28rpx;">→</text>
        </view>
      </view>
    </view>

    <!-- 功能入口 -->
    <view class="func-list">
      <view class="func-item" @click="goExam">
        <view class="func-icon-wrap"><text style="font-size:32rpx;">📝</text></view>
        <view class="func-info">
          <text class="func-title">在线试卷</text>
          <text class="func-desc">限时模拟考试</text>
        </view>
        <text class="func-arrow">›</text>
      </view>
      <!-- ... 其余功能入口 ... -->
    </view>

    <!-- 分类标签 -->
    <view class="section">
      <text class="section-label">题目分类</text>
      <view class="tag-list">
        <view class="tag" v-for="cat in categories" :key="cat.id" @click="goCategory(cat)">
          <text>{{ cat.name }}</text>
        </view>
      </view>
    </view>
  </view>
</template>
```

- [ ] **Step 4: 添加 goProfile 方法**

```typescript
// 在 script setup 中添加
import { useUserStore } from '@/store/user';
const userStore = useUserStore();
function goProfile() { uni.switchTab({ url: '/pages/profile/profile' }); }
```

- [ ] **Step 5: 提交**

```bash
git add pages/index/index.vue static/icons/
git commit -m "feat: redesign homepage in Warm Tech style"
```

---

### Task 4: 重设计 AI 面试对话页 interview/chat.vue

**Files:**
- Modify: `pages/interview/chat.vue`

- [ ] **Step 1: 重写 `<style>` 块**

```scss
<style lang="scss" scoped>
.chat { min-height: 100vh; background: var(--bg-canvas); display: flex; flex-direction: column; }

// 岗位选择页
.position-screen { padding: 60rpx 28rpx; }
.pos-header { text-align: center; margin-bottom: 48rpx; }
.pos-icon { font-size: 96rpx; display: block; margin-bottom: 24rpx; }
.pos-title { font-family: var(--font-serif); font-size: 40rpx; font-weight: 600; color: var(--text-main); display: block; }
.pos-desc { font-size: 26rpx; color: var(--text-muted); margin-top: 12rpx; display: block; line-height: 1.6; }

.pos-grid { display: flex; flex-direction: column; gap: 16rpx; }
.pos-card {
  display: flex; align-items: center; gap: 20rpx;
  background: var(--bg-paper); border: 1px solid var(--border-light);
  padding: 32rpx 28rpx; border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}
.pos-card:active { background: var(--bg-surface); transform: scale(0.98); }
.pos-emoji { font-size: 44rpx; }
.pos-name { flex: 1; font-size: 30rpx; font-weight: 500; color: var(--text-main); }
.pos-arrow { font-size: 32rpx; color: var(--border-medium); }

// 聊天页
.chat-wrap { flex: 1; display: flex; flex-direction: column; }

// 顶部栏
.chat-topbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16rpx 28rpx; border-bottom: 1px solid var(--border-light);
  background: var(--bg-paper);
}
.chat-back { font-size: 36rpx; color: var(--text-muted); }
.chat-title { font-size: 28rpx; font-weight: 600; color: var(--text-main); }
.chat-status { font-size: 22rpx; color: var(--color-success); display: flex; align-items: center; gap: 8rpx; }
.chat-status-dot { width: 12rpx; height: 12rpx; background: var(--color-success); border-radius: 50%; }
.chat-end-btn {
  background: none; border: 1px solid var(--border-medium); border-radius: var(--radius-sm);
  padding: 8rpx 20rpx; font-size: 22rpx; color: var(--color-danger); font-weight: 500;
}

// 消息
.msg-list { flex: 1; padding: 24rpx; overflow-y: auto; }
.msg-row { margin-bottom: 24rpx; display: flex; gap: 12rpx; max-width: 88%; }
.msg-row.user { align-self: flex-end; justify-content: flex-end; margin-left: auto; }
.msg-avatar { width: 56rpx; height: 56rpx; border-radius: var(--radius-sm); flex-shrink: 0; object-fit: cover; border: 1px solid var(--border-light); }
.msg-bubble { padding: 20rpx 24rpx; border-radius: var(--radius-md); }
.msg-row.ai .msg-bubble { background: var(--bg-paper); border: 1px solid var(--border-light); border-top-left-radius: 4rpx; }
.msg-row.user .msg-bubble { background: var(--bg-dark); border-top-right-radius: 4rpx; }
.msg-badge { font-size: 22rpx; font-weight: 600; color: var(--text-muted); margin-bottom: 8rpx; display: block; }
.msg-text { font-size: 28rpx; line-height: 1.7; color: var(--text-main); }
.msg-row.user .msg-text { color: #FDFCFB; }

// 代码块
.code-block {
  background: var(--bg-dark); border-radius: var(--radius-md); overflow: hidden; margin-top: 12rpx;
  box-shadow: var(--shadow-md);
}
.code-header {
  display: flex; align-items: center; padding: 12rpx 16rpx;
  background: rgba(255,255,255,0.04); border-bottom: 1px solid rgba(255,255,255,0.05);
}
.code-dot { width: 16rpx; height: 16rpx; border-radius: 50%; margin-right: 10rpx; }
.code-dot.r { background: #ED6A5E; }
.code-dot.y { background: #F4BF4F; }
.code-dot.g { background: #61C554; }
.code-file { font-size: 20rpx; color: #888; font-family: var(--font-mono); flex: 1; }
.code-body { padding: 16rpx; overflow-x: auto; }
.code-body text { font-family: var(--font-mono); font-size: 22rpx; line-height: 1.6; color: #E4E4E4; }

// 思考中骨架
.skeleton { padding: 20rpx 0; }
.skeleton-bar { height: 16rpx; margin-bottom: 10rpx; }

// 输入区
.input-zone {
  padding: 16rpx 24rpx 32rpx; border-top: 1px solid var(--border-light); background: var(--bg-paper);
  display: flex; align-items: flex-end; gap: 12rpx;
}
.end-btn {
  height: 72rpx; padding: 0 20rpx; background: var(--bg-surface); color: var(--color-danger);
  font-size: 24rpx; font-weight: 600; border-radius: var(--radius-xl); border: none; white-space: nowrap; flex-shrink: 0;
}
.end-btn[disabled] { opacity: 0.4; }
.input-area {
  flex: 1; min-height: 72rpx; max-height: 200rpx; background: var(--bg-surface);
  border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 16rpx 20rpx;
  font-size: 28rpx; line-height: 1.5;
}
.send-btn {
  width: 120rpx; height: 72rpx; background: var(--bg-dark); color: #fff; font-size: 28rpx;
  font-weight: 600; border-radius: var(--radius-lg); border: none; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
}
.send-btn[disabled] { background: var(--bg-surface); color: var(--text-light); }

// 模型选择
.model-bar { display: flex; align-items: center; justify-content: center; gap: 16rpx; margin-bottom: 24rpx; }
.model-bar-label { font-size: 24rpx; color: var(--text-light); }
.model-opts { display: flex; gap: 0; background: var(--bg-surface); border-radius: var(--radius-sm); overflow: hidden; }
.model-opt { font-size: 22rpx; padding: 10rpx 32rpx; color: var(--text-light); }
.model-opt.active { background: var(--bg-dark); color: #fff; }

// 完成页
.finish-overlay { display: flex; flex-direction: column; align-items: center; padding-top: 220rpx; }
.fin-icon { font-size: 120rpx; }
.fin-title { font-size: 40rpx; font-weight: 600; color: var(--text-main); margin-top: 20rpx; font-family: var(--font-serif); }
.fin-btn { width: 460rpx; height: 96rpx; background: var(--bg-dark); color: #fff; font-size: 32rpx; font-weight: 600; border-radius: var(--radius-xl); border: none; margin-top: 60rpx; }

// PC 端：三栏布局
@media (min-width: 1025px) {
  .chat-wrap { flex-direction: row; }
  .chat-topbar { display: none; } // 顶部栏在 PC 端用侧边栏替代

  // 左侧面试进度
  .pc-sidebar {
    width: 280px; background: var(--bg-surface); border-right: 1px solid var(--border-light);
    padding: 24rpx; display: flex; flex-direction: column;
  }
  .pc-sidebar-title { font-family: var(--font-serif); font-size: 32rpx; font-weight: 600; color: var(--text-main); margin-bottom: 32rpx; }
  .pc-progress-label { font-size: 22rpx; font-weight: 600; color: var(--text-light); text-transform: uppercase; letter-spacing: 2rpx; margin-bottom: 20rpx; }

  // 中间对话区
  .pc-chat { flex: 1; display: flex; flex-direction: column; min-width: 0; }
  .pc-topbar { height: 60px; border-bottom: 1px solid var(--border-light); display: flex; align-items: center; justify-content: space-between; padding: 0 24px; }
  .msg-list { max-width: 900px; margin: 0 auto; width: 100%; }

  // 右侧代码编辑器
  .pc-editor {
    width: 450px; background: var(--bg-dark); display: flex; flex-direction: column;
    border-left: 1px solid var(--border-medium);
  }
  .pc-editor-header {
    display: flex; align-items: center; justify-content: space-between;
    padding: 12px 16px; background: rgba(255,255,255,0.05);
    border-bottom: 1px solid rgba(255,255,255,0.05);
  }
  .pc-editor-body { flex: 1; padding: 20px; overflow-y: auto; }
}
</style>
```

- [ ] **Step 2: 更新 `<template>` 添加 PC 端三栏布局**

```vue
<!-- 在 chat-wrap 内部，移动端用顶部栏，PC 端用侧边栏+三栏 -->
<view class="chat-wrap" v-if="started && !finished">
  <!-- PC 侧边栏 -->
  <view class="pc-sidebar">...</view>

  <!-- 对话区 -->
  <view class="pc-chat">
    <!-- 移动端顶部栏 -->
    <view class="chat-topbar">...</view>
    <!-- PC 端顶部栏 -->
    <view class="pc-topbar">...</view>
    <!-- 消息列表（共用） -->
    <scroll-view class="msg-list">...</scroll-view>
    <!-- 输入区（共用） -->
    <view class="input-zone">...</view>
  </view>

  <!-- PC 代码编辑器 -->
  <view class="pc-editor">...</view>
</view>
```

- [ ] **Step 3: 添加 AI 思考骨架屏模板**

```vue
<!-- AI 正在思考的加载状态 -->
<view v-if="loading" class="msg-row ai">
  <image class="msg-avatar" src="/static/interviewer-avatar.jpg" mode="aspectFill" />
  <view class="msg-bubble" style="min-width: 200rpx;">
    <view class="skeleton">
      <view class="skeleton-bar" style="width: 80%; height: 12px;" />
      <view class="skeleton-bar" style="width: 55%; height: 12px;" />
      <view class="skeleton-bar" style="width: 70%; height: 12px;" />
    </view>
    <text style="font-size: 22rpx; color: #aaa; display: flex; align-items: center; gap: 8rpx;">
      正在分析你的回答...
    </text>
  </view>
</view>
```

- [ ] **Step 4: 提交**

```bash
git add pages/interview/chat.vue
git commit -m "feat: redesign interview chat page with Warm Tech + 3-col desktop layout"
```

---

### Task 5: 重设计个人中心 profile.vue

**Files:**
- Modify: `pages/profile/profile.vue`

- [ ] **Step: 完整重写 `<style>` 和 `<template>`**

移除所有 `<DeepStatusBar />`，用新配色+结构：

- 头部：大号首字母头像 + 衬线字体昵称
- 数据卡片：浅底 + 深色数字
- 菜单：列表式，左侧图标 + 文字 + 右箭头
- 弹窗：圆角卡片，深色保存按钮

完整样式：

```scss
<style lang="scss" scoped>
.profile { min-height: 100vh; background: var(--bg-canvas); }

// 头像区
.head {
  display: flex; flex-direction: column; align-items: center; padding: 80rpx 0 40rpx;
  background: var(--bg-paper); border-bottom: 1px solid var(--border-light);
}
.avatar { width: 140rpx; height: 140rpx; border-radius: 50%; background: var(--bg-dark); display: flex; align-items: center; justify-content: center; margin-bottom: 20rpx; }
.avatar-txt { font-family: var(--font-serif); font-size: 56rpx; font-weight: 600; color: #FDFCFB; }
.name { font-family: var(--font-serif); font-size: 36rpx; font-weight: 600; color: var(--text-main); margin-bottom: 8rpx; }
.uid { font-size: 24rpx; color: var(--text-light); }

// 数据行
.data-row { display: flex; gap: 16rpx; padding: 32rpx 28rpx; }
.data-card {
  flex: 1; background: var(--bg-paper); border: 1px solid var(--border-light);
  border-radius: var(--radius-lg); padding: 32rpx 0; display: flex;
  flex-direction: column; align-items: center; box-shadow: var(--shadow-sm);
}
.data-num { font-family: var(--font-serif); font-size: 48rpx; font-weight: 600; color: var(--text-main); }
.data-lbl { font-size: 24rpx; color: var(--text-light); margin-top: 8rpx; }

// 菜单
.menu { background: var(--bg-paper); margin: 0 28rpx 24rpx; border: 1px solid var(--border-light); border-radius: var(--radius-lg); overflow: hidden; box-shadow: var(--shadow-sm); }
.menu-item { display: flex; align-items: center; justify-content: space-between; padding: 36rpx 28rpx; }
.menu-item + .menu-item { border-top: 1px solid var(--border-light); }
.menu-item:active { background: var(--bg-surface); }
.mi-left { display: flex; align-items: center; gap: 20rpx; }
.mi-icon { font-size: 36rpx; }
.mi-text { font-size: 28rpx; font-weight: 500; color: var(--text-main); }
.mi-right { display: flex; align-items: center; gap: 12rpx; }
.mi-arrow { font-size: 36rpx; color: #ccc; }
.mi-hint { font-size: 24rpx; color: var(--text-light); }
.mi-hint.configured { color: var(--color-success); font-weight: 600; }

// 弹窗
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 999; display: flex; align-items: center; justify-content: center; padding: 40rpx; }
.modal-card { background: var(--bg-paper); border-radius: var(--radius-xl); padding: 48rpx 36rpx; width: 100%; max-width: 600rpx; }
.modal-title { font-family: var(--font-serif); font-size: 32rpx; font-weight: 600; color: var(--text-main); display: block; margin-bottom: 12rpx; }
.modal-desc { font-size: 24rpx; color: var(--text-light); display: block; line-height: 1.8; margin-bottom: 32rpx; }
.form-item { margin-top: 28rpx; }
.form-label { font-size: 26rpx; font-weight: 600; color: var(--text-main); display: block; margin-bottom: 12rpx; }
.form-input { border: 1px solid var(--border-medium); border-radius: var(--radius-md); padding: 20rpx 24rpx; font-size: 26rpx; background: var(--bg-surface); width: 100%; box-sizing: border-box; }
.model-opts { display: flex; gap: 0; background: var(--bg-surface); border-radius: var(--radius-sm); overflow: hidden; width: fit-content; }
.model-opt { font-size: 24rpx; padding: 12rpx 32rpx; color: var(--text-light); }
.model-opt.active { background: var(--bg-dark); color: #fff; }
.form-picker { border: 1px solid var(--border-medium); border-radius: var(--radius-md); padding: 20rpx 24rpx; font-size: 26rpx; background: var(--bg-surface); color: var(--text-main); }
.modal-btns { display: flex; gap: 16rpx; margin-top: 40rpx; }
.mbtn { flex: 1; height: 80rpx; border: none; border-radius: var(--radius-xl); font-size: 28rpx; font-weight: 600; }
.mbtn.cancel { background: var(--bg-surface); color: var(--text-muted); }
.mbtn.save { background: var(--bg-dark); color: #fff; }

// PC 端
@media (min-width: 1025px) {
  .profile { display: flex; flex-direction: column; align-items: center; }
  .head, .data-row, .menu { width: 100%; max-width: 600px; padding-left: 0; padding-right: 0; }
  .menu { margin-left: 0; margin-right: 0; }
  .data-row { padding-left: 0; padding-right: 0; }
}
</style>
```

- [ ] **Step: 提交**

```bash
git add pages/profile/profile.vue
git commit -m "feat: redesign profile page in Warm Tech style"
```

---

### Task 6: 重设计登录页 login.vue

**Files:**
- Modify: `pages/login/login.vue`

- [ ] **Step: 极简居中登录页**

移除蓝色渐变背景，用暖杏底色 + 衬线标题 + 深色按钮：

```scss
<style lang="scss" scoped>
.login { display: flex; align-items: center; justify-content: center; min-height: 100vh; background: var(--bg-canvas); padding: 40rpx; }
.login-card { background: var(--bg-paper); border: 1px solid var(--border-light); border-radius: var(--radius-xl); padding: 80rpx 60rpx; width: 100%; max-width: 600rpx; display: flex; flex-direction: column; align-items: center; box-shadow: var(--shadow-md); }
.brand { font-family: var(--font-serif); font-size: 48rpx; font-weight: 600; color: var(--text-main); letter-spacing: -1px; margin-bottom: 12rpx; }
.desc { font-size: 28rpx; color: var(--text-muted); margin-bottom: 80rpx; text-align: center; }
.login-btn { width: 100%; height: 96rpx; background: var(--bg-dark); color: #fff; font-size: 32rpx; font-weight: 600; border-radius: var(--radius-xl); border: none; display: flex; align-items: center; justify-content: center; }
.login-btn:active { opacity: 0.9; transform: scale(0.98); }
.privacy { font-size: 24rpx; color: var(--text-light); margin-top: 32rpx; text-align: center; }
</style>
```

```vue
<template>
  <view class="login">
    <view class="login-card">
      <text class="brand">Mianmian.</text>
      <text class="desc">AI 模拟面试 · 智能刷题<br>专为计算机学生打造</text>
      <button class="login-btn" open-type="getUserInfo" @click="handleLogin">登录</button>
      <text class="privacy">登录即同意《用户协议》和《隐私政策》</text>
    </view>
  </view>
</template>
```

- [ ] **Step: 提交**

```bash
git add pages/login/login.vue
git commit -m "feat: redesign login page in Warm Tech style"
```

---

### Task 7: 重设计简历页（upload + report + history）

**Files:**
- Modify: `pages/resume/upload.vue`
- Modify: `pages/resume/report.vue`
- Modify: `pages/resume/history.vue`

- [ ] **Step 1: upload.vue — 上传页**

上传区域：大面积虚线框 + 图标居中：

```scss
<style lang="scss" scoped>
.resume-upload { min-height: 100vh; background: var(--bg-canvas); padding: 40rpx 28rpx; }
.page-title { font-family: var(--font-serif); font-size: 36rpx; font-weight: 600; color: var(--text-main); margin-bottom: 12rpx; }
.page-desc { font-size: 26rpx; color: var(--text-muted); margin-bottom: 48rpx; line-height: 1.6; }

.upload-zone {
  border: 2px dashed var(--border-medium); border-radius: var(--radius-xl);
  padding: 80rpx 40rpx; text-align: center; background: var(--bg-paper);
  margin-bottom: 32rpx;
}
.upload-zone:active { background: var(--bg-surface); }
.upload-icon { font-size: 80rpx; display: block; margin-bottom: 24rpx; }
.upload-text { font-size: 28rpx; font-weight: 500; color: var(--text-main); display: block; margin-bottom: 8rpx; }
.upload-hint { font-size: 24rpx; color: var(--text-light); display: block; }

.form-section { background: var(--bg-paper); border: 1px solid var(--border-light); border-radius: var(--radius-lg); padding: 32rpx 28rpx; margin-bottom: 24rpx; box-shadow: var(--shadow-sm); }
.form-label { font-size: 26rpx; font-weight: 600; color: var(--text-main); margin-bottom: 12rpx; display: block; }
.form-textarea { width: 100%; min-height: 150rpx; background: var(--bg-surface); border: 1px solid var(--border-medium); border-radius: var(--radius-md); padding: 20rpx; font-size: 26rpx; color: var(--text-main); }

.submit-btn { width: 100%; height: 96rpx; background: var(--bg-dark); color: #fff; font-size: 32rpx; font-weight: 600; border-radius: var(--radius-xl); border: none; display: flex; align-items: center; justify-content: center; }
.submit-btn[disabled] { opacity: 0.4; }

@media (min-width: 1025px) {
  .resume-upload { max-width: 700px; margin: 0 auto; }
}
</style>
```

- [ ] **Step 2: report.vue — 诊断报告页**

参照设计稿 mockup 4，分数圆圈 + 诊断卡片 + before-after：

```scss
<style lang="scss" scoped>
.report { min-height: 100vh; background: var(--bg-canvas); padding: 40rpx 28rpx; }
.report-header { display: flex; gap: 24rpx; align-items: flex-start; margin-bottom: 40rpx; }
.score-circle { width: 140rpx; height: 140rpx; border-radius: 28rpx; background: #F0FDF4; border: 1px solid #BBF7D0; display: flex; flex-direction: column; align-items: center; justify-content: center; flex-shrink: 0; }
.score-num { font-size: 48rpx; font-weight: 700; color: #16A34A; letter-spacing: -1px; }
.score-max { font-size: 22rpx; color: #15803D; font-weight: 500; }
.report-verdict { font-size: 30rpx; font-weight: 600; color: var(--text-main); margin-bottom: 8rpx; }
.report-summary { font-size: 26rpx; color: var(--text-muted); line-height: 1.6; }

.diag-card { background: var(--bg-paper); border: 1px solid var(--border-light); border-radius: var(--radius-lg); padding: 28rpx; margin-bottom: 24rpx; box-shadow: var(--shadow-sm); }
.diag-title { font-size: 28rpx; font-weight: 600; color: var(--text-main); margin-bottom: 12rpx; display: flex; align-items: center; gap: 8rpx; }
.diag-original { font-size: 26rpx; color: var(--text-muted); line-height: 1.6; margin-bottom: 16rpx; }
.diag-suggestion { background: var(--bg-surface); border-radius: var(--radius-md); padding: 20rpx; border-left: 4rpx solid var(--color-success); }
.diag-suggestion-label { font-size: 22rpx; color: var(--text-light); margin-bottom: 8rpx; display: block; }
.diag-suggestion-text { font-size: 26rpx; color: var(--text-main); line-height: 1.6; }

.export-btn { width: 100%; height: 96rpx; background: var(--bg-dark); color: #fff; font-size: 32rpx; font-weight: 600; border-radius: var(--radius-xl); border: none; display: flex; align-items: center; justify-content: center; gap: 12rpx; box-shadow: var(--shadow-md); margin-top: 32rpx; }

@media (min-width: 1025px) {
  .report { max-width: 700px; margin: 0 auto; }
}
</style>
```

- [ ] **Step 3: history.vue — 列表页**

```scss
<style lang="scss" scoped>
.history { min-height: 100vh; background: var(--bg-canvas); padding: 24rpx 28rpx; }
.history-item {
  background: var(--bg-paper); border: 1px solid var(--border-light); border-radius: var(--radius-lg);
  padding: 28rpx; margin-bottom: 16rpx; box-shadow: var(--shadow-sm);
}
.history-item:active { background: var(--bg-surface); }
.history-title { font-size: 28rpx; font-weight: 500; color: var(--text-main); display: block; margin-bottom: 8rpx; }
.history-meta { font-size: 24rpx; color: var(--text-light); display: flex; gap: 24rpx; }
.empty { text-align: center; padding-top: 200rpx; color: var(--text-light); font-size: 28rpx; }

@media (min-width: 1025px) {
  .history { max-width: 700px; margin: 0 auto; }
}
</style>
```

- [ ] **Step 4: 提交**

```bash
git add pages/resume/
git commit -m "feat: redesign resume pages in Warm Tech style"
```

---

### Task 8: 重设计题库/试卷/刷题/错题本（4 个列表页）

**Files:**
- Modify: `pages/question/list.vue`
- Modify: `pages/exam/index.vue`
- Modify: `pages/exam/do.vue`
- Modify: `pages/practice/practice.vue`
- Modify: `pages/wrong-book/wrong-book.vue`

这些页面共用相似的列表卡片模式，统一处理。

- [ ] **Step 1: 读取现有页面**

先读取所有页面的现有样式，确保修改时不丢失功能。

```bash
# 阅读现有文件，理解当前模板和数据绑定
```

- [ ] **Step 2: 批量更新为 Warm Tech 卡片风格**

所有列表页统一模式：
- 页面背景：`var(--bg-canvas)`
- 项目卡片：`var(--bg-paper)` + `border: 1px solid var(--border-light)` + `border-radius: var(--radius-lg)` + `box-shadow: var(--shadow-sm)`
- 标题：`font-weight: 500; color: var(--text-main)`
- 辅助信息：`color: var(--text-light)`
- 强调元素：`color: var(--accent)` 或 `color: var(--color-success)`
- 点击态：`:active { background: var(--bg-surface); }`
- PC 端：`max-width: 800px; margin: 0 auto;`

以 `question/list.vue` 为示例：

```scss
<style lang="scss" scoped>
.question-list { min-height: 100vh; background: var(--bg-canvas); padding: 24rpx 28rpx; }

.filter-bar { display: flex; gap: 12rpx; margin-bottom: 24rpx; flex-wrap: wrap; }
.filter-tag {
  padding: 10rpx 24rpx; border: 1px solid var(--border-light); border-radius: var(--radius-full);
  font-size: 24rpx; color: var(--text-muted); background: var(--bg-paper);
}
.filter-tag.active { background: var(--bg-dark); color: #fff; border-color: var(--bg-dark); }

.q-card {
  background: var(--bg-paper); border: 1px solid var(--border-light);
  border-radius: var(--radius-lg); padding: 28rpx; margin-bottom: 16rpx;
  box-shadow: var(--shadow-sm);
}
.q-card:active { background: var(--bg-surface); }
.q-header { display: flex; align-items: center; gap: 12rpx; margin-bottom: 12rpx; }
.q-type { font-size: 20rpx; font-weight: 600; padding: 4rpx 12rpx; border-radius: var(--radius-sm); }
.q-type.single { background: #EEF2FF; color: #4F46E5; }
.q-type.multi { background: #FEF3C7; color: #D97706; }
.q-type.judge { background: #FCE7F3; color: #DB2777; }
.q-type.fill { background: #DCFCE7; color: #16A34A; }
.q-title { font-size: 28rpx; font-weight: 500; color: var(--text-main); line-height: 1.6; flex: 1; }
.q-tags { display: flex; gap: 8rpx; flex-wrap: wrap; }
.q-tag { font-size: 20rpx; color: var(--text-light); padding: 4rpx 12rpx; background: var(--bg-surface); border-radius: var(--radius-sm); }
.q-difficulty { font-size: 20rpx; margin-left: auto; }
.q-difficulty.easy { color: var(--color-success); }
.q-difficulty.medium { color: var(--accent); }
.q-difficulty.hard { color: var(--color-danger); }

@media (min-width: 1025px) {
  .question-list { max-width: 800px; margin: 0 auto; }
}
</style>
```

- [ ] **Step 3: 对其他页面应用相同模式**

`exam/index.vue`、`practice/practice.vue`、`wrong-book/wrong-book.vue`、`exam/do.vue` 同理，保持数据绑定不变，只替换样式。

- [ ] **Step 4: 提交**

```bash
git add pages/question/ pages/exam/ pages/practice/ pages/wrong-book/
git commit -m "feat: redesign question/exam/practice/wrong-book pages in Warm Tech style"
```

---

### Task 9: 重设计面试报告和历史页

**Files:**
- Modify: `pages/interview/report.vue`
- Modify: `pages/interview/history.vue`

- [ ] **Step: Warm Tech 风格重写**

报告页：评分圆环 + 维度卡片 + 建议区，暖杏底 + 深色文本。
历史页：复用简历历史的卡片模式。

```bash
git add pages/interview/report.vue pages/interview/history.vue
git commit -m "feat: redesign interview report/history in Warm Tech style"
```

---

### Task 10: 清理和验证

**Files:**
- Modify: `components/DeepStatusBar.vue` — 清理或移除

- [ ] **Step 1: 清理 DeepStatusBar**

当前 DeepStatusBar 被大量冗余使用（几乎每个元素前都插了一个），这是 AI 生成代码的典型问题。有两种处理方式：

**方案 A**（推荐）：移除所有重复的 `<DeepStatusBar />` 调用，只保留 App.vue 根级一个。
**方案 B**：保留组件但清空内容。

选择方案 A，在之前重写页面 template 时已经移除了。

- [ ] **Step 2: HBuilderX 编译验证**

```bash
# 在 HBuilderX 中运行到小程序模拟器 + 发行 H5，确认无报错
```

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "feat: final Warm Tech cleanup and verification"
```
