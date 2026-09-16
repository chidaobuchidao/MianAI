<script setup lang="ts">
import { computed, ref, useId, watch } from 'vue'
import { ArrowUpRight, RefreshCw, Zap } from 'lucide-vue-next'
import TokenUsageTrend from '@/components/TokenUsageTrend.vue'
import { useUserStore } from '@/stores/user'
import { useTokenUsage } from '@/modules/token-usage/useTokenUsage'
import { formatNumber, type UsageDays } from '@/modules/token-usage/types'

const userStore = useUserStore()
const id = useId()
const days = ref<UsageDays>(7)
const scope = ref<'personal' | 'system'>('personal')
const system = computed(() => userStore.isAdmin && scope.value === 'system')
const showTrend = ref(false)
const { data, loading, error, load } = useTokenUsage()
const refresh = () => load(system.value, { days: days.value })
watch(() => userStore.isAdmin, () => { scope.value = 'personal' })
watch([days, system, () => userStore.userId], refresh, { immediate: true })
</script>

<template>
  <section id="token-usage" class="profile-usage" :aria-labelledby="`${id}-title`">
    <header><h2 :id="`${id}-title`"><Zap :size="18" aria-hidden="true" />Token 用量</h2><button class="refresh" :disabled="loading" aria-label="刷新 Token 用量" @click="refresh"><RefreshCw :size="16" aria-hidden="true" /></button></header>
    <div class="controls">
      <div v-if="userStore.isAdmin" class="scope" role="group" aria-label="用量范围"><button :aria-pressed="!system" @click="scope = 'personal'">我的用量</button><button :aria-pressed="system" @click="scope = 'system'">系统 Key 用量</button></div>
      <span v-else class="scope-label">我的用量</span>
      <label><span class="sr-only">用量时间范围</span><select v-model="days"><option v-for="value in ([7, 30, 90] as const)" :key="value" :value="value">最近 {{ value }} 天</option></select></label>
    </div>
    <p class="scope-note">{{ system ? '仅统计平台提供的系统 Key 调用，不包含用户个人 Key。' : '仅查看你自己的调用，包含使用系统 Key 和个人 Key 的用量。' }}</p>
    <p v-if="loading" class="state" role="status">正在加载用量…</p>
    <div v-else-if="error" class="state" role="alert"><p>用量暂时无法加载</p><p class="error-detail">{{ error }}</p><button class="retry" @click="refresh">重新加载</button></div>
    <template v-else-if="data">
      <div class="total"><span>已知消耗 Tokens</span><strong>{{ formatNumber(data.summary.totalTokens) }}</strong></div>
      <dl class="metrics"><div><dt>输入</dt><dd>{{ formatNumber(data.summary.inputTokens) }}</dd></div><div><dt>输出</dt><dd>{{ formatNumber(data.summary.outputTokens) }}</dd></div><div><dt>调用次数</dt><dd>{{ formatNumber(data.summary.calls) }}</dd></div></dl>
      <p v-if="data.summary.calls === 0" class="notice">这段时间暂无{{ system ? '系统 Key' : '个人' }}调用记录。</p>
      <p v-else-if="data.summary.unknownCalls" class="notice">{{ formatNumber(data.summary.unknownCalls) }} 次调用未返回完整用量，实际消耗可能更高。</p>
      <p class="period">{{ data.startDate }} 至 {{ data.daily.at(-1)?.date || data.endDate }} · {{ data.timezone }}</p>
      <details v-if="data.summary.calls > 0" @toggle="showTrend = ($event.target as HTMLDetailsElement).open"><summary>查看每日趋势</summary><TokenUsageTrend v-if="showTrend" :key="system ? 'system' : 'personal'" :days="data.daily" /></details>
      <RouterLink class="details-link" :to="system ? '/admin/token-usage' : '/token-usage'">{{ system ? '查看系统 Key 详细统计' : '查看我的详细统计' }}<ArrowUpRight :size="15" aria-hidden="true" /></RouterLink>
    </template>
  </section>
</template>

<style scoped>
.profile-usage { min-width: 0; margin-bottom: 24px; padding: 20px; border: 1px solid var(--border-light); border-radius: var(--radius-lg); background: var(--bg-paper); box-shadow: var(--shadow-sm); }
header, h2, .controls, .scope, .details-link { display: flex; align-items: center; } header, .controls { justify-content: space-between; gap: 12px; } h2 { gap: 8px; font-family: var(--font-serif); font-size: 18px; font-weight: 600; } h2 svg { color: var(--accent); } .refresh { display: grid; place-items: center; width: 36px; height: 36px; color: var(--text-muted); border-radius: var(--radius-full); } .refresh:hover { background: var(--bg-surface); } .refresh:disabled { opacity: .5; cursor: wait; }
.controls { flex-wrap: wrap; margin-top: 12px; } .scope { gap: 3px; padding: 3px; background: var(--bg-surface); border-radius: var(--radius-full); } .scope button { min-height: 36px; padding: 0 12px; border-radius: var(--radius-full); font-size: 12px; color: var(--text-muted); } .scope button[aria-pressed=true] { background: var(--bg-dark); color: white; } .scope-label { font-size: 13px; color: var(--text-muted); } select { min-height: 38px; padding: 0 9px; border: 1px solid var(--border-medium); border-radius: var(--radius-sm); font: inherit; font-size: 12px; background: var(--bg-paper); color: var(--text-main); }
.scope-note, .period { color: var(--text-muted); font-size: 11px; line-height: 1.8; margin-top: 10px; } .total { margin: 20px 0 16px; } .total > span { display: block; font-size: 12px; color: var(--text-muted); } .total strong { display: block; font: 600 32px var(--font-serif); margin-top: 4px; overflow-wrap: anywhere; } .metrics { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; } .metrics div { min-width: 0; padding: 10px; border-radius: var(--radius-sm); background: var(--bg-surface); } dt { color: var(--text-muted); font-size: 11px; } dd { font-size: 15px; font-weight: 600; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
.notice { font-size: 12px; line-height: 1.7; color: #805a1a; margin-top: 14px; } .state { padding: 26px 0; color: var(--text-muted); font-size: 13px; } .error-detail { margin-top: 8px; overflow-wrap: anywhere; font-size: 12px; } .retry { margin-top: 12px; border-radius: var(--radius-full); background: var(--bg-dark); color: white; padding: 8px 16px; }
details { margin-top: 12px; border-top: 1px solid var(--border-light); } summary { cursor: pointer; padding: 12px 0; font-size: 12px; color: var(--text-muted); } :deep(.trend) { border: 0; padding: 12px 0 0; box-shadow: none; } .details-link { justify-content: space-between; min-height: 44px; margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--border-light); font-size: 13px; }
:is(button, select, summary, a):focus-visible { outline: 2px solid var(--accent); outline-offset: 3px; } .sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; }
@media (max-width: 420px) { .profile-usage { padding: 16px; } .controls { gap: 10px; } .scope { flex: 1 0 100%; } .scope button { flex: 1; } select { font-size: 16px; } dd { font-size: 14px; } }
</style>
