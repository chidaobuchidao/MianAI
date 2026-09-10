<script setup lang="ts">
import { computed, ref, useId, watch } from 'vue'
import { Activity, ChartColumn, ListFilter, Search, Users } from 'lucide-vue-next'
import { featureLabels, formatNumber, type TokenUsage } from '@/modules/token-usage/types'

const props = defineProps<{ data: TokenUsage; admin: boolean }>()
const id = useId()
const tab = ref('daily')
const search = ref('')
const status = ref('all')
const tabs = computed(() => [
  { key: 'daily', label: '每日明细', icon: ListFilter },
  { key: 'features', label: '功能统计', icon: Activity },
  { key: 'models', label: '模型统计', icon: ChartColumn },
  ...(props.admin ? [{ key: 'users', label: '用户统计', icon: Users }] : [])
])
const firstColumn = computed(() => ({ daily: '日期', features: '功能', models: '模型', users: '用户' })[tab.value])
const rows = computed(() => {
  const source = tab.value === 'daily'
    ? props.data.daily.map(day => ({ ...day, key: day.date, label: day.date })).reverse()
    : (tab.value === 'models' ? props.data.models : tab.value === 'features' ? props.data.features : props.data.users || [])
      .map(group => ({ ...group, label: tab.value === 'features' ? featureLabels[group.key] || group.key
        : tab.value === 'users' ? group.key === 'unassigned' ? '未归属用户' : `用户 ${group.key}` : group.key }))
      .sort((a, b) => b.totalTokens - a.totalTokens)
  return source.filter(row => row.label.toLowerCase().includes(search.value.trim().toLowerCase())
    && (status.value === 'all' || (status.value === 'unknown' ? row.unknownCalls > 0 : row.failedCalls > 0)))
})
watch(tab, () => { search.value = ''; status.value = 'all' })
watch(() => props.admin, () => { tab.value = 'daily' })
function move(event: KeyboardEvent, index: number) {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return
  event.preventDefault()
  const next = event.key === 'Home' ? 0 : event.key === 'End' ? tabs.value.length - 1
    : (index + (event.key === 'ArrowRight' ? 1 : -1) + tabs.value.length) % tabs.value.length
  tab.value = tabs.value[next]!.key
  const target = event.currentTarget as HTMLButtonElement
  ;(target.parentElement?.children[next] as HTMLButtonElement)?.focus()
}
</script>

<template>
  <section class="details" aria-label="用量明细统计">
    <div class="tabs" role="tablist" aria-label="统计维度"><button v-for="(item, index) in tabs" :id="`${id}-${item.key}`" :key="item.key" role="tab" :aria-selected="tab === item.key" :aria-controls="`${id}-panel`" :tabindex="tab === item.key ? 0 : -1" @click="tab = item.key" @keydown="move($event, index)"><component :is="item.icon" :size="16" aria-hidden="true" />{{ item.label }}</button></div>
    <div :id="`${id}-panel`" role="tabpanel" :aria-labelledby="`${id}-${tab}`" tabindex="0">
      <p v-if="tab !== 'daily'" class="scope-note">按已知 Token 排行前 20 项，搜索和状态筛选仅作用于当前结果。其他模型或用户请通过顶部筛选查询。</p>
      <div class="table-tools"><label><span class="sr-only">明细状态</span><select v-model="status"><option value="all">全部记录</option><option value="unknown">含未知用量</option><option value="failed">含失败调用</option></select></label><label class="search"><Search :size="15" aria-hidden="true" /><span class="sr-only">搜索{{ firstColumn }}</span><input v-model="search" :placeholder="`搜索${firstColumn}`" /></label><span class="row-count">{{ rows.length }} 条记录</span></div>
      <div class="table-scroll" role="region" aria-label="用量明细表，可横向滚动" tabindex="0"><table><caption class="sr-only">{{ tabs.find(item => item.key === tab)?.label }}，{{ tab === 'daily' ? '日期倒序' : '按已知 Token 总量降序' }}</caption><thead><tr><th scope="col">{{ firstColumn }}</th><th scope="col">输入 Token</th><th scope="col">输出 Token</th><th scope="col">总 Token</th><th scope="col">调用次数</th><th scope="col">未知用量</th><th scope="col">失败 / 中断</th></tr></thead><tbody><tr v-for="row in rows" :key="row.key"><th scope="row">{{ row.label }}</th><td>{{ formatNumber(row.inputTokens) }}</td><td>{{ formatNumber(row.outputTokens) }}</td><td class="total">{{ formatNumber(row.totalTokens) }}</td><td>{{ formatNumber(row.calls) }}</td><td><span :class="{ warning: row.unknownCalls > 0 }">{{ formatNumber(row.unknownCalls) }}</span></td><td>{{ formatNumber(row.failedCalls) }}</td></tr><tr v-if="!rows.length"><td colspan="7" class="empty">没有符合条件的记录</td></tr></tbody></table></div>
    </div>
  </section>
</template>

<style scoped>
.scope-note { margin: 0 0 12px; color: #6b7280; font-size: 12px; line-height: 1.7; }
.details { margin-top: 30px; } .tabs { display: inline-flex; flex-wrap: wrap; gap: 4px; padding: 4px; margin-bottom: 16px; background: #f8f9fb; border-radius: 9px; } .tabs button { display: inline-flex; align-items: center; gap: 8px; min-height: 36px; border-radius: 7px; padding: 0 17px; color: #687180; font-size: 13px; cursor: pointer; } .tabs button[aria-selected=true] { background: #006fe6; color: white; }
.table-tools { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; padding: 9px; border: 1px solid #e7eaf0; border-radius: 12px; margin-bottom: 16px; } select, input { height: 36px; font: inherit; font-size: 12px; color: #414958; background: white; border: 1px solid #e4e8ee; border-radius: 8px; padding: 0 12px; } .search { position: relative; display: flex; align-items: center; } .search svg { position: absolute; left: 11px; color: #7d8490; } .search input { padding-left: 34px; width: 200px; } .row-count { margin-left: auto; padding-right: 8px; font-size: 12px; color: #6b7280; }
.table-scroll { overflow: auto; max-height: 480px; border: 1px solid #e7eaf0; border-radius: 12px; } table { border-collapse: collapse; width: 100%; min-width: 760px; font-size: 13px; text-align: right; font-variant-numeric: tabular-nums; } th, td { padding: 16px 22px; border-bottom: 1px solid #edf0f4; } thead th { position: sticky; top: 0; background: #fff; color: #6b7280; font-weight: 500; font-size: 12px; white-space: nowrap; } th:first-child { text-align: left; } tbody th { font-weight: 500; color: #3d4552; max-width: 320px; overflow-wrap: anywhere; } tbody td { color: #5d6572; } tbody tr:hover { background: #fafbfe; } tbody tr:last-child :is(th, td) { border-bottom: 0; } td.total { color: #202633; font-weight: 550; } .warning { color: #98631a; background: #fff7e8; border-radius: 4px; padding: 3px 7px; } td.empty { text-align: center; padding: 40px 20px; }
:is(button, input, select, [tabindex]):focus-visible { outline: 2px solid #1687ff; outline-offset: 2px; } .sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; }
@media (max-width: 600px) { .tabs { display: flex; } .tabs button { flex: 1 0 auto; justify-content: center; padding: 0 10px; font-size: 12px; } .search { flex: 1; min-width: 120px; } .search input { width: 100%; } .row-count { width: 100%; text-align: right; } input, select { font-size: 16px; } }
</style>
