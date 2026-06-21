<template>
  <view class="home">
    <!-- PC 端 Canvas 粒子背景 -->
    <ParticleBg />

    <!-- 小程序 + H5 通用 CSS 纹理层 -->
    <view class="bg-pattern" />

    <!-- Header -->
    <view class="header">
      <text class="brand">Mianmian.</text>
      <view class="header-avatar" @click="goProfile">
        <image v-if="userStore.avatarUrl" class="avatar-img" :src="userStore.avatarUrl" mode="aspectFill" />
        <text v-else class="avatar-letter">{{ (userStore.nickname || '?')[0] }}</text>
      </view>
    </view>

    <view class="announcement" v-if="announcement" @click="showAnnouncement = true">
      <text class="announcement-label">公告</text>
      <text class="announcement-title">{{ announcement.title }}</text>
      <text class="announcement-arrow">→</text>
    </view>

    <!-- Hero -->
    <view class="hero">
      <view class="hero-accent-line" />
      <text class="hero-label">AI 面试平台</text>
      <text class="hero-title">为真实环境
做好准备。</text>
      <text class="hero-sub">与硅谷标准对齐的 AI 技术面试，全流程语音对话与代码考察。</text>

      <!-- Dark Card -->
      <view class="hero-card" @click="goInterview">
        <image
          class="hero-card-bg"
          src="https://images.unsplash.com/photo-1550684848-fac1c5b4e853?auto=format&fit=crop&w=800&q=80"
          mode="aspectFill"
          @error="onHeroImgError"
        />
        <view v-if="heroImgFailed" class="hero-card-fallback" />
        <view class="hero-card-gradient" />
        <view class="hero-card-inner">
          <view class="hero-card-icon-box">
            <uni-icons type="compose" size="22" color="#fff" />
          </view>
          <view class="hero-card-spacer" />
          <view class="hero-card-row">
            <view class="hero-card-copy">
              <text class="hero-card-title">开始 AI 面试</text>
              <text class="hero-card-desc">语音流 · 代码编辑器 · 深度追问</text>
            </view>
            <view class="hero-card-circle">
              <uni-icons type="arrow-right" size="18" color="#141413" />
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 功能入口 — 两列布局带色彩点缀 -->
    <view class="func-grid">
      <view class="func-card func-card-accent" @click="goResume">
        <view class="func-card-top">
          <view class="func-icon-box icon-amber">
            <uni-icons type="paperclip" size="20" color="#D9750A" />
          </view>
          <text class="func-card-tag">推荐</text>
        </view>
        <text class="func-card-title">简历深度诊断</text>
        <text class="func-card-desc">上传 PDF，AI 定位项目薄弱点</text>
      </view>

      <view class="func-card" @click="goPaperTools">
        <view class="func-card-top">
          <view class="func-icon-box icon-amber">
            <uni-icons type="compose" size="20" color="#D9750A" />
          </view>
          <text class="func-card-tag">NEW</text>
        </view>
        <text class="func-card-title">文章助手</text>
        <text class="func-card-desc">润色 · 降AI · 降查重</text>
      </view>

      <view class="func-card" @click="goPractice">
        <view class="func-card-top">
          <view class="func-icon-box">
            <uni-icons type="calendar" size="20" color="#4A4A4A" />
          </view>
        </view>
        <text class="func-card-title">自由刷题</text>
        <text class="func-card-desc">随机组卷或按专题专项突破</text>
      </view>

      <view class="func-card" @click="goQuestionBank">
        <view class="func-card-top">
          <view class="func-icon-box">
            <uni-icons type="person" size="20" color="#4A4A4A" />
          </view>
        </view>
        <text class="func-card-title">查看题库</text>
        <text class="func-card-desc">分类浏览 · 逐题精学</text>
      </view>

      <view class="func-card" @click="goWrongBook">
        <view class="func-card-top">
          <view class="func-icon-box">
            <uni-icons type="bars" size="20" color="#4A4A4A" />
          </view>
        </view>
        <text class="func-card-title">错题本</text>
        <text class="func-card-desc">记录薄弱环节，反复巩固</text>
      </view>
    </view>

    <!-- Hot Topics -->
    <view class="section">
      <view class="section-head">
        <view class="section-head-line" />
        <text class="section-label">Hot Topics</text>
      </view>
      <view class="chip-list">
        <text class="chip chip-active">Redis 穿透</text>
        <text class="chip">MySQL 索引优化</text>
        <text class="chip">ConcurrentHashMap</text>
        <text class="chip">AQS 源码分析</text>
        <text class="chip">TCP 拥塞控制</text>
      </view>
    </view>

    <!-- 题目分类 -->
    <view class="section">
      <view class="section-head">
        <view class="section-head-line" />
        <text class="section-label">题目分类</text>
      </view>
      <view class="chip-list">
        <text class="chip" v-for="cat in categories" :key="cat.id" @click="goCategory(cat)">{{ cat.name }}</text>
      </view>
    </view>

    <view class="modal-mask" v-if="showAnnouncement && announcement" @click="dismissAnnouncement">
      <view class="modal-card" @click.stop>
        <text class="modal-title">{{ announcement.title }}</text>
        <rich-text class="modal-content" :nodes="announcementNodes" />
        <view class="modal-btn" @click="dismissAnnouncement">知道了</view>
      </view>
    </view>

    <view class="bottom-safe" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue';
import { get } from '@/utils/request';
import { useUserStore } from '@/store/user';
import ParticleBg from '@/components/ParticleBg.vue';

const userStore = useUserStore();
const heroImgFailed = ref(false);
const showAnnouncement = ref(false);
function onHeroImgError() { heroImgFailed.value = true; }

function normalizeAnnouncement(data: unknown): Announcement | null {
  const raw = (data || {}) as Record<string, unknown>;
  const nested = raw.data && typeof raw.data === 'object' ? raw.data as Record<string, unknown> : raw;
  const title = typeof nested.title === 'string' ? nested.title : '';
  const content = typeof nested.content === 'string' ? nested.content : '';
  if (!title && !content) return null;
  const idValue = nested.id;
  const id = typeof idValue === 'number' ? idValue : undefined;
  return { id, title: title || '平台公告', content };
}

function dismissAnnouncement() {
  if (announcement.value) {
    uni.setStorageSync('mp_announcement_' + (announcement.value.id || announcement.value.title), '1');
  }
  showAnnouncement.value = false;
}

const ANN_ROOT_STYLE = 'font-size:14px;line-height:1.52;color:#6f6a63;word-break:break-word;';
const ANN_P_STYLE = 'margin:0 0 9px 0;font-size:14px;line-height:1.52;color:#6f6a63;';
const ANN_H1_STYLE = 'margin:0 0 10px 0;font-size:18px;line-height:1.35;font-weight:700;color:#2f2c28;';
const ANN_H2_STYLE = 'margin:13px 0 7px 0;font-size:16px;line-height:1.35;font-weight:700;color:#3d3934;';
const ANN_H3_STYLE = 'margin:11px 0 6px 0;font-size:15px;line-height:1.35;font-weight:700;color:#4a4540;';
const ANN_UL_STYLE = 'margin:4px 0 9px 18px;padding:0;';
const ANN_LI_STYLE = 'margin:0 0 6px 0;font-size:14px;line-height:1.5;color:#6f6a63;';
const ANN_CODE_STYLE = 'font-size:13px;background:#f4efe8;border-radius:4px;padding:1px 4px;color:#3d3934;';
const ANN_PRE_STYLE = 'margin:6px 0 10px 0;padding:8px;background:#f4efe8;border-radius:8px;font-size:13px;line-height:1.45;color:#3d3934;white-space:pre-wrap;';

function renderAnnouncement(content: string): string {
  const source = content.trim();
  if (!source) return '';
  if (/<[a-z][\s\S]*>/i.test(source)) return normalizeRichHtml(source);

  const lines = escapeHtml(source).split(/\r?\n/);
  const blocks: string[] = [];
  let paragraph: string[] = [];
  let listItems: string[] = [];
  let codeLines: string[] = [];
  let inCode = false;

  const flushParagraph = () => {
    if (!paragraph.length) return;
    blocks.push(`<p style="${ANN_P_STYLE}">${renderInlineMarkdown(paragraph.join('<br/>'))}</p>`);
    paragraph = [];
  };
  const flushList = () => {
    if (!listItems.length) return;
    blocks.push(`<ul style="${ANN_UL_STYLE}">${listItems.join('')}</ul>`);
    listItems = [];
  };
  const flushCode = () => {
    if (!codeLines.length) return;
    blocks.push(`<pre style="${ANN_PRE_STYLE}"><code>${codeLines.join('<br/>')}</code></pre>`);
    codeLines = [];
  };

  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (line.startsWith('```')) {
      if (inCode) { flushCode(); inCode = false; }
      else { flushParagraph(); flushList(); inCode = true; }
      continue;
    }
    if (inCode) { codeLines.push(rawLine); continue; }
    if (!line) { flushParagraph(); flushList(); continue; }

    const heading = line.match(/^(#{1,3})\s+(.+)$/);
    if (heading) {
      flushParagraph();
      flushList();
      const level = heading[1].length;
      const style = level === 1 ? ANN_H1_STYLE : level === 2 ? ANN_H2_STYLE : ANN_H3_STYLE;
      blocks.push(`<h${level} style="${style}">${renderInlineMarkdown(heading[2])}</h${level}>`);
      continue;
    }

    const item = line.match(/^(?:[-*]|\d+\.)\s+(.+)$/);
    if (item) {
      flushParagraph();
      listItems.push(`<li style="${ANN_LI_STYLE}">${renderInlineMarkdown(item[1])}</li>`);
      continue;
    }

    flushList();
    paragraph.push(line);
  }

  flushParagraph();
  flushList();
  flushCode();
  return `<div style="${ANN_ROOT_STYLE}">${blocks.join('')}</div>`;
}

function renderInlineMarkdown(html: string): string {
  return html
    .replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1" style="max-width:100%;height:auto;margin:4px 0;"/>')
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" style="color:#d9750a;text-decoration:none;">$1</a>')
    .replace(/`([^`]+)`/g, `<code style="${ANN_CODE_STYLE}">$1</code>`)
    .replace(/\*\*(.+?)\*\*/g, '<strong style="font-weight:700;color:#4a4540;">$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>');
}

function normalizeRichHtml(value: string): string {
  const html = value.trim()
    .replace(/<h1[^>]*>/gi, `<h1 style="${ANN_H1_STYLE}">`)
    .replace(/<h2[^>]*>/gi, `<h2 style="${ANN_H2_STYLE}">`)
    .replace(/<h3[^>]*>/gi, `<h3 style="${ANN_H3_STYLE}">`)
    .replace(/<p[^>]*>/gi, `<p style="${ANN_P_STYLE}">`)
    .replace(/<ul[^>]*>/gi, `<ul style="${ANN_UL_STYLE}">`)
    .replace(/<li[^>]*>/gi, `<li style="${ANN_LI_STYLE}">`)
    .replace(/<pre[^>]*>/gi, `<pre style="${ANN_PRE_STYLE}">`)
    .replace(/<code[^>]*>/gi, `<code style="${ANN_CODE_STYLE}">`);
  return `<div style="${ANN_ROOT_STYLE}">${html}</div>`;
}
function escapeHtml(value: string): string {
  if (/<[a-z][\s\S]*>/i.test(value)) return value;
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

interface Category { id: number; name: string; icon: string; sortOrder: number; }
interface Announcement { id?: number; title: string; content: string; }
const categories = ref<Category[]>([]);
const announcement = ref<Announcement | null>(null);
const announcementNodes = computed(() => renderAnnouncement(announcement.value?.content || ''));

onMounted(async () => {
  try { const res = await get<Category[]>('/api/questions/categories'); categories.value = res.data || []; } catch {}
  try {
    const res = await get<Announcement>('/api/announcement/latest');
    const latest = normalizeAnnouncement(res.data);
    if (latest) {
      announcement.value = latest;
      const dismissKey = 'mp_announcement_' + (latest.id || latest.title);
      if (!uni.getStorageSync(dismissKey)) showAnnouncement.value = true;
    }
  } catch {}
});

function goCategory(c: Category) {
  if (!c) return;
  uni.switchTab({ url: '/pages/question-bank/index' });
}
function goQuestionBank() { uni.switchTab({ url: '/pages/question-bank/index' }); }
function goInterview() { uni.navigateTo({ url: '/pages/interview/chat' }); }
function goPractice() { uni.navigateTo({ url: '/pages/practice-entry/index' }); }
function goWrongBook() { uni.switchTab({ url: '/pages/wrong-book/wrong-book' }); }
function goResume() { uni.navigateTo({ url: '/pages/resume/upload' }); }
function goPaperTools() { uni.navigateTo({ url: '/pages/paper-tools/polish' }); }
function goProfile() { uni.switchTab({ url: '/pages/profile/profile' }); }
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

// ===== 基础 =====
.home {
  background: $bg-canvas;
  min-height: 100vh;
  position: relative;
}

// ===== CSS 纹理层（全平台） =====
.bg-pattern {
  position: fixed; inset: 0; pointer-events: none; z-index: 0;

  // 圆点图案 — 用径向渐变模拟
  background-image:
    radial-gradient(circle at 15% 20%, rgba(217,117,10,0.05) 0%, transparent 45%),
    radial-gradient(circle at 85% 75%, rgba(20,20,19,0.04) 0%, transparent 40%),
    radial-gradient(circle at 50% 50%, rgba(217,117,10,0.03) 0%, transparent 60%);
}

// ===== Header =====
.header {
  position: relative; z-index: 2;
  display: flex; justify-content: space-between; align-items: center;
  padding: 28rpx 32rpx 20rpx;
}
.brand {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 42rpx; font-weight: 600;
  letter-spacing: -1px; color: $text-main;
}
.header-avatar {
  width: 68rpx; height: 68rpx; border-radius: 50%;
  border: 1.5px solid $border-medium;
  display: flex; align-items: center; justify-content: center;
  background: $bg-paper; overflow: hidden;
}
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-letter {
  font-family: Georgia, serif;
  font-size: 30rpx; font-weight: 600; color: $text-main;
}

.announcement {
  position: relative; z-index: 2;
  margin: 0 32rpx 20rpx; padding: 16rpx 20rpx;
  background: rgba(217,117,10,0.08); border: 1px solid rgba(217,117,10,0.16);
  border-radius: $radius-md; display: flex; align-items: center; gap: 14rpx;
}
.announcement-label {
  flex-shrink: 0; font-size: 20rpx; font-weight: 700; color: #fff;
  background: $accent; padding: 4rpx 10rpx; border-radius: $radius-full;
}
.announcement-title {
  flex: 1; min-width: 0; font-size: 24rpx; color: $text-main; font-weight: 600;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.announcement-arrow { flex-shrink: 0; font-size: 24rpx; color: $accent; }
.modal-mask { position: fixed; inset: 0; z-index: 1000; background: rgba(0,0,0,0.42); display: flex; align-items: center; justify-content: center; padding: 40rpx; }
.modal-card { width: 100%; max-width: 620rpx; max-height: 76vh; overflow: auto; background: $bg-paper; border-radius: $radius-xl; padding: 40rpx 34rpx; box-sizing: border-box; }
.modal-title { font-size: 32rpx; font-weight: 700; color: $text-main; display: block; margin-bottom: 18rpx; }
.modal-content { font-size: 26rpx; color: $text-muted; line-height: 1.5; display: block; overflow-wrap: break-word; }
.modal-btn { margin-top: 32rpx; height: 78rpx; background: $bg-dark; color: #fff; border-radius: $radius-lg; display: flex; align-items: center; justify-content: center; font-size: 26rpx; font-weight: 600; }

// ===== Hero =====
.hero {
  position: relative; z-index: 2;
  padding: 16rpx 32rpx 36rpx;
}
.hero-accent-line {
  width: 48rpx; height: 6rpx;
  background: $accent; border-radius: 3rpx; margin-bottom: 20rpx;
}
.hero-label {
  font-size: 22rpx; font-weight: 600; color: $accent;
  letter-spacing: 4rpx; text-transform: uppercase;
  display: block; margin-bottom: 14rpx;
}
.hero-title {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 58rpx; line-height: 1.18; color: $text-main;
  letter-spacing: -1.5px; margin-bottom: 18rpx;
  white-space: pre-line; display: block;
}
.hero-sub {
  font-size: 26rpx; color: $text-muted; line-height: 1.7;
  margin-bottom: 36rpx; display: block; max-width: 520rpx;
}

// ===== Dark Hero Card =====
.hero-card {
  position: relative; border-radius: 20px; overflow: hidden;
  box-shadow:
    0 8px 32px rgba(20,20,19,0.12),
    0 2px 8px rgba(20,20,19,0.06);
  height: 340rpx;
}
.hero-card-bg {
  position: absolute; inset: 0; width: 100%; height: 100%;
  opacity: 0.35; object-fit: cover;
}
.hero-card-fallback {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 50%, #1a1a1a 100%);
}
.hero-card-gradient {
  position: absolute; inset: 0;
  background: linear-gradient(180deg, rgba(20,20,19,0.15) 0%, rgba(20,20,19,0.93) 100%);
}
.hero-card-inner {
  position: relative; z-index: 1; height: 100%; box-sizing: border-box;
  padding: 36rpx; display: flex; flex-direction: column;
}
.hero-card-icon-box {
  width: 72rpx; height: 72rpx;
  background: rgba(255,255,255,0.08);
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 18rpx;
  display: flex; align-items: center; justify-content: center;
}
.hero-card-spacer { flex: 1; }
.hero-card-row {
  display: flex; justify-content: space-between; align-items: flex-end; gap: 22rpx;
}
.hero-card-copy { flex: 1; min-width: 0; padding-right: 8rpx; }
.hero-card-title {
  font-size: 38rpx; line-height: 1.25; font-weight: 600; color: #fff;
  display: block; margin-bottom: 8rpx; letter-spacing: 0;
  white-space: nowrap; overflow: visible;
}
.hero-card-desc {
  font-size: 22rpx; color: rgba(255,255,255,0.58); display: block; line-height: 1.4;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.hero-card-circle {
  width: 58rpx; height: 58rpx; background: #fff; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

// ===== 功能卡片网格 =====
.func-grid {
  position: relative; z-index: 2;
  display: grid; grid-template-columns: 1fr 1fr; gap: 16rpx;
  padding: 0 32rpx; margin-bottom: 44rpx;
}
.func-card {
  background: $bg-paper;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: 28rpx 24rpx 26rpx;
  box-shadow: $shadow-sm;
  display: flex; flex-direction: column;
  transition: all 0.15s;
  position: relative; overflow: hidden;
}
// 第一个卡片加琥珀色顶部细线
.func-card-accent::before {
  content: ''; position: absolute; top: 0; left: 0; right: 0; height: 4rpx;
  background: $accent; border-radius: 0 0 2rpx 2rpx;
}
.func-card:active { background: $bg-surface; border-color: $border-medium; }
.func-card-top {
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 18rpx;
}
.func-icon-box {
  width: 60rpx; height: 60rpx;
  background: $bg-surface; border-radius: 14rpx;
  display: flex; align-items: center; justify-content: center;
}
// 推荐卡片的图标用琥珀底色
.icon-amber { background: rgba(217,117,10,0.08); }
.func-card-tag {
  font-size: 20rpx; font-weight: 600; color: $accent;
  background: rgba(217,117,10,0.08);
  padding: 4rpx 14rpx; border-radius: $radius-full;
}
.func-card-title {
  font-size: 28rpx; font-weight: 500; color: $text-main;
  margin-bottom: 6rpx;
}
.func-card-desc {
  font-size: 22rpx; color: $text-light; line-height: 1.5;
}

// ===== 标签区 =====
.section {
  position: relative; z-index: 2;
  padding: 0 32rpx; margin-bottom: 36rpx;
}
.section-head {
  display: flex; align-items: center; gap: 14rpx; margin-bottom: 18rpx;
}
.section-head-line {
  width: 6rpx; height: 24rpx; background: $text-main; border-radius: 3rpx;
}
.section-label {
  font-size: 24rpx; font-weight: 600; color: $text-main;
  letter-spacing: 0.5rpx;
}
.chip-list { display: flex; flex-wrap: wrap; gap: 12rpx; }
.chip {
  border: 1px solid $border-light;
  padding: 12rpx 26rpx; border-radius: $radius-full;
  font-size: 26rpx; color: $text-main; background: $bg-paper;
  transition: all 0.15s;
}
.chip-active {
  background: $bg-dark; color: #fff; border-color: $bg-dark;
}
.chip:active { background: $bg-surface; border-color: $text-main; }

.bottom-safe { height: 80rpx; }

// ===== PC 端 =====
@media (min-width: 1025px) {
  .home { display: flex; flex-direction: column; align-items: center; }
  .header, .hero, .func-grid, .section {
    width: 100%; max-width: 820px; padding-left: 0; padding-right: 0;
  }
  .hero-card { height: 380rpx; }
  .hero-title { font-size: 64rpx; }
  .func-grid { grid-template-columns: 1fr 1fr; gap: 20rpx; }
}
</style>
