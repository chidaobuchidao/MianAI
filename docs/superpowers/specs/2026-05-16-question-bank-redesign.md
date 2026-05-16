# 题库与刷题功能重构 — 设计规格

> 日期: 2026-05-16 | 状态: 待实施

## 一、概述

两大功能模块互换定位：

| 模块 | 旧名称 | 新名称 | 用途 |
|------|--------|--------|------|
| 原 Practice | 自由刷题 | **查看题库** | 分类浏览全部题目，逐题精学 |
| 原 Exam | 在线试卷 | **自由刷题** | 随机组卷或按专题专项练习 |

## 二、路由变更

| 旧路由 | 新路由 | 新名称 | 视图文件 |
|--------|--------|--------|----------|
| `/practice` | `/questions` | 查看题库(列表) | QuestionsView.vue |
| — | `/questions/:id` | 题目详情 | QuestionDetailView.vue |
| `/exam` | `/practice` | 自由刷题(入口) | PracticeView.vue |
| `/exam/do` | `/practice/do` | 刷题中 | PracticeDoView.vue |

## 三、页面设计

### 3.1 查看题库 — 列表 (`/questions`)

沿用现有设计稿 Section 08 布局：
- 返回栏标题"查看题库"
- 分类标签 chip-row 横向滚动（全部 / Java基础 / 并发 / Redis / MySQL / 网络协议...）
- 题目列表 q-card：Q1, Q2... 显示题号 + 题干 + 分类·难度
- 点击 q-card 跳转 `/questions/:id?categoryId=X`
- 分页加载，滚动触底加载更多

### 3.2 查看题库 — 详情 (`/questions/:id`)

- 返回栏标题"题目详情"，右侧显示当前分类名
- 题目卡片：题型标签 + 题干 + 全部选项
  - 正确选项：`--color-success` 绿色边框 + `rgba(34,197,94,0.04)` 底色
- 答案解析卡片：纸白卡片 + "答案解析" 标题 + 解析文本
- 标签行：分类 tag + 难度 tag
- 底部操作栏：`< 上一题` / `下一题 >` 按钮
  - 按当前分类列表顺序切换（从列表页传入 categoryId 上下文）
  - 到达边界时对应按钮禁用

### 3.3 自由刷题 — 入口 (`/practice`)

替代原"在线试卷"页面，双入口设计：
- 返回栏标题"自由刷题"
- hero 区域：label "自由练" + title "自由刷题" + desc "随机组卷或按专题专项突破"
- **入口卡片一：随机组卷**
  - 图标 + "随机组卷" + "从题库随机抽取，模拟真实考试"
  - 题量选择器：[5] [10] [15] [20]（选中项琥珀色高亮）
  - "开始"按钮，跳转 `/practice/do?mode=random&count=N`
- **入口卡片二：按专题刷题**
  - 图标 + "按专题刷题" + "选择分类，集中突破薄弱环节"
  - 分类标签 chip-row，点击选中后跳转 `/practice/do?mode=topic&categoryId=X`
- 下方"最近记录"区域：刷题历史列表

### 3.4 刷题中 (`/practice/do`)

复用现有 ExamDoView 答题逻辑：
- 进度条 + 题号计数
- 题目卡片 + 选项 + 判分结果
- 上一题 / 下一题 / 提交（交卷）
- 完成屏：得分环 + 评语 + 再刷一次 / 返回

## 四、首页入口卡片

| 位置 | 旧文案 | 新文案 |
|------|--------|--------|
| 卡片3 | 在线试卷 / 限时模拟考试 | 自由刷题 / 随机组卷 · 按专题练习 |
| 卡片4 | 自由刷题 / 按分类随机练习 | 查看题库 / 分类浏览 · 逐题精学 |

图标和排列位置不变。

## 五、后端 API

无需改动。现有接口已覆盖：

- `GET /api/questions?categoryId=&difficulty=&type=&page=&size=` — 分类分页列表
- `GET /api/questions/{id}` — 题目详情
- `GET /api/questions/random?categoryId=&difficulty=&size=` — 随机抽题
- `GET /api/questions/categories` — 分类列表
- `POST /api/answers` — 提交答案（判分）

## 六、设计约束

- 严格遵循 Warm Tech 设计令牌（bg-canvas #F3EFE8, bg-paper #FDFCFB, accent #D9750A, font-serif Georgia, font-sans Inter）
- 不使用 emoji 图标，统一用 SVG
- 组件风格对齐现有 func-card、q-card、question-card、option-btn、chip-row 等
- 支持移动端 375px 和 PC 端 700px max-width 布局
