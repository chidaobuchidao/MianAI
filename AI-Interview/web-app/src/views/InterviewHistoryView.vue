<template>
  <div class="page">
    <div class="page__inner">
      <div class="page-head">
        <button class="back-btn" @click="$router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <span class="page-head__title">面试历史</span>
        <div style="width:36px" />
      </div>

      <ScrollReveal :stagger="0.08" :y="20" :duration="0.6" v-if="list.length > 0">
        <div class="list">
          <div class="list-item card-hover" v-for="item in list" :key="item.id" @click="$router.push(`/interview/report?id=${item.id}`)">
            <div class="list-item__left">
              <span class="list-item__title">{{ item.position }}</span>
              <span class="list-item__time">{{ formatTime(item.createTime) }}</span>
            </div>
            <div class="list-item__right">
              <span class="list-item__status" v-if="item.status === 0" style="background:rgba(217,117,10,0.08);color:var(--accent);">进行中</span>
              <span class="list-item__badge" v-else-if="item.overallScore != null" :class="(item.overallScore || 0) >= 7 ? 'badge--high' : 'badge--mid'">{{ item.overallScore }}分</span>
              <span class="list-item__badge" v-else style="background:var(--bg-surface);color:var(--text-light);">未评分</span>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#CCC" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
            </div>
          </div>
        </div>
      </ScrollReveal>

      <div class="loading" v-if="loading">
        <SkeletonBar :lines="3" :widths="['90%', '80%', '60%']" height="14px" gap="16px" />
      </div>

      <div class="empty" v-if="!loading && list.length === 0">
        <span class="empty__icon"><svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="var(--text-light)" stroke-width="1.2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg></span>
        <span class="empty__title">暂无面试记录</span>
        <button class="empty__btn" @click="$router.push('/interview')">开始第一场面试</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { get } from '@/utils/request'
import ScrollReveal from '@/components/ScrollReveal.vue'
import SkeletonBar from '@/components/SkeletonBar.vue'

interface HistItem { id: number; position: string; createTime: string; overallScore: number | null; status: number }

const list = ref<HistItem[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await get<HistItem[]>('/api/interview/list')
    list.value = (res.data || []).map(item => ({
      ...item,
      position: item.position || '未命名岗位'
    }))
  } catch { /* empty */ }
  loading.value = false
})

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}
</script>

<style scoped>
.page { min-height: 100vh; background: var(--bg-canvas); }
.page__inner { max-width: 640px; margin: 0 auto; padding: 0 20px; }

.page-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 0 24px;
}
.back-btn {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-main);
}
.page-head__title {
  font-family: var(--font-serif);
  font-size: 18px; font-weight: 600;
}

.list { display: flex; flex-direction: column; gap: 10px; }
.list-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px;
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
}
.list-item__left { }
.list-item__title { font-size: 15px; font-weight: 500; display: block; margin-bottom: 2px; }
.list-item__time { font-size: 12px; color: var(--text-light); }
.list-item__right { display: flex; align-items: center; gap: 10px; }
.list-item__status {
  font-size: 11px; font-weight: 600; padding: 4px 10px; border-radius: 100px;
}
.list-item__badge {
  font-size: 12px; font-weight: 600; padding: 3px 10px; border-radius: var(--radius-full);
}
.badge--high { background: #F0FDF4; color: #16A34A; }
.badge--mid { background: #FFFBEB; color: #D97706; }

.loading { padding-top: 20px; }
.empty {
  text-align: center; padding-top: 160px;
}
.empty__icon { display: flex; justify-content: center; margin-bottom: 16px; }
.empty__icon svg { display: block; }
.empty__title { font-size: 16px; color: var(--text-light); display: block; margin-bottom: 24px; }
.empty__btn {
  background: var(--bg-dark); color: #fff; padding: 12px 32px;
  border-radius: var(--radius-lg); font-size: 15px; font-weight: 500;
  border: none; cursor: pointer;
}
</style>
