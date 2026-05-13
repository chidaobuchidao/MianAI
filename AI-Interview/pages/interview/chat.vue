<template>
  <view class="chat">
    <!-- 岗位选择 -->
    <view class="position-screen" v-if="!started">
      <view class="pos-header">
        <text class="pos-icon">🤖</text>
        <text class="pos-title">AI 模拟面试</text>
        <text class="pos-desc">选择岗位，AI面试官将与你进行一场真实技术面试（支持手动结束）</text>
      </view>
      <view class="resume-section" v-if="resumeList.length > 0">
        <text class="rs-label">📋 已有简历分析报告，可引入面试</text>
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
          <text class="pos-emoji">{{ getPosEmoji(p) }}</text>
          <text class="pos-name">{{ p }}</text>
          <text class="pos-arrow">→</text>
        </view>
      </view>
    </view>

    <!-- 聊天 -->
    <view class="chat-wrap" v-if="started && !finished">
      <scroll-view class="msg-list" scroll-y :scroll-into-view="'m-' + (messages.length - 1)" scroll-with-animation>
        <view v-for="(m, i) in messages" :key="i" :id="'m-' + i" class="msg-row" :class="m.role">
          <view class="msg-bubble">
            <text v-if="m.role === 'ai'" class="msg-badge">AI 面试官</text>
            <rich-text v-if="m.role === 'ai'" :nodes="renderMarkdown(m.content)" class="msg-richtext" />
            <text v-else class="msg-text">{{ m.content }}</text>
          </view>
        </view>
        <view v-if="loading" class="msg-row ai">
          <view class="msg-bubble typing">
            <view class="dot" /><view class="dot" /><view class="dot" />
          </view>
        </view>
      </scroll-view>

      <view class="input-zone">
        <button class="end-btn" @click="endInterview" :disabled="loading">结束</button>
        <textarea class="input-area" v-model="inputText" placeholder="输入你的回答（支持多行）..." :disabled="loading" :maxlength="-1" auto-height />
        <button class="send-btn" @click="sendAnswer" :disabled="loading || !inputText.trim()">发送</button>
      </view>
    </view>

    <!-- 完成 -->
    <view class="finish-overlay" v-if="finished">
      <text class="fin-icon">{{ report?.score && (report.score as number) >= 7 ? '🎉' : '💪' }}</text>
      <text class="fin-title">面试完成</text>
      <button class="fin-btn" @click="goReport">查看面试报告</button>
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

interface ResumeItem { id: number; position: string; }
const resumeList = ref<ResumeItem[]>([]);
const resumeNames = computed(() => resumeList.value.map(r => r.position || `简历 #${r.id}`));
const selectedResumeId = ref<number | null>(null);
const selectedResumeName = ref('');

onMounted(async () => {
  try {
    const r = await get<ResumeItem[]>('/api/resume/list');
    resumeList.value = r.data || [];
  } catch {
    // ignore
  }
});

function onResumePick(e: { detail: { value: number } }) {
  const idx = e.detail.value;
  selectedResumeId.value = resumeList.value[idx]?.id ?? null;
  selectedResumeName.value = resumeNames.value[idx] ?? '';
}

/* ========== Markdown → rich-text 节点 ========== */
interface RichNode { name?: string; attrs?: Record<string,string>; children?: RichNode[]; type?: string; text?: string; }

function renderMarkdown(md: string): RichNode[] {
  // 去掉 [面试结束] 标记和 JSON 报告块
  let text = md.replace(/\[面试结束\]/g, '');
  text = text.replace(/\{[^{]*"score"[^}]*\}/g, '');
  text = text.trim();

  // 逐行解析
  const lines = text.split('\n');
  const nodes: RichNode[] = [];

  for (let i = 0; i < lines.length; i++) {
    if (i > 0) nodes.push({ name: 'br' });
    const line = lines[i];
    if (!line.trim()) continue;

    // 解析行内格式
    const lineNodes = parseInline(line);
    for (const n of lineNodes) nodes.push(n);
  }

  return nodes;
}

function parseInline(text: string): RichNode[] {
  const nodes: RichNode[] = [];
  let remaining = text;

  while (remaining.length > 0) {
    // 检测 **bold**
    const boldMatch = remaining.match(/^(.*?)\*\*(.+?)\*\*/);
    // 检测 *italic*
    const italicMatch = remaining.match(/^(.*?)\*(.+?)\*/);
    // 检测 `code`
    const codeMatch = remaining.match(/^(.*?)`([^`]+)`/);
    // 检测有序列表 1. 2. 等
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
          nodes.push({ name: 'span', attrs: { style: 'background:#f1f5f9;padding:1px 4px;border-radius:3px;font-family:monospace;font-size:13px;color:#2b6ff2;' }, children: [{ type: 'text', text: content }] });
          break;
      }

      remaining = remaining.substring(m.match[0].length);
    } else {
      // 列表前缀特殊处理
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

/* ========== API 调用 ========== */
async function startInterview(pos: string) {
  try { uni.showLoading({ title:'思考中...' });
    const body: Record<string, unknown> = { position: pos, model: interviewModel.value };
    if (selectedResumeId.value) {
      body.resumeId = selectedResumeId.value;
    }
    const r = await post<{sessionId:number;question:string}>('/api/interview/start', body);
    sessionId.value = r.data.sessionId; messages.value.push({role:'ai',content:r.data.question});
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
  // 保存聊天记录到本地存储
  uni.setStorageSync('lastChatMessages', JSON.stringify(messages.value));
  uni.setStorageSync('lastChatSessionId', sessionId.value);
  setTimeout(() => {
    uni.redirectTo({ url: `/pages/interview/report?sessionId=${sessionId.value}&report=${encodeURIComponent(JSON.stringify(report.value||{}))}` });
  }, 600);
}

function goReport() {
  saveAndGo();
}
</script>

<style lang="scss" scoped>
.chat { min-height: 100vh; background: #f0f4ff; }
.position-screen { padding: 60rpx 30rpx; }
.pos-header { text-align: center; margin-bottom: 50rpx; }
.pos-icon { font-size: 100rpx; }
.pos-title { display: block; font-size: 40rpx; font-weight: 800; color: #0f172a; margin-top: 16rpx; }
.pos-desc { display: block; font-size: 26rpx; color: #94a3b8; margin-top: 10rpx; }
.pos-grid { display: flex; flex-direction: column; gap: 16rpx; }
.pos-card { display: flex; align-items: center; gap: 20rpx; background: #fff; padding: 32rpx 28rpx; border-radius: 20rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03); transition: all 0.15s; }
.pos-card:active { background: #f0f4ff; transform: scale(0.98); }
.pos-emoji { font-size: 44rpx; }
.pos-name { flex: 1; font-size: 30rpx; font-weight: 600; color: #1e293b; }
.pos-arrow { font-size: 32rpx; color: #cbd5e1; }

.resume-section { margin-bottom: 30rpx; }
.rs-label { font-size: 24rpx; color: #2b6ff2; display: block; margin-bottom: 12rpx; }
.rs-picker { background: #fff; border: 1rpx solid #e2e8f0; border-radius: 12rpx; padding: 20rpx 24rpx; font-size: 26rpx; color: #64748b; }

.chat-wrap { display: flex; flex-direction: column; height: calc(100vh - 88rpx); }
.msg-list { flex: 1; padding: 24rpx; }
.msg-row { margin-bottom: 20rpx; display: flex; }
.msg-row.user { justify-content: flex-end; }
.msg-bubble { max-width: 82%; padding: 20rpx 24rpx; border-radius: 22rpx; position: relative; }
.msg-row.user .msg-bubble { background: linear-gradient(135deg, #2b6ff2, #4f8dff); border-bottom-right-radius: 6rpx; }
.msg-row.ai .msg-bubble { background: #fff; border-bottom-left-radius: 6rpx; box-shadow: 0 2rpx 10rpx rgba(0,0,0,0.04); }
.msg-badge { display: block; font-size: 20rpx; font-weight: 700; color: #2b6ff2; margin-bottom: 10rpx; }
.msg-text { font-size: 28rpx; line-height: 1.8; display: block; }
.msg-row.user .msg-text { color: #fff; }
.msg-richtext { display: block; font-size: 28rpx; line-height: 1.8; color: #1e293b; }

.typing { display: flex; gap: 8rpx; padding: 28rpx 32rpx; }
.dot { width: 12rpx; height: 12rpx; background: #94a3b8; border-radius: 50%; animation: bounce 1.4s infinite ease-in-out both; }
.dot:nth-child(2) { animation-delay: 0.16s; }
.dot:nth-child(3) { animation-delay: 0.32s; }
@keyframes bounce { 0%,80%,100% { transform: scale(0); } 40% { transform: scale(1); } }

.input-zone { display: flex; align-items: flex-end; gap: 12rpx; padding: 16rpx 20rpx 40rpx; background: #fff; border-top: 1rpx solid #f1f5f9; }
.end-btn { height: 72rpx; padding: 0 20rpx; background: #fef2f2; color: #ef4444; font-size: 24rpx; font-weight: 600; border-radius: 36rpx; border: none; white-space: nowrap; flex-shrink: 0; }
.end-btn[disabled] { opacity: 0.4; }
.input-area { flex: 1; min-height: 72rpx; max-height: 200rpx; background: #f1f5f9; border-radius: 16rpx; padding: 16rpx 20rpx; font-size: 28rpx; line-height: 1.5; }
.send-btn { width: 120rpx; height: 72rpx; background: linear-gradient(135deg, #2b6ff2, #4f8dff); color: #fff; font-size: 28rpx; font-weight: 700; border-radius: 36rpx; border: none; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.send-btn[disabled] { background: #e2e8f0; color: #94a3b8; }

.finish-overlay { display: flex; flex-direction: column; align-items: center; padding-top: 220rpx; }
.fin-icon { font-size: 120rpx; }
.fin-title { font-size: 40rpx; font-weight: 800; color: #0f172a; margin-top: 20rpx; }
.fin-btn { width: 460rpx; height: 96rpx; background: linear-gradient(135deg, #2b6ff2, #4f8dff); color: #fff; font-size: 32rpx; font-weight: 700; border-radius: 48rpx; border: none; margin-top: 60rpx; }
.model-bar { display: flex; align-items: center; justify-content: center; gap: 16rpx; margin-bottom: 20rpx; }
.model-bar-label { font-size: 24rpx; color: #64748b; }
.model-opts { display: flex; gap: 0; background: #f1f5f9; border-radius: 12rpx; overflow: hidden; }
.model-opt { font-size: 22rpx; padding: 10rpx 24rpx; color: #94a3b8; transition: all 0.15s; }
.model-opt.active { background: #2b6ff2; color: #fff; }
</style>
