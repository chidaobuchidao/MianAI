# Paper Tools Port — 论文工具移植设计文档

> 从纸研社 (AI_paper) 移植文档润色、降AI、降查重 + 格式保留导出至面面通 (IntervVault)
>
> 2026-05-23 | chidaobuchidao

---

## 1. 概述

### 1.1 移植范围

| 功能 | 来源 | 目标 |
|------|------|------|
| **学术润色** | AI_paper `polisher.py` | 新增 `PolishService` + `/api/polish` |
| **降AI** | AI_paper `ai_reducer.py` | 新增 `AiReduceService` + `/api/ai-reduce` |
| **降查重** | AI_paper `plagiarism.py` | 新增 `PlagiarismReduceService` + `/api/plagiarism-reduce` |
| **格式保留导出** | AI_paper `aux_tools.py:export_docx` + IntervVault `TemplatePreservingExportService` | 改造 `TemplatePreservingExportService` |

### 1.2 核心设计原则

- **段落级 1:1 映射**：AI 改写保持段落结构不变，避免模糊匹配
- **沿用现有架构**：复用 `DeepSeekAiService`、`WordExportService`、Apache POI
- **Warm Tech 设计系统**：UI 沿用现有设计令牌（`#F3EFE8` canvas, `#D9750A` accent, Inter + Georgia 字体）

---

## 2. 架构设计

### 2.1 后端服务架构

```
Controller 层                Service 层                    数据层
─────────────────────────────────────────────────────────────────
PolishController ────────→ PolishService ──────→ (无持久化，流式返回)
AiReduceController ──────→ AiReduceService ────→ (无持久化)
PlagiarismController ────→ PlagiarismService ──→ (无持久化)
ExportController ────────→ TemplatePreservingExportService ──→ Resume (读取原文件)
                            ├── DeepSeekAiService (streamChat / chat)
                            └── WordExportService (fallback)
```

### 2.2 新增 API 端点

```
POST /api/polish/run              # 润色（SSE 流式）
POST /api/polish/scan             # 格式规范检查（非流式）

POST /api/ai-reduce/scan          # AI 痕迹扫描（非流式）
POST /api/ai-reduce/rewrite       # 降AI改写（SSE 流式）

POST /api/plagiarism-reduce/scan  # 重复风险检测（非流式）
POST /api/plagiarism-reduce/run   # 降查重（SSE 流式）

POST /api/export/preserve-format  # 格式保留导出（接收段落映射，输出 DOCX）
```

### 2.3 数据流

```
┌──────────┐    POST /api/polish/run     ┌──────────────┐    SSE tokens     ┌──────────┐
│  Vue 前端 │ ──────────────────────────→ │ PolishService │ ────────────────→ │  Vue 前端 │
│          │   {text, taskType, mode}    │              │   streamChat()    │  (渲染)   │
│          │                             │ DeepSeekAi   │                   │          │
│          │                             │ Service      │                   │          │
└──────────┘                             └──────────────┘                   └──────────┘

┌──────────┐  POST /api/export/preserve-format   ┌──────────────────────────┐
│  Vue 前端 │ ─────────────────────────────────→ │ TemplatePreservingExport │
│          │  {paragraphs: [{id,text}],           │ Service (改造版)         │
│          │   fileId: "xxx"}                    │                          │
│          │                                     │ 1. 加载原始 DOCX         │
│          │ ←──────── DOCX binary ─────────────│ 2. 按 para_id 匹配段落    │
└──────────┘                                     │ 3. 替换文本保留格式       │
                                                  │ 4. 写入输出 DOCX         │
                                                  └──────────────────────────┘
```

---

## 3. 格式保留导出设计（核心难点）

### 3.1 问题

现有 `WordExportService` 将 AI 生成的 Markdown → 新建裸 DOCX，丢失原文档所有格式。
现有 `TemplatePreservingExportService` 使用模糊匹配寻找 "before→after" 文本，匹配不可靠。

### 3.2 方案：段落 ID 标记法

不再依赖文本匹配，改为**强制段落结构对等**：

```
输入 DOCX 解析                      Prompt 构造                      输出 DOCX 组装
┌─────────────────┐           ┌──────────────────┐           ┌─────────────────┐
│ Para 0: "引言..." │  ───→    │ [P0] 引言...       │  ───→    │ Para 0: 黑体16pt │
│ style: H1        │           │ [P1] 研究背景...   │           │ "引言..."        │
│ font: 黑体 16pt  │           │ [P2] 本文提出...   │           │ (改写后)         │
├─────────────────┤           │                  │           ├─────────────────┤
│ Para 1: "研究..." │  ───→    │ 要求:              │  ───→    │ Para 1: 宋体12pt │
│ style: body      │           │ - 逐段改写         │           │ "研究..."        │
│ font: 宋体 12pt  │           │ - 保持 [Pn] 标记   │           │ (改写后)         │
├─────────────────┤           │ - 段落数一致       │           ├─────────────────┤
│ Para 2: "本文..." │  ───→    └──────────────────┘  ───→    │ Para 2: 宋体12pt │
│ style: body      │                                           │ "本文..."        │
│ font: 宋体 12pt  │                                           │ (改写后)         │
└─────────────────┘                                           └─────────────────┘
```

### 3.3 实现步骤

**Step 1: 解析 DOCX 提取段落**

```java
// ParagraphProfile.java — 段落格式快照
record ParagraphProfile(
    int index,           // 段落序号
    String text,         // 原文
    String styleId,      // 样式ID (Heading1, body...)
    String fontFamily,   // 中文字体
    Double fontSize,     // 字号
    boolean bold,
    boolean italic,
    String color,
    String alignment,    // LEFT/CENTER/RIGHT
    Double indentLeft,   // 左缩进
    Double firstLineIndent // 首行缩进
) {}
```

解析逻辑：
- 遍历 `XWPFDocument.getParagraphs()`
- 跳过表格内段落、空段落、目录段落
- 记录每个正文段落的首个非空 run 格式
- 跳过图片/公式段落（保留位置）

**Step 2: 构造 Prompt 发送 AI**

```
系统指令:
你是一位学术写作助手。你需要逐段处理以下文本。每个段落以 [P{n}] 标记开头。
你必须:
1. 逐段处理，保持 [P{n}] 标记不变
2. 段落数量必须与输入一致
3. 不要合并或拆分段落
4. 只改写内容，保持段落结构

输入:
[P0] 引言内容...
[P1] 研究背景内容...
[P2] 本文提出...

输出:
[P0] (改写后的引言)...
[P1] (改写后的研究背景)...
[P2] (改写后的本文提出)...
```

**Step 3: 解析 AI 响应**

```java
// 正则提取 [P{n}] 标记
Pattern pattern = Pattern.compile("\\[P(\\d+)\\](.*?)(?=\\[P\\d+\\]|$)", Pattern.DOTALL);
Matcher matcher = pattern.matcher(aiResponse);
Map<Integer, String> paragraphMap = new HashMap<>();
while (matcher.find()) {
    int idx = Integer.parseInt(matcher.group(1));
    String text = matcher.group(2).trim();
    paragraphMap.put(idx, text);
}
```

**Step 4: 写回 DOCX**

```java
// 复用 TemplatePreservingExportService.replaceParagraphText 逻辑
for (int i = 0; i < paragraphs.size(); i++) {
    String newText = paragraphMap.get(i);
    if (newText == null) continue; // 匹配失败，保留原文
    
    XWPFParagraph para = paragraphs.get(i);
    replaceParagraphText(para, newText); // 清空旧run，用首run格式创建新run
}
```

### 3.4 回退策略

| 场景 | 处理 |
|------|------|
| 单个段落未匹配 | 保留该段原文 |
| 段落数偏差 > 20% | 整体降级到裸 markdown 导出 |
| AI 返回不含 [Pn] 标记 | 整体降级，提示用户重试 |
| 原文档无格式信息 | 使用默认学术格式（宋体12pt + 首行缩进） |

### 3.5 工作评估

- 新增代码：~300 行 Java（ParagraphProfile + 解析器 + 匹配写回）
- 可复用代码：`TemplatePreservingExportService` 的 `replaceParagraphText()`、`fuzzyMatch()`
- 不新增依赖（Apache POI 已引入）

---

## 4. 学术润色 (Polish)

### 4.1 后端 Service

```java
@Service
public class PolishService {
    
    private final DeepSeekAiService aiService;
    
    /**
     * 润色主入口
     * @param text 待处理文本
     * @param taskType 摘要/引言/章节正文/结论/自定义段落
     * @param polishType vocab/logic/full
     * @param executionMode 标准模式/学术强化/结构重组/精炼压缩
     * @param topic 主题（可选）
     * @param notes 补充说明（可选）
     */
    public SseEmitter runPolish(String text, String taskType, 
            String polishType, String executionMode, 
            String topic, String notes) {
        
        String systemPrompt = loadPrompt("polish.run_task.system");
        String userPrompt = renderPrompt("polish.run_task", 
            Map.of("text", text, "task_type", taskType,
                   "polish_type", polishType, "execution_mode", executionMode,
                   "topic", topic, "notes", notes));
        
        return aiService.streamChat(systemPrompt, userPrompt);
    }
    
    /** 格式规范检查（纯本地，不调AI） */
    public FormatCheckResult scanFormat(String text) {
        // 端口 AI_paper AcademicPolisher.check_format() 的规则
        // - 中英文标点混用检测
        // - 中文数字 vs 阿拉伯数字
        // - 过短段落检测
        // - 参考文献引用标记缺失检测
    }
}
```

### 4.2 Prompt 模板映射

从 AI_paper 的 `prompt_defaults.json` 移植以下场景：

| scene_id | system prompt |
|----------|---------------|
| `polish.run_task` | 统一润色入口，按 task_type+polish_type+execution_mode 处理 |
| `polish.grammar` | 语法标点校对 |
| `polish.academic_vocab` | 学术词汇优化 |
| `polish.logic` | 逻辑段落优化 |
| `polish.full` | 全面润色 |
| `polish.translate` | 翻译润色（中→英/英→中） |

### 4.3 API 请求/响应

```json
// POST /api/polish/run
{
  "text": "待润色的文本内容...",
  "taskType": "章节正文",
  "polishType": "full",
  "executionMode": "标准模式",
  "topic": "",
  "notes": ""
}

// Response: SSE 流式
// data: {"token": "润"}  → {"token": "色"} → {"token": "后"} → ...
// data: [DONE]
```

---

## 5. 降AI (AI Trace Reduction)

### 5.1 后端 Service

```java
@Service
public class AiReduceService {
    
    /**
     * 本地扫描 AI 写作痕迹（不调AI）
     */
    public AiScanResult scanAiFeatures(String text) {
        // 端口 AI_paper AIReducer.scan_ai_features() 全部规则:
        // 1. 模板表达检测 (首先..其次..最后, 综上所述, 值得注意的是...)
        //    12 个 regex pattern
        // 2. 句长方差分析 (variance < 100 → 模板化)
        // 3. 段落长度均匀度 (all abs(len-avg) < avg*0.3 → 模板化)
        // 4. 连接词密度 (连接词数 > 句子数*0.4 → 密集)
        // 5. AI高频词标记 (综上所述/不可否认/毋庸置疑/众所周知...)
        //
        // 返回: {score: 0-100, riskLevel, features[], sentencesFlagged[]}
    }
    
    /**
     * 降AI改写
     * @param mode light/deep/academic
     */
    public SseEmitter rewrite(String text, String mode) {
        String systemPrompt = loadPrompt("ai_reduce.transform.system");
        String modeLabel = Map.of("light","轻度去痕","deep","深度重构","academic","学术拟合").get(mode);
        String userPrompt = renderPrompt("ai_reduce.transform",
            Map.of("text", text, "mode", mode, "mode_label", modeLabel));
        
        return aiService.streamChat(systemPrompt, userPrompt);
    }
}
```

### 5.2 AI 写作特征检测规则（全量端口）

```java
// 12个 regex pattern，来自 ai_reducer.py:16-28
static final Pattern[] AI_PATTERNS = {
    Pattern.compile("首先.*其次.*最后"),
    Pattern.compile("综上所述"),
    Pattern.compile("值得注意的是"),
    Pattern.compile("不可否认"),
    Pattern.compile("毋庸置疑"),
    Pattern.compile("总而言之"),
    Pattern.compile("由此可见"),
    Pattern.compile("显而易见"),
    Pattern.compile("众所周知"),
    Pattern.compile("不言而喻"),
    Pattern.compile("此外.*同时.*另外"),
};
    
// 8个高频连接词
static final List<String> FLOW_CONNECTORS = List.of(
    "因此","然而","同时","此外","另外","由此可见","综上","进一步说","相较之下","具体而言"
);

// 6个结尾标记
static final List<String> CONCLUSION_MARKERS = List.of(
    "综上","总体来看","由此可见","总之","可以看出","从上述分析可知"
);
```

### 5.3 API 端点

```
POST /api/ai-reduce/scan
  Request:  { "text": "..." }
  Response: { "score": 35, "riskLevel": "高风险", "features": [...], "sentencesFlagged": [...] }

POST /api/ai-reduce/rewrite
  Request:  { "text": "...", "mode": "academic" }
  Response: SSE stream
```

---

## 6. 降查重 (Plagiarism Reduction)

### 6.1 后端 Service

```java
@Service
public class PlagiarismReduceService {
    
    /**
     * 本地重复风险检测 + 相似度对比
     */
    public PlagiarismScanResult scanRepetition(String text, String sourceText) {
        // 端口 AI_paper PlagiarismReducer 全部检测:
        // 1. 重复短语统计 (4字以上中文词组，>=3次)
        // 2. 长句检测 (>100字)
        // 3. 风险段落识别 (含高频重复词的段落)
        // 4. 词汇相似度 (Jaccard: |A∩B|/|A∪B|)
        // 5. 最长公共子序列匹配片段
        // 6. 模拟查重率估算
        //    - 有source: simulatedRate = similarity*0.45 + overlap*0.55
        //    - 无source: simulatedRate = localRisk*0.9
    }
    
    /**
     * 引文格式检查
     */
    public CitationCheckResult checkCitations(String text) {
        // 端口 check_citation_format():
        // - 正文引用编号 vs 参考文献编号对应
        // - 作者-年份引用标记检测
        // - 参考文献区编号连续性检查
        // - 缺失/未使用引用报告
    }
    
    /**
     * 降重改写
     * @param mode light/medium/deep
     */
    public SseEmitter reduce(String text, String sourceText, String mode) {
        // 温度参数: light=0.72, medium=0.78, deep=0.84
        double temp = Map.of("light",0.72,"medium",0.78,"deep",0.84).getOrDefault(mode, 0.72);
        // ...prompt渲染 + streamChat
    }
}
```

### 6.2 降重技法（Prompt 内置）

从 `plagiarism.transform` prompt 移植 6 种降重技法：
1. **同义替换** — 词语级 + 成语级
2. **句式转换** — 主动↔被动，长句↔短句，肯定↔双重否定
3. **语序调整** — 状语位移，因果倒置
4. **概念重述** — 等价语义不同表达
5. **段内逻辑重构** — 改变论据呈现顺序
6. **扩写与压缩** — 增加限定语或精炼化

### 6.3 引文保护

必须确保改写后 `\[\d+\]` 引用标记不丢失。AI_paper 已有 `_preserves_citation_marks()` 校验，移植到 Java：
```java
static boolean preservesCitationMarks(String original, String rewritten) {
    Set<String> required = extractRefs(original);  // regex \[\d+\]
    Set<String> current = extractRefs(rewritten);
    return current.containsAll(required);
}
```

---

## 7. UI 设计（Warm Tech 设计系统）

### 7.1 设计令牌

沿用 `2026-05-16-mianmian-design.html` 全部令牌：

```css
:root {
  --bg-canvas: #F3EFE8;      /* 页面底色 */
  --bg-paper: #FDFCFB;       /* 卡片/面板底色 */
  --bg-surface: #F7F7F5;     /* 次级底色 */
  --bg-dark: #141413;        /* 暗色卡片 */

  --text-main: #141413;
  --text-muted: #4A4A4A;
  --text-light: #888888;

  --accent: #D9750A;         /* 琥珀橙强调 */
  --color-success: #22C55E;
  --color-danger: #EF4444;

  --border-light: rgba(0,0,0,0.06);
  --border-medium: rgba(0,0,0,0.10);

  --shadow-sm: 0 1px 2px rgba(0,0,0,0.02);
  --shadow-md: 0 4px 12px rgba(0,0,0,0.06);
  --shadow-lg: 0 16px 32px rgba(0,0,0,0.10);
  --shadow-xl: 0 24px 48px rgba(0,0,0,0.12);

  --font-sans: 'Inter', -apple-system, 'PingFang SC', sans-serif;
  --font-serif: 'Georgia', 'Noto Serif SC', serif;
  --font-mono: 'JetBrains Mono', monospace;
}
```

### 7.2 页面结构：论文工具（新一级导航）

```
首页导航
├── 面试练习
├── 题库刷题
├── 简历优化
└── 论文工具  ← 新增
    ├── 文档润色   (PolishView.vue)
    ├── 降AI      (AiReduceView.vue)
    └── 降查重     (PlagiarismReduceView.vue)
```

### 7.3 通用三栏布局

三个页面共用同一布局框架：

```
┌──────────────────────────────────────────────────────────┐
│  ← 返回    Georgia 标题    tag: 润色方式                 │
├──────────────────────┬───────────────────────────────────┤
│                      │                                   │
│   原文输入区          │   结果预览区                       │
│   (左侧 45%)         │   (右侧 55%)                      │
│                      │                                   │
│   placeholder:        │   流式渲染 + 逐字动画              │
│   "粘贴或拖拽上传      │                                   │
│   论文正文..."        │   [导出 Word ▾]                   │
│                      │   ├ 保留原格式                      │
│   [上传 DOCX]        │   └ 标准导出                        │
│                      │                                   │
│                      │                                   │
├──────────────────────┴───────────────────────────────────┤
│  任务类型 ▾   润色方式 ▾   执行模式 ▾   [开始处理]         │
└──────────────────────────────────────────────────────────┘
```

### 7.4 页面设计详情

#### 7.4.1 文档润色 (PolishView.vue)

```
┌─ Hero 区 ──────────────────────────────────────┐
│  ┌── accent bar ──┐                            │
│  │  学术润色        │  Georgia 32px              │
│  │  ACADEMIC POLISH │  Inter 11px, accent       │
│  │  精准优化论文表达，从语法到逻辑全面提升         │
│  └─────────────────┘                            │
├─ 功能卡片区 ────────────────────────────────────┤
│  ┌──────────┐ ┌──────────┐ ┌──────────┐        │
│  │ 语法修正   │ │ 词汇优化  │ │ 逻辑强化  │  ...   │
│  │ icon      │ │ icon     │ │ icon     │        │
│  └──────────┘ └──────────┘ └──────────┘        │
├─ 编辑区 ───────────────────────────────────────┤
│  ┌─────────────┐ ┌─────────────────────────┐   │
│  │ 原文输入     │ │ 结果预览                 │   │
│  │             │ │                         │   │
│  │ [上传DOCX]  │ │ (SSE流式渲染)           │   │
│  │             │ │                         │   │
│  │ textarea    │ │ diff-highlight          │   │
│  └─────────────┘ └─────────────────────────┘   │
├─ 控制栏 ───────────────────────────────────────┤
│  任务: [章节正文 ▾]  方式: [全面润色 ▾]         │
│  模式: [标准模式 ▾]  主题: [________]           │
│  [开始润色]  [导出 ▾]                          │
└────────────────────────────────────────────────┘
```

关键交互：
- 左侧输入实时字符计数
- 右侧 SSE 流式打字机效果（复用 InterviewView 的动画组件）
- 润色完成后的 diff 对比视图（复用 UnifiedDiff.vue）
- 导出下拉：保留原格式 / 标准导出

#### 7.4.2 降AI (AiReduceView.vue)

```
┌─ Hero ─────────────────────────────────────────┐
│  ┌── accent bar ──┐                            │
│  │  降AI检测        │                           │
│  │  AI TRACE REDUCTION                         │
│  │  识别并弱化AI写作痕迹，使文本更接近真实学者写作  │
│  └─────────────────┘                           │
├─ 扫描结果卡片（处理前展示）───────────────────────┤
│  ┌─────────────────────────────────────────┐   │
│  │  AI痕迹风险: ████████░░ 75% (高风险)      │   │
│  │                                         │   │
│  │  ⚠ 发现高频模板表达：首先..其次..最后       │   │
│  │  ⚠ 句子长度分布过于均匀 (var=89)          │   │
│  │  ⚠ 连接词使用偏密集，共出现 23 次          │   │
│  │  ⚠ 段落长度过于整齐                       │   │
│  │                                         │   │
│  │  [查看标记段落]                           │   │
│  └─────────────────────────────────────────┘   │
├─ 编辑区 ───────────────────────────────────────┤
│  同三栏布局                                     │
├─ 控制栏 ───────────────────────────────────────┤
│  强度: [轻度去痕] [深度重构] [学术拟合]           │
│  [开始改写]  [导出 ▾]                          │
└────────────────────────────────────────────────┘
```

关键交互：
- 先 `scan` 展示风险评分和具体问题
- 用户确认后 `rewrite` 进行改写
- 风险卡片用 amber/orange/red 色阶表示低/中/高风险

#### 7.4.3 降查重 (PlagiarismReduceView.vue)

```
┌─ Hero ─────────────────────────────────────────┐
│  ┌── accent bar ──┐                            │
│  │  降查重          │                           │
│  │  PLAGIARISM REDUCTION                        │
│  │  预检重复表达，优化文本降低查重率               │
│  └─────────────────┘                           │
├─ 对比输入区 ────────────────────────────────────┤
│  ┌──────────────────┐ ┌──────────────────┐     │
│  │ 待检测正文        │ │ 重复源/查重报告    │     │
│  │                  │ │ (可选)            │     │
│  │ textarea         │ │ textarea          │     │
│  └──────────────────┘ └──────────────────┘     │
├─ 检测结果 ──────────────────────────────────────┤
│  ┌─────────────────────────────────────────┐   │
│  │  模拟查重率: 42.3% (高风险)               │   │
│  │  重复短语: 15处  风险段落: 4处             │   │
│  │  源文本重叠: 38.5%                        │   │
│  │                                         │   │
│  │  匹配片段:                               │   │
│  │  "机器学习作为人工智能的核心..."           │   │
│  │  "本文提出了一种基于深度学习的..."         │   │
│  └─────────────────────────────────────────┘   │
├─ 编辑区 ───────────────────────────────────────┤
│  同三栏布局                                     │
├─ 控制栏 ───────────────────────────────────────┤
│  强度: [轻度 ▾] [中度 ▾] [深度 ▾]               │
│  [开始降重]  [导出 ▾]                          │
└────────────────────────────────────────────────┘
```

### 7.5 组件复用清单

| 现有组件 | 用途 | 所在页面 |
|----------|------|---------|
| `ParticleBg.vue` | 背景粒子效果 | 三个页面 Hero 区 |
| `SplitText.vue` | 文字入场动画 | Hero 标题 |
| `GlareCard.vue` | 功能卡片光效 | 功能选择区 |
| `UnifiedDiff.vue` | 改写前后对比 | 结果预览区 |
| `SkeletonBar.vue` | 加载骨架屏 | 扫描等待态 |
| `ScrollReveal.vue` | 滚动渐入 | 页面整体 |
| SSE Chat 动画 | 打字机流式效果 | 结果预览区（从 InterviewView 提取为 composable） |

### 7.6 路由配置

```typescript
// router/index.ts 新增
{
  path: '/paper-tools',
  children: [
    { path: 'polish', name: 'Polish', component: () => import('@/views/PolishView.vue') },
    { path: 'ai-reduce', name: 'AiReduce', component: () => import('@/views/AiReduceView.vue') },
    { path: 'plagiarism-reduce', name: 'PlagiarismReduce', component: () => import('@/views/PlagiarismReduceView.vue') },
  ]
}
```

### 7.7 导航入口

首页 `HomeView.vue` 的 `func-grid` 新增卡片：

```html
<!-- 第四个功能卡片，with accent bar -->
<div class="func-card accent">
  <div class="func-card-top">
    <div class="func-icon-box func-icon-amber">📝</div>
    <span class="func-tag">NEW</span>
  </div>
  <span class="func-title">论文工具</span>
  <span class="func-desc">润色 · 降AI · 降查重，一站式论文优化</span>
</div>
```

---

## 8. 实施计划

### Phase 1: 格式保留导出 (2 天)

- [ ] 创建 `ParagraphProfile` 记录类
- [ ] 改造 `TemplatePreservingExportService`:
  - 新增 `parseParagraphs(byte[] docx)`: 提取段落格式快照
  - 新增 `buildPrompt(List<ParagraphProfile>)`: 构造 [Pn] 标记 prompt
  - 新增 `parseAiResponse(String)`: 解析 [Pn] 响应
  - 改造 `writeBack(List<ParagraphProfile>, Map<Integer,String>)`: 按索引写回
- [ ] 新增端点 `POST /api/export/preserve-format`
- [ ] 单元测试：段落提取 → prompt构造 → 响应解析 → 写回 全链路

### Phase 2: 学术润色 (2 天)

- [ ] 移植 prompt 模板至 `resources/prompts/polish/`
- [ ] 创建 `PolishService` 含 `runPolish()` + `scanFormat()`
- [ ] 创建 `PolishController` 含 SSE 端点
- [ ] 创建 `PolishView.vue` 前端页面
- [ ] 集成 `UnifiedDiff.vue` 做改写前后对比
- [ ] 集成导出下拉（保留格式 / 标准导出）

### Phase 3: 降AI (2 天)

- [ ] 移植 AI 痕迹检测规则（12 regex + 句长方差 + 段落均匀度 + 连接词密度）
- [ ] 移植降AI prompt 模板
- [ ] 创建 `AiReduceService` + `AiReduceController`
- [ ] 创建 `AiReduceView.vue` 前端页面
- [ ] 实现扫描→展示风险→确认改写 交互流

### Phase 4: 降查重 (2 天)

- [ ] 移植重复检测规则（短语统计 + 长句检测 + 相似度 + LCS）
- [ ] 移植降查重 prompt 模板
- [ ] 创建 `PlagiarismReduceService` + `PlagiarismController`
- [ ] 创建 `PlagiarismReduceView.vue` 前端页面
- [ ] 实现双输入（正文 + 重复源）+ 风险卡片

### Phase 5: 联调与测试 (1 天)

- [ ] 三条流式链路端到端测试
- [ ] 格式保留导出回归测试（含回退路径）
- [ ] 移动端响应式适配（复用 InterviewView 的移动端布局经验）

---

## 9. 风险与缓解

| 风险 | 概率 | 缓解措施 |
|------|------|---------|
| AI 不遵守 [Pn] 标记约束 | 中 | 回退到裸 markdown 导出；增加 prompt 强调 |
| 段落数偏差 | 中 | 偏差 <20% 时逐段尽力匹配，否则整体降级 |
| Python regex → Java regex 差异 | 低 | 全部使用 `Pattern.UNICODE_CHARACTER_CLASS` |
| python-docx → Apache POI 行为差异 | 低 | 中文字体用 `w:eastAsia` 属性设置，已在现有代码验证 |
| SSE 长连接超时 | 低 | 设置 120s timeout，30s 心跳保活 |
| 大型 DOCX 解析性能 | 低 | 只取前 200 段落，表格和超大文档提醒用户 |

---

## 10. 文件清单

### 后端新增

```
mianmiantong-server/src/main/java/com/mianmiantong/
├── controller/
│   ├── PolishController.java
│   ├── AiReduceController.java
│   ├── PlagiarismReduceController.java
│   └── ExportController.java (改造，新增 /export/preserve-format)
├── service/
│   ├── paper/
│   │   ├── PolishService.java
│   │   ├── AiReduceService.java
│   │   └── PlagiarismReduceService.java
│   └── document/
│       ├── ParagraphProfile.java          (新增)
│       └── TemplatePreservingExportService.java (改造)
└── resources/
    └── prompts/
        ├── polish_run_task.txt
        ├── polish_grammar.txt
        ├── polish_academic_vocab.txt
        ├── polish_logic.txt
        ├── polish_full.txt
        ├── polish_translate.txt
        ├── ai_reduce_transform.txt
        └── plagiarism_transform.txt
```

### 前端新增

```
AI-Interview/web-app/src/
├── views/
│   ├── PolishView.vue
│   ├── AiReduceView.vue
│   └── PlagiarismReduceView.vue
├── components/
│   └── paper/
│       ├── TextComparePanel.vue    (左右对比面板)
│       ├── RiskScoreCard.vue       (风险评分卡片)
│       └── PolishControlBar.vue    (润色控制栏)
├── composables/
│   └── useStreamPolish.ts         (SSE 流式 composable)
└── router/
    └── index.ts                    (改造，新增 paper-tools 路由)
```

---

> **下一步**：确认设计后进入 Phase 1 — 格式保留导出实现。
