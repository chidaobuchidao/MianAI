# 简历深度优化 — 高优问题修复设计

> 日期: 2026-05-13 | 状态: 已确认 | 关联: TODO-优化计划 #2 #3 #4

---

## 1. 概述

修复简历优化模块 3 个高优问题：
- **#2** 深度优化失败后无重试机制
- **#3** 分析超时后无中间状态保存
- **#4** 上传页无已有简历的"继续优化"入口

核心变更：深度优化从"轮询"改为"SSE 流式"，统一解决超时和中间状态问题。

---

## 2. 数据模型

`resume_analysis` 表新增 2 字段：

```sql
ALTER TABLE resume_analysis
  ADD COLUMN retry_count INT DEFAULT 0 COMMENT '深度优化重试次数',
  ADD COLUMN partial_response MEDIUMTEXT COMMENT '深度优化中间结果';
```

迁移文件: `V6__resume_analysis_deep.sql`

---

## 3. API 变更

### 3.1 深度优化 SSE 流式（改造）

```
POST /api/resume/{resumeId}/analyze-deep
Content-Type: application/json
Authorization: Bearer <token>
```

SSE 事件流:

| event | data | 说明 |
|-------|------|------|
| `token` | 文本片段 | AI 输出 token |
| `progress` | `{"stage":"scoring","percent":60}` | 分析进度 |
| `finish` | `{"overallScore":7,...}` | 完成 |
| `error` | `{"message":"...","retryCount":1}` | 失败（含重试次数） |

### 3.2 重试（新增）

```
POST /api/resume/{resumeId}/retry-deep
```

检查 retryCount < 3，复用 partialResponse 续传。

### 3.3 轮询状态（保留）

```
GET /api/resume/{resumeId}/deep-status
```

返回 `{deepStatus, retryCount, partialResponseLength}`

---

## 4. 后端改动

### 4.1 ResumeAnalysisService

- `analyzeDeepStream(resumeId)` — SSE 流式深度优化：
  - SseEmitter timeout 600s
  - 每收到 token → `emitter.send(event:token)`
  - 每 5s → 将 buffer 写入 `ResumeAnalysis.partialResponse`
  - retryCount >= 3 → 直接返回 error
  - AI 异常 → deepStatus=-1, partialResponse 保留
- `retryDeepOptimize(resumeId)` — 检查 retryCount，重置 deepStatus=1
- `getDeepStatus(resumeId)` — 新增返回 retryCount 和 partialResponseLength

### 4.2 ResumeController

- `/api/resume/{id}/analyze-deep` — 改造为返回 SseEmitter
- `/api/resume/{id}/retry-deep` — 新增重试端点

### 4.3 ResumeAnalysis 实体

新增字段: `retryCount`, `partialResponse`

---

## 5. 前端改动

### 5.1 report.vue

- 深度优化从轮询改为 SSE 流式接收
- wx.request timeout 600s, enableChunked: true
- 失败时展示重试面板（当前次数/最多3次）
- 30s 无事件心跳检测，弹 toast 安抚用户
- SSE 断连时优先检查后端 deepStatus 再决定是否重连

### 5.2 upload.vue

- 页面新增「历史记录」入口链接

### 5.3 history.vue（新建）

- 简历历史列表页
- 展示：文件名、岗位、状态标签、评分、时间
- 点击进入 report 页
- 右滑删除

### 5.4 index.vue

- 简历卡片改为两行布局：上传新简历 / 历史记录(N)

### 5.5 pages.json

- 注册 `pages/resume/history` 路由

### 5.6 request.ts

- streamRequest timeout 调整为 600s

---

## 6. 重试与断点续传流程

```
深度优化开始
  │
  ├─ retryCount >= 3? → 返回 error, 前端显示"已达最大重试次数"
  │
  ├─ retryCount == 0? → 全新开始, partialResponse 为空
  └─ retryCount > 0?  → 检查 partialResponse
                          ├─ 非空 → 追加 prompt "请继续输出"
                          └─ 为空 → 全新开始
  │
  ├─ 每 5s 保存 partialResponse
  │
  ├─ 正常完成 → deepStatus=2, partialResponse 清空
  └─ 异常 → deepStatus=-1, partialResponse 保留, 前端显示重试面板
```

---

## 7. 改动文件清单

| 文件 | 改动 |
|------|------|
| `mianmiantong-server/src/main/java/.../entity/resume/ResumeAnalysis.java` | +retryCount, +partialResponse |
| `mianmiantong-server/src/main/java/.../service/resume/ResumeAnalysisService.java` | SSE 流式 + retry + partial |
| `mianmiantong-server/src/main/java/.../controller/resume/ResumeController.java` | analyze-deep 改 SSE, +retry-deep |
| `mianmiantong-server/src/main/resources/db/migration/V6__resume_analysis_deep.sql` | 迁移 |
| `AI-Interview/pages/resume/report.vue` | SSE 接收 + 重试面板 |
| `AI-Interview/pages/resume/upload.vue` | +历史入口 |
| `AI-Interview/pages/resume/history.vue` | 新建 |
| `AI-Interview/pages/index/index.vue` | 简历卡片两行 |
| `AI-Interview/pages.json` | +history 路由 |
| `AI-Interview/utils/request.ts` | timeout 600s |
