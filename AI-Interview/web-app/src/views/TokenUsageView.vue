<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Activity, ArrowDownToLine, ArrowLeft, ArrowUpFromLine, BarChart3, CalendarDays, CircleAlert, Database, Info, RefreshCw, SlidersHorizontal, Sparkles, Zap } from 'lucide-vue-next'
import TokenUsageTrend from '@/components/TokenUsageTrend.vue'
import TokenUsageTable from '@/components/TokenUsageTable.vue'
import { useUserStore } from '@/stores/user'
import { useTokenUsage } from '@/modules/token-usage/useTokenUsage'
import { featureLabels, formatNumber, type UsageDays, type UsageFilters } from '@/modules/token-usage/types'

const route = useRoute()
const userStore = useUserStore()
const admin = computed(() => route.meta.requiresAdmin === true)
const days = ref<UsageDays>(7)
const filters = reactive({ model: '', feature: '', userId: '', keySource: '' as '' | 'SYSTEM' | 'PERSONAL' })
const applied = ref<UsageFilters>({ days: 7 })
const validation = ref('')
const models = ref<string[]>([])
const { data, loading, error, load } = useTokenUsage()
const updatedAt = ref('')
const coverage = computed(() => data.value?.summary.calls
  ? ((data.value.summary.calls - data.value.summary.unknownCalls) / data.value.summary.calls * 100).toFixed(1) : null)
const filtered = computed(() => !!(applied.value.model || applied.value.feature || applied.value.userId || applied.value.keySource))
const dirty = computed(() => filters.model.trim() !== (applied.value.model || '') || filters.feature !== (applied.value.feature || '')
  || (admin.value && (filters.userId.trim() !== (applied.value.userId || '') || filters.keySource !== (applied.value.keySource || ''))))

watch(data, value => {
  if (value) {
    models.value = [...new Set([...models.value, ...value.models.map(model => model.key)])].sort()
    updatedAt.value = new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', timeZone: value.timezone }).format(new Date())
  }
})
watch(admin, () => {
  validation.value = ''
  Object.assign(filters, { model: '', feature: '', userId: '', keySource: '' })
  days.value = 7
  applied.value = { days: 7 }
  models.value = []
  void load(admin.value, applied.value)
}, { immediate: true })
function apply() {
  validation.value = ''
  const userId = filters.userId.trim()
  if (admin.value && userId && (!/^[1-9]\d*$/.test(userId) || BigInt(userId) > 9223372036854775807n)) {
    validation.value = '用户 ID 需为有效的正整数'
    return
  }
  applied.value = {
    days: days.value, model: filters.model.trim(), feature: filters.feature,
    ...(admin.value ? { userId, keySource: filters.keySource } : {})
  }
  void load(admin.value, applied.value)
}
function setDays(value: UsageDays) {
  days.value = value
  applied.value = { ...applied.value, days: value }
  void load(admin.value, applied.value)
}
function reset() {
  Object.assign(filters, { model: '', feature: '', userId: '', keySource: '' })
  apply()
}
</script>

<template>
  <main class="usage-page">
    <header class="topbar">
      <RouterLink class="back-link" :to="admin ? '/admin' : '/profile'" :aria-label="admin ? '返回管理后台' : '返回个人中心'"><ArrowLeft :size="18" aria-hidden="true" /></RouterLink>
      <span>{{ admin ? '管理后台' : '个人中心' }}</span>
      <span class="topbar-caption">Mianmian.</span>
    </header>

    <nav class="section-nav" aria-label="用量页面导航">
      <RouterLink to="/profile">个人中心</RouterLink>
      <RouterLink to="/token-usage" :class="{ active: !admin }" :aria-current="!admin ? 'page' : undefined"><BarChart3 :size="15" aria-hidden="true" />个人用量</RouterLink>
      <RouterLink v-if="userStore.isAdmin" to="/admin/token-usage" :class="{ active: admin }" :aria-current="admin ? 'page' : undefined"><Activity :size="15" aria-hidden="true" />全站用量</RouterLink>
      <RouterLink v-if="userStore.isAdmin" to="/admin">管理后台</RouterLink>
    </nav>

    <div class="usage-content">
      <div class="page-toolbar">
        <div class="page-title"><h1>{{ admin ? '全站使用统计' : '使用统计' }}</h1><p>查看 AI 模型的调用与 Token 用量</p></div>
        <form class="filters" aria-label="用量筛选" @submit.prevent="apply">
          <label class="model-field"><span class="sr-only">模型</span><input v-model="filters.model" list="usage-models" maxlength="255" placeholder="全部模型" /><datalist id="usage-models"><option v-for="model in models" :key="model" :value="model" /></datalist></label>
          <label class="feature-field"><span class="sr-only">功能</span><select v-model="filters.feature"><option value="">全部功能</option><option v-for="(name, key) in featureLabels" :key="key" :value="key">{{ name }}</option></select></label>
          <template v-if="admin">
            <label class="user-field"><span class="sr-only">用户 ID</span><input v-model="filters.userId" inputmode="numeric" maxlength="19" placeholder="全部用户 ID" :aria-invalid="!!validation" :aria-describedby="validation ? 'filter-validation' : undefined" /></label>
            <label class="source-field"><span class="sr-only">Key 来源</span><select v-model="filters.keySource"><option value="">全部 Key 来源</option><option value="SYSTEM">系统 Key</option><option value="PERSONAL">个人 Key</option></select></label>
          </template>
          <button class="apply-btn compact-button" type="submit"><SlidersHorizontal :size="14" aria-hidden="true" /><span>应用筛选</span></button>
          <button v-if="filtered || dirty" class="reset-btn compact-button" type="button" @click="reset">重置</button>
          <span class="filter-divider" aria-hidden="true" />
          <button class="refresh-btn compact-button" type="button" :disabled="loading" aria-label="刷新用量" @click="load(admin, applied)"><RefreshCw :size="14" :class="{ spinning: loading }" aria-hidden="true" /><span>刷新</span></button>
          <label class="days-field"><span class="sr-only">统计时间范围</span><CalendarDays :size="14" aria-hidden="true" /><select v-model="days" @change="setDays(days)"><option v-for="value in ([7, 30, 90] as const)" :key="value" :value="value">最近 {{ value }} 天</option></select></label>
        </form>
      </div>
      <p v-if="validation" id="filter-validation" class="validation filter-message" role="alert">{{ validation }}</p>
      <p v-else-if="dirty" class="filter-message">筛选条件已修改，点击“应用筛选”更新统计。</p>

      <div v-if="loading" class="loading-state" role="status" aria-live="polite"><div class="skeleton skeleton-total" /><div class="skeleton skeleton-chart" /><p>正在汇总用量…</p></div>
      <section v-else-if="error" class="state-card" role="alert"><CircleAlert class="state-icon error-icon" :size="34" aria-hidden="true" /><h2>用量暂时无法加载</h2><p>{{ error }}</p><button class="apply-btn" @click="load(admin, applied)"><RefreshCw :size="15" aria-hidden="true" />重新加载</button></section>
      <template v-else-if="data">
        <section class="overview" aria-label="用量概览">
          <div class="summary-row">
            <div class="total-card"><span class="total-icon"><Zap :size="24" :stroke-width="1.8" aria-hidden="true" /></span><div><span class="total-label">已知消耗 Tokens</span><div class="total-value"><strong>{{ formatNumber(data.summary.totalTokens) }}</strong><span class="total-unit">Tokens</span></div></div></div>
            <div class="request-summary"><div><span>总请求数</span><strong><Activity :size="14" aria-hidden="true" />{{ formatNumber(data.summary.calls) }}</strong></div><div><span>总费用</span><strong class="unavailable">暂无数据</strong></div></div>
          </div>
          <div class="stat-grid">
            <div class="stat"><span class="stat-label"><ArrowDownToLine :size="14" class="input-color" aria-hidden="true" />输入 Tokens</span><strong>{{ formatNumber(data.summary.inputTokens) }}</strong></div>
            <div class="stat"><span class="stat-label"><ArrowUpFromLine :size="14" class="output-color" aria-hidden="true" />输出 Tokens</span><strong>{{ formatNumber(data.summary.outputTokens) }}</strong></div>
            <div class="stat"><span class="stat-label"><Database :size="14" class="cache-create-color" aria-hidden="true" />缓存创建<Info :size="12" class="info-icon" aria-hidden="true" /></span><strong class="unavailable">暂无数据</strong><span class="sr-only">当前未采集缓存创建用量</span></div>
            <div class="stat"><span class="stat-label"><Sparkles :size="14" class="cache-hit-color" aria-hidden="true" />缓存命中<Info :size="12" class="info-icon" aria-hidden="true" /></span><strong class="unavailable">暂无数据</strong><span class="sr-only">当前未采集缓存命中用量</span></div>
            <div class="stat coverage-stat"><div class="coverage-label"><span class="stat-label">用量覆盖率</span><strong v-if="coverage !== null">{{ coverage }}%</strong><span v-else class="unavailable">暂无数据</span></div><div v-if="coverage !== null" class="coverage-track" role="progressbar" aria-label="返回完整用量的调用占比" :aria-valuenow="Number(coverage)" :aria-valuemin="0" :aria-valuemax="100"><span :style="{ width: `${coverage}%` }" /></div><p v-else>当前无调用记录</p></div>
          </div>
        </section>

        <div class="report-meta"><div class="coverage" :class="{ 'coverage--partial': data.summary.unknownCalls > 0 }"><Info :size="13" aria-hidden="true" /><span v-if="data.summary.unknownCalls">{{ formatNumber(data.summary.unknownCalls) }} 次调用未返回完整用量，实际消耗可能更高。</span><span v-else>{{ data.summary.calls ? '当前调用均已返回完整用量。' : '当前无调用记录。' }}</span><span class="failed-count">失败 / 中断 {{ formatNumber(data.summary.failedCalls) }} 次</span></div><span class="date-note">{{ data.startDate }} 至 {{ data.daily.at(-1)?.date || data.endDate }} · {{ data.timezone }}<span v-if="updatedAt"> · {{ updatedAt }} 更新</span></span></div>

        <section v-if="data.summary.calls === 0" class="state-card empty-state"><BarChart3 :size="38" class="state-icon" aria-hidden="true" /><h2>{{ filtered ? '当前筛选下暂无调用' : '用量记录从这里开始' }}</h2><p>{{ filtered ? '尝试调整时间范围、模型或功能。' : '使用面试、简历或论文工具后，可在这里查看记录。' }}</p><button v-if="filtered" class="apply-btn" @click="reset">清除筛选</button><RouterLink v-else class="apply-btn" :to="admin ? '/admin' : '/profile'">{{ admin ? '返回管理后台' : '返回个人中心' }}</RouterLink></section>
        <TokenUsageTrend v-else :days="data.daily" />
        <div class="usage-table"><TokenUsageTable :data="data" :admin="admin" /></div>

        <footer class="usage-footer"><Info :size="13" aria-hidden="true" /><p>仅统计本应用发出的 AI 调用，记录从功能上线后开始积累。未知用量不计入合计，失败或中断的调用可能仍产生 Token。缓存用量和费用尚未采集。</p></footer>
      </template>
    </div>
  </main>
</template>

<style scoped>
:global(body:has(.usage-page)), :global(#app:has(.usage-page)) { background: #fff; }
.usage-page { min-height: calc(100vh - 44px); width: 100%; background: #fff; color: #181b22; --usage-blue: #006fe6; --usage-border: #eceef2; --usage-muted: #6b7280; }
.topbar { height: 64px; padding: 0 24px; display: flex; align-items: center; gap: 16px; font-size: 17px; font-weight: 600; }
.back-link { display: inline-flex; align-items: center; justify-content: center; width: 36px; height: 36px; border: 1px solid #e6e8ec; border-radius: 12px; color: #767d88; }
.topbar-caption { margin-left: auto; color: #6b7280; font-size: 13px; font-weight: 500; letter-spacing: -.3px; }
.section-nav { display: flex; align-items: stretch; margin: 0 24px; padding: 4px; min-height: 42px; border: 1px solid var(--usage-border); border-radius: 11px; gap: 4px; }
.section-nav a { flex: 1; min-width: 0; display: flex; align-items: center; justify-content: center; gap: 7px; padding: 6px 14px; border-radius: 8px; font-size: 13px; font-weight: 500; color: #6b7280; }
.section-nav a:hover { background: #f5f7fa; } .section-nav a.active { color: #fff; background: var(--usage-blue); }
.usage-content { padding: 24px; }
.page-toolbar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 18px 24px; margin: 0 0 28px; min-height: 58px; }
.page-title h1 { font-size: 23px; font-weight: 650; letter-spacing: -.65px; line-height: 1.4; }
.page-title p { font-size: 13px; color: var(--usage-muted); margin-top: 4px; }
.filters { display: flex; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 8px; }
.filters label { display: block; position: relative; min-width: 0; }
.model-field { width: 144px; } .feature-field { width: 125px; } .source-field { width: 135px; } .user-field { width: 128px; } .days-field { width: 129px; }
input, select { height: 36px; width: 100%; border: 1px solid #e5e8ef; border-radius: 8px; background: #fff; color: #444b56; font-size: 12px; padding: 0 10px; font-family: inherit; box-shadow: 0 1px 2px #17243d04; }
input::placeholder { color: #545b67; } select { padding-right: 5px; }
.days-field > svg { position: absolute; top: 11px; left: 10px; color: #7c8390; pointer-events: none; } .days-field select { padding-left: 31px; }
.compact-button { height: 36px; min-height: 36px; padding: 0 11px; white-space: nowrap; }
.apply-btn, .refresh-btn, .reset-btn { display: inline-flex; align-items: center; justify-content: center; gap: 7px; border-radius: 8px; font-size: 12px; font-weight: 500; }
.apply-btn { color: #fff; background: var(--usage-blue); min-height: 36px; padding: 8px 14px; border: 1px solid var(--usage-blue); }
.apply-btn:hover { background: #066bda; } .refresh-btn { border: 1px solid #e5e8ef; color: #4e5560; background: #fff; } .refresh-btn:hover, .reset-btn:hover { background: #f5f8fc; } .refresh-btn:disabled { opacity: .55; cursor: wait; } .reset-btn { color: #697381; }
.filter-divider { width: 1px; height: 20px; background: #e7eaf0; margin: 0 2px; }
.filter-message { color: #64748b; font-size: 12px; margin: -15px 0 18px; text-align: right; } .validation { color: #b42318; }
.overview { border: 1px solid var(--usage-border); border-radius: 14px; padding: 20px; box-shadow: 0 1px 2px #18233d03; }
.summary-row { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 19px; }
.total-card { display: flex; align-items: center; gap: 13px; min-width: 0; } .total-icon { flex-shrink: 0; width: 42px; height: 42px; display: inline-flex; align-items: center; justify-content: center; color: #1684ff; background: #eaf4ff; border-radius: 14px; }
.total-label { display: block; font-size: 11px; color: #6b7280; font-weight: 500; margin-bottom: 1px; }
.total-value { display: flex; align-items: baseline; flex-wrap: wrap; gap: 10px; } .total-value > strong { font-size: 30px; line-height: 1.25; font-weight: 680; letter-spacing: -.8px; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; } .total-unit { color: #6b7280; font-size: 11px; }
.request-summary { display: flex; align-items: center; border: 1px solid #eef0f4; border-radius: 14px; padding: 11px 15px; gap: 20px; box-shadow: 0 1px 2px #1b2d4504; }
.request-summary > div + div { border-left: 1px solid #edf0f4; padding-left: 20px; } .request-summary > div > span { display: block; font-size: 10px; color: #6b7280; margin-bottom: 4px; }
.request-summary strong { display: flex; align-items: center; gap: 6px; font-size: 13px; line-height: 1.2; font-weight: 600; white-space: nowrap; font-variant-numeric: tabular-nums; } .request-summary strong svg { color: #3987f6; }
.stat-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 12px; }
.stat { min-width: 0; padding: 12px 13px; min-height: 71px; border: 1px solid #eef0f4; border-radius: 13px; box-shadow: 0 1px 2px #14264603; }
.stat-label { display: flex; align-items: center; gap: 6px; color: #6b7280; font-size: 11px; font-weight: 500; }
.stat > strong { display: block; color: #272c35; margin-top: 4px; font-size: 15px; font-weight: 600; line-height: 1.5; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
.stat .info-icon { margin-left: auto; color: #6b7280; flex-shrink: 0; } .input-color { color: #3b82f6; } .output-color { color: #19b76b; } .cache-create-color { color: #df862c; } .cache-hit-color { color: #a363ed; }
.stat strong.unavailable, .request-summary strong.unavailable, .unavailable { color: #6b7280; font-size: 13px; font-weight: 500; }
.coverage-label { display: flex; align-items: center; justify-content: space-between; gap: 6px; } .coverage-label > strong { color: #00845c; font-size: 12px; line-height: 1.6; font-weight: 600; }
.coverage-track { height: 6px; border-radius: 5px; background: #f0f2f5; overflow: hidden; margin-top: 12px; } .coverage-track > span { display: block; height: 100%; background: #10b981; border-radius: inherit; } .coverage-stat p { color: #6b7280; font-size: 10px; margin-top: 7px; }
.report-meta { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 6px 16px; color: #6b7280; font-size: 11px; margin: 11px 2px 21px; }
.coverage { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; } .coverage--partial { color: #8e6828; } .failed-count { color: #6b7280; margin-left: 7px; } .date-note { color: #6b7280; }
.usage-table { margin-top: 30px; }
.usage-footer { display: flex; gap: 7px; align-items: flex-start; margin-top: 21px; color: #6b7280; font-size: 11px; line-height: 1.8; } .usage-footer > svg { flex-shrink: 0; margin-top: 3px; }
.state-card { display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; min-height: 390px; padding: 48px 24px; border: 1px solid var(--usage-border); border-radius: 14px; }
.state-icon { color: #72aef8; } .error-icon { color: #c65a4f; } .state-card h2 { font-size: 20px; font-weight: 600; margin: 16px 0 8px; } .state-card p { font-size: 13px; color: #6b7280; margin-bottom: 23px; max-width: 560px; overflow-wrap: anywhere; }
.skeleton { background: #f4f6f9; border: 1px solid var(--usage-border); border-radius: 14px; animation: breathe 1.4s ease-in-out infinite alternate; } .skeleton-total { height: 190px; } .skeleton-chart { height: 440px; margin-top: 30px; } .loading-state p { font-size: 12px; margin-top: 12px; color: #6e7888; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
:is(button, a, input, select):focus-visible { outline: 2px solid #087fff; outline-offset: 3px; }
.spinning { animation: spin 1s linear infinite; } @keyframes spin { to { transform: rotate(360deg); } } @keyframes breathe { to { opacity: .5; } }
@media (max-width: 1250px) { .page-toolbar { align-items: flex-start; gap: 16px; } .filters { justify-content: flex-start; } .page-title { flex: 1 0 100%; } }
@media (max-width: 800px) { .topbar { height: 58px; padding: 0 18px; } .section-nav { margin: 0 18px; } .usage-content { padding: 22px 18px; } .section-nav a { padding: 7px; font-size: 12px; gap: 4px; } .stat-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } .coverage-stat { grid-column: span 2; } .summary-row { align-items: flex-start; } .request-summary { gap: 12px; padding: 11px; } .request-summary > div + div { padding-left: 12px; } .total-value > strong { font-size: 27px; } .total-unit { display: none; } .date-note { flex-basis: 100%; } }
@media (max-width: 520px) { .topbar { padding: 0 14px; gap: 12px; } .section-nav { margin: 0 14px; } .section-nav a { font-size: 11px; } .section-nav svg { display: none; } .usage-content { padding: 20px 14px; } .page-title h1 { font-size: 22px; } .page-toolbar { margin-bottom: 20px; } .filters { width: 100%; gap: 8px; } .filters label { flex: 1 1 calc(50% - 4px); width: auto; } .filters .days-field { flex: 1 1 128px; } input, select { height: 40px; font-size: 16px; } .days-field > svg { top: 13px; } .compact-button { min-height: 40px; height: 40px; } .filter-divider { display: none; } .filter-message { text-align: left; margin-top: -8px; } .overview { padding: 16px; } .summary-row { flex-wrap: wrap; gap: 14px; margin-bottom: 16px; } .total-icon { width: 38px; height: 38px; border-radius: 11px; } .total-card { gap: 10px; } .total-value > strong { font-size: 28px; } .request-summary { width: 100%; justify-content: space-around; padding: 10px 12px; } .request-summary > div { flex: 1; } .request-summary > div + div { padding-left: 22px; } .stat-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 9px; } .stat { padding: 11px; border-radius: 10px; } .coverage-stat { grid-column: span 2; } .coverage-track { margin-top: 10px; } .stat > strong { font-size: 14px; } .stat-label { font-size: 10px; gap: 4px; } .report-meta { font-size: 10px; margin-bottom: 18px; } .failed-count { margin-left: 0; } .usage-table { margin-top: 23px; } }
@media (prefers-reduced-motion: reduce) { .skeleton, .spinning { animation: none; } }
</style>
