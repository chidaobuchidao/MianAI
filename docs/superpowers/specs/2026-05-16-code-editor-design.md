# 面试代码编辑器 — 设计规格

> 日期: 2026-05-16 | 设计稿: Section 04 PC 三栏布局

## 一、概述

在 AI 模拟面试 PC 端三栏布局中增加右侧代码编辑器面板，用于面试中的代码编写与审查。

## 二、布局规格（严格对齐设计稿）

### PC 三栏布局

```
┌─ Sidebar(280px) ─┬─ Chat(flex:1) ─┬─ CodePanel(420px) ─┐
│ Mianmian.         │ AI 面试官·Kevin│ ● ● ●  AQS.java  ▦ │
│ Java后端面试       │                 │                      │
│                   │ 请分析右侧代码... │  1 │ public final   │
│ Progress          │                 │  2 │   void acquire │
│ ● Completed       │                 │  3 │   (int arg) {  │
│ ● Active          │ [输入框...] [→]  │  4 │   if (!try...  │
│ ○ Pending         │                 │    │                │
└───────────────────┴─────────────────┴────────────────────┘
```

### CodePanel 样式（来自设计稿 CSS）

| 属性 | 值 |
|------|-----|
| 宽度 | `420px` |
| 背景 | `var(--bg-dark)` / `#141413` |
| 左边框 | `1px solid var(--border-medium)` |
| 头部高度 | `44px`，背景 `rgba(255,255,255,0.05)` |
| 字体 | `JetBrains Mono` 12px |
| 行号色 | `#888` |
| 语法色 | 关键字 `#79C0FF` / 方法 `#D2A8FF` / 注释 `#8B949E` / 字符串 `#A5D6FF` / 类型 `#FFA657` |

### 头部元素

- 左侧：macOS 三色圆点（红 #ED6A5E / 黄 #F4BF4F / 绿 #61C554）
- 中间：文件名（11px, #888, JetBrains Mono）
- 右侧：行/列切换图标（SVG）

### 代码区

- 行号 + 代码编辑区
- 暗色背景，CodeMirror oneDark 主题适配上述语法色
- 底部闪烁光标模拟可编辑状态

## 三、组件结构

```
CodeEditor.vue (新)
├── .code-panel-head     ← 三色圆点 + 文件名 + 图标
└── .code-panel-body     ← CodeMirror 6 编辑器实例

InterviewView.vue (改)
├── .sidebar             ← 不变
├── .chat-main           ← 不变
└── .code-panel          ← 新增：嵌入 CodeEditor
```

## 四、CodeEditor 组件接口

```typescript
interface Props {
  modelValue: string      // v-model 代码内容
  language: string        // 'java' | 'python' | 'javascript'
  filename?: string       // 显示在标题栏
  readonly?: boolean      // 只读模式（AI展示代码时）
}

interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'change', value: string): void
}
```

## 五、技术选型

- **CodeMirror 6**（`@codemirror/view` + `@codemirror/state` + `@codemirror/lang-java` + `@codemirror/lang-javascript` + `@codemirror/lang-python`）
- **oneDark 主题**（`@codemirror/theme-one-dark`），适配 Warm Tech 语法色
- 动态 import 按需加载（~200KB gzipped）

## 六、面试流程（PC 侧栏进度）


进度树严格按 5 阶段展示：

```
● 自我介绍与破冰         Completed (4m)
● 项目/技术面考察         In Progress...
○ 项目深度问答            Pending
○ 笔试编程环节 [进入]     Pending   ← 用户可自主选择
○ 面试总结                Pending
```

第 4 阶段"笔试编程环节"旁有 **[进入]** 按钮：
- 用户点击 → 激活右侧代码编辑器，通知 AI 进入编程模式
- 用户不点击 → 正常进行到第 5 阶段，跳过编程

仅 PC 端生效（`v-if="isDesktop"`），小程序端不受影响。

## 七、面试流程集成

1. AI 出编程题 → `messages[i].code` 非空 → 编辑器激活，加载初始代码
2. 用户在编辑器中写代码 → 内容通过 `v-model` 绑定
3. 提交回答时附带 `code` 字段 → SSE 流式发送给 AI
4. AI 返回审查结果 → 编辑器可切为只读展示

## 七、影响范围

| 文件 | 操作 | 说明 |
|------|------|------|
| `components/CodeEditor.vue` | 新建 | 代码编辑器组件 |
| `views/InterviewView.vue` | 修改 | PC 布局增加 `.code-panel` |
| `composables/useInterviewStream.ts` | 修改 | 回答时附带 code 字段 |
| `package.json` | 修改 | 新增 CodeMirror 依赖 |
