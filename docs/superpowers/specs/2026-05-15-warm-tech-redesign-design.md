# Warm Tech UI 重设计规范

**日期**: 2026-05-15  
**基于**: 用户设计稿 `E:\My_Projects\Test_Lixm\2.html`  
**配色参考**: Claude.ai 暖杏色调  
**目标**: 去除 AI 模板味，适配移动端+PC 端，保留全部微交互动画

---

## 1. 设计 Token

### 1.1 色彩

```
--bg-canvas:    #F3EFE8  页面外部底色（暖杏）
--bg-paper:     #FDFCFB  卡片/界面主背景（纸白）
--bg-surface:   #F7F7F5  次级背景 / 输入框底色
--bg-dark:      #141413  深色卡片/按钮/代码块
--text-main:    #141413  主文本
--text-muted:   #4A4A4A  次要文本
--text-light:   #888888  辅助/占位文本
--accent:       #D9750A  强调色 / 按钮 / 链接（琥珀）
--color-success: #22C55E  成功/在线状态
--color-danger:  #EF4444  危险/错误
--border-light:  rgba(0, 0, 0, 0.06)
--border-medium: rgba(0, 0, 0, 0.10)

--shadow-sm:  0 1px 2px rgba(0,0,0,0.02)
--shadow-md:  0 4px 12px rgba(0,0,0,0.06)
--shadow-lg:  0 16px 32px rgba(0,0,0,0.10)
--shadow-xl:  0 24px 48px rgba(0,0,0,0.12)
```

### 1.2 字体

```
--font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif
--font-serif: 'Georgia', serif
--font-mono: 'JetBrains Mono', 'Fira Code', monospace
```

- 品牌标识（Mianmian.）使用衬线体 Georgia
- UI 正文使用无衬线 Inter / PingFang SC
- 代码块使用等宽 JetBrains Mono
- 加载 Google Fonts（H5），小程序使用系统字体 fallback

### 1.3 间距与圆角

```
--radius-sm:  8px   小元素（标签、按钮）
--radius-md:  12px  输入框、代码块
--radius-lg:  16px  普通卡片
--radius-xl:  20px  大卡片、设备框
--radius-full: 100px 胶囊标签、圆形头像
```

### 1.4 断点（响应式）

```
Mobile:   < 768px   单栏
Tablet:   768-1024px  双栏
Desktop:  > 1024px  三栏（面试页）、宽栏（首页）
```

---

## 2. 动画系统（从设计稿提取，全部保留）

### 2.1 光标闪烁

```css
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
.animate-blink { animation: blink 1s infinite; }
```

用途：AI 对话中正在生成文本的指示，录音中语音转文字的尾部光标

### 2.2 骨架屏加载光影

```css
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
.skeleton-bar {
  background: linear-gradient(90deg, #F0F0F0 25%, #E4E4E4 50%, #F0F0F0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite linear;
}
```

用途：AI 思考中状态、数据加载中占位

### 2.3 语音波形跳动

```css
@keyframes wave {
  0%, 100% { transform: scaleY(0.5); }
  50% { transform: scaleY(1); }
}
```

用途：用户录音中、AI 正在说话状态的视觉反馈

### 2.4 交互微动效

- 按钮 `:active` 缩放：`transform: scale(0.97)` — 所有可点击元素
- 卡片 hover 上浮：`translateY(-2px)` + 阴影加深（PC 端）
- 页面过渡：简单的 fade 或 slide（路由切换）

---

## 3. 逐页设计

### 3.1 首页 (index)

**移动端**（参照设计稿 mockup 1）：
- 顶部：品牌名 "Mianmian." + 用户头像
- Hero：衬线大标题 + 副标题
- 核心入口：深色 Hero Card（带 Unsplash 背景图半透明叠加）→ "开始 AI 面试"
- 次级入口：列表卡片（简历诊断等）+ SVG 图标 + 右箭头
- 分类标签：胶囊形状 tag 展示 8 个题目分类
- "Hot Topics" 区域（可选）

**PC 端**：
- Hero 区拉宽，标题更大
- 功能入口横向排列（flex/grid）
- 最大宽度 1200px 居中

### 3.2 AI 面试对话页 (interview/chat)

**移动端**（参照设计稿 mockup 2+3）：
- 顶部栏：返回按钮 + 面试标题 + 在线状态（绿色圆点）
- 消息列表：AI 气泡（左侧，带头像和名称）+ 用户气泡（右侧，浅灰底）
- AI 气泡中可嵌入代码块（深色背景，带窗口三色点 + 文件名 + 语法高亮）
- 三种交互状态：
  - AI 思考：骨架屏 shimmer 动画
  - 用户录音：波形动画 + 闪烁光标
  - 正常输入：文本输入框
- 底部面板：键盘输入 / 语音输入切换

**PC 端**（参照设计稿 Desktop View）：
- 三栏布局：
  - 左栏 280px：面试进度树（已完成/进行中/待进行）
  - 中栏 flex：对话区域
  - 右栏 450px：深色代码编辑器（语法高亮 + 行号）
- 代码编辑器支持用户写注释

### 3.3 个人中心 (profile)

**移动端**：
- 头部：用户头像（首字母或真实照片）+ 昵称 + ID
- 数据卡片行：刷题数 / 面试次数 / 错题积累
- 功能菜单：面试历史 / 在线试卷 / 错题本（列表式，SVG 图标 + 右箭头）
- AI API Key 配置入口
- 退出登录

**PC 端**：
- 数据卡片横向展开
- 菜单以卡片网格展示

### 3.4 简历页 (resume)

**上传页**（upload）：
- 大号上传区域（虚线边框 + 图标 + 提示文字）
- 支持 PDF/DOCX/JPG/PNG
- 上传后显示文件名 + 进度

**诊断报告页**（report，参照设计稿 mockup 4）：
- 分数圆圈（绿色背景）
- 诊断标题 + 描述
- 问题卡片：原句 → AI 建议重写（左右对比 / before-after）
- 导出按钮（深色，底部固定）

### 3.5 登录页 (login)

**移动端 + H5**：
- 简洁居中布局
- 品牌名 "Mianmian." 衬线体
- 副标题：简短描述
- 微信登录按钮（小程序）；邮箱/手机登录（H5 备选）
- 隐私协议链接
- 非微信环境自动 fallback 到 mock 登录

### 3.6 题库/试卷/错题本

**通用列表模式**：
- 题目卡片：编号 + 题型标签（单选/多选/判断/填空）+ 标题 + 难度指示
- 分类筛选：顶部标签或下拉
- 分页 / 无限滚动

---

## 4. 实现原则

### 4.1 图标策略
- **禁用 emoji**，全部改用 SVG 内联图标
- 使用简单的 24x24 stroke 图标（Heroicons 风格）
- 常用图标：面试🤖→火花/对话，刷题🎯→靶心，错题📊→图表，简历📋→文档

### 4.2 头像策略
- 面试官头像：Unsplash 真人照片（固定几张）
- 用户头像：首字母圆形（无照片时）
- H5 环境用 `<img>`，小程序用 `<image>`

### 4.3 字体加载
- H5：`<link>` 加载 Google Fonts（Inter + JetBrains Mono）
- 小程序：系统字体 fallback（PingFang SC）
- Georgia 在 iOS/macOS 上系统自带，Windows 需 fallback

### 4.4 uni-app 条件编译
```
/* #ifdef H5 */
@import url('https://fonts.googleapis.com/css2?family=Inter:...');
/* #endif */
```

### 4.5 暗色模式
- 这一个版本不做暗色模式
- 深色元素（代码块、Dark Card）保持固定深色
- 主界面始终浅色

---

## 5. 覆盖范围

| 页面 | 移动端 | PC 端 | 动画 |
|------|--------|-------|------|
| 首页 index | ✅ | ✅ | hover |
| AI 面试 chat | ✅ | ✅ 三栏 | shimmer + wave + blink |
| 个人中心 profile | ✅ | ✅ | hover |
| 简历上传 upload | ✅ | ✅ | progress |
| 简历报告 report | ✅ | ✅ | — |
| 登录 login | ✅ | ✅ | — |
| 题库列表 question/list | ✅ | ✅ | hover |
| 试卷 exam | ✅ | ✅ | timer |
| 刷题 practice | ✅ | ✅ | — |
| 错题本 wrong-book | ✅ | ✅ | — |

---

## 6. 不做的事

- 不引入新的 UI 框架（不用 shadcn/ui、Vant 等）
- 不做暗色模式
- 不改变现有路由/数据流
- 不修改后端逻辑
