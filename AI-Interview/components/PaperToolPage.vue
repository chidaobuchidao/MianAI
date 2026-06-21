<template>
  <view class="paper-tool">
    <view class="page-head">
      <text class="brand-tag">文章助手</text>
      <text class="page-title">{{ meta.title }}</text>
      <text class="page-desc">{{ meta.desc }}</text>
    </view>

    <view class="switch-row">
      <view class="switch-chip" :class="{ active: mode === 'polish' }" @click="go('polish')">润色</view>
      <view class="switch-chip" :class="{ active: mode === 'ai-reduce' }" @click="go('ai-reduce')">降AI</view>
      <view class="switch-chip" :class="{ active: mode === 'plagiarism-reduce' }" @click="go('plagiarism-reduce')">降重</view>
    </view>

    <view class="card upload-card">
      <view class="card-head">
        <text class="card-title">原文</text>
        <view class="small-btn" @click="chooseFile">上传 DOCX/PDF</view>
      </view>
      <text class="file-name" v-if="file">{{ file.name }} · {{ formatSize(file.size) }}</text>
      <textarea class="paper-input" v-model="sourceText" :maxlength="-1" placeholder="粘贴正文，或上传 DOCX/PDF 后自动解析..." />
    </view>

    <view class="card settings-card">
      <view class="row">
        <text class="row-label">模型</text>
        <view class="model-opts">
          <view v-for="m in options" :key="m.id" class="model-opt" :class="{ active: currentModel === m.id }" @click="selectModel(m.id)">{{ m.label }}</view>
        </view>
      </view>
      <view class="row" v-if="mode !== 'polish'">
        <text class="row-label">强度</text>
        <view class="model-opts">
          <view v-for="m in rewriteModes" :key="m.id" class="model-opt" :class="{ active: rewriteMode === m.id }" @click="rewriteMode = m.id">{{ m.label }}</view>
        </view>
      </view>
      <view class="kb-note">
        <text>{{ quota?.knowledgeBaseEnabled ? '知识库权限已开启；移动端暂不维护本地论文库。' : '知识库需要配置 API Key 或由管理员开放。' }}</text>
      </view>
    </view>

    <view class="action-btn" :class="{ disabled: busy || !sourceText.trim() }" @click="startRun">
      <text>{{ busy ? meta.running : meta.action }}</text>
    </view>

    <view class="card scan-card" v-if="scanSummary">
      <text class="card-title">扫描结果</text>
      <text class="scan-text">{{ scanSummary }}</text>
    </view>

    <view class="card result-card" v-if="resultText || busy">
      <view class="card-head">
        <text class="card-title">处理结果</text>
        <view class="small-btn" v-if="resultText" @click="copyResult">复制</view>
      </view>
      <textarea class="result-input" v-model="resultText" :maxlength="-1" placeholder="AI 输出会显示在这里..." />
      <view class="export-row" v-if="resultText">
        <view class="export-btn primary" @click="exportStandard">标准导出</view>
        <view class="export-btn" :class="{ disabled: !canPreserve }" @click="exportPreserve">原格式导出</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onMounted } from 'vue';
import { BASE_URL, getToken, rawPost, streamRequest } from '@/utils/request';
import { useModelToggle } from '@/composables/useModelToggle';
import { useQuota, type QuotaInfo } from '@/composables/useQuota';

export type PaperToolMode = 'polish' | 'ai-reduce' | 'plagiarism-reduce';

const props = defineProps<{ mode: PaperToolMode }>();
const mode = computed(() => props.mode);

interface FileInfo { name: string; size: number; path: string; }
interface Paragraph { index: number; text: string; }
interface UploadResponse { fullText: string; paragraphs?: Paragraph[]; fileType?: string; error?: string; }

const metas: Record<PaperToolMode, { title: string; desc: string; action: string; running: string }> = {
  polish: { title: '学术润色', desc: '逐段优化表达，保留论文结构。', action: '开始润色', running: '润色中...' },
  'ai-reduce': { title: '降 AI 检测', desc: '扫描 AI 痕迹并改写高风险表达。', action: '开始降AI', running: '改写中...' },
  'plagiarism-reduce': { title: '降查重', desc: '检测重复风险，生成降重版本。', action: '开始降重', running: '降重中...' },
};
const meta = computed(() => metas[mode.value]);

const file = ref<FileInfo | null>(null);
const sourceText = ref('');
const resultText = ref('');
const scanSummary = ref('');
const paragraphs = ref<Paragraph[]>([]);
const busy = ref(false);
const rewriteMode = ref('medium');
const quota = ref<(QuotaInfo & { knowledgeBaseEnabled?: boolean }) | null>(null);
const { currentModel, options, selectModel } = useModelToggle();
const { fetchQuota } = useQuota();

const rewriteModes = computed(() => mode.value === 'ai-reduce'
  ? [{ id: 'light', label: '轻度' }, { id: 'medium', label: '中度' }, { id: 'deep', label: '深度' }]
  : [{ id: 'light', label: '轻度' }, { id: 'medium', label: '中度' }, { id: 'deep', label: '深度' }]
);
const canPreserve = computed(() => !!file.value && file.value.name.toLowerCase().endsWith('.docx'));

onMounted(async () => {
  quota.value = await fetchQuota().catch(() => null) as (QuotaInfo & { knowledgeBaseEnabled?: boolean }) | null;
});

function go(next: PaperToolMode) {
  if (next === mode.value) return;
  uni.redirectTo({ url: `/pages/paper-tools/${next}` });
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / 1024 / 1024).toFixed(1) + ' MB';
}

function chooseFile() {
  uni.chooseMessageFile({
    count: 1,
    type: 'file',
    extension: ['docx', 'pdf'],
    success: async (res) => {
      const first = res.tempFiles[0];
      if (!first) return;
      file.value = { name: first.name, size: first.size, path: first.path };
      await uploadPaper();
    },
  });
}

async function uploadPaper() {
  if (!file.value) return;
  uni.showLoading({ title: '解析中...' });
  try {
    const res = await uni.uploadFile({
      url: `${BASE_URL}/api/paper/upload`,
      filePath: file.value.path,
      name: 'file',
      header: { Authorization: 'Bearer ' + getToken() },
    });
    const data = JSON.parse(res.data || '{}') as UploadResponse;
    if (data.error) throw new Error(data.error);
    sourceText.value = data.fullText || '';
    paragraphs.value = data.paragraphs || [];
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '文件解析失败';
    uni.showToast({ title: msg, icon: 'none' });
  } finally {
    uni.hideLoading();
  }
}

async function startRun() {
  if (busy.value || !sourceText.value.trim()) return;
  busy.value = true;
  resultText.value = '';
  scanSummary.value = '';
  try {
    if (mode.value === 'ai-reduce') {
      const scan = await rawPost<Record<string, unknown>>('/api/ai-reduce/scan', { text: sourceText.value });
      scanSummary.value = summarize(scan.result || scan);
      stream('/api/ai-reduce/rewrite', { text: sourceText.value, mode: rewriteMode.value, model: currentModel.value, flaggedSentences: [] });
    } else if (mode.value === 'plagiarism-reduce') {
      const scan = await rawPost<Record<string, unknown>>('/api/plagiarism-reduce/scan', { text: sourceText.value, sourceText: '', mode: rewriteMode.value, model: currentModel.value });
      scanSummary.value = summarize(scan);
      stream('/api/plagiarism-reduce/run', { text: sourceText.value, sourceText: '', mode: rewriteMode.value, model: currentModel.value, annotations: [] });
    } else {
      stream('/api/polish/run', { text: sourceText.value, taskType: '章节正文', polishType: 'full', language: 'zh', model: currentModel.value });
    }
  } catch (e: unknown) {
    busy.value = false;
    const msg = e instanceof Error ? e.message : '请求失败';
    uni.showToast({ title: msg, icon: 'none' });
  }
}

function stream(url: string, body: Record<string, unknown>) {
  streamRequest(url, body, {
    onToken: (token) => { resultText.value += token; },
    onFinish: () => { busy.value = false; },
    onError: (err) => {
      busy.value = false;
      uni.showToast({ title: err.message || 'AI 服务异常', icon: 'none' });
    },
  });
}

function summarize(data: unknown): string {
  if (!data) return '';
  if (typeof data === 'string') return data.slice(0, 240);
  try { return JSON.stringify(data).slice(0, 240); } catch { return String(data).slice(0, 240); }
}

function copyResult() {
  uni.setClipboardData({ data: resultText.value, success: () => uni.showToast({ title: '已复制' }) });
}

function exportPayload(fileName: string) {
  const paras = paragraphs.value.length
    ? paragraphs.value.map((p) => ({ index: p.index, originalText: p.text, text: resultText.value }))
    : [{ index: 0, originalText: sourceText.value, text: resultText.value }];
  return { fileName, paragraphs: paras };
}

function exportStandard() {
  downloadJsonDoc('/api/paper-export/standard', exportPayload(downloadName()));
}

function exportPreserve() {
  if (!file.value || !canPreserve.value) {
    uni.showToast({ title: 'Preserve export supports DOCX only', icon: 'none' });
    return;
  }
  uni.showLoading({ title: 'Exporting...' });
  uni.uploadFile({
    url: getBaseUrlForFile('/api/paper-export/preserve-format'),
    filePath: file.value.path,
    name: 'file',
    formData: { mappings: JSON.stringify(exportPayload(downloadName())) },
    header: { Authorization: 'Bearer ' + getToken() },
    success: (res) => {
      uni.hideLoading();
      if (res.statusCode < 200 || res.statusCode >= 300) {
        uni.showToast({ title: 'Preserve export failed', icon: 'none' });
        return;
      }
      const json = tryParseJson<{ url?: string; fileUrl?: string; downloadUrl?: string }>(res.data);
      const remoteUrl = json?.downloadUrl || json?.fileUrl || json?.url;
      if (remoteUrl) {
        downloadRemoteDoc(remoteUrl, `${downloadName()}-preserve.docx`);
        return;
      }
      if (!json && res.data) {
        openBinaryString(res.data, `${downloadName()}-preserve.docx`);
        return;
      }
      uni.showToast({ title: 'Export finished, but no file was returned', icon: 'none' });
    },
    fail: () => { uni.hideLoading(); uni.showToast({ title: 'Preserve export failed', icon: 'none' }); },
  });
}

function tryParseJson<T>(value: string): T | null {
  try { return JSON.parse(value) as T; } catch { return null; }
}

function getBaseUrlForFile(url: string): string {
  return BASE_URL + url;
}

function downloadRemoteDoc(url: string, name: string) {
  const fullUrl = /^https?:///.test(url) ? url : BASE_URL + url;
  uni.showLoading({ title: 'Opening...' });
  uni.downloadFile({
    url: fullUrl,
    header: { Authorization: 'Bearer ' + getToken() },
    success: (res) => {
      uni.hideLoading();
      if (res.statusCode && (res.statusCode < 200 || res.statusCode >= 300)) {
        uni.showToast({ title: 'Download failed', icon: 'none' });
        return;
      }
      uni.openDocument({ filePath: res.tempFilePath, fileType: name.endsWith('.pdf') ? 'pdf' : 'docx', showMenu: true });
    },
    fail: () => { uni.hideLoading(); uni.showToast({ title: 'Download failed', icon: 'none' }); },
  });
}

function openBinaryString(data: string, name: string) {
  // #ifdef MP-WEIXIN
  const filePath = `${wx.env.USER_DATA_PATH}/${name}`;
  wx.getFileSystemManager().writeFile({
    filePath,
    data,
    encoding: 'binary',
    success: () => uni.openDocument({ filePath, fileType: 'docx', showMenu: true }),
    fail: () => uni.showToast({ title: 'Open document failed', icon: 'none' }),
  });
  // #endif
  // #ifndef MP-WEIXIN
  uni.showToast({ title: 'Export finished', icon: 'none' });
  // #endif
}
function downloadJsonDoc(url: string, payload: Record<string, unknown>) {
  uni.showLoading({ title: '导出中...' });
  uni.request({
    url: BASE_URL + url,
    method: 'POST',
    data: payload,
    responseType: 'arraybuffer',
    header: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + getToken() },
    success: (res) => {
      uni.hideLoading();
      if (res.statusCode < 200 || res.statusCode >= 300 || !(res.data instanceof ArrayBuffer)) {
        uni.showToast({ title: '导出失败', icon: 'none' });
        return;
      }
      openArrayBuffer(res.data, `${downloadName()}.docx`);
    },
    fail: () => { uni.hideLoading(); uni.showToast({ title: '导出失败', icon: 'none' }); },
  });
}

function openArrayBuffer(buffer: ArrayBuffer, name: string) {
  // #ifdef MP-WEIXIN
  const filePath = `${wx.env.USER_DATA_PATH}/${name}`;
  wx.getFileSystemManager().writeFile({
    filePath,
    data: buffer,
    encoding: 'binary',
    success: () => uni.openDocument({ filePath, showMenu: true }),
    fail: () => uni.showToast({ title: '保存文件失败', icon: 'none' }),
  });
  // #endif
  // #ifndef MP-WEIXIN
  uni.showToast({ title: '当前平台请使用网页端导出', icon: 'none' });
  // #endif
}

function downloadName(): string {
  return mode.value === 'polish' ? 'polished' : mode.value === 'ai-reduce' ? 'ai-reduced' : 'plagiarism-reduced';
}
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";
.paper-tool { min-height: 100vh; background: $bg-canvas; padding: 36rpx 28rpx 60rpx; }
.page-head { margin-bottom: 28rpx; }
.brand-tag { color: $accent; font-size: 22rpx; font-weight: 700; letter-spacing: 4rpx; display: block; margin-bottom: 10rpx; }
.page-title { font-family: Georgia, serif; font-size: 42rpx; font-weight: 700; color: $text-main; display: block; margin-bottom: 10rpx; }
.page-desc { font-size: 26rpx; color: $text-muted; line-height: 1.6; display: block; }
.switch-row { display: flex; gap: 12rpx; margin-bottom: 24rpx; }
.switch-chip { flex: 1; text-align: center; padding: 18rpx 0; background: $bg-paper; border: 1px solid $border-light; border-radius: $radius-md; font-size: 24rpx; color: $text-muted; }
.switch-chip.active { background: $bg-dark; color: #fff; border-color: $bg-dark; }
.card { background: $bg-paper; border: 1px solid $border-light; border-radius: $radius-lg; padding: 26rpx; box-shadow: $shadow-sm; margin-bottom: 20rpx; }
.card-head { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; margin-bottom: 14rpx; }
.card-title { font-size: 28rpx; font-weight: 700; color: $text-main; }
.small-btn { font-size: 22rpx; color: $accent; background: rgba(217,117,10,0.08); padding: 10rpx 18rpx; border-radius: $radius-full; }
.file-name { font-size: 22rpx; color: $text-light; display: block; margin-bottom: 12rpx; word-break: break-all; }
.paper-input, .result-input { width: 100%; min-height: 280rpx; box-sizing: border-box; background: $bg-surface; border: 1px solid $border-medium; border-radius: $radius-md; padding: 20rpx; font-size: 26rpx; color: $text-main; line-height: 1.7; }
.result-input { min-height: 340rpx; }
.row { display: flex; align-items: center; justify-content: space-between; gap: 18rpx; margin-bottom: 18rpx; }
.row-label { font-size: 24rpx; color: $text-muted; flex-shrink: 0; }
.model-opts { display: flex; flex-wrap: wrap; gap: 8rpx; justify-content: flex-end; }
.model-opt { font-size: 22rpx; color: $text-light; background: $bg-surface; border: 1px solid $border-light; padding: 10rpx 20rpx; border-radius: $radius-full; }
.model-opt.active { color: #fff; background: $bg-dark; border-color: $bg-dark; }
.kb-note { font-size: 22rpx; color: $text-light; line-height: 1.6; background: rgba(217,117,10,0.05); padding: 16rpx; border-radius: $radius-md; }
.action-btn { height: 92rpx; background: $bg-dark; color: #fff; border-radius: $radius-lg; display: flex; align-items: center; justify-content: center; font-size: 30rpx; font-weight: 700; margin-bottom: 20rpx; }
.action-btn.disabled { opacity: 0.45; }
.scan-text { display: block; margin-top: 14rpx; font-size: 24rpx; color: $text-muted; line-height: 1.6; }
.export-row { display: flex; gap: 14rpx; margin-top: 18rpx; }
.export-btn { flex: 1; text-align: center; padding: 18rpx 0; border-radius: $radius-md; background: $bg-surface; color: $text-main; font-size: 24rpx; font-weight: 600; }
.export-btn.primary { background: $accent; color: #fff; }
.export-btn.disabled { opacity: 0.4; }
@media (min-width: 1025px) { .paper-tool { max-width: 760px; margin: 0 auto; } }
</style>
