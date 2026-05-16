<template>
  <div class="page">
    <div class="page__inner">
      <div class="page-head">
        <button class="back-btn" @click="$router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <span class="page-head__title">简历记录</span>
        <div style="width:36px" />
      </div>

      <ScrollReveal :stagger="0.08" :y="20" :duration="0.6" v-if="list.length > 0">
        <div class="list">
          <div
            class="history-item card-hover"
            v-for="(item, i) in list"
            :key="item.id"
            @click="$router.push(`/resume/report?resumeId=${item.id}`)"
          >
            <div class="history-item__top">
              <div class="history-item__icon">
                <Folder
                  :color="folderColors[i % folderColors.length]"
                  :size="0.75"
                  :items="[item.position || '简历']"
                />
              </div>
              <div class="history-item__info">
                <span class="history-item__title">{{ getDisplayName(item) }}</span>
                <span class="history-item__time">{{ formatTime(item.createTime) }}</span>
              </div>
              <span class="history-item__status" :class="statusClass(item.parseStatus)">
                {{ statusLabel(item.parseStatus) }}
              </span>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#CCC" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
            </div>
            <div class="history-item__meta" v-if="item.parseStatus === 1 && item.overallScore != null">
              <span class="history-item__score">综合评分 {{ item.overallScore }} 分</span>
            </div>
          </div>
        </div>
      </ScrollReveal>

      <div class="loading" v-if="loading">
        <SkeletonBar :lines="3" :widths="['90%', '80%', '60%']" height="14px" gap="16px" />
      </div>

      <div class="empty" v-if="!loading && list.length === 0">
        <span class="empty__icon"><svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="var(--text-light)" stroke-width="1.2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg></span>
        <span class="empty__title">暂无简历记录</span>
        <p class="empty__desc">上传简历后，AI 会为你生成深度诊断报告</p>
        <button class="empty__btn" @click="$router.push('/resume/upload')">上传第一份简历</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { get } from '@/utils/request'
import ScrollReveal from '@/components/ScrollReveal.vue'
import SkeletonBar from '@/components/SkeletonBar.vue'
import Folder from '@/components/Folder.vue'

interface ResumeItem {
  id: number
  fileName: string
  position?: string
  parseStatus: number
  overallScore?: number
  createTime: string
}

function getDisplayName(item: ResumeItem): string {
  return item.position || item.fileName || '未命名'
}

function getDisplayScore(item: ResumeItem): string {
  if (item.overallScore != null) return `${item.overallScore} 分`
  return ''
}

let cachedList: ResumeItem[] | null = null

const list = ref<ResumeItem[]>(cachedList || [])
const loading = ref(!cachedList)
const folderColors = ['#D9750A', '#4A90D9', '#16A34A', '#9333EA', '#E11D48']

async function fetchList() {
  try {
    const r = await get<ResumeItem[]>('/api/resume/list')
    list.value = r.data || []
    cachedList = list.value
  } catch {}
  loading.value = false
}

onMounted(() => {
  if (cachedList) {
    list.value = cachedList
    loading.value = false
  }
  fetchList()
})

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

function statusClass(s: number) {
  if (s === 1) return 'status--done'
  if (s === -1) return 'status--fail'
  return 'status--pending'
}

function statusLabel(s: number) {
  if (s === 1) return '解析完成'
  if (s === -1) return '解析失败'
  return '解析中'
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
  font-family: var(--font-serif); font-size: 22px; font-weight: 600;
}

.list { display: flex; flex-direction: column; gap: 10px; }

.history-item {
  background: var(--bg-paper); border: 1px solid var(--border-light);
  border-radius: var(--radius-lg); padding: 20px; box-shadow: var(--shadow-sm);
  cursor: pointer;
}
.history-item__top {
  display: flex; align-items: center; gap: 12px;
}
.history-item__icon {
  width: 48px; height: 40px; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.history-item__info { flex: 1; min-width: 0; }
.history-item__title {
  font-size: 15px; font-weight: 500; color: var(--text-main); display: block;
  margin-bottom: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.history-item__time {
  font-size: 12px; color: var(--text-light);
}
.history-item__status {
  font-size: 11px; font-weight: 600; padding: 4px 10px; border-radius: 100px;
  flex-shrink: 0;
}
.status--done { background: rgba(34,197,94,0.08); color: var(--color-success); }
.status--fail { background: rgba(239,68,68,0.08); color: var(--color-danger); }
.status--pending { background: rgba(217,117,10,0.08); color: var(--accent); }

.history-item__meta { margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--border-light); }
.history-item__score {
  font-size: 13px; color: var(--accent); font-weight: 500;
}

.loading { padding-top: 20px; }

.empty { text-align: center; padding-top: 160px; }
.empty__icon { display: flex; justify-content: center; margin-bottom: 16px; }
.empty__icon svg { display: block; }
.empty__title { font-size: 16px; font-weight: 500; display: block; margin-bottom: 8px; }
.empty__desc { font-size: 13px; color: var(--text-light); margin-bottom: 24px; }
.empty__btn {
  background: var(--bg-dark); color: #fff; padding: 12px 32px;
  border-radius: var(--radius-lg); font-size: 15px; font-weight: 500;
  border: none; cursor: pointer;
}
</style>
