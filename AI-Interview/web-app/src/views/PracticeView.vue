<template>
  <div class="page">
    <div class="page__inner">
      <div class="page-head">
        <button class="back-btn" @click="$router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>
        <span class="page-head__title">自由刷题</span>
        <div style="width:36px" />
      </div>

      <div class="hero-banner">
        <span class="hero-banner__label">自由练</span>
        <h1 class="hero-banner__title">自由刷题</h1>
        <p class="hero-banner__desc">随机组卷或按专题专项突破</p>
      </div>

      <!-- Entry 1: Random -->
      <div class="entry-card">
        <div class="entry-card__icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#D9750A" stroke-width="1.5">
            <rect x="2" y="2" width="20" height="20" rx="4"/>
            <circle cx="8.5" cy="8.5" r="1.5" fill="#D9750A"/>
            <circle cx="15.5" cy="8.5" r="1.5" fill="#D9750A"/>
            <circle cx="8.5" cy="15.5" r="1.5" fill="#D9750A"/>
            <circle cx="15.5" cy="15.5" r="1.5" fill="#D9750A"/>
          </svg>
        </div>
        <span class="entry-card__title">随机组卷</span>
        <span class="entry-card__desc">从题库随机抽取，模拟真实考试</span>
        <div class="count-row">
          <button
            v-for="n in [5, 10, 15, 20]"
            :key="n"
            class="count-chip"
            :class="{ active: randomCount === n }"
            @click="randomCount = n"
          >{{ n }}</button>
          <button class="count-go" @click="startRandom">开始</button>
        </div>
      </div>

      <!-- Entry 2: Topic -->
      <div class="entry-card">
        <div class="entry-card__icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#D9750A" stroke-width="1.5">
            <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
          </svg>
        </div>
        <span class="entry-card__title">按专题刷题</span>
        <span class="entry-card__desc">选择分类，集中突破薄弱环节</span>
        <div class="chip-row no-scrollbar">
          <TopicChip
            v-for="cat in categories"
            :key="cat.id"
            :active="selectedCategory === cat.id"
            @click="selectedCategory = cat.id; startTopic(cat)"
          >
            {{ cat.name }}
          </TopicChip>
        </div>
      </div>

      <!-- Recent history -->
      <div class="section-spacer">
        <span class="section-label">最近记录</span>
      </div>
      <div class="hist-list" v-if="history.length > 0">
        <div class="hist-item" v-for="h in history" :key="h.id">
          <div class="hist-info">
            <span class="hist-name">{{ h.modeLabel }} · {{ h.questionCount }}题</span>
            <span class="hist-time">正确 {{ h.correctCount }} 题</span>
          </div>
          <span class="hist-badge" :class="h.correctCount / h.questionCount >= 0.7 ? 'badge-high' : 'badge-mid'">
            {{ Math.round(h.correctCount / h.questionCount * 100) }}%
          </span>
        </div>
      </div>
      <div class="empty" v-else>
        <span class="empty__title">暂无刷题记录</span>
        <p class="empty__desc">选择上方模式开始刷题</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { get } from '@/utils/request'
import TopicChip from '@/components/TopicChip.vue'

interface Category { id: number; name: string }
interface HistoryItem {
  id: number; modeLabel: string; questionCount: number; correctCount: number;
}

const router = useRouter()
const randomCount = ref(10)
const selectedCategory = ref<number | null>(null)
const categories = ref<Category[]>([])
const history = ref<HistoryItem[]>([])

function startRandom() {
  router.push(`/practice/do?mode=random&count=${randomCount.value}`)
}

function startTopic(cat: Category) {
  router.push(`/practice/do?mode=topic&categoryId=${cat.id}&categoryName=${encodeURIComponent(cat.name)}`)
}

onMounted(async () => {
  try {
    const res = await get<Category[]>('/api/questions/categories')
    categories.value = res.data || []
  } catch { /* ignore */ }
})
</script>

<style scoped>
.page { min-height: 100vh; background: var(--bg-canvas); }
.page__inner { max-width: 700px; margin: 0 auto; padding: 0 20px; }

.page-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 0 24px;
}
.back-btn {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-main);
  background: none; border: none; cursor: pointer;
}
.page-head__title {
  font-family: var(--font-serif); font-size: 18px; font-weight: 600;
}

.hero-banner { margin-bottom: 24px; }
.hero-banner__label {
  font-size: 12px; font-weight: 600; color: var(--accent);
  letter-spacing: 3px; text-transform: uppercase; display: block; margin-bottom: 8px;
}
.hero-banner__title {
  font-family: var(--font-serif); font-size: 32px; font-weight: 600;
  letter-spacing: -1px; margin-bottom: 8px;
}
.hero-banner__desc { font-size: 14px; color: var(--text-light); }

/* Entry cards */
.entry-card {
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  padding: 24px 20px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 16px;
}
.entry-card__icon {
  width: 44px; height: 44px; border-radius: 12px;
  background: rgba(217,117,10,0.06);
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 14px;
}
.entry-card__title { font-size: 17px; font-weight: 500; display: block; margin-bottom: 4px; }
.entry-card__desc { font-size: 13px; color: var(--text-light); display: block; margin-bottom: 16px; }

/* Count selector */
.count-row { display: flex; gap: 8px; align-items: center; }
.count-chip {
  width: 40px; height: 40px; border-radius: 10px;
  border: 1px solid var(--border-light);
  background: var(--bg-paper);
  font-size: 14px; font-weight: 500; color: var(--text-main);
  cursor: pointer;
}
.count-chip.active {
  border-color: var(--accent);
  background: rgba(217,117,10,0.06);
  color: var(--accent); font-weight: 600;
}
.count-go {
  margin-left: auto; height: 40px; padding: 0 24px;
  background: var(--bg-dark); color: #fff;
  border: none; border-radius: 10px;
  font-size: 14px; font-weight: 500; cursor: pointer;
}

/* Chips */
.chip-row { display: flex; gap: 8px; flex-wrap: wrap; }

/* Section */
.section-spacer { margin: 28px 0 12px; }
.section-label {
  font-size: 11px; font-weight: 600; color: var(--text-light);
  text-transform: uppercase; letter-spacing: 1px;
}

/* History */
.hist-list { display: flex; flex-direction: column; gap: 8px; }
.hist-item {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px;
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
}
.hist-info { flex: 1; min-width: 0; }
.hist-name { font-size: 14px; font-weight: 500; display: block; }
.hist-time { font-size: 12px; color: var(--text-light); }
.hist-badge {
  font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: var(--radius-full);
}
.badge-high { background: #F0FDF4; color: #16A34A; }
.badge-mid { background: #FFFBEB; color: #D97706; }

.empty { text-align: center; padding-top: 40px; }
.empty__title { font-size: 15px; font-weight: 500; display: block; margin-bottom: 6px; }
.empty__desc { font-size: 13px; color: var(--text-light); }
</style>
