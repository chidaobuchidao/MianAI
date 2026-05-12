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

      <!-- 逐段优化对比 -->
      <view class="card" v-if="report.highlights && report.highlights.length">
        <text class="card-label">逐段优化对比</text>
        <view class="highlight-item" v-for="(h, i) in report.highlights" :key="i">
          <text class="hl-section">{{ h.section }}</text>
          <view class="hl-before"><text class="hl-tag">原文</text><text class="hl-text">{{ h.before }}</text></view>
          <view class="hl-after"><text class="hl-tag opt">优化</text><text class="hl-text">{{ h.after }}</text></view>
          <text class="hl-reason">{{ h.reason }}</text>
        </view>
      </view>

      <!-- 优化后完整简历 -->
      <view class="card" v-if="report.optimizedText">
        <view class="card-label-row">
          <text class="card-label">优化后简历</text>
          <button class="btn-copy" @click="copyText(report.optimizedText)">复制</button>
        </view>
        <view class="optimized-resume">
          <text class="opt-text">{{ report.optimizedText }}</text>
        </view>
      </view>

      <!-- 面试追问 -->
      <view class="card" v-if="report.interviewQuestions && report.interviewQuestions.length">
        <text class="card-label">面试可能追问</text>
        <view class="iq-item" v-for="(q, i) in report.interviewQuestions" :key="i">
          <text class="iq-num">{{ i + 1 }}</text>
          <text class="iq-text">{{ q }}</text>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="actions">
        <button class="btn-primary" @click="goInterview">应用到面试</button>
        <button class="btn-secondary" @click="goHome">返回首页</button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { get, streamRequest } from '@/utils/request';

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

onLoad(async (opts) => {
  const resumeId = Number(opts?.resumeId);

  // 优先加载已有报告
  try {
    const r = await get<Report>(`/api/resume/${resumeId}/analysis`);
    if (r.data && r.data.overallScore != null) {
      report.value = r.data;
      score.value = r.data.overallScore;
      loading.value = false;
      return;
    }
  } catch {
    // 无报告则触发AI分析
  }

  // 无报告则触发 AI 分析 (SSE流式)
  const tokenList: string[] = [];
  streamRequest(
    `/api/resume/${resumeId}/analyze`,
    {},
    {
      onToken: (token) => {
        tokenList.push(token);
        loadingText.value = tokenList.join('').slice(-50);
      },
      onFinish: async (_data) => {
        loadingText.value = '分析完成，加载报告...';
        const r = await get<Report>(`/api/resume/${resumeId}/analysis`);
        report.value = r.data;
        score.value = r.data.overallScore || 0;
        loading.value = false;
      },
      onError: (err) => {
        uni.showToast({ title: err.message, icon: 'error' });
        loading.value = false;
      },
    }
  );
});

function copyText(text: string) {
  uni.setClipboardData({ data: text, success: () => uni.showToast({ title: '已复制' }) });
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
.btn-copy { font-size: 24rpx; color: #2b6ff2; background: #f0f4ff; padding: 8rpx 24rpx; border-radius: 20rpx; border: none; }

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
.hl-section { font-size: 24rpx; font-weight: 700; color: #2b6ff2; display: block; margin-bottom: 12rpx; }
.hl-before, .hl-after { display: flex; gap: 12rpx; margin-bottom: 8rpx; }
.hl-tag { font-size: 20rpx; padding: 2rpx 10rpx; border-radius: 6rpx; background: #fef2f2; color: #ef4444; font-weight: 600; }
.hl-tag.opt { background: #ecfdf5; color: #10b981; }
.hl-text { font-size: 24rpx; color: #64748b; flex: 1; line-height: 1.6; }
.hl-reason { font-size: 22rpx; color: #94a3b8; padding-left: 48rpx; display: block; }

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
