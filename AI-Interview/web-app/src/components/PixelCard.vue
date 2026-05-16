<template>
  <div
    ref="containerRef"
    class="pixel-card"
    :class="className"
  >
    <canvas ref="canvasRef" class="pixel-canvas" />
    <span class="pixel-card__content">
      <slot />
    </span>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'

interface Props {
  gap?: number
  dotRadius?: number
  colors?: string
  opacityMin?: number
  opacityMax?: number
  className?: string
}

const props = withDefaults(defineProps<Props>(), {
  gap: 14,
  dotRadius: 1.8,
  colors: '#f8fafc,#f1f5f9,#cbd5e1',
  opacityMin: 0.12,
  opacityMax: 0.5,
  className: ''
})

const containerRef = ref<HTMLElement>()
const canvasRef = ref<HTMLCanvasElement>()

interface Dot {
  x: number; y: number
  color: string
  baseAlpha: number
  jitter: number // random offset for organic spread
}
let dots: Dot[] = []
let observer: ResizeObserver | null = null
let rafId = 0
let cursorX = -9999
let cursorY = -9999
let hovering = false
let targetAlpha = 0
let currentAlpha = 0

// ===== Draw =====
const maxDist = 220 // wave expands 220px from cursor, covers most of card at peak
let resetFadeOut = false

function redraw() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')!
  const dpr = window.devicePixelRatio || 1

  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.save()
  ctx.scale(dpr, dpr)

  const r = props.dotRadius
  const spread = 60 // width of the expanding ring

  for (const dot of dots) {
    const dx = dot.x - cursorX
    const dy = dot.y - cursorY
    const dist = Math.sqrt(dx * dx + dy * dy)

    // Wave ring with per-dot jitter for organic irregular spread
    const effectiveDist = dist - dot.jitter * (1 - wavePhase) // jitter fades as wave expands
    const waveFront = wavePhase * maxDist
    const behind = waveFront - spread
    const ahead = waveFront

    let alpha = 0
    if (resetFadeOut) {
      alpha = 0
    } else if (effectiveDist <= behind) {
      alpha = props.opacityMax
    } else if (effectiveDist <= ahead) {
      const t = 1 - (effectiveDist - behind) / spread
      alpha = props.opacityMin + t * t * (props.opacityMax - props.opacityMin)
    }

    const smoothing = resetFadeOut ? 0.15 : 0.25
    dot.baseAlpha += (alpha - dot.baseAlpha) * smoothing

    if (dot.baseAlpha > 0.01) {
      ctx.fillStyle = dot.color
      ctx.globalAlpha = dot.baseAlpha
      ctx.beginPath()
      ctx.arc(dot.x, dot.y, r, 0, Math.PI * 2)
      ctx.fill()
    }
  }

  ctx.globalAlpha = 1
  ctx.restore()
}

// ===== Animation =====
let wavePhase = 0
let lastTime = 0

function animate(timestamp: number) {
  if (!lastTime) lastTime = timestamp
  const dt = Math.min(timestamp - lastTime, 50) // cap at 50ms
  lastTime = timestamp

  if (hovering) {
    // Advance wave: ~3s per cycle
    wavePhase += dt / 3000
    if (wavePhase >= 1) {
      wavePhase = 0
      // Briefly keep all dots dark before next cycle
      resetFadeOut = true
      setTimeout(() => { resetFadeOut = false }, 300)
    }
  } else {
    // Fade out when not hovering
    wavePhase += dt / 600
    if (wavePhase >= 1) wavePhase = 1
  }

  redraw()

  const allDark = dots.every(d => d.baseAlpha < 0.005)
  if (hovering || !allDark) {
    rafId = requestAnimationFrame(animate)
  } else {
    rafId = 0
    wavePhase = 0
  }
}

function startLoop() {
  if (rafId) return
  lastTime = 0
  rafId = requestAnimationFrame(animate)
}

function stopLoop() {
  cancelAnimationFrame(rafId)
  rafId = 0
}

// ===== Events =====
function onMouseMove(e: MouseEvent) {
  const rect = containerRef.value!.getBoundingClientRect()
  cursorX = e.clientX - rect.left
  cursorY = e.clientY - rect.top
}

function onMouseEnter(e: MouseEvent) {
  if (hovering) return // guard against repeated mouseover events
  const rect = containerRef.value!.getBoundingClientRect()
  cursorX = e.clientX - rect.left
  cursorY = e.clientY - rect.top
  hovering = true
  wavePhase = 0
  startLoop()
}

function onMouseLeave(e: MouseEvent) {
  const el = containerRef.value
  if (!el) { hovering = false; return }
  const to = (e as any).toElement || e.relatedTarget
  if (to && el.contains(to as Node)) return
  hovering = false
}

// ===== Init =====
function initDots() {
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
  const seed = Math.floor(Math.random() * 10000)
  dots = []

  for (let x = props.gap; x < w - props.gap; x += props.gap) {
    for (let y = props.gap; y < h - props.gap; y += props.gap) {
      const hash = ((x * 374761393 + y * 668265263 + seed) & 0x7fffffff) / 0x7fffffff
      dots.push({
        x, y,
        color: cols[Math.floor(hash * cols.length)],
        baseAlpha: 0,
        jitter: hash * 80 - 40 // ±40px random activation offset per dot
      })
    }
  }
  redraw()
}

onMounted(async () => {
  await nextTick()
  initDots()
  observer = new ResizeObserver(() => initDots())
  if (containerRef.value) observer.observe(containerRef.value)

  // Listen on the container div itself (events bubble up from children)
  const el = containerRef.value!
  el.addEventListener('mousemove', onMouseMove)
  el.addEventListener('mouseenter', onMouseEnter)
  el.addEventListener('mouseleave', onMouseLeave)

})

onUnmounted(() => {
  observer?.disconnect()
  stopLoop()
  const el = containerRef.value
  if (el) {
    el.removeEventListener('mousemove', onMouseMove)
    el.removeEventListener('mouseenter', onMouseEnter)
    el.removeEventListener('mouseleave', onMouseLeave)
  }
})
</script>

<style scoped>
.pixel-card {
  position: relative;
  display: inline-block;
  overflow: hidden;
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
