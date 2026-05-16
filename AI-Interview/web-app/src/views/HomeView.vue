<template>
  <div class="home">
    <ParticleBg :particleCount="180" :particleSpread="12" :speed="0.08" :alphaParticles="true" :particleBaseSize="80"
      :sizeRandomness="0.8" :moveOnHover="true" :hoverFactor="0.6" :disableRotation="false" />

    <div class="home__inner">
      <!-- Header -->
      <header class="header">
        <span class="brand">Mianmian.</span>
        <div class="header__avatar" @click="$router.push('/profile')">
          <img v-if="userStore.avatarUrl" class="avatar-img" :src="userStore.avatarUrl" alt="" />
          <span v-else class="avatar-letter">{{ (userStore.nickname || '?')[0] }}</span>
        </div>
      </header>

      <!-- Hero -->
      <section class="hero">
        <div class="hero__accent" />
        <ScrollReveal :stagger="0" :y="20">
          <span class="hero__label">AI 面试平台</span>
        </ScrollReveal>
        <ScrollReveal :stagger="0" :y="30" :duration="0.8">
          <h1 class="hero__title">
            <span class="hero__title-line">
              <DecryptedText text="为真实环境" :speed="80" :maxIterations="8" :delay="500" sequential
                revealDirection="start" />
            </span>
            <span class="hero__title-line">
              <DecryptedText text="做好准备。" :speed="100" :maxIterations="8" :delay="1200" sequential
                revealDirection="start" :characters="'!@#$%^&*<>?|[]{}'" />
            </span>
          </h1>
        </ScrollReveal>
        <ScrollReveal :stagger="0" :y="20" :delay="0.3">
          <p class="hero__sub">与硅谷标准对齐的 AI 技术面试，全流程语音对话与代码考察。</p>
        </ScrollReveal>

        <GlareCard background="#141413" borderRadius="20px" glareColor="#ffffff" :glareOpacity="0.15" :glareSize="280"
          @click="$router.push('/interview')">
          <div class="hero-card">
            <img class="hero-card__bg"
              src="https://images.unsplash.com/photo-1550684848-fac1c5b4e853?auto=format&fit=crop&w=800&q=80" alt="" />
            <div class="hero-card__gradient" />
            <div class="hero-card__inner">
              <div class="hero-card__icon-box">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#FFF" stroke-width="1.5">
                  <path
                    d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48l2.83-2.83" />
                </svg>
              </div>
              <div class="hero-card__spacer" />
              <div class="hero-card__row">
                <div>
                  <span class="hero-card__title">开始 AI 面试</span>
                  <span class="hero-card__desc">语音流 · 代码编辑器 · 深度追问</span>
                </div>
                <div class="hero-card__circle">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <polyline points="12 5 19 12 12 19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                </div>
              </div>
            </div>
          </div>
        </GlareCard>
      </section>

      <!-- Feature Cards Grid -->
      <ScrollReveal :stagger="0.1" :y="30" :duration="0.7">
        <section class="func-grid">
          <PixelCard :gap="10" :dotRadius="1.0" colors="#fef08a,#fde047,#eab308" :opacityMin="0.15" :opacityMax="0.5"
            className="func-card func-card--accent" @click="$router.push('/resume/upload')">
            <div class="func-card__top">
              <div class="func-icon func-icon--amber">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#D9750A" stroke-width="1.5">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                  <polyline points="14 2 14 8 20 8" />
                </svg>
              </div>
              <span class="func-card__tag">推荐</span>
            </div>
            <span class="func-card__title">简历深度诊断</span>
            <span class="func-card__desc">上传 PDF，AI 定位项目薄弱点</span>
          </PixelCard>

          <PixelCard :gap="10" :dotRadius="1.0" colors="#e0f2fe,#7dd3fc,#0ea5e9" :opacityMin="0.15" :opacityMax="0.5"
            className="func-card" @click="$router.push('/exam')">
            <div class="func-card__top">
              <div class="func-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#4A4A4A" stroke-width="1.5">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                  <line x1="3" y1="9" x2="21" y2="9" />
                  <line x1="9" y1="21" x2="9" y2="9" />
                </svg>
              </div>
            </div>
            <span class="func-card__title">在线试卷</span>
            <span class="func-card__desc">限时模拟考试，查漏补缺</span>
          </PixelCard>

          <PixelCard :gap="10" :dotRadius="1.0" colors="#f8fafc,#f1f5f9,#cbd5e1" :opacityMin="0.12" :opacityMax="0.4"
            className="func-card" @click="$router.push('/practice')">
            <div class="func-card__top">
              <div class="func-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#4A4A4A" stroke-width="1.5">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                  <circle cx="9" cy="7" r="4" />
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                  <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                </svg>
              </div>
            </div>
            <span class="func-card__title">自由刷题</span>
            <span class="func-card__desc">按分类随机练习</span>
          </PixelCard>

          <PixelCard :gap="10" :dotRadius="1.0" colors="#fecdd3,#fda4af,#e11d48" :opacityMin="0.15" :opacityMax="0.5"
            className="func-card" @click="$router.push('/wrong-book')">
            <div class="func-card__top">
              <div class="func-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#4A4A4A" stroke-width="1.5">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                  <polyline points="14 2 14 8 20 8" />
                  <line x1="16" y1="13" x2="8" y2="13" />
                  <line x1="16" y1="17" x2="8" y2="17" />
                </svg>
              </div>
            </div>
            <span class="func-card__title">错题本</span>
            <span class="func-card__desc">记录薄弱环节，反复巩固</span>
          </PixelCard>
        </section>
      </ScrollReveal>

      <!-- Hot Topics -->
      <ScrollReveal :stagger="0.06" :y="20" :duration="0.6">
        <section class="section">
          <div class="section-head">
            <div class="section-head__line" />
            <span class="section-head__label">Hot Topics</span>
          </div>
          <div class="chip-list">
            <TopicChip v-for="(topic, i) in hotTopics" :key="topic" :active="i === 0" @click="goTopic(topic)">
              {{ topic }}
            </TopicChip>
          </div>
        </section>
      </ScrollReveal>

      <!-- Categories -->
      <ScrollReveal :stagger="0.06" :y="20" :duration="0.6">
        <section class="section" v-if="categories.length > 0">
          <div class="section-head">
            <div class="section-head__line" />
            <span class="section-head__label">题目分类</span>
          </div>
          <div class="chip-list">
            <TopicChip v-for="cat in categories" :key="cat.id" @click="goCategory(cat)">
              {{ cat.name }}
            </TopicChip>
          </div>
        </section>
      </ScrollReveal>

      <div class="bottom-safe" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { get } from '@/utils/request'
import ParticleBg from '@/components/ParticleBg.vue'
import PixelCard from '@/components/PixelCard.vue'
import GlareCard from '@/components/GlareCard.vue'
import TopicChip from '@/components/TopicChip.vue'
import ScrollReveal from '@/components/ScrollReveal.vue'
import DecryptedText from '@/components/DecryptedText.vue'

interface Category { id: number; name: string; icon?: string; sortOrder: number }

const router = useRouter()
const userStore = useUserStore()

const hotTopics = ['Redis 穿透', 'MySQL 索引优化', 'ConcurrentHashMap', 'AQS 源码分析', 'TCP 拥塞控制']
const categories = ref<Category[]>([])

onMounted(async () => {
  try {
    const res = await get<Category[]>('/api/questions/categories')
    categories.value = res.data || []
  } catch { /* ignore */ }
})

function goTopic(t: string) {
  router.push(`/practice?tag=${encodeURIComponent(t)}`)
}
function goCategory(c: Category) {
  router.push(`/practice?categoryId=${c.id}&categoryName=${encodeURIComponent(c.name)}`)
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: var(--bg-canvas);
  position: relative;
  overflow: hidden;
}

.bg-pattern {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  background-image:
    radial-gradient(circle at 15% 20%, rgba(217, 117, 10, 0.06) 0%, transparent 45%),
    radial-gradient(circle at 85% 75%, rgba(20, 20, 19, 0.05) 0%, transparent 40%),
    radial-gradient(circle at 50% 50%, rgba(217, 117, 10, 0.04) 0%, transparent 60%);
}

.home__inner {
  position: relative;
  z-index: 1;
  max-width: 820px;
  margin: 0 auto;
  padding: 0 20px;
}

/* Header */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 0 16px;
}

.brand {
  font-family: var(--font-serif);
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.5px;
  color: var(--text-main);
}

.header__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1.5px solid var(--border-medium);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-paper);
  overflow: hidden;
  cursor: pointer;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-letter {
  font-family: var(--font-serif);
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
}

/* Hero */
.hero {
  padding: 12px 0 32px;
}

.hero__accent {
  width: 32px;
  height: 4px;
  background: var(--accent);
  border-radius: 2px;
  margin-bottom: 14px;
}

.hero__label {
  font-size: 12px;
  font-weight: 600;
  color: var(--accent);
  letter-spacing: 3px;
  text-transform: uppercase;
  display: block;
  margin-bottom: 10px;
}

.hero__title {
  font-family: var(--font-serif);
  font-size: 40px;
  line-height: 1.2;
  color: var(--text-main);
  letter-spacing: -1.5px;
  margin-bottom: 14px;
  font-weight: 600;
  display: flex;
  flex-direction: column;
}

.hero__title-line {
  display: block;
}

.hero__sub {
  font-size: 15px;
  color: var(--text-muted);
  line-height: 1.7;
  margin-bottom: 28px;
  max-width: 440px;
}

/* Dark Card */
.hero-card {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  height: 300px;
  cursor: pointer;
}

.hero-card__bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0.38;
  object-fit: cover;
  mix-blend-mode: luminosity;
}

.hero-card__gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(20, 20, 19, 0.15) 0%, rgba(20, 20, 19, 0.93) 100%);
}

.hero-card__inner {
  position: relative;
  z-index: 1;
  height: 100%;
  padding: 28px;
  display: flex;
  flex-direction: column;
}

.hero-card__icon-box {
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.hero-card__spacer {
  flex: 1;
}

.hero-card__row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.hero-card__title {
  display: block;
  font-size: 22px;
  font-weight: 500;
  color: #fff;
  margin-bottom: 6px;
  letter-spacing: -0.5px;
}

.hero-card__desc {
  display: block;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
}

.hero-card__circle {
  width: 34px;
  height: 34px;
  background: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #141413;
}

/* Feature grid */
.func-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 38px;
}

.func-grid :deep(.pixel-card) {
  display: block;
  transition: transform 0.3s var(--ease-out-expo), box-shadow 0.3s var(--ease-out-expo);
}

.func-grid :deep(.pixel-card:hover) {
  transform: scale(1.03);
  box-shadow: var(--shadow-lg);
}

.func-grid :deep(.pixel-card:active) {
  transform: scale(0.98);
}

.func-card {
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 22px 18px 20px;
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.func-card--accent::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--accent);
  border-radius: 0 0 2px 2px;
}

.func-card:active {
  background: var(--bg-surface);
  border-color: var(--border-medium);
}

.func-card__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
}

.func-icon {
  width: 44px;
  height: 44px;
  background: var(--bg-surface);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.func-icon--amber {
  background: rgba(217, 117, 10, 0.08);
}

.func-card__tag {
  font-size: 11px;
  font-weight: 600;
  color: var(--accent);
  background: rgba(217, 117, 10, 0.08);
  padding: 3px 10px;
  border-radius: var(--radius-full);
}

.func-card__title {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-main);
  margin-bottom: 4px;
}

.func-card__desc {
  font-size: 12px;
  color: var(--text-light);
  line-height: 1.5;
}

/* Sections */
.section {
  margin-bottom: 32px;
}

.section-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.section-head__line {
  width: 5px;
  height: 18px;
  background: var(--text-main);
  border-radius: 3px;
}

.section-head__label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  letter-spacing: 0.3px;
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.bottom-safe {
  height: 60px;
}

/* PC */
@media (min-width: 1025px) {
  .hero__title {
    font-size: 52px;
    max-width: 540px;
  }

  .hero-card {
    height: 340px;
  }

  .func-grid {
    gap: 18px;
  }
}
</style>
