# 论文知识库引用标注 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现论文知识库引用标注功能，让 AI 润色/降重结果自动标注引用来源，前端渲染可点击 chip + 参考文献列表。

**Architecture:** 新增独立 `citation/` 模块（4 个纯函数），通过接口与现有 paper-kb 模块交互。不侵入现有管线，导出时自动剥离标记。

**Tech Stack:** TypeScript (Vue 3), Java (Spring Boot), MiniSearch, IndexedDB (Dexie)

**Spec:** `docs/superpowers/specs/2026-05-31-paper-kb-citation-design.md`

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `AI-Interview/web-app/src/modules/paper-kb/citation/types.ts` | Create | PaperCitation, CitedChunk 类型 |
| `AI-Interview/web-app/src/modules/paper-kb/citation/citationExtractor.ts` | Create | 本地正则提取元数据 |
| `AI-Interview/web-app/src/modules/paper-kb/citation/citationFormatter.ts` | Create | 格式化引用 context 块 |
| `AI-Interview/web-app/src/modules/paper-kb/citation/citationRenderer.ts` | Create | 解析 [N] 标记渲染 HTML |
| `AI-Interview/web-app/src/modules/paper-kb/citation/citationStripper.ts` | Create | 剥离标记（导出用） |
| `AI-Interview/web-app/src/modules/paper-kb/citation/index.ts` | Create | 模块统一导出 |
| `AI-Interview/web-app/src/modules/paper-kb/types.ts` | Modify | LocalPaper 新增 citation 字段 |
| `AI-Interview/web-app/src/modules/paper-kb/usePaperKnowledgeBase.ts` | Modify | importFile 调用 extractCitation |
| `AI-Interview/web-app/src/composables/usePaperKbPanel.ts` | Modify | retrieveAndFormat 使用 citationFormatter |
| `AI-Interview/web-app/src/modules/paper-kb/index.ts` | Modify | 导出 citation 模块 |
| `AI-Interview/web-app/src/views/PolishView.vue` | Modify | 结果区渲染 chip |
| `AI-Interview/web-app/src/views/AiReduceView.vue` | Modify | 结果区渲染 chip |
| `AI-Interview/web-app/src/views/PlagiarismReduceView.vue` | Modify | 结果区渲染 chip |
| `mianmiantong-server/.../ResumeController.java` | Modify | 导出前 stripMarkers |

---

### Task 1: 类型定义 — PaperCitation 与 CitedChunk

**Files:**
- Create: `AI-Interview/web-app/src/modules/paper-kb/citation/types.ts`

- [ ] **Step 1: 创建 citation/types.ts**

```typescript
/**
 * 论文引用元数据。
 * 上传时从论文文本中提取，存储在 LocalPaper.citation。
 */
export interface PaperCitation {
  authors: string[]
  year?: number
  journal?: string
  doi?: string
  rawReference?: string
}

/**
 * 带引用索引的检索 chunk。
 * 用于 citationFormatter 生成带 [N] 标记的 context 块。
 */
export interface CitedChunk {
  index: number
  paperTitle: string
  citation?: PaperCitation
  section?: string
  content: string
}
```

- [ ] **Step 2: 修改 paper-kb/types.ts，LocalPaper 新增 citation 字段**

在 `AI-Interview/web-app/src/modules/paper-kb/types.ts` 的 `LocalPaper` 接口中添加：

```typescript
import type { PaperCitation } from './citation/types'

export interface LocalPaper {
  id?: number
  title: string
  fingerprint: string
  fileType: 'pdf' | 'docx' | 'txt' | 'md'
  uploadTime: number
  wordCount: number
  chunkCount: number
  citation?: PaperCitation  // 新增
}
```

- [ ] **Step 3: 修改 paper-kb/index.ts，导出 citation 类型和模块**

```typescript
export { usePaperKnowledgeBase } from './usePaperKnowledgeBase'
export { paperKnowledgeDb } from './paperKnowledgeDb'
export { parsePaperFile } from './paperParser'
export { chunkPaper } from './paperChunker'
export { searchChunks, ensureSearchIndex } from './paperSearchIndex'
export { retrieveContext, toContextChunks } from './paperRetriever'
export { buildQuestionWithContext, buildContextBlock } from './paperPromptBuilder'
export { exportBackup, importBackup, downloadBackupFile } from './paperKbBackup'

export {
  extractCitation,
  formatCitedContext,
  renderCitations,
  stripCitationMarkers,
} from './citation'

export type {
  LocalPaper,
  LocalPaperChunk,
  RetrievedChunk,
  KbBackup,
  PaperTaskType,
  ContextChunk,
} from './types'

export type { PaperCitation, CitedChunk } from './citation/types'
```

- [ ] **Step 4: 验证 TypeScript 编译**

Run: `cd AI-Interview/web-app && npx tsc --noEmit`
Expected: 无错误

- [ ] **Step 5: Commit**

```bash
git add AI-Interview/web-app/src/modules/paper-kb/citation/types.ts \
        AI-Interview/web-app/src/modules/paper-kb/types.ts \
        AI-Interview/web-app/src/modules/paper-kb/index.ts
git commit -m "feat(citation): add PaperCitation and CitedChunk types"
```

---

### Task 2: citationExtractor — 本地正则提取元数据

**Files:**
- Create: `AI-Interview/web-app/src/modules/paper-kb/citation/citationExtractor.ts`

- [ ] **Step 1: 创建 citationExtractor.ts**

```typescript
import type { PaperCitation } from './types'

/**
 * 从论文文本中提取引用元数据（作者、年份、期刊）。
 * 纯函数，无外部依赖。提取不到返回 null。
 */
export function extractCitation(text: string): PaperCitation | null {
  if (!text || text.trim().length < 50) return null

  const first500 = text.slice(0, 500)
  const refLine = extractFirstReferenceLine(text)

  const authors = extractAuthors(first500) ?? (refLine ? extractAuthorsFromRef(refLine) : [])
  const year = extractYear(first500) ?? (refLine ? extractYear(refLine) : undefined)
  const journal = extractJournal(refLine ?? first500)

  if (authors.length === 0 && !year && !journal) return null

  return {
    authors,
    year: year ?? undefined,
    journal: journal ?? undefined,
    rawReference: refLine ?? undefined,
  }
}

/**
 * 提取参考文献第一行。
 */
function extractFirstReferenceLine(text: string): string | null {
  // 匹配 [1] 或 1. 开头的参考文献行
  const refPatterns = [
    /\[(\d+)\]\s*(.+)/,
    /^(\d+)\.\s+(.+)/m,
  ]

  for (const pattern of refPatterns) {
    const match = text.match(pattern)
    if (match && match[2]) {
      const line = match[2].trim()
      // 简单验证：包含年份或期刊标记
      if (/\b(?:19|20)\d{2}\b/.test(line) || /\[J\]|\[M\]|\[C\]/.test(line)) {
        return line
      }
    }
  }
  return null
}

/**
 * 从论文首页提取作者。
 * 中文：2-4 个汉字，逗号/顿号分隔
 * 英文：Last, First 或 First Last 格式
 */
function extractAuthors(text: string): string[] | null {
  // 中文作者：连续 2-4 个汉字，用逗号/顿号/空格分隔
  const cnMatch = text.match(/[一-鿿]{2,4}(?:[,，、\s]+[一-鿿]{2,4})+/)
  if (cnMatch) {
    const names = cnMatch[0].split(/[,，、\s]+/).filter(n => n.length >= 2 && n.length <= 4)
    if (names.length >= 1 && names.length <= 10) return names
  }

  // 英文作者：A. B. Last 或 Last, A. B.
  const enMatch = text.match(/[A-Z]\.\s*(?:[A-Z]\.\s*)?[A-Z][a-z]+(?:\s+et al\.?)?/)
  if (enMatch) {
    return [enMatch[0].trim()]
  }

  return null
}

/**
 * 从参考文献行提取作者。
 */
function extractAuthorsFromRef(refLine: string): string[] {
  // 参考文献格式：作者. 标题[J]. 期刊, 年份
  const dotIndex = refLine.indexOf('.')
  if (dotIndex > 0 && dotIndex < 80) {
    const authorPart = refLine.slice(0, dotIndex).trim()
    if (authorPart.length >= 2) {
      // 中文作者
      const cnNames = authorPart.match(/[一-鿿]{2,4}/g)
      if (cnNames && cnNames.length >= 1) return cnNames
      // 英文作者
      return [authorPart]
    }
  }
  return []
}

/**
 * 提取年份。
 */
function extractYear(text: string): number | undefined {
  const match = text.match(/\b((?:19|20)\d{2})\b/)
  if (match) {
    const year = parseInt(match[1], 10)
    if (year >= 1900 && year <= 2099) return year
  }
  return undefined
}

/**
 * 提取期刊名。
 * 从 [J] 标记后提取，或从参考文献行中提取。
 */
function extractJournal(text: string): string | null {
  // [J] 后面的期刊名
  const jMatch = text.match(/\[J\]\s*[.。]?\s*(.+?)(?:[,，]|\s*\d{4}|\s*$)/)
  if (jMatch && jMatch[1]) {
    const journal = jMatch[1].trim()
    if (journal.length >= 2 && journal.length <= 60) return journal
  }

  // 常见期刊关键词
  const journalKeywords = text.match(/([一-鿿]*(?:学报|科学|研究|通讯|通报|杂志)[一-鿿]*)/)
  if (journalKeywords && journalKeywords[1]) {
    return journalKeywords[1].trim()
  }

  return null
}
```

- [ ] **Step 2: 创建 citation/index.ts 统一导出**

```typescript
export { extractCitation } from './citationExtractor'
export { formatCitedContext } from './citationFormatter'
export { renderCitations } from './citationRenderer'
export { stripCitationMarkers } from './citationStripper'

export type { PaperCitation, CitedChunk } from './types'
```

- [ ] **Step 3: 验证 TypeScript 编译**

Run: `cd AI-Interview/web-app && npx tsc --noEmit`
Expected: 无错误

- [ ] **Step 4: Commit**

```bash
git add AI-Interview/web-app/src/modules/paper-kb/citation/
git commit -m "feat(citation): add citationExtractor with local regex metadata extraction"
```

---

### Task 3: citationFormatter — 格式化引用 context 块

**Files:**
- Create: `AI-Interview/web-app/src/modules/paper-kb/citation/citationFormatter.ts`

- [ ] **Step 1: 创建 citationFormatter.ts**

```typescript
import type { CitedChunk } from './types'

/**
 * 将带索引的 chunk 列表格式化为带引用标记的 prompt context 块。
 * 纯函数，替代 buildContextBlock 的引用场景。
 */
export function formatCitedContext(chunks: CitedChunk[]): string {
  if (chunks.length === 0) return ''

  const parts = chunks.map(c => {
    const citation = formatCitationLine(c)
    const header = c.section ? `【${c.section}】` : ''
    return `[${c.index}] ${citation}${header}\n${c.content}`
  })

  return [
    '以下是从知识库中检索到的相关内容，请参考并在引用时标注来源[N]：',
    '',
    ...parts,
    '',
    '---',
    '引用规则：',
    '- 在引用相关观点、数据、方法的句子后标注 [N]',
    '- 仅标注实际参考了的文献，不要无中生有',
    '- 一句话可标注多个来源，如 [1][2]',
    '- 未参考文献不要标注',
  ].join('\n')
}

/**
 * 格式化单条引用的标题行。
 */
function formatCitationLine(c: CitedChunk): string {
  const { citation, paperTitle } = c
  if (!citation) return `${paperTitle} `

  const parts: string[] = []

  // 作者
  if (citation.authors.length > 0) {
    const authorStr = citation.authors.length > 3
      ? `${citation.authors[0]}等`
      : citation.authors.join(', ')
    parts.push(authorStr)
  }

  // 年份
  if (citation.year) {
    parts.push(`(${citation.year})`)
  }

  // 标题
  parts.push(paperTitle)

  // 期刊
  if (citation.journal) {
    parts.push(citation.journal)
  }

  return parts.join(' ') + ' '
}
```

- [ ] **Step 2: 验证 TypeScript 编译**

Run: `cd AI-Interview/web-app && npx tsc --noEmit`
Expected: 无错误

- [ ] **Step 3: Commit**

```bash
git add AI-Interview/web-app/src/modules/paper-kb/citation/citationFormatter.ts \
        AI-Interview/web-app/src/modules/paper-kb/citation/index.ts
git commit -m "feat(citation): add citationFormatter for structured context blocks"
```

---

### Task 4: citationRenderer — 解析标记渲染 HTML

**Files:**
- Create: `AI-Interview/web-app/src/modules/paper-kb/citation/citationRenderer.ts`

- [ ] **Step 1: 创建 citationRenderer.ts**

```typescript
import type { CitedChunk } from './types'

export interface CitationRenderResult {
  html: string
  references: string[]
  citedIndices: number[]
}

/**
 * 解析 AI 输出中的 [N] 引用标记，生成带 chip 的 HTML 和参考文献列表。
 * 纯函数。
 */
export function renderCitations(text: string, citedChunks: CitedChunk[]): CitationRenderResult {
  const citedIndices: number[] = []
  const chunkMap = new Map(citedChunks.map(c => [c.index, c]))

  // 替换 [N] 为 chip HTML
  const html = text.replace(/\[(\d+)\]/g, (match, numStr) => {
    const index = parseInt(numStr, 10)
    if (!chunkMap.has(index)) return match
    if (!citedIndices.includes(index)) citedIndices.push(index)
    return `<span class="cite-chip" data-cite-index="${index}">${index}</span>`
  })

  // 生成参考文献列表（按引用顺序）
  citedIndices.sort((a, b) => a - b)
  const references = citedIndices.map(index => {
    const chunk = chunkMap.get(index)
    if (!chunk) return `[${index}] 未知来源`
    return formatReference(index, chunk)
  })

  return { html, references, citedIndices }
}

/**
 * 格式化单条参考文献。
 */
function formatReference(index: number, chunk: CitedChunk): string {
  const { citation, paperTitle, section } = chunk
  if (!citation) return `[${index}] ${paperTitle}`

  const parts: string[] = [`[${index}]`]

  if (citation.authors.length > 0) {
    const authorStr = citation.authors.length > 3
      ? `${citation.authors.slice(0, 3).join(', ')} et al.`
      : citation.authors.join(', ')
    parts.push(authorStr)
  }

  if (citation.year) {
    parts.push(`(${citation.year}).`)
  }

  parts.push(paperTitle + '.')

  if (citation.journal) {
    parts.push(citation.journal + '.')
  }

  if (section) {
    parts.push(`【${section}】`)
  }

  return parts.join(' ')
}
```

- [ ] **Step 2: 验证 TypeScript 编译**

Run: `cd AI-Interview/web-app && npx tsc --noEmit`
Expected: 无错误

- [ ] **Step 3: Commit**

```bash
git add AI-Interview/web-app/src/modules/paper-kb/citation/citationRenderer.ts
git commit -m "feat(citation): add citationRenderer for chip HTML and reference list"
```

---

### Task 5: citationStripper — 剥离标记

**Files:**
- Create: `AI-Interview/web-app/src/modules/paper-kb/citation/citationStripper.ts`

- [ ] **Step 1: 创建 citationStripper.ts**

```typescript
/**
 * 剥离文本中的 [N] 引用标记。
 * 纯函数，用于导出前清洗，确保 DOCX 写回的是干净文本。
 */
export function stripCitationMarkers(text: string): string {
  if (!text) return text
  return text.replace(/\[\d+\]/g, '')
}
```

- [ ] **Step 2: 验证 TypeScript 编译**

Run: `cd AI-Interview/web-app && npx tsc --noEmit`
Expected: 无错误

- [ ] **Step 3: Commit**

```bash
git add AI-Interview/web-app/src/modules/paper-kb/citation/citationStripper.ts
git commit -m "feat(citation): add citationStripper for export-safe text"
```

---

### Task 6: 集成 — usePaperKnowledgeBase 上传时提取元数据

**Files:**
- Modify: `AI-Interview/web-app/src/modules/paper-kb/usePaperKnowledgeBase.ts`

- [ ] **Step 1: 修改 importFile，上传时调用 extractCitation**

在 `usePaperKnowledgeBase.ts` 中：

1. 添加 import：
```typescript
import { extractCitation } from './citation'
```

2. 在 `importFile` 函数中，`chunkPaper` 之后、`paperKnowledgeDb.papers.add` 之前，添加元数据提取：

```typescript
// 在 const chunks = chunkPaper(parsed.fullText) 之后添加：
const citation = extractCitation(parsed.fullText)

// 修改 paperKnowledgeDb.papers.add 调用，加入 citation：
const paperId = await paperKnowledgeDb.papers.add({
  title: parsed.title,
  fingerprint: parsed.fingerprint,
  fileType: parsed.fileType,
  uploadTime: Date.now(),
  wordCount,
  chunkCount,
  citation: citation ?? undefined,
})
```

- [ ] **Step 2: 验证 TypeScript 编译**

Run: `cd AI-Interview/web-app && npx tsc --noEmit`
Expected: 无错误

- [ ] **Step 3: Commit**

```bash
git add AI-Interview/web-app/src/modules/paper-kb/usePaperKnowledgeBase.ts
git commit -m "feat(citation): extract citation metadata on paper upload"
```

---

### Task 7: 集成 — usePaperKbPanel 使用 citationFormatter

**Files:**
- Modify: `AI-Interview/web-app/src/composables/usePaperKbPanel.ts`

- [ ] **Step 1: 修改 retrieveAndFormat 使用 citationFormatter**

1. 修改 import：
```typescript
import { usePaperKnowledgeBase, buildContextBlock, formatCitedContext } from '@/modules/paper-kb'
import type { PaperTaskType, ContextChunk, CitedChunk } from '@/modules/paper-kb'
```

2. 修改 `retrieveAndFormat` 函数：
```typescript
async function retrieveAndFormat(queryText: string, hints?: KbRetrievalHints): Promise<string> {
  const chunks = await retrieveRaw(queryText, hints)
  if (chunks.length === 0) return ''

  // 将 ContextChunk 转为 CitedChunk（带索引和 citation 元数据）
  const citedChunks: CitedChunk[] = chunks.map((c, i) => ({
    index: i + 1,
    paperTitle: c.paperTitle,
    section: c.section,
    content: c.content,
    // citation 元数据通过 paperTitle 在知识库中查找
    // 这里先不传，后续可通过 paperId 关联
  }))

  return formatCitedContext(citedChunks)
}
```

- [ ] **Step 2: 验证 TypeScript 编译**

Run: `cd AI-Interview/web-app && npx tsc --noEmit`
Expected: 无错误

- [ ] **Step 3: Commit**

```bash
git add AI-Interview/web-app/src/composables/usePaperKbPanel.ts
git commit -m "feat(citation): use citationFormatter in usePaperKbPanel"
```

---

### Task 8: 前端渲染 — 结果区展示引用 chip

**Files:**
- Modify: `AI-Interview/web-app/src/views/PolishView.vue`
- Modify: `AI-Interview/web-app/src/views/AiReduceView.vue`
- Modify: `AI-Interview/web-app/src/views/PlagiarismReduceView.vue`

- [ ] **Step 1: 创建 citation chip 样式**

在 `AI-Interview/web-app/src/views/PolishView.vue` 的 `<style>` 中添加：

```css
.cite-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.2em;
  height: 1.2em;
  padding: 0 0.3em;
  margin: 0 0.1em;
  font-size: 0.7em;
  font-weight: 600;
  line-height: 1;
  color: #4f6ef7;
  background: rgba(79, 110, 247, 0.1);
  border: 1px solid rgba(79, 110, 247, 0.3);
  border-radius: 0.3em;
  cursor: pointer;
  vertical-align: super;
  transition: background 0.15s;
}

.cite-chip:hover {
  background: rgba(79, 110, 247, 0.2);
}

.cite-popover {
  position: absolute;
  z-index: 100;
  max-width: 320px;
  padding: 0.75em 1em;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  font-size: 0.85em;
  line-height: 1.5;
}

.cite-popover-title {
  font-weight: 600;
  margin-bottom: 0.3em;
}

.cite-popover-meta {
  color: #666;
  font-size: 0.9em;
}

.reference-list {
  margin-top: 1.5em;
  padding-top: 1em;
  border-top: 1px solid #e8e8e8;
}

.reference-list h4 {
  font-size: 0.95em;
  margin-bottom: 0.5em;
  color: #333;
}

.reference-list ol {
  padding-left: 1.5em;
  font-size: 0.85em;
  color: #555;
  line-height: 1.8;
}

.reference-list li {
  margin-bottom: 0.3em;
}
```

- [ ] **Step 2: 在 PolishView 结果区渲染 chip**

在 PolishView 的结果展示区域，将纯文本替换为带 chip 的 HTML 渲染。具体位置需要根据实际模板确定，核心逻辑：

```typescript
import { renderCitations, stripCitationMarkers } from '@/modules/paper-kb'
import type { CitedChunk } from '@/modules/paper-kb'

// 在结果展示时：
const renderResult = computed(() => {
  if (!polishedText.value) return { html: '', references: [], citedIndices: [] }
  return renderCitations(polishedText.value, lastRetrievedChunks.value.map((c, i) => ({
    index: i + 1,
    paperTitle: c.paperTitle,
    section: c.section,
    content: c.content,
  })))
})
```

模板中使用 `v-html` 渲染 `renderResult.html`，并用 `v-if="renderResult.references.length > 0"` 展示参考文献列表。

- [ ] **Step 3: 同样修改 AiReduceView 和 PlagiarismReduceView**

逻辑与 PolishView 相同，复制渲染逻辑到另外两个视图。

- [ ] **Step 4: 验证前端构建**

Run: `cd AI-Interview/web-app && npm run build`
Expected: 构建成功

- [ ] **Step 5: Commit**

```bash
git add AI-Interview/web-app/src/views/PolishView.vue \
        AI-Interview/web-app/src/views/AiReduceView.vue \
        AI-Interview/web-app/src/views/PlagiarismReduceView.vue
git commit -m "feat(citation): render citation chips and reference list in result views"
```

---

### Task 9: 后端 — 导出前剥离引用标记

**Files:**
- Modify: `mianmiantong-server/src/main/java/com/mianmiantong/controller/resume/ResumeController.java`

- [ ] **Step 1: 在 buildFormatPreservedDocx 中添加 stripMarkers**

在 `ResumeController.java` 的 `buildFormatPreservedDocx` 方法中，在获取 `optimizedText` 后添加清洗：

```java
// 在 String optimizedText = (String) report.get("optimizedText"); 之后添加：
if (optimizedText != null) {
    optimizedText = optimizedText.replaceAll("\\[\\d+\\]", "");
}
```

- [ ] **Step 2: 验证后端编译**

Run: `cd mianmiantong-server && mvn compile -q`
Expected: 编译成功

- [ ] **Step 3: 运行测试**

Run: `cd mianmiantong-server && mvn test -q`
Expected: 所有测试通过

- [ ] **Step 4: Commit**

```bash
git add mianmiantong-server/src/main/java/com/mianmiantong/controller/resume/ResumeController.java
git commit -m "feat(citation): strip citation markers before DOCX export"
```

---

### Task 10: 端到端验证

- [ ] **Step 1: 后端编译验证**

Run: `cd mianmiantong-server && mvn compile -q`
Expected: 编译成功

- [ ] **Step 2: 前端构建验证**

Run: `cd AI-Interview/web-app && npm run build`
Expected: 构建成功

- [ ] **Step 3: 功能验证清单**

手动测试：
1. 上传一篇带参考文献的论文 → 检查 `LocalPaper.citation` 是否包含 authors/year/journal
2. 润色时启用知识库 → 检查 prompt 中是否包含 `[1] 作者(年份). 标题. 期刊` 格式
3. 润色结果中是否出现 `[1][2]` 标记
4. 点击标记是否弹出引用详情
5. 文末是否生成参考文献列表
6. 导出 DOCX → 检查文件中是否不含 `[N]` 标记
7. 无知识库时润色功能是否正常（向后兼容）

- [ ] **Step 4: Final Commit**

```bash
git add -A
git commit -m "feat(citation): complete citation annotation feature for paper knowledge base"
```
