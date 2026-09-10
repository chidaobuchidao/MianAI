<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, useId, watch } from 'vue'
import { compactNumber, formatNumber, type UsageDay } from '@/modules/token-usage/types'
const props = defineProps<{ days: UsageDay[] }>()
const id = useId()
const canvas = ref<HTMLElement>()
const width = ref(1000)
const selectedIndex = ref(0)
const inspecting = ref(false)
const series = [
  { key: 'totalTokens', label: '总量', color: 'var(--accent)' },
  { key: 'inputTokens', label: '输入', color: '#596a7c' },
  { key: 'outputTokens', label: '输出', color: '#397362' }
] as const
type SeriesKey = typeof series[number]['key']
const visible = ref<SeriesKey[]>(series.map(item => item.key))
const active = computed(() => series.filter(item => visible.value.includes(item.key)))
const maximum = computed(() => {
  const value = Math.max(1, ...props.days.flatMap(day => active.value.map(item => day[item.key])))
  const unit = 10 ** Math.floor(Math.log10(value))
  return Math.ceil(value / unit) * unit
})
const selected = computed(() => props.days[selectedIndex.value])
const x = (index: number) => props.days.length < 2 ? width.value / 2 : 62 + index / (props.days.length - 1) * (width.value - 90)
const y = (value: number) => 302 - value / maximum.value * 270
const tooltipLeft = computed(() => Math.max(8, Math.min(width.value - 218, x(selectedIndex.value) + (x(selectedIndex.value) < width.value / 2 ? 18 : -218))))
const stride = computed(() => Math.max(1, Math.ceil(props.days.length / Math.max(2, Math.floor(width.value / 100)))))
// Horizontal control points keep the curve within each pair's actual values.
function path(key: SeriesKey) {
  return props.days.map((day, index) => {
    if (!index) return `M ${x(index)} ${y(day[key])}`
    const previous = props.days[index - 1]!
    const middle = (x(index - 1) + x(index)) / 2
    return `C ${middle} ${y(previous[key])}, ${middle} ${y(day[key])}, ${x(index)} ${y(day[key])}`
  }).join(' ')
}
function toggle(key: SeriesKey) {
  if (visible.value.includes(key)) {
    if (visible.value.length > 1) visible.value = visible.value.filter(value => value !== key)
  } else visible.value = [...visible.value, key]
}
function point(event: PointerEvent) {
  const bounds = (event.currentTarget as SVGSVGElement).getBoundingClientRect()
  const local = (event.clientX - bounds.left) * width.value / bounds.width
  selectedIndex.value = Math.max(0, Math.min(props.days.length - 1, Math.round((local - 62) / (width.value - 90) * (props.days.length - 1))))
  inspecting.value = true
}
function move(event: KeyboardEvent) {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End', 'Escape'].includes(event.key)) return
  event.preventDefault()
  if (event.key === 'Escape') { inspecting.value = false; return }
  selectedIndex.value = event.key === 'Home' ? 0 : event.key === 'End' ? props.days.length - 1
    : Math.max(0, Math.min(props.days.length - 1, selectedIndex.value + (event.key === 'ArrowRight' ? 1 : -1)))
  inspecting.value = true
}
watch(() => props.days, value => { selectedIndex.value = Math.max(0, value.length - 1); inspecting.value = false }, { immediate: true })
let observer: ResizeObserver | undefined
onMounted(() => {
  observer = new ResizeObserver(entries => { width.value = Math.max(640, entries[0]?.contentRect.width || 640) })
  if (canvas.value) observer.observe(canvas.value)
})
onBeforeUnmount(() => observer?.disconnect())
</script>

<template>
  <section class="trend" :aria-labelledby="`${id}-title`">
    <header><h2 :id="`${id}-title`">使用趋势</h2><span>近 {{ days.length }} 天 · 按天汇总</span></header>
    <div class="chart-scroll"><div ref="canvas" class="chart-canvas">
      <svg :viewBox="`0 0 ${width} 342`" class="chart" tabindex="0" role="group" aria-label="Token 用量折线图，使用左右方向键查看每日数值，Home 和 End 跳至首尾日期" :aria-describedby="`${id}-detail`"
        @pointermove="point" @pointerdown="point" @pointerleave="inspecting = false" @focus="inspecting = true" @blur="inspecting = false" @keydown="move">
        <defs><linearGradient :id="`${id}-fill`" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="var(--accent)" stop-opacity=".17" /><stop offset="100%" stop-color="var(--accent)" stop-opacity="0" /></linearGradient></defs>
        <g aria-hidden="true">
          <template v-for="fraction in [0, .25, .5, .75, 1]" :key="fraction"><line x1="62" :x2="width - 28" :y1="y(maximum * fraction)" :y2="y(maximum * fraction)" stroke="var(--border-light)" stroke-dasharray="2 4" /><text x="52" :y="y(maximum * fraction) + 4" text-anchor="end">{{ compactNumber(maximum * fraction) }}</text></template>
          <path v-if="days.length && visible.includes('totalTokens')" :d="`${path('totalTokens')} L ${x(days.length - 1)} 302 L ${x(0)} 302 Z`" :fill="`url(#${id}-fill)`" />
          <path v-for="item in active" :key="item.key" :d="path(item.key)" fill="none" :stroke="item.color" stroke-width="2.2" stroke-linejoin="round" />
          <template v-for="(day, index) in days" :key="day.date"><text v-if="(index % stride === 0 && days.length - 1 - index >= stride * .65) || index === days.length - 1" :x="x(index)" y="327" :text-anchor="index === 0 ? 'start' : index === days.length - 1 ? 'end' : 'middle'">{{ day.date.slice(5).replace('-', '/') }}</text></template>
          <template v-if="inspecting && selected"><line :x1="x(selectedIndex)" :x2="x(selectedIndex)" y1="32" y2="302" stroke="#d4d8df" /><circle v-for="item in active" :key="item.key" :cx="x(selectedIndex)" :cy="y(selected[item.key])" r="4" :fill="item.color" stroke="var(--bg-paper)" stroke-width="2" /></template>
          <template v-else-if="days.length === 1"><circle v-for="item in active" :key="item.key" :cx="x(0)" :cy="y(days[0]![item.key])" r="3" :fill="item.color" /></template>
        </g>
      </svg>
      <div v-if="inspecting && selected" class="tooltip" :style="{ left: `${tooltipLeft}px` }" aria-hidden="true"><strong>{{ selected.date }}</strong><div v-for="item in series" :key="item.key"><span><i :style="{ background: item.color }" />{{ item.label }}</span><b>{{ formatNumber(selected[item.key]) }}</b></div><p>{{ formatNumber(selected.calls) }} 次调用<span v-if="selected.unknownCalls"> · {{ formatNumber(selected.unknownCalls) }} 次用量未知</span></p></div>
    </div></div>
    <div class="legend" role="group" aria-label="趋势指标"><button v-for="item in series" :key="item.key" :aria-pressed="visible.includes(item.key)" @click="toggle(item.key)"><i :style="{ background: item.color }" />{{ item.label }}</button></div>
    <p :id="`${id}-detail`" class="sr-only" aria-live="polite">{{ selected ? `${selected.date}，输入 ${formatNumber(selected.inputTokens)}，输出 ${formatNumber(selected.outputTokens)}，总量 ${formatNumber(selected.totalTokens)}，未知用量 ${selected.unknownCalls} 次。` : '暂无用量' }}</p>
  </section>
</template>

<style scoped>
.trend { padding: 26px 24px 16px; border: 1px solid var(--border-light); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); background: var(--bg-paper); }
header { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 14px; } h2 { font-family: var(--font-serif); margin: 0; font-size: 18px; font-weight: 650; color: var(--text-main); } header span { font-size: 12px; color: var(--text-muted); }
.chart-scroll { overflow-x: auto; } .chart-canvas { position: relative; min-width: 640px; } .chart { display: block; width: 100%; height: 342px; overflow: visible; } text { font-family: var(--font-sans); font-size: 11px; fill: var(--text-muted); }
.tooltip { position: absolute; top: 45px; width: 202px; pointer-events: none; background: var(--bg-paper); border: 1px solid var(--border-light); border-radius: 12px; box-shadow: var(--shadow-md); padding: 14px; font-size: 12px; color: var(--text-main); } .tooltip strong { display: block; margin-bottom: 10px; font-size: 13px; } .tooltip div, .tooltip span { display: flex; align-items: center; gap: 7px; } .tooltip div { justify-content: space-between; margin-top: 7px; } .tooltip b { font-weight: 500; font-variant-numeric: tabular-nums; } .tooltip p { margin-top: 12px; padding-top: 10px; border-top: 1px solid var(--border-light); color: var(--text-muted); font-size: 11px; } .tooltip p span { margin-top: 3px; }
i { display: inline-block; width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; } .legend { display: flex; justify-content: center; gap: 6px; } .legend button { display: inline-flex; align-items: center; gap: 6px; min-height: 36px; padding: 0 12px; color: var(--text-muted); font: inherit; font-size: 12px; border-radius: 6px; cursor: pointer; } .legend button[aria-pressed=false] { color: #747b86; text-decoration: line-through; } .legend button[aria-pressed=false] i { background: #c5c9d0 !important; } .legend button:hover { background: var(--bg-surface); }
:is(button, svg):focus-visible { outline: 2px solid var(--accent); outline-offset: -2px; } .sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; }
@media (max-width: 600px) { .trend { padding: 20px 12px 12px; } h2 { font-size: 16px; } header { padding: 0 4px; } header span { font-size: 11px; } }
</style>
