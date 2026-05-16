<template>
  <view class="chat">
    <!-- 岗位选择 -->
    <view class="position-screen" v-if="!started">
      <view class="pos-header">
        <text class="pos-icon">🤖</text>
        <text class="pos-title">AI 模拟面试</text>
        <text class="pos-desc">选择岗位，AI 面试官将与你进行一场真实技术面试</text>
      </view>

      <view class="resume-section" v-if="resumeList.length > 0">
        <text class="rs-label">已有简历分析报告，可引入面试</text>
        <picker mode="selector" :range="resumeNames" @change="onResumePick">
          <view class="rs-picker">{{ selectedResumeName || '选择简历（可选）' }}</view>
        </picker>
      </view>

      <view class="model-bar">
        <text class="model-bar-label">模型</text>
        <view class="model-opts">
          <view class="model-opt" :class="{ active: interviewModel === 'deepseek-v4-flash' }"
            @click="interviewModel = 'deepseek-v4-flash'">Flash</view>
          <view class="model-opt" :class="{ active: interviewModel === 'deepseek-v4-pro' }"
            @click="interviewModel = 'deepseek-v4-pro'">Pro</view>
        </view>
      </view>

      <view class="pos-grid">
        <view class="pos-card" v-for="p in positions" :key="p" @click="startInterview(p)">
          <view class="pos-card-icon">
            <text>{{ getPosEmoji(p) }}</text>
          </view>
          <view class="pos-card-body">
            <text class="pos-name">{{ p }}</text>
            <text class="pos-hint">点击开始面试</text>
          </view>
          <text class="pos-arrow">→</text>
        </view>
      </view>
    </view>

    <!-- 聊天区域 -->
    <view class="chat-wrap" v-if="started && !finished">
      <!-- 顶部栏 -->
      <view class="chat-topbar">
        <text class="chat-back" @click="endInterview">←</text>
        <view class="chat-topbar-center">
          <text class="chat-title">{{ currentPosition }}</text>
          <view class="chat-status">
            <view class="chat-status-dot" />
            <text class="chat-status-text">AI 面试官在线</text>
          </view>
        </view>
        <text class="chat-end-btn" @click="endInterview">结束</text>
      </view>

      <!-- 消息列表 -->
      <scroll-view class="msg-list" scroll-y :scroll-into-view="'m-' + (messages.length - 1)" scroll-with-animation>
        <view v-for="(m, i) in messages" :key="i" :id="'m-' + i">
          <!-- AI 消息 -->
          <view class="msg-row ai" v-if="m.role === 'ai'">
            <view class="msg-avatar">
              <text class="msg-avatar-text">AI</text>
            </view>
            <view class="msg-main">
              <text class="msg-role">AI 面试官</text>
              <view class="msg-bubble ai-bubble">
                <!-- 代码块检测 -->
                <template v-if="hasCodeBlock(m.content)">
                  <block v-for="(seg, si) in parseCodeSegments(m.content)" :key="si">
                    <rich-text v-if="seg.type === 'text'" class="msg-richtext" :nodes="renderMarkdown(seg.content)" />
                    <view v-else class="code-block">
                      <view class="code-header">
                        <view class="code-dots">
                          <view class="dot dot-red" />
                          <view class="dot dot-yellow" />
                          <view class="dot dot-green" />
                        </view>
                        <text class="code-filename">{{ seg.filename || 'code' }}</text>
                      </view>
                      <scroll-view class="code-body" scroll-x>
                        <text class="code-text">{{ seg.content }}</text>
                      </scroll-view>
                    </view>
                  </block>
                </template>
                <rich-text v-else class="msg-richtext" :nodes="renderMarkdown(m.content)" />
              </view>
            </view>
          </view>

          <!-- 用户消息 -->
          <view class="msg-row user" v-else>
            <view class="msg-main user-main">
              <view class="msg-bubble user-bubble">
                <text class="msg-text-light">{{ m.content }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 思考中 -->
        <view v-if="loading" class="msg-row ai">
          <view class="msg-avatar">
            <text class="msg-avatar-text">AI</text>
          </view>
          <view class="msg-main">
            <text class="msg-role">AI 面试官</text>
            <view class="msg-bubble ai-bubble skeleton-bubble">
              <view class="skeleton">
                <view class="skeleton-bar" style="width: 85%;" />
                <view class="skeleton-bar" style="width: 60%;" />
                <view class="skeleton-bar" style="width: 72%;" />
              </view>
              <text class="skeleton-hint">正在分析你的回答...</text>
            </view>
          </view>
        </view>

        <view class="msg-bottom-safe" />
      </scroll-view>

      <!-- 输入区 -->
      <view class="input-zone">
        <!-- 语音面板 -->
        <view class="voice-panel" v-if="voiceMode">
          <view class="voice-bar">
            <view class="voice-btn-keyboard" @click="voiceMode = false">
              <text>⌨</text>
            </view>
            <text class="voice-hint">轻触说话...</text>
            <view class="voice-btn-mic" @touchstart="startRecord" @touchend="stopRecord">
              <text>🎤</text>
            </view>
          </view>
        </view>

        <!-- 录音中 -->
        <view class="recording-panel" v-if="recording">
          <view class="recording-cancel" @click="cancelRecord">
            <text class="recording-cancel-icon">✕</text>
          </view>
          <view class="recording-waves">
            <view class="wave-bar" v-for="i in 5" :key="i" :style="{ animationDelay: (i * 0.12) + 's' }" />
          </view>
          <view class="recording-send" @click="finishRecord">
            <text class="recording-send-icon">↑</text>
          </view>
        </view>

        <!-- 文字输入 -->
        <view class="text-input-row" v-if="!voiceMode && !recording">
          <view class="input-switch" @click="voiceMode = true">
            <text class="input-switch-icon">🎤</text>
          </view>
          <view class="input-area-wrap">
            <textarea
              class="input-area"
              v-model="inputText"
              placeholder="输入你的回答..."
              :disabled="loading"
              :maxlength="-1"
              auto-height
              :adjust-position="false"
            />
          </view>
          <view class="send-btn" :class="{ disabled: loading || !inputText.trim() }" @click="sendAnswer" v-if="inputText.trim()">
            <text class="send-btn-icon">↑</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 完成 -->
    <view class="finish-overlay" v-if="finished">
      <view class="finish-icon-box">
        <text class="fin-icon">{{ report?.score && (report.score as number) >= 7 ? '🎉' : '💪' }}</text>
      </view>
      <text class="fin-title">面试完成</text>
      <text class="fin-desc">AI 已基于对话内容生成评估报告</text>
      <view class="fin-btn" @click="goReport">
        <text>查看面试报告</text>
        <text class="fin-btn-arrow">→</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { get, post } from '@/utils/request';

const positions = ['Java后端开发','前端开发','C++开发','Python开发','算法工程师','测试开发'];
const posEmoji: Record<string,string> = { 'Java后端开发':'☕','前端开发':'🎨','C++开发':'⚙️','Python开发':'🐍','算法工程师':'🧮','测试开发':'🔍' };
function getPosEmoji(p: string) { return posEmoji[p] || '💼'; }

const started = ref(false); const finished = ref(false); const loading = ref(false);
const interviewModel = ref('deepseek-v4-flash');
const sessionId = ref(0); const messages = ref<{role:string;content:string}[]>([]);
const inputText = ref(''); const report = ref<Record<string,unknown>|null>(null);
const currentPosition = ref('');
const voiceMode = ref(false);
const recording = ref(false);

interface ResumeItem { id: number; position: string; }
const resumeList = ref<ResumeItem[]>([]);
const resumeNames = computed(() => resumeList.value.map(r => r.position || `简历 #${r.id}`));
const selectedResumeId = ref<number | null>(null);
const selectedResumeName = ref('');

onMounted(async () => {
  try {
    const r = await get<ResumeItem[]>('/api/resume/list');
    resumeList.value = r.data || [];
  } catch { /* ignore */ }
});

function onResumePick(e: { detail: { value: number } }) {
  const idx = e.detail.value;
  selectedResumeId.value = resumeList.value[idx]?.id ?? null;
  selectedResumeName.value = resumeNames.value[idx] ?? '';
}

/* ========== 代码块检测 ========== */
interface CodeSegment { type: 'text' | 'code'; content: string; filename?: string; }

function hasCodeBlock(md: string): boolean {
  return /```/.test(md);
}

function parseCodeSegments(md: string): CodeSegment[] {
  const segments: CodeSegment[] = [];
  const parts = md.split(/(```[\s\S]*?```)/g);
  for (const part of parts) {
    const m = part.match(/```(\w+)?\s*\n?([\s\S]*?)```/);
    if (m) {
      const lang = m[1] || '';
      const code = m[2].trim();
      segments.push({ type: 'code', content: code, filename: lang ? `${lang}` : 'code' });
    } else if (part.trim()) {
      segments.push({ type: 'text', content: part });
    }
  }
  return segments;
}

/* ========== Markdown → rich-text 节点 ========== */
interface RichNode { name?: string; attrs?: Record<string,string>; children?: RichNode[]; type?: string; text?: string; }

function renderMarkdown(md: string): RichNode[] {
  let text = md.replace(/\[面试结束\]/g, '');
  text = text.replace(/\{[^{]*"score"[^}]*\}/g, '');
  text = text.trim();

  const lines = text.split('\n');
  const nodes: RichNode[] = [];

  for (let i = 0; i < lines.length; i++) {
    if (i > 0) nodes.push({ name: 'br' });
    const line = lines[i];
    if (!line.trim()) continue;

    const lineNodes = parseInline(line);
    for (const n of lineNodes) nodes.push(n);
  }

  return nodes;
}

function parseInline(text: string): RichNode[] {
  const nodes: RichNode[] = [];
  let remaining = text;

  while (remaining.length > 0) {
    const boldMatch = remaining.match(/^(.*?)\*\*(.+?)\*\*/);
    const italicMatch = remaining.match(/^(.*?)\*(.+?)\*/);
    const codeMatch = remaining.match(/^(.*?)`([^`]+)`/);
    const listMatch = remaining.match(/^(\d+\.\s+)/);

    const matches: Array<{ type: string; match: RegExpMatchArray; idx: number }> = [];
    if (boldMatch) matches.push({ type: 'bold', match: boldMatch, idx: boldMatch.index! });
    if (italicMatch) matches.push({ type: 'italic', match: italicMatch, idx: italicMatch.index! });
    if (codeMatch) matches.push({ type: 'code', match: codeMatch, idx: codeMatch.index! });

    matches.sort((a, b) => a.idx - b.idx);

    if (matches.length > 0) {
      const m = matches[0];
      const before = m.match[1] || '';
      const content = m.match[2] || '';

      if (before) nodes.push({ type: 'text', text: before });

      switch (m.type) {
        case 'bold':
          nodes.push({ name: 'strong', children: [{ type: 'text', text: content }] });
          break;
        case 'italic':
          nodes.push({ name: 'em', children: [{ type: 'text', text: content }] });
          break;
        case 'code':
          nodes.push({ name: 'span', attrs: { style: 'background:#F7F7F5;padding:2px 6px;border-radius:4px;font-family:monospace;font-size:13px;color:#D9750A;' }, children: [{ type: 'text', text: content }] });
          break;
      }

      remaining = remaining.substring(m.match[0].length);
    } else {
      if (listMatch) {
        nodes.push({ name: 'strong', children: [{ type: 'text', text: listMatch[1] }] });
        remaining = remaining.substring(listMatch[0].length);
      } else {
        nodes.push({ type: 'text', text: remaining });
        remaining = '';
      }
    }
  }

  return nodes;
}

/* ========== 语音相关 ========== */
function startRecord() {
  recording.value = true;
  // TODO: 接入 uni.getRecorderManager() 实现录音
}

function stopRecord() {
  // 录音结束
}

function cancelRecord() {
  recording.value = false;
}

function finishRecord() {
  recording.value = false;
  voiceMode.value = false;
  // TODO: 语音转文字后填入 inputText
}

/* ========== API 调用 ========== */
async function startInterview(pos: string) {
  try { uni.showLoading({ title:'思考中...' });
    const body: Record<string, unknown> = { position: pos, model: interviewModel.value };
    if (selectedResumeId.value) {
      body.resumeId = selectedResumeId.value;
    }
    const r = await post<{sessionId:number;question:string}>('/api/interview/start', body);
    sessionId.value = r.data.sessionId;
    currentPosition.value = pos;
    messages.value.push({role:'ai',content:r.data.question});
    started.value = true; uni.hideLoading();
  } catch { uni.hideLoading(); uni.showToast({title:'启动失败',icon:'error'}); }
}

async function sendAnswer() {
  if (!inputText.value.trim() || loading.value) return;
  const ans = inputText.value.trim();
  messages.value.push({role:'user',content:ans});
  inputText.value = ''; loading.value = true;
  try { const r = await post<{finished:boolean;question?:string;report?:Record<string,unknown>}>(
    `/api/interview/${sessionId.value}/answer`,{answer:ans});
    if (r.data.finished) { finished.value = true; report.value = r.data.report||null; saveAndGo(); }
    else if (r.data.question) { messages.value.push({role:'ai',content:r.data.question}); }
  } catch { messages.value.push({role:'ai',content:'抱歉，AI服务暂时不可用。'}); }
  finally { loading.value = false; }
}

async function endInterview() {
  uni.showModal({ title:'结束面试', content:'确定要结束当前面试吗？AI将基于对话内容生成评估报告。', success: async res => {
    if (!res.confirm) return;
    uni.showLoading({ title:'生成报告中...' });
    try { const r = await post<{finished:boolean;report?:Record<string,unknown>}>(`/api/interview/${sessionId.value}/end`);
      finished.value = true; report.value = r.data.report||null; uni.hideLoading(); saveAndGo();
    } catch { uni.hideLoading(); uni.showToast({title:'结束失败',icon:'error'}); }
  }});
}

function saveAndGo() {
  uni.setStorageSync('lastChatMessages', JSON.stringify(messages.value));
  uni.setStorageSync('lastChatSessionId', sessionId.value);
  setTimeout(() => {
    uni.redirectTo({ url: `/pages/interview/report?sessionId=${sessionId.value}&report=${encodeURIComponent(JSON.stringify(report.value||{}))}` });
  }, 600);
}

function goReport() { saveAndGo(); }
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.chat { min-height: 100vh; background: $bg-canvas; }

// ===== 岗位选择 =====
.position-screen { padding: 60rpx 28rpx; }
.pos-header { text-align: center; margin-bottom: 48rpx; }
.pos-icon { font-size: 96rpx; display: block; margin-bottom: 24rpx; }
.pos-title { font-family: Georgia, serif; font-size: 40rpx; font-weight: 600; color: $text-main; display: block; }
.pos-desc { font-size: 26rpx; color: $text-muted; margin-top: 12rpx; display: block; line-height: 1.6; }

.pos-grid { display: flex; flex-direction: column; gap: 16rpx; }
.pos-card {
  display: flex; align-items: center; gap: 20rpx;
  background: $bg-paper; border: 1px solid $border-light;
  padding: 28rpx 24rpx; border-radius: $radius-lg;
  box-shadow: $shadow-sm;
}
.pos-card:active { background: $bg-surface; }
.pos-card-icon {
  width: 68rpx; height: 68rpx; background: $bg-surface;
  border-radius: $radius-md; display: flex; align-items: center; justify-content: center;
  font-size: 32rpx; flex-shrink: 0;
}
.pos-card-body { flex: 1; display: flex; flex-direction: column; }
.pos-name { font-size: 28rpx; font-weight: 500; color: $text-main; margin-bottom: 4rpx; }
.pos-hint { font-size: 22rpx; color: $text-light; }
.pos-arrow { font-size: 28rpx; color: $text-light; }

.resume-section { margin-bottom: 30rpx; }
.rs-label { font-size: 24rpx; color: $text-muted; display: block; margin-bottom: 12rpx; }
.rs-picker { background: $bg-paper; border: 1px solid $border-light; border-radius: $radius-md; padding: 20rpx 24rpx; font-size: 26rpx; color: $text-light; }

.model-bar { display: flex; align-items: center; justify-content: center; gap: 16rpx; margin-bottom: 24rpx; }
.model-bar-label { font-size: 24rpx; color: $text-light; }
.model-opts { display: flex; gap: 0; background: $bg-surface; border-radius: $radius-sm; overflow: hidden; }
.model-opt { font-size: 22rpx; padding: 10rpx 32rpx; color: $text-light; }
.model-opt.active { background: $bg-dark; color: #fff; }

// ===== 聊天区 =====
.chat-wrap { display: flex; flex-direction: column; height: 100vh; }

// 顶部栏
.chat-topbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16rpx 24rpx; background: $bg-paper;
  border-bottom: 1px solid $border-light; flex-shrink: 0;
}
.chat-back { font-size: 36rpx; color: $text-muted; padding: 8rpx 12rpx 8rpx 0; }
.chat-topbar-center { text-align: center; }
.chat-title { font-size: 28rpx; font-weight: 600; color: $text-main; display: block; }
.chat-status { display: flex; align-items: center; gap: 8rpx; justify-content: center; margin-top: 4rpx; }
.chat-status-dot { width: 10rpx; height: 10rpx; background: $color-success; border-radius: 50%; }
.chat-status-text { font-size: 22rpx; color: $color-success; }
.chat-end-btn {
  font-size: 22rpx; font-weight: 500; color: $color-danger;
  border: 1px solid $border-medium; border-radius: $radius-sm;
  padding: 10rpx 24rpx; background: none;
}

// ===== 消息列表 =====
.msg-list { flex: 1; padding: 24rpx; overflow-y: auto; }
.msg-row { margin-bottom: 28rpx; display: flex; gap: 14rpx; }
.msg-row.user { justify-content: flex-end; }

// AI 头像 — 方形圆角
.msg-avatar {
  width: 64rpx; height: 64rpx; border-radius: $radius-sm; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: $bg-surface; border: 1px solid $border-light;
  margin-top: 4rpx;
}
.msg-avatar-text {
  font-size: 22rpx; font-weight: 700; color: $text-muted;
  font-family: Georgia, serif; letter-spacing: -0.5px;
}

.msg-main { flex: 1; min-width: 0; }
.user-main { flex: 0 0 auto; max-width: 85%; display: flex; justify-content: flex-end; }

// 角色名
.msg-role {
  font-size: 24rpx; font-weight: 500; color: $text-muted;
  margin-bottom: 8rpx; display: block; padding-left: 4rpx;
}

// AI 消息气泡
.msg-bubble {
  padding: 20rpx 24rpx; border-radius: $radius-md;
  border-top-left-radius: 4rpx;
}
.ai-bubble {
  background: $bg-paper; border: 1px solid $border-light;
}
.msg-richtext {
  display: block; font-size: 28rpx; line-height: 1.7; color: $text-main;
}

// 用户消息气泡
.user-bubble {
  background: $bg-surface; border: 1px solid $border-light;
  border-radius: $radius-md; border-top-right-radius: 4rpx;
}
.msg-text-light { font-size: 28rpx; line-height: 1.7; color: $text-main; }

// ===== 暗色代码块 =====
.code-block {
  background: $bg-dark; border-radius: $radius-lg;
  margin-top: 16rpx; margin-bottom: 8rpx;
  overflow: hidden;
}
.code-header {
  display: flex; align-items: center; gap: 12rpx;
  padding: 18rpx 20rpx 12rpx;
}
.code-dots { display: flex; gap: 8rpx; flex-shrink: 0; }
.dot { width: 12rpx; height: 12rpx; border-radius: 50%; }
.dot-red { background: #ED6A5E; }
.dot-yellow { background: #F4BF4F; }
.dot-green { background: #61C554; }
.code-filename {
  font-size: 22rpx; color: rgba(255,255,255,0.40);
  font-family: monospace; margin-left: 4rpx;
}
.code-body {
  padding: 0 20rpx 20rpx; overflow-x: auto;
  white-space: pre;
}
.code-text {
  font-family: monospace; font-size: 22rpx; line-height: 1.7;
  color: #E4E4E4;
}

// ===== 骨架屏 =====
.skeleton-bubble { min-width: 300rpx; }
.skeleton { padding: 4rpx 0; }
.skeleton-bar {
  height: 20rpx; margin-bottom: 14rpx; border-radius: 6rpx;
  background: linear-gradient(90deg, #eee 25%, #e0e0e0 50%, #eee 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite linear;
}
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
.skeleton-hint {
  font-size: 22rpx; color: $text-light; margin-top: 8rpx; display: block;
}

// ===== 输入区 =====
.input-zone { flex-shrink: 0; }

// 语音面板
.voice-panel { padding: 16rpx 24rpx 40rpx; }
.voice-bar {
  display: flex; align-items: center; justify-content: space-between;
  background: $bg-paper; border-radius: 24px;
  padding: 16rpx 28rpx;
  box-shadow: $shadow-lg;
}
.voice-btn-keyboard {
  width: 72rpx; height: 72rpx; background: $bg-surface;
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: 32rpx;
}
.voice-hint { font-size: 28rpx; color: $text-light; }
.voice-btn-mic {
  width: 80rpx; height: 80rpx; background: $bg-dark;
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: 36rpx;
}

// 录音中面板
.recording-panel {
  display: flex; align-items: center; justify-content: space-between;
  background: $bg-dark; border-radius: 24px;
  padding: 28rpx 32rpx; margin: 16rpx 24rpx 40rpx;
}
.recording-cancel, .recording-send {
  width: 80rpx; height: 80rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.recording-cancel { background: rgba(255,255,255,0.12); }
.recording-cancel-icon { font-size: 32rpx; color: #fff; }
.recording-send { background: $accent; }
.recording-send-icon { font-size: 36rpx; color: #fff; font-weight: 700; }

.recording-waves { display: flex; align-items: center; gap: 8rpx; }
.wave-bar {
  width: 6rpx; height: 28rpx; background: $accent; border-radius: 3rpx;
  animation: wave 0.6s ease-in-out infinite alternate;
}
@keyframes wave {
  0% { height: 16rpx; }
  100% { height: 44rpx; }
}

// 文字输入行
.text-input-row {
  display: flex; align-items: flex-end; gap: 12rpx;
  padding: 12rpx 20rpx 40rpx; background: $bg-paper;
  border-top: 1px solid $border-light;
}
.input-switch {
  width: 72rpx; height: 72rpx; background: $bg-surface;
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; font-size: 28rpx;
}
.input-area-wrap { flex: 1; }
.input-area {
  width: 100%; min-height: 72rpx; max-height: 200rpx;
  background: $bg-surface; border: 1px solid $border-light;
  border-radius: $radius-md; padding: 16rpx 20rpx;
  font-size: 28rpx; line-height: 1.5; box-sizing: border-box;
}
.send-btn {
  width: 72rpx; height: 72rpx; background: $bg-dark;
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.send-btn.disabled { background: $bg-surface; }
.send-btn-icon { font-size: 32rpx; color: #fff; font-weight: 700; }
.send-btn.disabled .send-btn-icon { color: $text-light; }

.msg-bottom-safe { height: 20rpx; }

// ===== 完成页 =====
.finish-overlay {
  display: flex; flex-direction: column; align-items: center;
  padding: 200rpx 48rpx 0;
}
.finish-icon-box {
  width: 140rpx; height: 140rpx; background: $bg-paper;
  border: 1px solid $border-light; border-radius: 28px;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 32rpx; box-shadow: $shadow-md;
}
.fin-icon { font-size: 72rpx; }
.fin-title {
  font-family: Georgia, serif; font-size: 40rpx; font-weight: 600;
  color: $text-main; margin-bottom: 12rpx;
}
.fin-desc { font-size: 26rpx; color: $text-muted; margin-bottom: 56rpx; }
.fin-btn {
  width: 100%; max-width: 500rpx; height: 100rpx;
  background: $bg-dark; color: #fff; font-size: 30rpx; font-weight: 600;
  border-radius: 50px; border: none;
  display: flex; align-items: center; justify-content: center; gap: 12rpx;
}
.fin-btn:active { opacity: 0.9; }
.fin-btn-arrow { font-size: 24rpx; }

// ===== PC =====
@media (min-width: 1025px) {
  .chat-wrap { max-width: 800px; margin: 0 auto; }
  .position-screen { max-width: 600px; margin: 0 auto; }
  .finish-overlay { max-width: 600px; margin: 0 auto; }
}
</style>
