# 论文知识库引用标注设计

Status: approved
Date: 2026-05-31

## 背景

当前论文工具的知识库模块在润色/降重时，会将检索到的相关 chunk 整体拼在 prompt 前面作为 `[参考N]` 块。这种方式有两个问题：

1. 无法在润色/降重结果中标注哪句话引用了哪篇文献
2. 缺少作者、年份、期刊等结构化元数据，输出不符合学术引用规范

## 目标

1. AI 生成的润色/降重结果中自动标注引用来源 `[1][2]`
2. 前端展示引用标记为可点击 chip，点击显示引用详情
3. 文末自动生成参考文献列表
4. 用户可手动修正引用（删除/添加）
5. 导出功能不受影响，引用标记不进入 DOCX

## 非目标

- 不做语义 embedding 检索（保持现有 MiniSearch）
- 不做引用格式标准化（APA/MLA/GB 等）
- 不做跨文献去重

## 设计约束

- 引用模块是独立模块，不侵入现有管线
- 所有函数为纯函数，无副作用
- 不引入新依赖
- 现有 `paperPromptBuilder.buildContextBlock` 保留不动

## 数据模型

### PaperCitation（新增）

```typescript
interface PaperCitation {
  authors: string[]     // ['张三', '李四']
  year?: number         // 2023
  journal?: string      // '计算机学报'
  doi?: string
  rawReference?: string // 原始参考文献格式字符串
}
```

### LocalPaper 扩展

```typescript
interface LocalPaper {
  // ...现有字段不变
  citation?: PaperCitation  // 新增，可选
}
```

### CitedChunk（新增）

```typescript
interface CitedChunk {
  index: number           // [N] 的 N
  paperTitle: string
  citation?: PaperCitation
  section?: string
  content: string
}
```

## 模块架构

```
┌─────────────────────────────────────────────────┐
│  现有管线（不改动）                                │
│  paperParser → paperChunker → paperSearchIndex   │
│  → paperRetriever → paperPromptBuilder           │
│  → AI → optimizedText → writeBack → DOCX         │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│  新增 citation 模块（独立，通过接口交互）           │
│                                                   │
│  citationExtractor  ← 提取元数据（上传时调用）      │
│  citationFormatter  ← 格式化引用块（prompt 拼接时） │
│  citationRenderer   ← 解析标记渲染 chip（展示时）   │
│  citationStripper   ← 剥离标记（导出时调用）        │
└─────────────────────────────────────────────────┘
```

## 数据流

### 上传阶段

```
用户上传论文
  → paperParser 解析
  → extractCitation(text) 提取元数据
  → 提取不到的字段 LLM 补充（仅一次，约 500 token）
  → 存入 LocalPaper.citation
```

### 检索阶段

```
用户任务
  → retrieveContext（现有逻辑不变）
  → ContextChunk 携带 citation 元数据
```

### 生成阶段

```
ContextChunk[]
  → citationFormatter.formatCitedContext()
  → 拼入 prompt（带结构化引用格式 + 引用规则指令）
  → AI 输出带 [N] 标记的文本
```

### 展示阶段

```
AI 输出
  → citationRenderer.renderCitations()
  → 渲染为 chip HTML + 文末参考文献列表
  → 用户可手动修正
```

### 导出阶段

```
optimizedText
  → citationStripper.stripCitationMarkers()
  → 干净文本
  → 匹配原文段落
  → 写回 DOCX（不含标记）
```

## 模块接口

### citationExtractor.ts

```typescript
/**
 * 从论文文本中提取引用元数据。
 * 纯函数，无外部依赖。
 */
export function extractCitation(text: string): PaperCitation | null
```

**提取策略**：
1. 本地正则（零成本）：
   - 参考文献第一行匹配 `作者. 标题[J]. 期刊, 年份`
   - 论文首页标题下方匹配作者行
   - 年份：`/(19|20)\d{2}/`
   - 期刊：`[J]` 后面的文本
2. LLM 补充（仅正则提取不到时）：
   - 发送论文前 300 字
   - Prompt：`从以下学术文本中提取作者、年份、期刊，返回 JSON`

### citationFormatter.ts

```typescript
/**
 * 将带索引的 chunk 列表格式化为 prompt context 块。
 * 纯函数，替代 buildContextBlock 的引用场景。
 */
export function formatCitedContext(chunks: CitedChunk[]): string
```

**输出格式**：
```
以下是从知识库中检索到的相关内容，请参考并在引用时标注来源[N]：

[1] 张三等(2023). 基于深度学习的文本分类. 计算机学报【方法】
{chunk 内容}

[2] Li et al.(2022). Text Classification Survey. ACL【实验】
{chunk 内容}

引用规则：
- 在引用相关观点、数据、方法的句子后标注 [N]
- 仅标注实际参考了的文献，不要无中生有
- 一句话可标注多个来源，如 [1][2]
- 未参考文献不要标注
```

### citationRenderer.ts

```typescript
/**
 * 解析 AI 输出中的 [N] 标记，生成渲染结果。
 * 纯函数。
 */
export function renderCitations(text: string, citedChunks: CitedChunk[]): {
  html: string              // 带 chip 的 HTML
  references: string[]      // 文末参考文献列表
  citedIndices: number[]    // 实际引用了的索引
}
```

**渲染逻辑**：
- `[N]` → `<span class="cite-chip" data-index="N">N</span>`
- chip 点击弹出 popover：论文标题、作者、年份、期刊、来源章节
- 文末生成参考文献列表：`[1] 张三等(2023). 论文标题. 期刊.`

### citationStripper.ts

```typescript
/**
 * 剥离文本中的 [N] 引用标记。
 * 纯函数，用于导出前清洗。
 */
export function stripCitationMarkers(text: string): string
```

**实现**：`text.replace(/\[\d+\]/g, '')`

## Prompt 改造

### Context 块格式

现有格式（保留不动）：
```
[参考1] 论文标题【章节】
内容
```

新增引用格式（通过 `citationFormatter` 生成）：
```
[1] 作者等(年份). 论文标题. 期刊【章节】
内容
```

### 任务指令

在现有任务 prompt 后追加引用规则：
```
引用规则：
- 在引用相关观点、数据、方法的句子后标注 [N]
- 仅标注实际参考了的文献，不要无中生有
- 一句话可标注多个来源，如 [1][2]
- 未参考文献不要标注
```

## 前端渲染

### 结果区改造

1. AI 输出的 `[1][2]` 解析为可点击 chip
2. chip 样式：小号圆形徽标，与正文区分
3. 点击 chip 弹出 popover：论文标题、作者、年份、期刊
4. 结果末尾生成 `参考文献` 区块

### 手动修正

- chip 提供 `×` 删除按钮
- 文末列表提供 `+` 按钮手动添加引用
- 修正后实时更新参考文献列表

### 导出兼容

- 复制/导出时 strip 所有 chip
- 参考文献列表可选是否一起导出

## 导出安全

**设计约束**：引用标记只存在于前端展示层，不进入导出管线。

```
AI 输出 "该方法基于BERT模型[1]进行文本分类"
         ↓
┌─ 前端展示：渲染为带 [1] chip 的 HTML
│
└─ 导出路径：strip("[1]") → "该方法基于BERT模型进行文本分类" → 匹配 → 写回
```

- `optimizedText` 存储时保留标记（供前端展示）
- `writeBack` / `writeBackSnippets` 执行前统一 `stripCitationMarkers()`
- DOCX 写回的是干净文本，不含 `[N]`
- 导出功能零改动，只在调用入口加一层清洗

## 耦合控制

| 现有模块 | 交互方式 | 是否改动 |
|---------|---------|---------|
| `paperChunker` | 不动 | 否 |
| `paperSearchIndex` | 不动 | 否 |
| `paperRetriever` | 不动 | 否 |
| `paperPromptBuilder` | 不动，新增 `citationFormatter` 作为替代入口 | 否 |
| `usePaperKnowledgeBase` | 调用 `extractCitation` 存入 LocalPaper | 小改 |
| `PaperContextSanitizer` | 不动 | 否 |
| `PolishView` / `AiReduceView` | 结果区调用 `renderCitations` | 小改 |
| `ResumeController` | 导出前调用 `stripCitationMarkers` | 小改 |

## 语法与可维护性约束

- 所有 citation 模块函数为**纯函数**，无副作用，无状态
- 不引入新依赖（正则提取不用第三方库）
- TypeScript：`interface` 优先、避免 `any`、显式返回类型
- Java 侧 `PaperContextSanitizer` 不改，只在调用方加 `stripMarkers()`
- citation 模块可独立测试，不依赖 IndexedDB / 网络 / AI
- 删除 `citation/` 目录 + 移除几处调用即可完全移除功能

## 涉及文件

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `citation/types.ts` | 新增 | PaperCitation, CitedChunk 类型定义 |
| `citation/citationExtractor.ts` | 新增 | 本地正则 + LLM 元数据提取 |
| `citation/citationFormatter.ts` | 新增 | 格式化引用 context 块 |
| `citation/citationRenderer.ts` | 新增 | 解析标记渲染 chip |
| `citation/citationStripper.ts` | 新增 | 剥离标记（导出用） |
| `citation/index.ts` | 新增 | 模块统一导出 |
| `types.ts` | 修改 | LocalPaper 新增 citation 字段 |
| `usePaperKnowledgeBase.ts` | 修改 | importFile 时调用 extractCitation |
| `PolishView.vue` | 修改 | 结果区渲染 chip + 参考文献 |
| `AiReduceView.vue` | 修改 | 结果区渲染 chip + 参考文献 |
| `PlagiarismReduceView.vue` | 修改 | 结果区渲染 chip + 参考文献 |
| `ResumeController.java` | 修改 | 导出前 stripMarkers |

## 验证标准

1. 上传论文后 `LocalPaper.citation` 包含正确的 authors/year/journal
2. 润色结果中出现 `[1][2]` 标记，点击可查看详情
3. 文末自动生成参考文献列表
4. 手动删除 chip 后参考文献列表同步更新
5. 导出的 DOCX 中不含 `[N]` 标记，文本内容完整
6. 现有非引用场景（无知识库）功能不受影响
7. 现有 `buildContextBlock` 保留不动，向后兼容
