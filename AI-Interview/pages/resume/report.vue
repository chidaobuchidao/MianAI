<template>
  <view class="report-page">
    <!-- 加载中 -->
    <view class="loading-screen" v-if="loading">
      <view class="loading-spinner" />
      <text class="loading-text">{{ loadingText }}</text>
    </view>

    <!-- 报告内容 -->
    <template v-if="!loading && report">
      <!-- 评分 -->
      <view class="score-hero">
        <view class="score-ring" :class="score >= 7 ? 'great' : score >= 4 ? 'ok' : 'low'">
          <text class="score-num">{{ score }}</text>
          <text class="score-unit">/10</text>
        </view>
        <text class="score-label">简历综合评分</text>
        <text class="score-file">{{ report.fileName }}</text>
      </view>

      <!-- 维度评分 -->
      <view class="card" v-if="report.dimensions && report.dimensions.length">
        <text class="card-label">能力维度</text>
        <view class="dim-item" v-for="d in report.dimensions" :key="d.name">
          <view class="dim-head">
            <text class="dim-name">{{ d.name }}</text>
            <text class="dim-score">{{ d.score }}/10</text>
          </view>
          <view class="dim-bar-bg"><view class="dim-bar-fill" :style="{ width: (d.score * 10) + '%' }" /></view>
          <text class="dim-comment" v-if="d.comment">{{ d.comment }}</text>
        </view>
      </view>

      <!-- 缺失关键词 -->
      <view class="card" v-if="report.missingKeywords && report.missingKeywords.length">
        <text class="card-label">缺失关键词（对标 JD）</text>
        <view class="keywords">
          <text class="kw-tag" v-for="kw in report.missingKeywords" :key="kw">{{ kw }}</text>
        </view>
      </view>

      <!-- 优化建议 -->
      <view class="card" v-if="report.suggestion">
        <text class="card-label">总体建议</text>
        <text class="card-text">{{ report.suggestion }}</text>
      </view>

      <!-- ===== 深度优化区 ===== -->
      <view class="card deep-section">
        <text class="card-label">深度优化</text>

        <!-- 等待评分完成 -->
        <view v-if="deepStatus === 0 && score === 0" class="deep-idle">
          <text class="deep-hint">等待简历评分完成...</text>
        </view>
        <!-- 评分完成，可以开始深度优化 -->
        <view v-if="deepStatus === 0 && score > 0" class="deep-idle">
          <text class="deep-hint">AI 将逐段优化简历并生成面试追问</text>
          <view class="model-pick">
            <text class="model-label">模型</text>
            <view class="model-opts">
              <view v-for="m in options" :key="m.id" class="model-opt" :class="{ active: deepModel === m.id }" @click="selectModel(m.id)">{{ m.label }}</view>
            </view>
          </view>
          <view class="btn-deep" @click="startDeepOptimize">
            <text>开始深度优化</text>
          </view>
        </view>

        <!-- 进行中 -->
        <view v-if="deepStatus === 1" class="deep-running">
          <view class="deep-spinner" />
          <text class="deep-text">AI 正在深度优化简历...</text>
          <text class="deep-time">已运行 {{ deepElapsed }}s</text>
        </view>

        <!-- 失败 -->
        <view v-if="deepStatus === -1" class="deep-failed">
          <text class="deep-fail-text">深度优化失败</text>
          <text class="deep-fail-hint" v-if="retryRemaining > 0">还可重试 {{ retryRemaining }} 次</text>
          <text class="deep-fail-hint" v-else>已达最大重试次数</text>
          <view class="btn-row" style="justify-content:center;margin-top:20rpx">
            <view v-if="retryRemaining > 0" class="btn-retry" @click="retryDeepOptimize">
              <text>重新优化</text>
            </view>
            <view class="btn-back" @click="goHome"><text>返回首页</text></view>
          </view>
        </view>

        <!-- 已完成 -->
        <template v-if="deepStatus === 2">
          <!-- 逐段优化对比 -->
          <view v-if="report.highlights && report.highlights.length">
            <text class="card-sub-label">逐段优化对比</text>
            <view class="highlight-item" v-for="(h, i) in report.highlights" :key="i">
              <UnifiedDiff :oldText="h.before" :newText="h.after" :sectionName="h.section" :contextLines="3" />
              <text class="hl-reason">{{ h.reason }}</text>
            </view>
          </view>

          <!-- 优化后完整简历 -->
          <view v-if="report.optimizedText" class="deep-result">
            <text class="card-sub-label">优化后简历</text>
            <view class="btn-row" style="margin-bottom:16rpx">
              <view class="btn-copy" @click="copyText(report.optimizedText)"><text>复制</text></view>
              <view class="btn-preview" @click="previewWord"><text>预览</text></view>
              <view class="btn-download" @click="downloadWord"><text>标准导出</text></view>
              <view class="btn-template" @click="downloadPreserveWord"><text>保留格式</text></view>
              <view class="btn-template" @click="showTemplatePicker = true"><text>换模板</text></view>
            </view>
            <view class="optimized-resume">
              <text class="opt-text">{{ report.optimizedText }}</text>
            </view>
          </view>

          <!-- 面试追问 -->
          <view v-if="report.interviewQuestions && report.interviewQuestions.length" class="deep-questions">
            <text class="card-sub-label">面试可能追问</text>
            <view class="iq-item" v-for="(q, i) in report.interviewQuestions" :key="i">
              <text class="iq-num">{{ i + 1 }}</text>
              <text class="iq-text">{{ q }}</text>
            </view>
          </view>
        </template>
      </view>

      <!-- 操作按钮 -->
      <view class="actions">
        <view class="btn-primary" @click="goInterview"><text>应用到面试</text></view>
        <view class="btn-secondary" @click="goHome"><text>返回首页</text></view>
      </view>
    </template>

    <!-- 模板选择器弹窗 -->
    <TemplateSelector
      :visible="showTemplatePicker"
      :list="templates"
      @close="showTemplatePicker = false"
      @select="onTemplateSelect"
    />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { get, post, BASE_URL, streamRequest, getToken } from '@/utils/request';
import { useModelToggle } from '@/composables/useModelToggle';
import UnifiedDiff from '@/components/UnifiedDiff.vue';
import TemplateSelector from '@/components/TemplateSelector.vue';

interface Dim { name: string; score: number; comment: string; }
interface Highlight { section: string; before: string; after: string; reason: string; }
interface Report {
  resumeId: number; overallScore: number; fileName: string; jobDescription: string;
  dimensions: Dim[]; missingKeywords: string[]; highlights: Highlight[];
  optimizedText: string; interviewQuestions: string[]; suggestion: string;
  parseStatus?: number; deepStatus?: number;
}

interface Template { id: number; name: string; description: string; styleClass: string; bgColor: string; accentColor: string; }

const loading = ref(true);
const loadingText = ref('AI 正在分析简历...');
const report = ref<Report | null>(null);
const score = ref(0);
const showTemplatePicker = ref(false);
const templates = ref<Template[]>([]);
const deepStatus = ref(0);
const retryRemaining = ref(3);
const retryCount = ref(0);
const deepElapsed = ref(0);
const { currentModel: deepModel, options, selectModel } = useModelToggle();
let deepTimer: ReturnType<typeof setInterval> | null = null;

onLoad(async (opts) => {
  const resumeId = Number(opts?.resumeId);

  // 加载模板列表
  get<Template[]>('/api/resume/template/list').then(r => { if (r.data) templates.value = r.data; }).catch(() => {});

  // === Phase 1: 加载已有报告 ===
  let r = await get<Report>(`/api/resume/${resumeId}/analysis`);
  if (isValidReport(r.data)) {
    report.value = r.data;
    score.value = r.data.overallScore || 0;
    deepStatus.value = r.data.deepStatus ?? 0;

    // 已有报告 → 直接展示
    if (r.data.overallScore != null) {
      loading.value = false;
      if (deepStatus.value === -1) loadRetryStatus(resumeId);
      if (deepStatus.value === 1) doStreamDeep(resumeId);
      return;
    }

    // 解析失败 → 直接展示错误
    if (r.data.parseStatus === -1) {
      loading.value = false;
      return;
    }
  }

  // === Phase 2: 解析完成但无报告 → 触发分析 ===
  if (report.value?.parseStatus === 1) {
    await post(`/api/resume/${resumeId}/analyze?model=${encodeURIComponent(deepModel.value)}`).catch(() => {});
  }

  // === Phase 3: 轮询（最多 20 次 = 40s） ===
  for (let i = 0; i < 20; i++) {
    await sleep(2000);
    loadingText.value = `AI 正在分析简历... (${(i + 1) * 2}s)`;

    r = await get<Report>(`/api/resume/${resumeId}/analysis`);
    if (!isValidReport(r.data)) continue;

    report.value = r.data;
    if (r.data.overallScore != null) {
      score.value = r.data.overallScore;
      deepStatus.value = r.data.deepStatus ?? 0;
      loading.value = false;
      return;
    }
    if (r.data.parseStatus === -1 || r.data.deepStatus === -1) {
      loading.value = false;
      return;
    }
  }

  loading.value = false;
  uni.showToast({ title: 'AI分析较慢，稍后刷新页面查看', icon: 'none', duration: 3000 });
});

function loadRetryStatus(resumeId: number) {
  get<{ retryCount: number }>(`/api/resume/${resumeId}/deep-status`).then(s => {
    retryCount.value = s.data?.retryCount ?? 0;
    retryRemaining.value = Math.max(0, 3 - retryCount.value);
  }).catch(() => {});
}

function isValidReport(d: unknown): d is Report {
  return d != null && typeof d === 'object' && 'resumeId' in d;
}

// ===== 深度优化 =====
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

  uni.setStorageSync('deep_optimizing', JSON.stringify({ resumeId, name: report.value.fileName || '简历' }));
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
  lastTokenTime = Date.now();

  heartbeatTimer = setInterval(() => {
    if (Date.now() - lastTokenTime > 30000 && deepStatus.value === 1) {
      uni.showToast({ title: 'AI 响应较慢，请耐心等待', icon: 'none', duration: 2000 });
      lastTokenTime = Date.now();
    }
  }, 10000);

  deepAbort = streamRequest(
    `/api/resume/${resumeId}/analyze-deep?model=${encodeURIComponent(deepModel.value)}`,
    {},
    {
      onToken: (_token) => {
        lastTokenTime = Date.now();
      },
      onFinish: async (_data) => {
        cleanupStream();
        deepStatus.value = 2;
        uni.removeStorageSync('deep_optimizing');
        try {
          const full = await get<Report>(`/api/resume/${resumeId}/analysis`);
          if (full.data) report.value = full.data;
        } catch (_) {}
      },
      onError: async (_err) => {
        if (deepStatus.value === 2) return;
        cleanupStream();
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
  deepAbort = null;
  if (heartbeatTimer) { clearInterval(heartbeatTimer); heartbeatTimer = null; }
  if (deepTimer) { clearInterval(deepTimer); deepTimer = null; }
}

// ===== 操作函数 =====
function copyText(text: string) {
  uni.setClipboardData({ data: text, success: () => uni.showToast({ title: '已复制' }) });
}

function previewWord() {
  const token = getToken();
  const resumeId = report.value?.resumeId;
  if (!resumeId) return;
  uni.showLoading({ title: '加载预览...' });
  uni.downloadFile({
    url: `${BASE_URL}/api/resume/${resumeId}/preview-html`,
    header: { Authorization: 'Bearer ' + token },
    success: (res) => {
      uni.hideLoading();
      if (res.statusCode === 200) {
        uni.openDocument({
          filePath: res.tempFilePath,
          fileType: 'docx',
          showMenu: true,
          fail: () => uni.showToast({ title: '请先安装WPS或Office', icon: 'none' }),
        });
      } else {
        uni.showToast({ title: '预览失败', icon: 'error' });
      }
    },
    fail: () => { uni.hideLoading(); uni.showToast({ title: '预览失败', icon: 'error' }); },
  });
}

function downloadWord() {
  downloadResumeDoc('export-word');
}

function downloadPreserveWord() {
  downloadResumeDoc('export-preserve-format');
}

function downloadResumeDoc(endpoint: string) {
  const token = getToken();
  const resumeId = report.value?.resumeId;
  if (!resumeId) return;
  uni.downloadFile({
    url: `${BASE_URL}/api/resume/${resumeId}/${endpoint}`,
    header: { Authorization: 'Bearer ' + token },
    success: (res) => {
      if (res.statusCode === 200) {
        uni.openDocument({ filePath: res.tempFilePath, showMenu: true, fail: () => uni.showToast({ title: '请先安装WPS或Office', icon: 'none' }) });
      } else {
        uni.showToast({ title: '下载失败', icon: 'error' });
      }
    },
    fail: () => uni.showToast({ title: '下载失败', icon: 'error' }),
  });
}

function onTemplateSelect(templateId: number) {
  if (!report.value) return;
  const token = getToken();
  const resumeId = report.value.resumeId;
  uni.showLoading({ title: '生成中...' });
  uni.downloadFile({
    url: `${BASE_URL}/api/resume/template/generate?resumeId=${resumeId}&templateId=${templateId}`,
    header: { Authorization: 'Bearer ' + token },
    success: (res) => {
      uni.hideLoading();
      if (res.statusCode === 200) {
        uni.openDocument({ filePath: res.tempFilePath, showMenu: true });
      } else {
        uni.showToast({ title: '生成失败', icon: 'error' });
      }
    },
    fail: () => { uni.hideLoading(); uni.showToast({ title: '生成失败', icon: 'error' }); },
  });
}

function goInterview() {
  if (report.value) {
    uni.setStorageSync('resumeForInterview', JSON.stringify({
      resumeId: report.value.resumeId,
      parsedText: report.value.optimizedText,
    }));
  }
  uni.navigateTo({ url: '/pages/interview/chat?fromResume=1' });
}

function goHome() { uni.switchTab({ url: '/pages/index/index' }); }
function sleep(ms: number) { return new Promise(r => setTimeout(r, ms)); }
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.report-page { min-height: 100vh; background: $bg-canvas; padding-bottom: 40rpx; }

// ===== 加载 =====
.loading-screen { display: flex; flex-direction: column; align-items: center; padding-top: 300rpx; }
.loading-spinner { width: 80rpx; height: 80rpx; border: 6rpx solid $border-light; border-top-color: $accent; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.loading-text { font-size: 26rpx; color: $text-muted; margin-top: 30rpx; }

// ===== 评分 =====
.score-hero {
  display: flex; flex-direction: column; align-items: center;
  padding: 60rpx 0 50rpx; background: $bg-dark;
}
.score-ring {
  width: 180rpx; height: 180rpx; border-radius: 50%;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  border: 6rpx solid rgba(255,255,255,0.2);
}
.score-ring.great { border-color: $color-success; }
.score-ring.ok { border-color: $accent; }
.score-ring.low { border-color: $color-danger; }
.score-num { font-size: 72rpx; font-weight: 900; color: #fff; font-family: Georgia, serif; }
.score-unit { font-size: 24rpx; color: rgba(255,255,255,0.5); }
.score-label { font-size: 28rpx; color: rgba(255,255,255,0.8); margin-top: 16rpx; }
.score-file { font-size: 22rpx; color: rgba(255,255,255,0.45); margin-top: 6rpx; }

// ===== 卡片 =====
.card {
  background: $bg-paper; margin: 20rpx 24rpx; padding: 28rpx;
  border-radius: $radius-lg; border: 1px solid $border-light;
  box-shadow: $shadow-sm;
}
.card-label { font-size: 28rpx; font-weight: 700; color: $text-main; display: block; margin-bottom: 16rpx; }
.card-sub-label { font-size: 26rpx; font-weight: 700; color: $text-main; display: block; margin-bottom: 16rpx; margin-top: 20rpx; }
.card-text { font-size: 26rpx; color: $text-muted; line-height: 1.8; display: block; }

.btn-row { display: flex; gap: 12rpx; flex-wrap: wrap; }
.btn-copy, .btn-download, .btn-preview, .btn-template {
  font-size: 24rpx; padding: 10rpx 24rpx; border-radius: $radius-full; border: none;
  display: inline-flex; align-items: center; justify-content: center;
}
.btn-copy { color: $accent; background: rgba(217,117,10,0.08); }
.btn-preview { color: $accent; background: $bg-surface; border: 1px solid $border-medium; }
.btn-download { color: #fff; background: $color-success; }
.btn-template { color: #fff; background: $accent; }

// ===== 维度 =====
.dim-item { padding: 16rpx 0; }
.dim-item + .dim-item { border-top: 1px solid $border-light; }
.dim-head { display: flex; justify-content: space-between; margin-bottom: 10rpx; }
.dim-name { font-size: 26rpx; font-weight: 600; color: $text-main; }
.dim-score { font-size: 26rpx; font-weight: 700; color: $accent; }
.dim-bar-bg { height: 6rpx; background: $bg-surface; border-radius: 3rpx; overflow: hidden; }
.dim-bar-fill { height: 100%; background: $accent; border-radius: 3rpx; transition: width 0.6s; }
.dim-comment { font-size: 24rpx; color: $text-light; margin-top: 6rpx; display: block; }

// ===== 关键词 =====
.keywords { display: flex; flex-wrap: wrap; gap: 12rpx; }
.kw-tag { font-size: 22rpx; background: rgba(239,68,68,0.1); color: $color-danger; padding: 6rpx 16rpx; border-radius: $radius-sm; }

// ===== 深度优化 =====
.deep-section { margin-top: 10rpx; }

.deep-idle { text-align: center; padding: 24rpx 0; }
.deep-hint { font-size: 24rpx; color: $text-light; display: block; margin-bottom: 20rpx; }
.model-pick { display: flex; align-items: center; justify-content: center; gap: 16rpx; margin-bottom: 24rpx; }
.model-label { font-size: 24rpx; color: $text-muted; }
.model-opts { display: flex; gap: 0; background: $bg-surface; border-radius: $radius-sm; overflow: hidden; }
.model-opt { font-size: 22rpx; padding: 10rpx 24rpx; color: $text-light; }
.model-opt.active { background: $bg-dark; color: #fff; }

.btn-deep {
  width: 100%; height: 80rpx; background: $bg-dark; color: #fff;
  font-size: 28rpx; font-weight: 600; border-radius: 40rpx; border: none;
  display: flex; align-items: center; justify-content: center;
}
.btn-deep:active { opacity: 0.9; }

.deep-running { display: flex; flex-direction: column; align-items: center; padding: 40rpx 0; }
.deep-spinner { width: 56rpx; height: 56rpx; border: 4rpx solid $border-light; border-top-color: $accent; border-radius: 50%; animation: spin 0.8s linear infinite; }
.deep-text { font-size: 26rpx; color: $accent; margin-top: 16rpx; }
.deep-time { font-size: 22rpx; color: $text-light; margin-top: 6rpx; }

.deep-failed { text-align: center; padding: 24rpx 0; }
.deep-fail-text { font-size: 28rpx; color: $color-danger; font-weight: 600; display: block; }
.deep-fail-hint { font-size: 24rpx; color: $text-light; margin-top: 8rpx; display: block; }
.btn-retry {
  font-size: 28rpx; color: #fff; background: $bg-dark;
  border-radius: 40rpx; border: none; padding: 16rpx 48rpx;
  display: inline-flex;
}
.btn-retry:active { opacity: 0.9; }
.btn-back {
  font-size: 28rpx; color: $text-muted; background: $bg-surface;
  border-radius: 40rpx; border: none; padding: 16rpx 48rpx;
  display: inline-flex;
}
.btn-back:active { background: #e8e8e5; }

// ===== GitHub 风格 Diff =====
.highlight-item { padding: 20rpx 0; }
.highlight-item + .highlight-item { border-top: 1px solid $border-light; }
.hl-reason { font-size: 24rpx; color: $text-light; margin-top: 10rpx; padding-left: 12rpx; display: block; }

// ===== 优化后简历 =====
.deep-result { margin-top: 8rpx; }
.optimized-resume { background: $bg-surface; border-radius: $radius-md; padding: 24rpx; }
.opt-text { font-size: 26rpx; color: $text-main; line-height: 1.8; white-space: pre-wrap; }

// ===== 面试追问 =====
.deep-questions { margin-top: 8rpx; }
.iq-item { display: flex; gap: 14rpx; padding: 14rpx 0; }
.iq-item + .iq-item { border-top: 1px solid $border-light; }
.iq-num {
  width: 40rpx; height: 40rpx; background: rgba(217,117,10,0.1);
  color: $accent; font-size: 22rpx; font-weight: 700;
  border-radius: $radius-sm; display: flex; align-items: center;
  justify-content: center; flex-shrink: 0;
}
.iq-text { font-size: 26rpx; color: $text-main; line-height: 1.6; }

// ===== 底部按钮 =====
.actions { padding: 30rpx 24rpx; display: flex; flex-direction: column; gap: 16rpx; }
.btn-primary {
  width: 100%; height: 96rpx; background: $bg-dark; color: #fff;
  font-size: 32rpx; font-weight: 600; border-radius: 48rpx; border: none;
  display: flex; align-items: center; justify-content: center;
}
.btn-primary:active { opacity: 0.9; }
.btn-secondary {
  width: 100%; height: 96rpx; background: $bg-surface; color: $text-muted;
  font-size: 32rpx; font-weight: 600; border-radius: 48rpx; border: none;
  display: flex; align-items: center; justify-content: center;
}
.btn-secondary:active { background: #e8e8e5; }

@media (min-width: 1025px) { .report-page { max-width: 700px; margin: 0 auto; } }
</style>
