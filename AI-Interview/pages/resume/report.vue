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
      <view class="card" v-if="report.dimensions">
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

        <!-- 未开始 -->
        <view v-if="deepStatus === 0" class="deep-idle">
          <text class="deep-hint">AI 将逐段优化简历并生成面试追问</text>
          <view class="model-pick">
            <text class="model-label">模型</text>
            <view class="model-opts">
              <view class="model-opt" :class="{ active: deepModel === 'deepseek-v4-flash' }"
                @click="deepModel = 'deepseek-v4-flash'">Flash</view>
              <view class="model-opt" :class="{ active: deepModel === 'deepseek-v4-pro' }"
                @click="deepModel = 'deepseek-v4-pro'">Pro</view>
            </view>
          </view>
          <button class="btn-deep" @click="startDeepOptimize">开始深度优化</button>
        </view>

        <!-- 进行中 -->
        <view v-if="deepStatus === 1" class="deep-running">
          <view class="deep-spinner" />
          <text class="deep-text">AI 正在深度优化简历...</text>
          <text class="deep-time">已运行 {{ deepElapsed }}s</text>
        </view>

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
              <button class="btn-copy" @click="copyText(report.optimizedText)">复制</button>
              <button class="btn-preview" @click="previewWord">预览</button>
              <button class="btn-download" @click="downloadWord">下载Word</button>
              <button class="btn-template" @click="showTemplatePicker = true">换模板</button>
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
        <button class="btn-primary" @click="goInterview">应用到面试</button>
        <button class="btn-secondary" @click="goHome">返回首页</button>
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
import { get, post, BASE_URL, streamRequest } from '@/utils/request';
import UnifiedDiff from '@/components/UnifiedDiff.vue';
import TemplateSelector from '@/components/TemplateSelector.vue';

interface Dim { name: string; score: number; comment: string; }
interface Highlight { section: string; before: string; after: string; reason: string; }
interface Report {
  resumeId: number; overallScore: number; fileName: string; jobDescription: string;
  dimensions: Dim[]; missingKeywords: string[]; highlights: Highlight[];
  optimizedText: string; interviewQuestions: string[]; suggestion: string;
}

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
const deepModel = ref('deepseek-v4-flash');
let deepTimer: ReturnType<typeof setInterval> | null = null;

interface Template { id: number; name: string; description: string; styleClass: string; bgColor: string; accentColor: string; }

onLoad(async (opts) => {
  const resumeId = Number(opts?.resumeId);

  // 加载模板列表
  get<Template[]>('/api/resume/template/list').then(r => { if (r.data) templates.value = r.data; }).catch(() => {});

  // 先查已有报告
  try {
    const r = await get<Report>(`/api/resume/${resumeId}/analysis`);
    if (r.data && r.data.overallScore != null) {
      report.value = r.data;
      score.value = r.data.overallScore;
      deepStatus.value = (r.data as any).deepStatus ?? 0;
      loading.value = false;
      // 恢复失败状态（显示重试面板）
      if (deepStatus.value === -1) {
        try {
          const s = await get<{ retryCount: number }>(`/api/resume/${resumeId}/deep-status`);
          retryCount.value = s.data?.retryCount ?? 0;
          retryRemaining.value = Math.max(0, 3 - retryCount.value);
        } catch (_) {}
      }
      // 如果深度优化进行中，恢复流式连接
      if (deepStatus.value === 1) doStreamDeep(resumeId);
      return;
    }
  } catch {}

  // 触发快速评分（异步后台）
  post(`/api/resume/${resumeId}/analyze`).catch(() => {});

  // 轮询等待评分完成
  let pollAttempts = 0;
  while (pollAttempts < 60) {
    await sleep(2000);
    pollAttempts++;
    loadingText.value = `AI 正在分析简历... (${pollAttempts * 2}s)`;
    try {
      const r = await get<Report>(`/api/resume/${resumeId}/analysis`);
      if (r.data && r.data.overallScore != null) {
        report.value = r.data;
        score.value = r.data.overallScore;
        deepStatus.value = (r.data as any).deepStatus ?? 0;
        loading.value = false;
        return;
      }
    } catch (_) {}
  }
  uni.showToast({ title: '分析超时，请返回重试', icon: 'error' });
  loading.value = false;
});

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
  lastTokenTime = Date.now();

  // 心跳检测：30 秒无 token 则提示
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
      onToken: (token) => {
        lastTokenTime = Date.now();
      },
      onFinish: async (data) => {
        cleanupStream();
        deepStatus.value = 2;
        uni.removeStorageSync('deep_optimizing');
        try {
          const full = await get<Report>(`/api/resume/${resumeId}/analysis`);
          if (full.data) { report.value = full.data; }
        } catch (_) {}
      },
      onError: async (err) => {
        // 如果已经完成（onFinish 先触发），忽略后续的 error
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
  // 不主动 abort，避免触发 fail → onError 覆盖 onFinish 的状态
  deepAbort = null;
  if (heartbeatTimer) { clearInterval(heartbeatTimer); heartbeatTimer = null; }
  if (deepTimer) { clearInterval(deepTimer); deepTimer = null; }
}

function copyText(text: string) {
  uni.setClipboardData({ data: text, success: () => uni.showToast({ title: '已复制' }) });
}

function downloadWord() {
  const token = uni.getStorageSync('mianmiantong_token') || '';
  const resumeId = report.value?.resumeId;
  if (!resumeId) return;

  uni.downloadFile({
    url: `${BASE_URL}/api/resume/${resumeId}/export-word`,
    header: { Authorization: 'Bearer ' + token },
    success: (res) => {
      if (res.statusCode === 200) {
        uni.openDocument({ filePath: res.tempFilePath, showMenu: true,
          fail: () => uni.showToast({ title: '请先安装WPS或Office', icon: 'none' }) });
      }
    },
    fail: () => uni.showToast({ title: '下载失败', icon: 'error' }),
  });
}

function onTemplateSelect(templateId: number) {
  if (!report.value) return;
  const token = uni.getStorageSync('mianmiantong_token') || '';
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

function previewWord() {
  const token = uni.getStorageSync('mianmiantong_token') || '';
  const resumeId = report.value?.resumeId;
  if (!resumeId) return;
  uni.showLoading({ title: '加载预览...' });
  uni.downloadFile({
    url: `${BASE_URL}/api/resume/${resumeId}/preview-html?token=${encodeURIComponent(token)}`,
    success: (res) => {
      uni.hideLoading();
      if (res.statusCode === 200) {
        uni.openDocument({ filePath: res.tempFilePath, fileType: 'docx', showMenu: true,
          fail: () => uni.showToast({ title: '请先安装WPS或Office', icon: 'none' }) });
      }
    },
    fail: () => { uni.hideLoading(); uni.showToast({ title: '预览失败', icon: 'error' }); },
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
.report-page { min-height: 100vh; background: #f0f4ff; padding-bottom: 40rpx; }
.loading-screen { display: flex; flex-direction: column; align-items: center; padding-top: 300rpx; }
.loading-spinner { width: 80rpx; height: 80rpx; border: 6rpx solid #e2e8f0; border-top-color: #2b6ff2; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.loading-text { font-size: 26rpx; color: #64748b; margin-top: 30rpx; }

.score-hero { display: flex; flex-direction: column; align-items: center; padding: 60rpx 0 50rpx; background: linear-gradient(135deg, #1a3a6b, #2b6ff2, #4f8dff); }
.score-ring { width: 180rpx; height: 180rpx; border-radius: 50%; display: flex; flex-direction: column; align-items: center; justify-content: center; border: 6rpx solid rgba(255,255,255,0.3); }
.score-num { font-size: 72rpx; font-weight: 900; color: #fff; }
.score-unit { font-size: 24rpx; color: rgba(255,255,255,0.7); }
.score-label { font-size: 28rpx; color: rgba(255,255,255,0.8); margin-top: 16rpx; }
.score-file { font-size: 22rpx; color: rgba(255,255,255,0.5); margin-top: 6rpx; }

.card { background: #fff; margin: 20rpx 24rpx; padding: 28rpx; border-radius: 20rpx; box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.03); }
.card-label { font-size: 28rpx; font-weight: 700; color: #0f172a; display: block; margin-bottom: 16rpx; }
.card-label-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.card-text { font-size: 26rpx; color: #64748b; line-height: 1.8; display: block; }
.btn-row { display: flex; gap: 12rpx; }
.btn-copy, .btn-download, .btn-preview, .btn-template { font-size: 24rpx; padding: 8rpx 24rpx; border-radius: 20rpx; border: none; }
.btn-copy { color: #2b6ff2; background: #f0f4ff; }
.btn-preview { color: #6366f1; background: #f0f0ff; border: 1rpx solid #d0d0f0; }
.btn-download { color: #fff; background: linear-gradient(135deg, #10b981, #059669); }
.btn-template { color: #fff; background: linear-gradient(135deg, #f59e0b, #d97706); }

/* Deep optimization */
.deep-section { margin-top: 10rpx; }
.deep-idle { text-align: center; padding: 24rpx 0; }
.deep-hint { font-size: 24rpx; color: #94a3b8; display: block; margin-bottom: 20rpx; }
.model-pick { display: flex; align-items: center; justify-content: center; gap: 16rpx; margin-bottom: 24rpx; }
.model-label { font-size: 24rpx; color: #64748b; }
.model-opts { display: flex; gap: 0; background: #f1f5f9; border-radius: 12rpx; overflow: hidden; }
.model-opt { font-size: 22rpx; padding: 10rpx 24rpx; color: #94a3b8; transition: all 0.15s; }
.model-opt.active { background: #2b6ff2; color: #fff; }
.btn-deep { width: 100%; height: 80rpx; background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff; font-size: 28rpx; font-weight: 700; border-radius: 40rpx; border: none; }
.deep-running { display: flex; flex-direction: column; align-items: center; padding: 40rpx 0; }
.deep-spinner { width: 56rpx; height: 56rpx; border: 4rpx solid #e2e8f0; border-top-color: #6366f1; border-radius: 50%; animation: spin 0.8s linear infinite; }
.deep-text { font-size: 26rpx; color: #6366f1; margin-top: 16rpx; }
.deep-time { font-size: 22rpx; color: #94a3b8; margin-top: 6rpx; }

/* Deep optimization failed */
.deep-failed { text-align: center; padding: 24rpx 0; }
.deep-fail-text { font-size: 28rpx; color: #ef4444; font-weight: 600; display: block; }
.deep-fail-hint { font-size: 24rpx; color: #94a3b8; margin-top: 8rpx; display: block; }
.btn-retry { font-size: 28rpx; color: #fff; background: linear-gradient(135deg, #6366f1, #8b5cf6); border-radius: 40rpx; border: none; padding: 16rpx 48rpx; }
.btn-back { font-size: 28rpx; color: #64748b; background: #f1f5f9; border-radius: 40rpx; border: none; padding: 16rpx 48rpx; }

.card-sub-label { font-size: 26rpx; font-weight: 700; color: #0f172a; display: block; margin-bottom: 16rpx; margin-top: 20rpx; }
.deep-result { margin-top: 8rpx; }
.deep-questions { margin-top: 8rpx; }

.dim-item { padding: 16rpx 0; }
.dim-item + .dim-item { border-top: 1rpx solid #f1f5f9; }
.dim-head { display: flex; justify-content: space-between; margin-bottom: 10rpx; }
.dim-name { font-size: 26rpx; font-weight: 600; color: #1e293b; }
.dim-score { font-size: 26rpx; font-weight: 700; color: #2b6ff2; }
.dim-bar-bg { height: 6rpx; background: #e2e8f0; border-radius: 3rpx; overflow: hidden; }
.dim-bar-fill { height: 100%; background: linear-gradient(90deg, #2b6ff2, #6366f1); border-radius: 3rpx; transition: width 0.6s; }
.dim-comment { font-size: 22rpx; color: #94a3b8; margin-top: 6rpx; display: block; }

.keywords { display: flex; flex-wrap: wrap; gap: 12rpx; }
.kw-tag { font-size: 22rpx; background: #fef2f2; color: #ef4444; padding: 6rpx 16rpx; border-radius: 8rpx; }

.highlight-item { padding: 20rpx 0; }
.highlight-item + .highlight-item { border-top: 1rpx solid #f1f5f9; }
.hl-reason { font-size: 22rpx; color: #94a3b8; margin-top: 10rpx; padding-left: 12rpx; display: block; }

.optimized-resume { background: #f8fafc; border-radius: 12rpx; padding: 24rpx; }
.opt-text { font-size: 26rpx; color: #1e293b; line-height: 1.8; white-space: pre-wrap; }

.iq-item { display: flex; gap: 14rpx; padding: 14rpx 0; }
.iq-item + .iq-item { border-top: 1rpx solid #f1f5f9; }
.iq-num { width: 40rpx; height: 40rpx; background: #f0f4ff; color: #2b6ff2; font-size: 22rpx; font-weight: 700; border-radius: 10rpx; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.iq-text { font-size: 26rpx; color: #1e293b; line-height: 1.6; }

.actions { padding: 30rpx 24rpx; display: flex; flex-direction: column; gap: 16rpx; }
.btn-primary { width: 100%; height: 96rpx; background: linear-gradient(135deg, #2b6ff2, #4f8dff); color: #fff; font-size: 32rpx; font-weight: 700; border-radius: 48rpx; border: none; }
.btn-secondary { width: 100%; height: 96rpx; background: #f1f5f9; color: #64748b; font-size: 32rpx; font-weight: 700; border-radius: 48rpx; border: none; }
</style>
