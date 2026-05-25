# 面面通 Web 端 — 移动端布局适配开发文档

> 基于设计稿 `docs/superpowers/specs/2026-05-25-responsive-design.html`
> 状态: **待实施** | 最后更新: 2026-05-25

---

## 零、核心铁律

**本适配工作的唯一目标：改变布局，不改变任何功能。**

违反此原则 = 引入 bug。以下规则无例外：

| 规则 | 说明 |
|------|------|
| **禁止修改 `<script setup>` 业务逻辑** | 不增删任何状态变量、computed、watch、函数签名、API 调用、路由跳转逻辑、事件处理逻辑 |
| **禁止修改 `<template>` 条件渲染逻辑** | `v-if` / `v-for` / `v-show` 的条件表达式不变，只能调整 DOM 结构和 class |
| **禁止修改 props / emits / slots 接口** | 组件对外契约不变 |
| **禁止修改 composables / stores** | `useStreamPolish.ts`、`userStore` 等零改动 |
| **禁止修改 router / auth guard** | 路由定义和权限守卫完全不动 |

**允许做的事（仅限这些）：**

| 允许 | 方式 |
|------|------|
| 增删 CSS 规则（含 media query） | `<style scoped>` 块内 |
| 给元素新增 class | 仅在 `<template>` 标签上添加 `:class` 绑定或静态 class |
| 包裹元素（仅为了布局） | 用纯布局 `<div>` 包裹，不引入任何条件逻辑 |
| 新增 `isDesktop` / `isMobile` 响应式 ref | 只能用于 class 绑定，不能用于 `v-if` 功能切换 |
| 引入 composable `useResponsive()` | 可选，全局复用窗口宽度检测 |

---

## 一、断点体系

```
< 768px         窄屏移动端   单列全屏布局
≥ 768px         宽屏 PC 端   居中宽栏 / 多栏
≥ 1024px        面试三栏     侧栏 + 对话 + 代码
1280px 固定     论文工具     50/50 分屏 或 Bento
```

**断点必须使用 CSS 变量或统一常量，禁止各处硬编码数字。**

### 1.1 断点检测 composable（新增）

```typescript
// src/composables/useResponsive.ts
import { ref, onMounted, onUnmounted } from 'vue'

export function useResponsive() {
  const width = ref(window.innerWidth)
  const isDesktop = computed(() => width.value >= 768)
  const isWide = computed(() => width.value >= 1024)

  function onResize() { width.value = window.innerWidth }

  onMounted(() => window.addEventListener('resize', onResize))
  onUnmounted(() => window.removeEventListener('resize', onResize))

  return { width, isDesktop, isWide }
}
```

> **注意**：当前 `InterviewView.vue:400` 的 `isDesktop` 仅读取一次 `window.innerWidth > 768`，未监听 resize。本次适配必须修复为响应式（加 resize 监听），否则窗口拖拽时布局不更新。

---

## 二、逐页适配清单

### 2.1 首页 `HomeView.vue`

**当前状态**：已有单列居中 `max-width: 820px`，基本适合移动端。

| # | 适配项 | 移动端 (<768px) | PC 端 (≥768px) |
|---|--------|-----------------|-----------------|
| 1 | 功能卡片网格 | 2×2 grid（现状） | 2×2 grid，卡片更大 |
| 2 | Hero Card | 全宽 280px高（现状） | 310px高 |
| 3 | Lanyard 公告 | 全宽（现状） | 居中 520px |
| 4 | ToolPicker 模态 | 全宽 2列（现状） | 居中 3列 |

**不改动**：`DecryptedText` 动画、`GlareCard` 光标跟随、`PixelCard` 像素边框、`ParticleBg` 粒子背景、路由跳转逻辑、公告拉环交互。

**实施方式**：纯 CSS media query，不修改 `<template>` 结构。

---

### 2.2 登录页 `LoginView.vue`

| # | 适配项 | 移动端 (<768px) | PC 端 (≥768px) |
|---|--------|-----------------|-----------------|
| 1 | 登录卡片 | 全屏居中（现状） | 居中 400px 宽 |
| 2 | 登录/注册 tab | 全宽 | 居中 |
| 3 | Brand 字号 | 32px（现状） | 40px |

**实施方式**：纯 CSS。无 `<template>` 改动。

---

### 2.3 AI 面试 `InterviewView.vue`（最复杂）

**当前状态**：已有 `isDesktop` 变量控制三栏/单栏，但 resize 未监听。

| # | 适配项 | 移动端 (<768px) | PC 端 (≥768px) |
|---|--------|-----------------|-----------------|
| 1 | `isDesktop` 检测 | 修复为响应式 resize 监听 | - |
| 2 | 侧边栏 `aside.sidebar` | 隐藏 (`v-if="isDesktop"` 已有) | 显示 280px |
| 3 | 代码面板 `aside.code-panel` | 全屏浮层替代（见下方方案） | 侧边常驻 420px 可拖拽 |
| 4 | 顶部栏 | 返回按钮 + 标题 + 结束胶囊 | 标题 + 结束胶囊 |
| 5 | 聊天消息区 | 全宽 padding 20/16 | padding 24/32 |
| 6 | 输入栏 | 全宽胶囊（现状） | 全宽胶囊 |
| 7 | 岗位选择卡片 | 全屏居中（现状） | 居中 |

**3.1 代码面板移动端方案（仅布局，不改功能）**：

移动端无侧边代码面板。当用户进入编程环节或点击「查看代码」时，代码编辑器以 **全屏浮层** 展示——编辑器组件本身（`CodeEditor.vue`）及运行/提交按钮不改变，仅包裹一个全屏容器：

```html
<!-- 伪代码：移动端代码浮层 -->
<div class="code-overlay" v-if="!isDesktop && showCodePanel">
  <div class="code-overlay__header">
    <span>{{ currentFilename }}</span>
    <button @click="showCodePanel = false">关闭</button>
  </div>
  <!-- CodeEditor 组件原样嵌入 -->
  <CodeEditor ... />
  <!-- 运行/提交按钮原样嵌入 -->
</div>
```

**不改动**：`CodeEditor.vue` 组件、Piston 代码执行、SSE stream、语音/键盘切换、消息渲染、结束面试流程、进度树状态。

**实施方式**：修复 `isDesktop` 响应式 + 新增 `.code-overlay` 浮层（纯布局壳）+ 媒体查询调整 padding/字号。**所有 `v-if` 条件不变——`isDesktop` 只影响 DOM 排列方式，不影响什么内容出现。**

---

### 2.4 面试报告 `InterviewReportView.vue`

| # | 适配项 | 移动端 (<768px) | PC 端 (≥768px) |
|---|--------|-----------------|-----------------|
| 1 | 评分环 + 结论 | 水平排列（现状） | 居中，字号更大 |
| 2 | 维度卡片 | 单列（现状） | 最大宽 640px |
| 3 | 双 Tab（面试/笔试） | 全宽 pill toggle（现状） | 居中 |

**不改动**：评分计算、SSE 加载、tab 切换逻辑。

**实施方式**：纯 CSS media query。

---

### 2.5 面试历史 `InterviewHistoryView.vue`

| # | 适配项 | 移动端 (<768px) | PC 端 (≥768px) |
|---|--------|-----------------|-----------------|
| 1 | 列表 | 单列（现状） | 最大宽 640px 居中 |

**实施方式**：无模板改动。现有布局已适配。

---

### 2.6 简历上传 `ResumeUploadView.vue`

| # | 适配项 | 移动端 (<768px) | PC 端 (≥768px) |
|---|--------|-----------------|-----------------|
| 1 | 上传区域 | 全宽（现状） | 最大宽 640px 居中 |
| 2 | 模型切换 | 居中（现状） | 居中 |
| 3 | JD 输入框 | 全宽 | 全宽 |

**不改动**：`GlareCard` 拖拽上传、文件解析、模型切换。

**实施方式**：纯 CSS。

---

### 2.7 简历诊断报告 `ResumeReportView.vue`

| # | 适配项 | 移动端 (<768px) | PC 端 (≥768px) |
|---|--------|-----------------|-----------------|
| 1 | 暗色评分 Hero | 全宽 `margin: 0 -20px`（现状） | 全宽 |
| 2 | 维度卡片 / 关键词 / 深度优化 | 单列 padding 20px（现状） | 最大宽 640px |
| 3 | 深度优化 Diff 面板 | 全宽（现状） | 全宽 |

**不改动**：`GridScan` 动画、评分环动画、维度条动画、深度优化 SSE 流、`UnifiedDiff` 组件、导出逻辑。

**实施方式**：纯 CSS。

---

### 2.8 简历记录 `ResumeHistoryView.vue`

已适配（单列列表 + 空状态）。纯 CSS 调整 max-width。

---

### 2.9 自由刷题入口 `PracticeView.vue`

| # | 适配项 | 移动端 (<768px) | PC 端 (≥768px) |
|---|--------|-----------------|-----------------|
| 1 | 组卷卡片（随机/专题） | 全宽（现状） | 最大宽 640px |
| 2 | 数量选择 chips | 均分（现状） | 均分 |
| 3 | 历史记录 | 单列（现状） | 单列 |

**不改动**：`TopicChip` 组件、路由跳转、随机组卷逻辑。

**实施方式**：纯 CSS。

---

### 2.10 刷题中 `PracticeDoView.vue`

| # | 适配项 | 移动端 (<768px) | PC 端 (≥768px) |
|---|--------|-----------------|-----------------|
| 1 | 进度条 | 全宽（现状） | 最大宽 640px |
| 2 | 选项按钮 | 全宽（现状） | 全宽 |
| 3 | 完成覆盖层 | 全屏（现状） | 居中卡片 |

**不改动**：`QuestionContent` 组件、判分逻辑、选项选择、prev/next 导航。

**实施方式**：纯 CSS。

---

### 2.11 查看题库 `QuestionsView.vue` / 题目详情 `QuestionDetailView.vue`

已适配（单列列表 + chips 筛选 + 详情页）。纯 CSS 调整。

**不改动**：分类筛选、分页加载、`QuestionContent` 组件、上一题/下一题路由。

---

### 2.12 错题本 `WrongBookView.vue`

已适配（单列列表 + 错误次数标签 + 空状态）。无模板改动。

---

### 2.13 个人中心 `ProfileView.vue`

| # | 适配项 | 移动端 (<768px) | PC 端 (≥768px) |
|---|--------|-----------------|-----------------|
| 1 | 统计卡片 | 3列 grid（现状） | 3列，最大宽 500px |
| 2 | 菜单组 | 全宽（现状） | 全宽 |
| 3 | API Key 模态框 | 全屏居中（现状） | 居中 360px |

**不改动**：菜单路由跳转、Key 保存逻辑、配额显示。

**实施方式**：纯 CSS。

---

### 2.14 管理后台 `AdminView.vue`

**当前状态**：已有 `max-width: 800px` 和 `@media (max-width: 600px)` 将统计卡片改为 2 列。

| # | 适配项 | 移动端 (<768px) | PC 端 (≥768px) |
|---|--------|-----------------|-----------------|
| 1 | 统计卡片 | 2列 grid（已有） | 4列 |
| 2 | 表格 | 横向滚动 `.table-wrap`（已有） | 全宽 |
| 3 | 模态框 | 全屏 | 居中 |
| 4 | 操作按钮 | 换行（已有） | 单行 |

现有适配基本充分。验证即可。

**不改动**：用户表 CRUD、公告编辑/发布/删除、配额编辑、面试记录清空。

---

### 2.15 学术润色 `PolishView.vue`（重构为新布局）

**当前状态**：固定桌面布局（`max-width: 1280px` 卡片 + 50/50 分屏）。无任何移动端适配。

| # | 适配项 | 移动端 (<768px) | PC 端 (≥768px) |
|---|--------|-----------------|-----------------|
| 1 | 分屏模式 | **堆叠 + Tab 切换**原文/润色 | 50/50 并排（现状） |
| 2 | 顶部工具栏 | 换行（模型 + 按钮组） | 单行（现状） |
| 3 | 润色方式 Tab | 水平滚动或换行 | 居中 |
| 4 | 高级选项面板 | 下拉展开 | 下拉展开（现状） |
| 5 | 编辑器面板 | 全宽，Tab 切换原文/结果 | 左右两栏 |

**实现方案——必须保持功能完整**：

```html
<!-- 移动端：Tab 切换栈 -->
<template>
  <div class="app-shell" :class="{ 'app-shell--mobile': !isDesktop }">
    <!-- 工具栏完全相同，仅 CSS 调整 -->
    <header class="app-header">...</header>
    <div class="pro-toolbar">...</div>

    <!-- 移动端：Tab 切换 | PC 端：分屏并排 -->
    <template v-if="!isDesktop">
      <div class="mobile-tabs">
        <button :class="{ active: mobilePane === 'original' }" @click="mobilePane = 'original'">原文参考</button>
        <button :class="{ active: mobilePane === 'result' }" @click="mobilePane = 'result'">润色结果</button>
      </div>
      <div class="mobile-pane" v-show="mobilePane === 'original'">
        <!-- 左侧内容原样搬入 -->
      </div>
      <div class="mobile-pane" v-show="mobilePane === 'result'">
        <!-- 右侧内容原样搬入 -->
      </div>
    </template>

    <!-- PC 端：50/50 分屏完全保持现有结构 -->
    <div v-else class="split-50">...</div>
  </div>
</template>
```

> **关键约束**：两侧面板的 **所有功能** — SSE 流式渲染、LCS Diff 高亮、contenteditable 编辑、复制结果、格式保留导出、Flash/Pro 切换 — **一个不改，仅移动 DOM 位置**。`useStreamPolish` composable 零改动。

---

### 2.16 降AI检测 `AiReduceView.vue`（重构为新布局）

**当前状态**：固定 Bento 布局（`1fr + 340px`）。无移动端适配。

| # | 适配项 | 移动端 (<768px) | PC 端 (≥768px) |
|---|--------|-----------------|-----------------|
| 1 | 步骤流 | 压缩一行（保持 3 步） | 全宽（现状） |
| 2 | Bento 布局 | **单列堆叠**：先编辑区，再风险卡+洞察 | Bento（现状） |
| 3 | 扫描高亮 | 全宽，点击高亮→底部弹出洞察 | 左侧高亮+右侧洞察 |
| 4 | 风险评分环 | 置于顶部卡片内 | 右侧常驻 |
| 5 | 洞察面板 | 底部弹出 / 内嵌卡片 | 右侧常驻 |

**实施方式**：

```html
<template>
  <div class="app-shell" :class="{ 'app-shell--mobile': !isDesktop }">
    <!-- 顶部栏 / 工具栏 / 步骤流 / 高级选项 → 结构不变 -->
    <header>...</header>
    <div class="pro-toolbar">...</div>
    <div class="step-flow">...</div>

    <!-- 移动端：单列 | PC：Bento -->
    <div v-if="!isDesktop" class="mobile-stack">
      <div class="mobile-tabs">改写结果 / 文档扫描</div>
      <div class="doc-pane"><!-- 原文 + 高亮 --></div>
      <div class="risk-card"><!-- 评分环内嵌 --></div>
      <div class="insight-panel" v-if="activeInsight"><!-- 洞察 --></div>
    </div>
    <div v-else class="bento-layout"><!-- 现有 Bento 完全保留 --></div>
  </div>
</template>
```

**不改动**：AI 扫描逻辑、四级严重度检测、SSE 流式精修、高亮点击交互、AIGC 报告导入、Flash/Pro 切换。

---

### 2.17 降查重 `PlagiarismReduceView.vue`（重构为新布局）

**当前状态**：固定 50/50 分屏 + 统计卡片。无移动端适配。

| # | 适配项 | 移动端 (<768px) | PC 端 (≥768px) |
|---|--------|-----------------|-----------------|
| 1 | 统计卡片行 | 3列压缩（查重率/短语/片段） | 5列（现状） |
| 2 | 分屏模式 | **堆叠 + Tab 切换** | 50/50（现状） |
| 3 | 风险检测问题 chips | 换行 | 换行（现状） |
| 4 | 匹配片段面板 | 内嵌原文下方 | 原文内下方 |

**实施方式**：与 PolishView 相同的 Tab 切换策略。`useStreamPolish` composable 零改动。

**不改动**：查重扫描、SSE 流式降重、contenteditable 编辑、查重报告导入、标记高亮（`dup-mark` / `dup-phrase` / `dup-report`）。

---

## 三、全局 CSS 适配规则

### 3.1 全局 `.page` 容器

当前多数页面使用 `.page > .page__inner` 结构（`max-width: 640px` 居中）。统一添加：

```css
.page__inner {
  max-width: 640px;
  margin: 0 auto;
  padding: 0 20px;
}

@media (min-width: 768px) {
  .page__inner {
    padding: 0 32px;
  }
}
```

### 3.2 触摸优化（移动端）

```css
@media (max-width: 767px) {
  /* 增大点击区域 */
  button, .chip, .option-btn, .pos-btn, .menu-item {
    min-height: 44px;  /* iOS HIG 推荐最小触摸目标 */
  }

  /* 输入框防止 iOS 缩放 */
  input, textarea, select {
    font-size: 16px;  /* 避免 iOS Safari 自动缩放 */
  }
}
```

### 3.3 安全区域

```css
/* iPhone X+ 底部安全区 */
.page {
  padding-bottom: env(safe-area-inset-bottom, 0px);
}
```

---

## 四、被禁止的改动（红线）

以下每一项如果被改动，**必须视为 bug** 并在 code review 中直接拒绝：

### 4.1 禁止改动的文件

| 文件 | 原因 |
|------|------|
| `src/router/index.ts` | 路由和守卫不动 |
| `src/stores/user.ts` | 认证状态不动 |
| `src/composables/useStreamPolish.ts` | SSE 流式逻辑不动 |
| `src/utils/*.ts` | 工具函数不动 |
| `tailwind.config.js` / `vite.config.ts` | 构建配置不动 |
| 所有 `components/*.vue`（除仅布局包裹） | 组件对外接口不动 |

### 4.2 禁止改动的逻辑

- **所有 `v-if` 条件表达式**：不修改任何业务状态的判断条件
- **所有 `@click` / `@input` 等事件处理器**：不修改函数体、不分拆、不合并
- **所有 API 调用**（`fetch*`、`axios`、`SSE`）：不动调用时机和参数
- **所有路由跳转**（`router.push` / `router.replace`）：不动
- **所有 model / provider 切换逻辑**（Flash/Pro 胶囊切换）：不动
- **所有动画逻辑**（GSAP、CSS animation trigger）：不动

### 4.3 禁止改动的标志

- 任何 `.vue` 文件 `<script setup>` 块的行数变化 > 5 行 → **必须人工复核**
- 任何 composable 文件被修改 → **直接拒绝**
- 任何 `v-if` 条件表达式被修改 → **直接拒绝**
- 任何新增 `import` 语句引入功能性依赖 → **直接拒绝**（仅允许引入 `useResponsive` composable）

---

## 五、验证清单

实施每页适配后，必须逐项验证以下功能不变：

### 5.1 通用验证（所有页面）

- [ ] 页面在 375px / 768px / 1024px / 1280px / 1440px 宽度下无水平溢出
- [ ] 所有按钮均可点击（无被遮挡、无层级问题）
- [ ] 所有输入框正常输入
- [ ] 无 console 错误（除 favicon.ico 404）
- [ ] 窗口拖动从宽到窄、窄到宽，布局平滑过渡无闪烁
- [ ] iOS Safari / Chrome / Firefox 各浏览器无差异

### 5.2 面试相关（InterviewView + Report + History）

- [ ] 岗位选择 → 启动面试 → AI 消息到达 → 用户输入 → 发送 → 下一轮
- [ ] 语音模式切换 → 录音 → 取消/发送
- [ ] 代码编辑器：输入 → 运行 → 提交审查 → 结果展示
- [ ] 结束面试 → 报告生成 → 报告页查看
- [ ] 双 Tab（面试报告/笔试报告）切换正常
- [ ] PC 三栏：侧栏进度树、对话区、代码面板均正常

### 5.3 简历相关

- [ ] 上传 PDF → 解析 → 诊断报告加载
- [ ] 深度优化：Flash/Pro 切换 → 开始 → SSE 流 → Diff 展示 → 面试追问
- [ ] 导出（保留格式 / 标准 Word）
- [ ] 简历记录列表 → 点击查看历史报告

### 5.4 刷题 / 题库 / 错题

- [ ] 随机组卷：选择数量 → 开始 → 答题 → 判分 → 下一题 → 完成覆盖
- [ ] 专题刷题：选分类 → 开始
- [ ] 题库列表：分类筛选 → 点击题目 → 详情页 → 上一题/下一题
- [ ] 错题本：列表展示 → 空状态

### 5.5 个人中心 / 管理后台

- [ ] 菜单点击跳转正常
- [ ] API Key 模态框 → 选择提供商 → 输入 → 保存
- [ ] 管理后台：用户 CRUD、配额编辑、角色升降、公告 CRUD

### 5.6 论文工具

- [ ] 学术润色：上传 → 选模式 → 开始 → SSE 流 → Diff 展示 → 导出
- [ ] 降AI检测：上传 → 扫描 → 点击高亮 → 洞察面板 → AI 精修 → 应用替换 → 一键净化
- [ ] 降查重：上传 → 扫描 → 统计卡片 → 匹配片段 → 开始降重 → SSE 流 → 导出

---

## 六、实施顺序建议

按复杂度和依赖关系排序：

| 优先级 | 页面 | 理由 |
|--------|------|------|
| P0 | 全局 CSS (`global.css` / `tokens.css`) | 统一断点变量、触摸优化、安全区 |
| P0 | `useResponsive.ts` composable | 所有页面依赖 |
| P1 | InterviewView.vue | 最复杂、断点逻辑需修复、影响面最大 |
| P2 | PolishView.vue | 分屏 → Tab 切换重构 |
| P3 | AiReduceView.vue | Bento → 单列 + 洞察弹出重构 |
| P4 | PlagiarismReduceView.vue | 分屏 → Tab 切换 + 统计压缩 |
| P5 | HomeView.vue / ResumeReportView.vue | 纯 CSS，风险低 |
| P6 | 其余所有页面 | 纯 CSS 微调，风险极低 |

> **每完成一个 P 级别，必须运行一次完整验证清单，确认无回归再进入下一级别。**
