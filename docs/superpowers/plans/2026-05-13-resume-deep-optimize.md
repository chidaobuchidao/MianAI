# 简历深度优化 — 高优问题修复 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复深度优化无重试、无中间状态保存、缺少简历历史入口 3 个高优问题，核心变更是将深度优化从轮询改为 SSE 流式。

**Architecture:** 后端新增 `retryCount` 和 `partialResponse` 字段支持重试和断点续传；前端 `report.vue` 从轮询切换为 SSE 流式接收；新建 `history.vue` 简历历史页；首页和上传页增加历史入口。

**Tech Stack:** Spring Boot 3.2.0 / MyBatis-Plus 3.5.5 / DeepSeek SSE 流式 / uni-app Vue 3 / 微信小程序

---

## Task 1: 数据库迁移 V6

**Files:**
- Create: `mianmiantong-server/src/main/resources/db/migration/V6__resume_analysis_deep.sql`

- [ ] **Step 1: 编写迁移 SQL**

```sql
-- V6__resume_analysis_deep.sql - 深度优化重试与断点续传
ALTER TABLE resume_analysis
  ADD COLUMN retry_count INT DEFAULT 0 COMMENT '深度优化重试次数',
  ADD COLUMN partial_response MEDIUMTEXT COMMENT '深度优化中间结果（断点续传）';
```

- [ ] **Step 2: 执行迁移**

```bash
# 临时启用 Flyway 执行迁移
cd mianmiantong-server
# 修改 application.yml: flyway.enabled: true
mvn spring-boot:run -q &
sleep 10
# 确认字段已添加
# 改回 flyway.enabled: false
```

- [ ] **Step 3: 验证**

```bash
curl -s http://localhost:8080/doc.html | head -1
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/db/migration/V6__resume_analysis_deep.sql
git commit -m "feat: add retry_count and partial_response to resume_analysis"
```

---

## Task 2: 更新 ResumeAnalysis 实体

**Files:**
- Modify: `mianmiantong-server/src/main/java/com/mianmiantong/entity/resume/ResumeAnalysis.java`

- [ ] **Step 1: 新增字段**

在 `deepStatus` 字段后新增：

```java
private Integer deepStatus; // 0待优化 1进行中 2已完成 -1失败
private Integer retryCount; // 深度优化重试次数
private String partialResponse; // 深度优化中间结果
```

- [ ] **Step 2: 编译验证**

```bash
cd mianmiantong-server && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/mianmiantong/entity/resume/ResumeAnalysis.java
git commit -m "feat: add retryCount and partialResponse fields to ResumeAnalysis entity"
```

---

## Task 3: ResumeAnalysisService — SSE 流式深度优化 + 重试 + 断点续传

**Files:**
- Modify: `mianmiantong-server/src/main/java/com/mianmiantong/service/resume/ResumeAnalysisService.java`

核心改动：新增 `analyzeDeepStream()` SSE 流式方法，改造 `analyzeDeepAsync()` 增加 retryCount 检查，新增 `retryDeepOptimize()`。

- [ ] **Step 1: 新增 analyzeDeepStream() 方法（替换 analyzeDeepAsync 的 SSE 版本）**

在 `analyzeQuickAsync()` 方法之后插入：

```java
/** Phase 2: 深度优化 SSE 流式（含重试与断点续传） */
public SseEmitter analyzeDeepStream(Long resumeId) {
    Resume resume = resumeMapper.selectById(resumeId);
    ResumeAnalysis analysis = upsert(resumeId);

    // 检查重试次数
    int retryCount = analysis.getRetryCount() != null ? analysis.getRetryCount() : 0;
    if (retryCount >= 3) {
        SseEmitter rejectEmitter = new SseEmitter();
        rejectEmitter.onCompletion(() -> {});
        try {
            rejectEmitter.send(SseEmitter.event().name("error")
                    .data(escapeJson("{\"message\":\"已达最大重试次数(3次)\",\"retryCount\":" + retryCount + "}")));
            rejectEmitter.complete();
        } catch (Exception ignored) {}
        return rejectEmitter;
    }

    // 标记进行中，重试次数+1
    analysis.setRetryCount(retryCount + 1);
    analysis.setDeepStatus(1);
    save(analysis);

    // 构建 prompt
    String basePrompt = buildDeepPrompt(resume, analysis);

    // 如果有 partial，续传
    String partial = analysis.getPartialResponse();
    String userMessage;
    if (partial != null && !partial.isBlank()) {
        userMessage = "之前的输出被中断，已输出的内容：\n" + partial + "\n\n请从断点处继续输出完整的JSON，不要重复已输出的内容。";
    } else {
        userMessage = "开始深度优化";
    }

    List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", userMessage));
    SseEmitter emitter = new SseEmitter(600_000L);
    StringBuilder buf = new StringBuilder();
    if (partial != null) buf.append(partial);

    emitter.onTimeout(() -> {
        safeSavePartial(analysis, buf.toString());
        safeSend(emitter, "error", escapeJson(
                "{\"message\":\"分析超时，已保存中间结果，可重试\",\"retryCount\":" + analysis.getRetryCount() + "}"));
        emitter.complete();
    });

    emitter.onError(ex -> {
        safeSavePartial(analysis, buf.toString());
    });

    CompletableFuture.runAsync(() -> {
        try {
            final long[] lastSaveTime = {System.currentTimeMillis()};

            aiService.streamChat(basePrompt, messages, null, token -> {
                buf.append(token);
                safeSend(emitter, "token", token);

                // 每 5 秒保存中间结果
                long now = System.currentTimeMillis();
                if (now - lastSaveTime[0] >= 5000) {
                    lastSaveTime[0] = now;
                    safeSavePartial(analysis, buf.toString());
                }
            });

            // 解析最终 JSON
            String fullResponse = buf.toString();
            Map<String, Object> report = objectMapper.readValue(extractJson(fullResponse), Map.class);

            analysis.setHighlights(toJson(report.get("highlights")));
            analysis.setOptimizedText((String) report.get("optimizedText"));
            analysis.setInterviewQuestions(toJson(report.get("interviewQuestions")));
            analysis.setDeepStatus(2);
            analysis.setPartialResponse(null); // 清除中间结果
            save(analysis);

            emitter.send(SseEmitter.event().name("finish")
                    .data(objectMapper.writeValueAsString(Map.of(
                            "resumeId", resumeId, "deepStatus", 2,
                            "phase", "deep"))));
            emitter.complete();
            log.info("深度优化(SSE)完成: resumeId={}, retry={}", resumeId, analysis.getRetryCount());

        } catch (Exception e) {
            log.error("深度优化(SSE)失败: resumeId={}", resumeId, e);
            safeSavePartial(analysis, buf.toString());
            safeSend(emitter, "error", escapeJson(
                    "{\"message\":\"AI分析失败\",\"retryCount\":" + analysis.getRetryCount() + "}"));
            try {
                analysis.setDeepStatus(-1);
                save(analysis);
            } catch (Exception ignored) {}
            emitter.complete();
        }
    });

    return emitter;
}
```

- [ ] **Step 2: 新增辅助方法**

在类末尾（`parseJson` 之后）插入：

```java
/** 构建深度优化 prompt */
private String buildDeepPrompt(Resume resume, ResumeAnalysis analysis) {
    String keywords = analysis.getMissingKeywords() != null ? analysis.getMissingKeywords() : "无";
    return String.format(DEEP_PROMPT, resume.getJobDescription(),
            analysis.getOverallScore() != null ? analysis.getOverallScore() : 5,
            keywords, resume.getParsedText());
}

/** 安全保存 partialResponse */
private void safeSavePartial(ResumeAnalysis analysis, String partial) {
    try {
        analysis.setPartialResponse(partial);
        save(analysis);
    } catch (Exception e) {
        log.warn("保存中间结果失败: resumeId={}", analysis.getResumeId());
    }
}

/** JSON 字符串转义（用于 SSE data 字段） */
private String escapeJson(String json) {
    return json.replace("\\", "\\\\").replace("\"", "\\\"");
}
```

- [ ] **Step 3: 新增 retryDeepOptimize() 方法**

在 `getDeepStatus()` 之前插入：

```java
/** 检查是否可以重试 */
public Map<String, Object> retryDeepOptimize(Long resumeId) {
    ResumeAnalysis analysis = analysisMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResumeAnalysis>()
                    .eq(ResumeAnalysis::getResumeId, resumeId));
    Map<String, Object> result = new LinkedHashMap<>();
    if (analysis == null) {
        result.put("retryable", false);
        result.put("message", "分析记录不存在");
        return result;
    }
    int count = analysis.getRetryCount() != null ? analysis.getRetryCount() : 0;
    result.put("retryable", count < 3);
    result.put("retryCount", count);
    result.put("remaining", 3 - count);
    result.put("hasPartial", analysis.getPartialResponse() != null
            && !analysis.getPartialResponse().isBlank());
    return result;
}
```

- [ ] **Step 4: 更新 getDeepStatus() 返回更多信息**

替换现有 `getDeepStatus()` 方法：

```java
/** 获取深度优化状态（含重试与断点信息） */
public Map<String, Object> getDeepStatus(Long resumeId) {
    ResumeAnalysis analysis = analysisMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResumeAnalysis>()
                    .eq(ResumeAnalysis::getResumeId, resumeId));
    Map<String, Object> result = new LinkedHashMap<>();
    if (analysis == null) {
        result.put("deepStatus", null);
        return result;
    }
    result.put("deepStatus", analysis.getDeepStatus());
    result.put("retryCount", analysis.getRetryCount() != null ? analysis.getRetryCount() : 0);
    result.put("hasPartial", analysis.getPartialResponse() != null
            && !analysis.getPartialResponse().isBlank());
    return result;
}
```

- [ ] **Step 5: 移除待 deepStatus=-1 时的 dead code**

现有 `analyzeDeepAsync()` 方法保留但标记为 `@Deprecated`，SSE 版本上线后可移除。暂保留以兼容。

- [ ] **Step 6: 编译验证**

```bash
cd mianmiantong-server && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/mianmiantong/service/resume/ResumeAnalysisService.java
git commit -m "feat: add SSE streaming deep optimize with retry and partial save"
```

---

## Task 4: ResumeController — 改造 analyze-deep 为 SSE，新增 retry-deep

**Files:**
- Modify: `mianmiantong-server/src/main/java/com/mianmiantong/controller/resume/ResumeController.java`

- [ ] **Step 1: 改造 analyzeDeep 为 SSE 流式**

替换现有的 `analyzeDeep()` 方法：

```java
/** Phase 2: 深度优化 SSE 流式 */
@PostMapping("/{resumeId}/analyze-deep")
public SseEmitter analyzeDeep(@PathVariable Long resumeId) {
    return analysisService.analyzeDeepStream(resumeId);
}
```

- [ ] **Step 2: 新增 retry-deep 端点**

在 `analyzeDeep()` 之后插入：

```java
/** 检查重试状态并触发重试 */
@PostMapping("/{resumeId}/retry-deep")
public SseEmitter retryDeep(@PathVariable Long resumeId) {
    return analysisService.analyzeDeepStream(resumeId);
}

/** 查询是否可重试 */
@GetMapping("/{resumeId}/retry-deep")
public Result<?> retryDeepStatus(@PathVariable Long resumeId) {
    return Result.ok(analysisService.retryDeepOptimize(resumeId));
}
```

- [ ] **Step 3: 编译验证**

```bash
cd mianmiantong-server && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/mianmiantong/controller/resume/ResumeController.java
git commit -m "feat: convert analyze-deep to SSE, add retry-deep endpoint"
```

---

## Task 5: 启动验证后端

**Files:** none (manual verification)

- [ ] **Step 1: 启动后端**

```bash
cd mianmiantong-server && mvn spring-boot:run -q &
sleep 10
```

- [ ] **Step 2: 验证端点可访问**

```bash
# 登录
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"code":"test123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

# 验证 deep-status 端点
curl -s http://localhost:8080/api/resume/1/deep-status \
  -H "Authorization: Bearer $TOKEN"

# 验证 retry-deep 端点
curl -s http://localhost:8080/api/resume/1/retry-deep \
  -H "Authorization: Bearer $TOKEN"
```

Expected: 两个端点均返回 JSON（即使 resume 不存在也会 500，说明端点注册成功）

- [ ] **Step 5: Commit** (if not already committed above)

---

## Task 6: request.ts — SSE timeout 延长到 600s

**Files:**
- Modify: `AI-Interview/utils/request.ts`

- [ ] **Step 1: 修改 streamRequest 中的 timeout**

将 `streamRequest()` 函数中的 `timeout: 180000` 改为 `timeout: 600000`：

```typescript
// 第 142 行
timeout: 600000,
```

- [ ] **Step 2: 验证无编译错误**

在 HBuilderX 中运行 TypeScript 检查，或确认文件保存后无 lint 警告。

- [ ] **Step 3: Commit**

```bash
git add AI-Interview/utils/request.ts
git commit -m "fix: extend SSE stream timeout to 600s for deep optimization"
```

---

## Task 7: report.vue — 深度优化从轮询切换到 SSE 流式

**Files:**
- Modify: `AI-Interview/pages/resume/report.vue`

核心改动：`startDeepOptimize()` 从 POST + 轮询改为 SSE 流式；失败时不再只弹 toast，改为显示重试面板；新增心跳检测。

- [ ] **Step 1: 新增重试面板 UI**

在 `<template>` 中，`deepStatus === 1` 的 running 状态之后，`deepStatus === 2` 之前，插入失败面板（deepStatus === -1）：

```vue
<!-- 失败：重试面板 -->
<view v-if="deepStatus === -1" class="deep-failed">
  <text class="deep-fail-text">深度优化失败</text>
  <text class="deep-fail-hint" v-if="retryRemaining > 0">还可重试 {{ retryRemaining }} 次</text>
  <text class="deep-fail-hint" v-else>已达最大重试次数</text>
  <view class="btn-row" style="justify-content:center;margin-top:20rpx">
    <button v-if="retryRemaining > 0" class="btn-retry" @click="retryDeepOptimize">重新优化</button>
    <button class="btn-back" @click="goHome">返回首页</button>
  </view>
</view>
```

- [ ] **Step 2: 新增 SSE 流式接收逻辑**

替换 `startDeepOptimize()` 和 `startPollDeep()` 方法。在 `<script setup>` 中：

```typescript
const retryCount = ref(0);
const retryRemaining = ref(3);
let deepAbort: (() => void) | null = null;
let heartbeatTimer: ReturnType<typeof setInterval> | null = null;
let lastTokenTime = 0;

function startDeepOptimize() {
  if (!report.value) return;
  const resumeId = report.value.resumeId;
  deepStatus.value = 1;
  deepElapsed.value = 0;
  deepTimer = setInterval(() => deepElapsed.value++, 1000);
  lastTokenTime = Date.now();

  uni.setStorageSync('deep_optimizing', JSON.stringify({
    resumeId, name: report.value.fileName || '简历'
  }));

  doStreamDeep(resumeId);
}

function retryDeepOptimize() {
  if (!report.value) return;
  const resumeId = report.value.resumeId;
  deepStatus.value = 1;
  deepElapsed.value = 0;
  deepTimer = setInterval(() => deepElapsed.value++, 1000);
  lastTokenTime = Date.now();
  doStreamDeep(resumeId);
}

function doStreamDeep(resumeId: number) {
  const token = uni.getStorageSync('mianmiantong_token') || '';
  const tokenList: string[] = [];
  lastTokenTime = Date.now();

  // 心跳检测：30 秒无 token 则提示
  heartbeatTimer = setInterval(() => {
    if (Date.now() - lastTokenTime > 30000 && deepStatus.value === 1) {
      uni.showToast({ title: 'AI 响应较慢，请耐心等待', icon: 'none', duration: 2000 });
      lastTokenTime = Date.now(); // 重置，避免反复弹
    }
  }, 10000);

  deepAbort = streamRequest(
    `/api/resume/${resumeId}/analyze-deep`,
    {},
    {
      onToken: (token) => {
        lastTokenTime = Date.now();
        tokenList.push(token);
      },
      onFinish: async (data) => {
        cleanupStream();
        deepStatus.value = 2;
        uni.removeStorageSync('deep_optimizing');
        // 重新加载完整报告
        try {
          const full = await get<Report>(`/api/resume/${resumeId}/analysis`);
          if (full.data) { report.value = full.data; }
        } catch (_) {}
      },
      onError: async (err) => {
        cleanupStream();
        // 检查后端状态
        try {
          const r = await get<{ deepStatus: number; retryCount: number }>(
            `/api/resume/${resumeId}/deep-status`
          );
          deepStatus.value = r.data?.deepStatus ?? -1;
          retryCount.value = r.data?.retryCount ?? 0;
          retryRemaining.value = Math.max(0, 3 - (r.data?.retryCount ?? 0));
        } catch (_) {
          deepStatus.value = -1;
          retryRemaining.value = 0;
        }
      },
    }
  );
}

function cleanupStream() {
  if (deepAbort) { deepAbort(); deepAbort = null; }
  if (heartbeatTimer) { clearInterval(heartbeatTimer); heartbeatTimer = null; }
  if (deepTimer) { clearInterval(deepTimer); deepTimer = null; }
}
```

- [ ] **Step 3: 新增状态变量**

在 `<script setup>` 中已有的 `const deepStatus = ref(0);` 之后添加：

```typescript
const retryRemaining = ref(3);
const retryCount = ref(0);
```

- [ ] **Step 4: 新增重试按钮样式**

在 `<style>` 中添加：

```scss
/* Deep optimization failed */
.deep-failed { text-align: center; padding: 24rpx 0; }
.deep-fail-text { font-size: 28rpx; color: #ef4444; font-weight: 600; display: block; }
.deep-fail-hint { font-size: 24rpx; color: #94a3b8; margin-top: 8rpx; display: block; }
.btn-retry { font-size: 28rpx; color: #fff; background: linear-gradient(135deg, #6366f1, #8b5cf6); border-radius: 40rpx; border: none; padding: 16rpx 48rpx; }
.btn-back { font-size: 28rpx; color: #64748b; background: #f1f5f9; border-radius: 40rpx; border: none; padding: 16rpx 48rpx; }
```

- [ ] **Step 5: 在 onLoad 中处理 deepStatus=-1 恢复**

在 `onLoad` 函数中，`deepStatus.value === 1` 的恢复逻辑之前，添加失败状态恢复：

```typescript
// 恢复失败状态（显示重试面板）
if (deepStatus.value === -1) {
  try {
    const s = await get<{ retryCount: number }>(`/api/resume/${resumeId}/deep-status`);
    retryCount.value = s.data?.retryCount ?? 0;
    retryRemaining.value = Math.max(0, 3 - retryCount.value);
  } catch (_) {}
}
```

- [ ] **Step 6: Commit**

```bash
git add AI-Interview/pages/resume/report.vue
git commit -m "feat: switch deep optimization to SSE streaming with retry panel and heartbeat"
```

---

## Task 8: history.vue — 简历历史列表页（新建）

**Files:**
- Create: `AI-Interview/pages/resume/history.vue`

- [ ] **Step 1: 创建 history.vue**

```vue
<template>
  <view class="history-page">
    <DeepStatusBar />
    <view class="list" v-if="items.length">
      <view class="item" v-for="r in items" :key="r.id" @click="goReport(r.id)">
        <view class="item-left">
          <text class="item-name">{{ r.fileName }}</text>
          <text class="item-pos" v-if="r.position">{{ r.position }}</text>
          <text class="item-time">{{ formatTime(r.createTime) }}</text>
        </view>
        <view class="item-right">
          <text class="tag" :class="parseTagClass(r.parseStatus)">
            {{ parseTagText(r.parseStatus) }}
          </text>
          <text class="score" v-if="r.score != null">{{ r.score }}/10</text>
        </view>
        <view class="item-swipe">
          <button class="btn-del" @click.stop="doDelete(r.id)">删除</button>
        </view>
      </view>
    </view>
    <view class="empty" v-else>
      <text class="empty-icon">📋</text>
      <text class="empty-text">暂无简历记录</text>
      <button class="btn-upload" @click="goUpload">上传第一份简历</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { get, del } from '@/utils/request';
import DeepStatusBar from '@/components/DeepStatusBar.vue';

interface ResumeItem {
  id: number; fileName: string; position: string; fileType: string;
  parseStatus: number; score: number | null; createTime: string;
}

const items = ref<ResumeItem[]>([]);

onMounted(async () => {
  await loadList();
});

async function loadList() {
  try {
    const r = await get<ResumeItem[]>('/api/resume/list');
    if (r.data) {
      // 为每条记录获取评分
      const enriched: ResumeItem[] = [];
      for (const item of r.data) {
        try {
          const a = await get<{ overallScore: number }>(`/api/resume/${item.id}/analysis`);
          enriched.push({ ...item, score: a.data?.overallScore ?? null });
        } catch { enriched.push({ ...item, score: null }); }
      }
      items.value = enriched;
    }
  } catch {}
}

function parseTagClass(s: number) {
  if (s === 0) return 'tag-parsing';
  if (s === 1) return 'tag-done';
  return 'tag-fail';
}
function parseTagText(s: number) {
  if (s === 0) return '解析中';
  if (s === 1) return '已解析';
  return '失败';
}
function formatTime(t: string) {
  if (!t) return '';
  return t.replace('T', ' ').substring(0, 16);
}
function goReport(id: number) { uni.navigateTo({ url: `/pages/resume/report?resumeId=${id}` }); }
function goUpload() { uni.redirectTo({ url: '/pages/resume/upload' }); }

async function doDelete(id: number) {
  const res = await uni.showModal({ title: '确认删除', content: '删除后无法恢复' });
  if (!res.confirm) return;
  try {
    await del(`/api/resume/${id}`);
    items.value = items.value.filter(i => i.id !== id);
    uni.showToast({ title: '已删除', icon: 'success' });
  } catch {
    uni.showToast({ title: '删除失败', icon: 'error' });
  }
}
</script>

<style lang="scss" scoped>
.history-page { min-height: 100vh; background: #f0f4ff; }
.list { padding: 20rpx 24rpx; }
.item {
  background: #fff; border-radius: 16rpx; padding: 24rpx;
  margin-bottom: 14rpx; display: flex; justify-content: space-between; align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
  &:active { background: #f8fafc; }
}
.item-left { flex: 1; }
.item-name { font-size: 28rpx; font-weight: 600; color: #0f172a; display: block; }
.item-pos { font-size: 24rpx; color: #64748b; margin-top: 4rpx; display: block; }
.item-time { font-size: 22rpx; color: #94a3b8; margin-top: 4rpx; display: block; }
.item-right { display: flex; flex-direction: column; align-items: flex-end; gap: 6rpx; }
.tag { font-size: 20rpx; padding: 4rpx 12rpx; border-radius: 8rpx; }
.tag-parsing { background: #fef3c7; color: #d97706; }
.tag-done { background: #dcfce7; color: #16a34a; }
.tag-fail { background: #fee2e2; color: #ef4444; }
.score { font-size: 26rpx; font-weight: 700; color: #2b6ff2; }
.btn-del { font-size: 22rpx; color: #ef4444; background: #fef2f2; border: none; padding: 8rpx 16rpx; border-radius: 8rpx; }

.empty { display: flex; flex-direction: column; align-items: center; padding-top: 200rpx; }
.empty-icon { font-size: 80rpx; margin-bottom: 20rpx; }
.empty-text { font-size: 28rpx; color: #94a3b8; }
.btn-upload { margin-top: 30rpx; font-size: 28rpx; color: #fff; background: linear-gradient(135deg, #2b6ff2, #4f8dff); border-radius: 40rpx; border: none; padding: 16rpx 48rpx; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add AI-Interview/pages/resume/history.vue
git commit -m "feat: add resume history list page with delete support"
```

---

## Task 9: pages.json — 注册 history 路由

**Files:**
- Modify: `AI-Interview/pages.json`

- [ ] **Step 1: 在 resume 子包中新增 history 页面**

在 `"root": "pages/resume"` 的 `"pages"` 数组中，`report` 之后添加：

```json
{
    "path": "history",
    "style": {
        "navigationBarTitleText": "简历历史"
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add AI-Interview/pages.json
git commit -m "feat: register resume history route"
```

---

## Task 10: upload.vue — 新增历史记录入口

**Files:**
- Modify: `AI-Interview/pages/resume/upload.vue`

- [ ] **Step 1: 在页面底部添加历史入口**

在 `</view>` (最外层) 之前、progress-section 之后添加：

```vue
<!-- 历史入口 -->
<view class="history-entry" @click="goHistory">
  <text class="he-icon">📂</text>
  <text class="he-text">历史记录</text>
  <text class="he-arrow">›</text>
</view>
```

- [ ] **Step 2: 在 script 中添加导航方法**

```typescript
function goHistory() { uni.navigateTo({ url: '/pages/resume/history' }); }
```

- [ ] **Step 3: 添加样式**

在 `<style>` 末尾添加：

```scss
.history-entry {
  display: flex; align-items: center; background: #fff;
  margin: 0 24rpx; padding: 24rpx 28rpx; border-radius: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
}
.he-icon { font-size: 36rpx; margin-right: 14rpx; }
.he-text { font-size: 26rpx; color: #334155; flex: 1; }
.he-arrow { font-size: 32rpx; color: #cbd5e1; }
```

- [ ] **Step 4: Commit**

```bash
git add AI-Interview/pages/resume/upload.vue
git commit -m "feat: add history entry link on upload page"
```

---

## Task 11: index.vue — 首页简历卡片拆分为两行

**Files:**
- Modify: `AI-Interview/pages/index/index.vue`

- [ ] **Step 1: 替换 resume-opt 卡片**

将现有的单个 `resume-opt` action 卡片替换为两行布局：

```vue
<view class="action resume-row" @click="goResume">
  <view class="action-icon-wrap">
    <text class="action-icon">📋</text>
  </view>
  <text class="action-title">上传简历</text>
  <text class="action-desc">AI智能优化</text>
</view>
<view class="action resume-row" @click="goResumeHistory">
  <view class="action-icon-wrap">
    <text class="action-icon">📂</text>
  </view>
  <text class="action-title">历史记录</text>
  <text class="action-desc">查看优化报告</text>
</view>
```

- [ ] **Step 2: 添加导航方法**

```typescript
function goResumeHistory() { uni.navigateTo({ url: '/pages/resume/history' }); }
```

- [ ] **Step 3: 调整 grid 为奇数时最后一项居中**

将 `.actions` 的 grid 改为 `grid-template-columns: 1fr 1fr 1fr;` 以容纳 6 个入口（3 列），或保持 2 列让 resume 两行并排。保持 2 列即可，两行各占一个 cell。

- [ ] **Step 4: Commit**

```bash
git add AI-Interview/pages/index/index.vue
git commit -m "feat: split resume entry into upload and history on homepage"
```

---

## Task 12: 端到端验证

**Files:** none

- [ ] **Step 1: 启动后端**

```bash
cd mianmiantong-server && mvn spring-boot:run -q
```

- [ ] **Step 2: 前端编译**

在 HBuilderX 中编译至微信开发者工具。

- [ ] **Step 3: 验证完整流程**

1. 首页 → 点击「简历优化」→ 上传页 → 底部出现「历史记录」入口
2. 上传页 → 点击「历史记录」→ 进入 history 页（空或显示已有简历）
3. history 页 → 点击某条简历 → 进入 report 页
4. report 页 → 点击「开始深度优化」→ SSE 流式 token 实时可见 → 完成后展示优化结果
5. 模拟失败：断网 → 深度优化失败 → 显示重试面板 + 剩余次数
6. 点击「重新优化」→ 继续执行
7. 首页 → 点击「历史记录」→ 查看简历列表

- [ ] **Step 4: 验证重试上限**

连续重试 3 次全部失败后，应显示「已达最大重试次数」。

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "chore: end-to-end verification for deep optimize fixes"
```
