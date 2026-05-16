# 简历分析系统 — 修复总结与架构文档

> 2026-05-15 | 从诊断到修复，记录所有改动和架构决策

---

## 一、系统架构

```
┌──────────────┐     ┌─────────────────┐     ┌──────────────┐
│  uni-app 小程序 │ ──▶ │  Spring Boot API │ ──▶ │    MySQL     │
│  (Vue 3 + TS) │ ◀── │  (Java 17)      │ ◀── │  (MyBatis+)  │
└──────────────┘     └───────┬─────────┘     └──────────────┘
                             │
                    ┌────────┴────────┐
                    │        │        │
                    ▼        ▼        ▼
              阿里云DocMind  DeepSeek  (预留Qwen)
              (文档解析)    (AI分析)
```

### 数据流

```
上传 docx → Alibaba DocMind 解析 → parseStatus=1 (parsedText 已填充)
                                          │
                          GET /api/resume/{id}/analysis
                          (getReport 自动检测 parseStatus=1
                           且无分析记录 → 自动触发评分)
                                          │
                    ┌─────────────────────┤
                    ▼                     ▼
           Phase 1: 快速评分        Phase 2: 深度优化
           analyzeQuickAsync()      analyzeDeepStream()
           (异步 CompletableFuture)  (SSE 流式)
                    │                     │
                    ▼                     ▼
              overallScore            highlights[]
              dimensions[]           optimizedText
              missingKeywords[]      interviewQuestions
              suggestion             
```

### 关键表结构

- `resume` — 简历文件 + 解析状态(`parseStatus`: 0解析中/1完成/-1失败)
- `resume_analysis` — AI分析结果(`deepStatus`: 0待优化/1进行中/2完成/-1失败)
- `resume_template` — Word导出模板

---

## 二、本轮修复清单

### Fix 1: Jackson 容错 — AI JSON 解析

**问题:** `JsonParseException: Illegal unquoted character (CTRL-CHAR, code 10)`  
**根因:** DeepSeek 返回的 JSON 中 `optimizedText`(Markdown简历)包含真实换行符，不符合 JSON 标准  
**文件:** `ResumeAnalysisService.java:72`  
**修复:** ObjectMapper 启用 `ALLOW_UNESCAPED_CONTROL_CHARS`

```java
this.objectMapper.configure(
    JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
```

### Fix 2: JSON 提取 — 括号深度计数

**问题:** `extractJson` 用 `lastIndexOf("}")` → 被 Markdown 内容中的花括号误导  
**文件:** `ResumeAnalysisService.java` `extractJson()`  
**修复:** 用括号深度计数法，跳过字符串内的 `{}`、处理转义字符、自动去掉 markdown 围栏

### Fix 3: 模板保留导出 — 简化为 Markdown 导出

**问题:** AI 的 `before` 文本和原始 docx 段落无法精确匹配(4/4都失败) → 抛出异常  
**文件:** `ResumeController.java` `exportWord()`、`previewHtml()`  
**修复:** 直接使用 `wordExportService.exportMarkdown()` 从优化文本生成 Word  
**用户决策:** 模板保留太难，暂搁置。通过 Diff 视图(before→after)查看修改点

### Fix 4: 后端自动触发分析

**问题:** 前端 `POST /analyze` 请求经常不到达后端 → `resume_analysis` 永不被创建 → 无限轮询  
**文件:** `ResumeAnalysisService.java:370` `getReport()`  
**修复:** `getReport()` 检测到 `parseStatus=1` 且无分析记录时，自动 fire `analyzeQuickAsync`  
前端只需轮询 `GET /analysis`，无需额外 POST

### Fix 5: 前端轮询优化

**问题:** 60次×2秒=2分钟轮询，`catch{}`吞错误，停不下来  
**文件:** `pages/resume/report.vue`  
**修复:** 
- 20次×2秒=40秒，超时提示"AI分析较慢，稍后刷新"
- `analyzeTriggered` 防重复触发
- `validReport` 过滤请求失败时的空数组假数据
- 深度优化入口仅在评分完成后显示(`v-if="score > 0"`)

### Fix 6: JWT Filter — URL参数token

**问题:** `preview-html` 通过 `?token=xxx` 传token → Filter只从Header读 → 403  
**文件:** `JwtAuthFilter.java` `extractToken()`  
**修复:** 增加 `request.getParameter("token")` 兜底读取

### Fix 7: dev-token 跳过双重校验

**问题:** Filter通过 → Controller又校验 `jwtUtil.validateToken(token)` → dev-token 不是真JWT → 400  
**文件:** `ResumeController.java` `previewHtml()`  
**修复:** `token.startsWith("dev-token-")` 时跳过JWT校验

### Fix 8: SSE partial 写入频率

**问题:** 每5秒全量写 LONGBLOB(3000+字符) → DB I/O浪费  
**文件:** `ResumeAnalysisService.java:210`  
**修复:** 5秒→30秒

### Fix 9: Partial 恢复优先于重试次数检查

**问题:** `retryCount=3`时直接拒绝 → 已有完整数据的 `partial_response` 无法恢复  
**文件:** `ResumeAnalysisService.java:125` `analyzeDeepStream()`  
**修复:** 恢复检查移到重试次数检查→之前

---

## 三、当前运行状态

| 步骤 | 状态 | 说明 |
|------|------|------|
| 上传简历 | ✅ 正常 | 支持 PDF/DOCX/JPG/PNG |
| 阿里云解析 | ✅ 正常 | 开通DocMind后可用 |
| Phase 1 快速评分 | ✅ 正常 | 30秒内完成，5维度评分 |
| Phase 2 深度优化 | ✅ 正常 | SSE流式，完成后展示Diff |
| Diff 对比视图 | ✅ 正常 | GitHub风格，行号、+/-、绿增红删、折叠展开 |
| 导出 Word | ✅ 正常 | Markdown渲染，不含原格式 |
| 预览 HTML | ✅ 正常 | docx→HTML→小程序web-view |
| 模板保留 | ❌ 搁置 | AI文本与原文档精确匹配不可靠 |

---

## 四、已知限制

1. **模板保留导出不可靠** — AI生成的 before 文本和原始 docx 段落精确匹配几乎不可能(模糊匹配连一条都匹配不上)
2. **评分偶尔慢** — DeepSeek API 响应时间10-40秒不等，依赖网络和模型负载
3. **阿里云DocMind依赖** — 需要AccessKey有DocMind权限，否则parseStatus=-1永久失败
4. **小程序需重新编译** — 前端改动需要HBuilderX/微信开发者工具重新编译才能生效

---

## 五、关键文件索引

| 层 | 文件 | 职责 |
|----|------|------|
| 前端 | `pages/resume/report.vue` | 报告页：评分+维度+深度优化Diff+导出 |
| 前端 | `pages/resume/upload.vue` | 上传页：文件上传+JD输入 |
| 前端 | `components/UnifiedDiff.vue` | GitHub风格Diff组件(LCS算法) |
| 前端 | `utils/request.ts` | API封装，timeout=30s |
| 后端 | `controller/resume/ResumeController.java` | REST接口 |
| 后端 | `service/resume/ResumeService.java` | 上传+解析+重试 |
| 后端 | `service/resume/ResumeAnalysisService.java` | AI分析(快速+深度) |
| 后端 | `service/document/DocumentAiService.java` | 阿里云DocMind |
| 后端 | `service/ai/DeepSeekAiService.java` | DeepSeek API |
| 后端 | `service/document/TemplatePreservingExportService.java` | (已弃用)模板保留导出 |
| 后端 | `config/JwtAuthFilter.java` | JWT认证(含dev-token+URL参数) |
| 后端 | `.env` | 本地密钥(AccessKey/API Key/JWT Secret) |
