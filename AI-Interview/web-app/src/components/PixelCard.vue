<template>
  <div ref="containerRef" class="pixel-card" :class="className" @mouseenter="onEnter" @mouseleave="onLeave">
    <canvas ref="canvasRef" class="pixel-canvas" />
    <span class="pixel-card__content"><slot /></span>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'

const props = withDefaults(defineProps<{
  gap?: number
  speed?: number
  colors?: string
  noFocus?: boolean
  className?: string
}>(), {
  gap: 5,
  speed: 35,
  colors: '#f8fafc,#f1f5f9,#cbd5e1',
  noFocus: false,
  className: ''
})

const containerRef = ref<HTMLElement>()
const canvasRef = ref<HTMLCanvasElement>()

interface Pixel {
  x: number; y: number; color: string; baseSpeed: number; delay: number
  size: number; sizeStep: number; minSize: number; maxSize: number; maxSizeInt: number
  counter: number; counterStep: number
  isIdle: boolean; isReverse: boolean; isShimmer: boolean
}

let pixels: Pixel[] = []
let rafId = 0
let timePrev = 0
let observer: ResizeObserver | null = null
let hovering = false
const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

function getEffectiveSpeed(val: number): number {
  if (val <= 0 || reducedMotion) return 0
  if (val >= 100) return 100 * 0.001
  return val * 0.001
}

function init() {
  const container = containerRef.value
  const canvas = canvasRef.value
  if (!container || !canvas) return

  const rect = container.getBoundingClientRect()
  const w = Math.floor(rect.width)
  const h = Math.floor(rect.height)
  const dpr = window.devicePixelRatio || 1

  canvas.width = w * dpr
  canvas.height = h * dpr
  canvas.style.width = `${w}px`
  canvas.style.height = `${h}px`

  const cols = props.colors.split(',')
  const gap = props.gap
  const speed = getEffectiveSpeed(props.speed)
  const maxSizeInt = 2

  pixels = []
  for (let x = 0; x < w; x += gap) {
    for (let y = 0; y < h; y += gap) {
      const dx = x - w / 2, dy = y - h / 2
      const dist = Math.sqrt(dx * dx + dy * dy)
      pixels.push({
        x, y,
        color: cols[Math.floor(Math.random() * cols.length)],
        baseSpeed: (Math.random() * 0.8 + 0.1) * speed,
        delay: reducedMotion ? 0 : dist,
        size: 0,
        sizeStep: Math.random() * 0.4,
        minSize: 0.5,
        maxSize: Math.random() * (maxSizeInt - 0.5) + 0.5,
        maxSizeInt,
        counter: 0,
        counterStep: Math.random() * 4 + (w + h) * 0.01,
        isIdle: true, isReverse: false, isShimmer: false
      })
    }
  }
}

function tick(fn: 'appear' | 'disappear') {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')!
  const dpr = window.devicePixelRatio || 1

  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.save()
  ctx.scale(dpr, dpr)

  let allIdle = true
  for (const px of pixels) {
    if (fn === 'appear') {
      px.isIdle = false
      if (px.counter <= px.delay) { px.counter += px.counterStep; allIdle = false; continue }
      if (px.size >= px.maxSize) px.isShimmer = true
      if (px.isShimmer) {
        if (px.size >= px.maxSize) px.isReverse = true
        else if (px.size <= px.minSize) px.isReverse = false
        px.size += px.isReverse ? -px.baseSpeed : px.baseSpeed
      } else {
        px.size += px.sizeStep
      }
    } else {
      px.isShimmer = false; px.counter = 0
      if (px.size <= 0) { px.isIdle = true; continue }
      px.size -= 0.1
    }

    if (px.size > 0.01) {
      const centerOffset = px.maxSizeInt * 0.5 - px.size * 0.5
      ctx.fillStyle = px.color
      ctx.fillRect(px.x + centerOffset, px.y + centerOffset, px.size, px.size)
    }
    allIdle = false
  }

  ctx.restore()

  if (allIdle) {
    cancelAnimationFrame(rafId)
    rafId = 0
  }
}

function loop(fn: 'appear' | 'disappear') {
  cancelAnimationFrame(rafId)
  const frame = (ts: number) => {
    if (!timePrev) timePrev = ts
    const dt = ts - timePrev
    if (dt < 1000 / 60) { rafId = requestAnimationFrame(frame); return }
    timePrev = ts - (dt % (1000 / 60))
    tick(fn)
    rafId = requestAnimationFrame(frame)
  }
  rafId = requestAnimationFrame(frame)
}

function onEnter() { hovering = true; loop('appear') }
function onLeave() { hovering = false; loop('disappear') }

onMounted(async () => {
  await nextTick()
  init()
  observer = new ResizeObserver(() => init())
  if (containerRef.value) observer.observe(containerRef.value)
})

onUnmounted(() => {
  observer?.disconnect()
  cancelAnimationFrame(rafId)
})
</script>

<style scoped>
.pixel-card {
  position: relative;
  display: inline-block;
  overflow: hidden;
  isolation: isolate;
}

.pixel-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.pixel-card__content {
  position: relative;
  z-index: 1;
  pointer-events: auto;
}
</style>
