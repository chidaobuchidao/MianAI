<template>
  <div :style="{ transform: `scale(${size})`, transformOrigin: 'center center' }" :class="className">
    <div class="folder" :class="{ open }" :style="folderVars" @click.stop="toggle">
      <div class="folder__back">
        <div
          v-for="(paper, i) in displayPapers"
          :key="i"
          class="paper"
          :class="`paper-${i + 1}`"
          :style="paperStyle(i)"
          @mousemove="(e: MouseEvent) => onPaperMove(e, i)"
          @mouseleave="() => onPaperLeave(i)"
          @click.stop="onPaperClick(i)"
        >
          <span class="paper__label" v-if="paper">{{ paper }}</span>
        </div>
        <div class="folder__front" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface Props {
  color?: string
  size?: number
  items?: (string | null)[]
  className?: string
}

const props = withDefaults(defineProps<Props>(), {
  color: '#70a1ff',
  size: 1,
  items: () => [],
  className: ''
})

const emit = defineEmits<{
  'paper-click': [index: number, item: string]
}>()

const maxItems = 3
const open = ref(false)
const offsets = ref<{ x: number; y: number }[]>(Array.from({ length: maxItems }, () => ({ x: 0, y: 0 })))

const displayPapers = computed(() => {
  const p = props.items.slice(0, maxItems)
  while (p.length < maxItems) p.push(null)
  return p
})

function darken(hex: string, pct: number): string {
  const color = hex.startsWith('#') ? hex.slice(1) : hex
  const h = color.length === 3 ? color.split('').map(c => c + c).join('') : color
  const num = parseInt(h, 16)
  let r = (num >> 16) & 0xff, g = (num >> 8) & 0xff, b = num & 0xff
  r = Math.max(0, Math.min(255, Math.floor(r * (1 - pct))))
  g = Math.max(0, Math.min(255, Math.floor(g * (1 - pct))))
  b = Math.max(0, Math.min(255, Math.floor(b * (1 - pct))))
  return '#' + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1).toUpperCase()
}

const folderBack = computed(() => darken(props.color, 0.08))
const paperColors = computed(() => [
  darken('#ffffff', 0.1),
  darken('#ffffff', 0.05),
  '#ffffff'
])

const folderVars = computed(() => ({
  '--folder-color': props.color,
  '--folder-back-color': folderBack.value,
  '--paper-1': paperColors.value[0],
  '--paper-2': paperColors.value[1],
  '--paper-3': paperColors.value[2]
}))

function toggle() {
  open.value = !open.value
  if (!open.value) {
    offsets.value = Array.from({ length: maxItems }, () => ({ x: 0, y: 0 }))
  }
}

function onPaperClick(i: number) {
  const item = displayPapers.value[i]
  if (item && open.value) {
    emit('paper-click', i, item)
  }
}

function onPaperMove(e: MouseEvent, i: number) {
  if (!open.value) return
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  const cx = rect.left + rect.width / 2
  const cy = rect.top + rect.height / 2
  offsets.value[i] = { x: (e.clientX - cx) * 0.05, y: (e.clientY - cy) * 0.05 }
}

function onPaperLeave(i: number) {
  offsets.value[i] = { x: 0, y: 0 }
}

function paperStyle(i: number) {
  if (!open.value) return {}
  return {
    '--magnet-x': `${offsets.value[i]?.x || 0}px`,
    '--magnet-y': `${offsets.value[i]?.y || 0}px`
  }
}
</script>

<style scoped>
.folder {
  width: 100px; height: 80px;
  cursor: pointer; position: relative;
  transition: transform 0.2s ease-in;
}
.folder:hover { transform: translateY(-8px); }
.folder.open { transform: translateY(-8px); }

.folder__back {
  position: relative; width: 100%; height: 100%;
  background: var(--folder-back-color);
  border-radius: 0 10px 10px 10px;
}
.folder__back::after {
  position: absolute; z-index: 0; bottom: 98%; left: 0;
  content: ''; width: 30px; height: 10px;
  background: var(--folder-back-color);
  border-radius: 5px 5px 0 0;
}

/* Papers — stacked inside folder */
.paper {
  position: absolute; z-index: 2;
  bottom: 10%; left: 50%;
  transform: translate(-50%, 10%);
  width: 70%; height: 80%;
  border-radius: 10px;
  transition: all 0.3s ease-in-out;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
}
.paper-1 { background: var(--paper-1); }
.paper-2 {
  background: var(--paper-2);
  width: 80%; height: 70%;
}
.paper-3 {
  background: var(--paper-3);
  width: 90%; height: 60%;
}

.paper__label {
  font-size: 10px; font-weight: 500; color: var(--text-muted);
  text-align: center; line-height: 1.2; padding: 4px;
  overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}

/* Hover state (closed) */
.folder:not(.open):hover .paper {
  transform: translate(-50%, 0%);
}
.folder:not(.open):hover .folder__front {
  transform: skew(15deg) scaleY(0.6);
}

/* Open state — papers spread out */
.folder.open .paper-1 {
  transform: translate(calc(-120% + var(--magnet-x, 0px)), calc(-70% + var(--magnet-y, 0px))) rotateZ(-15deg);
}
.folder.open .paper-1:hover {
  transform: translate(calc(-120% + var(--magnet-x, 0px)), calc(-70% + var(--magnet-y, 0px))) rotateZ(-15deg) scale(1.1);
}
.folder.open .paper-2 {
  transform: translate(calc(5% + var(--magnet-x, 0px)), calc(-70% + var(--magnet-y, 0px))) rotateZ(15deg);
  height: 80%;
}
.folder.open .paper-2:hover {
  transform: translate(calc(5% + var(--magnet-x, 0px)), calc(-70% + var(--magnet-y, 0px))) rotateZ(15deg) scale(1.1);
}
.folder.open .paper-3 {
  transform: translate(calc(-50% + var(--magnet-x, 0px)), calc(-100% + var(--magnet-y, 0px))) rotateZ(5deg);
  height: 80%;
}
.folder.open .paper-3:hover {
  transform: translate(calc(-50% + var(--magnet-x, 0px)), calc(-100% + var(--magnet-y, 0px))) rotateZ(5deg) scale(1.1);
}

.folder.open .folder__front {
  transform: skew(15deg) scaleY(0.6);
}

/* Front flap — single piece, skews open */
.folder__front {
  position: absolute; z-index: 3;
  width: 100%; height: 100%;
  background: var(--folder-color);
  border-radius: 5px 10px 10px 10px;
  transform-origin: bottom;
  transition: all 0.3s ease-in-out;
}
</style>
