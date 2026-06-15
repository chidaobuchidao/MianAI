<template>
  <div
    class="puzzle-captcha"
    :class="{
      'puzzle-captcha--verified': verified,
      'puzzle-captcha--failed': failed,
      'puzzle-captcha--dragging': dragging
    }"
  >
    <div class="puzzle-captcha__canvas-wrap" ref="canvasWrapRef">
      <canvas ref="canvasRef" class="puzzle-captcha__canvas"></canvas>
      <svg class="puzzle-captcha__target" :style="targetStyle" viewBox="0 0 44 44" aria-hidden="true">
        <path :d="PUZZLE_PATH" />
      </svg>
      <svg class="puzzle-captcha__piece" :style="pieceStyle" viewBox="0 0 44 44" aria-hidden="true">
        <defs>
          <linearGradient id="puzzlePieceFill" x1="0" x2="1" y1="0" y2="1">
            <stop offset="0%" stop-color="#fffaf2" />
            <stop offset="100%" stop-color="#d7c5ad" />
          </linearGradient>
        </defs>
        <path :d="PUZZLE_PATH" />
      </svg>
      <button class="puzzle-captcha__refresh" type="button" @click="resetPuzzle" aria-label="刷新验证图形">
        ↻
      </button>
    </div>

    <div class="puzzle-captcha__track" ref="trackRef">
      <div class="puzzle-captcha__progress" :style="{ width: progressPct + '%' }"></div>
      <div
        class="puzzle-captcha__handle"
        :class="{ 'puzzle-captcha__handle--dragging': dragging }"
        :style="{ left: handleLeft + 'px' }"
        @mousedown="startDrag"
        @touchstart.prevent="startDrag"
      >
        <span class="puzzle-captcha__arrow" v-if="!verified">→</span>
        <span class="puzzle-captcha__check" v-else>✓</span>
      </div>
      <span class="puzzle-captcha__track-text">{{ trackText }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'

const emit = defineEmits<{ verified: [] }>()

const verified = ref(false)
const dragging = ref(false)
const failed = ref(false)
const handleLeft = ref(0)
const pieceLeft = ref(0)
const pieceTop = ref(0)
const targetLeft = ref(0)
const targetTop = ref(0)

const canvasRef = ref<HTMLCanvasElement | null>(null)
const canvasWrapRef = ref<HTMLDivElement | null>(null)
const trackRef = ref<HTMLDivElement | null>(null)

let trackWidth = 0
let handleWidth = 0
let maxLeft = 0
let startX = 0
let startLeft = 0
const TOLERANCE = 5

const PUZZLE_PATH = [
  'M 8 0',
  'H 16',
  'C 16 5 19 8 22 8',
  'C 25 8 28 5 28 0',
  'H 36',
  'Q 44 0 44 8',
  'V 16',
  'C 39 16 36 19 36 22',
  'C 36 25 39 28 44 28',
  'V 36',
  'Q 44 44 36 44',
  'H 28',
  'C 28 39 25 36 22 36',
  'C 19 36 16 39 16 44',
  'H 8',
  'Q 0 44 0 36',
  'V 28',
  'C 5 28 8 25 8 22',
  'C 8 19 5 16 0 16',
  'V 8',
  'Q 0 0 8 0',
  'Z',
].join(' ')

const pieceStyle = computed(() => ({
  left: pieceLeft.value + 'px',
  top: pieceTop.value + 'px',
}))

const targetStyle = computed(() => ({
  left: targetLeft.value + 'px',
  top: targetTop.value + 'px',
}))

const trackText = computed(() => {
  if (verified.value) return '验证通过'
  if (failed.value) return '未对齐，请重试'
  if (dragging.value) return '松手完成验证'
  return '拖动滑块完成验证'
})

const progressPct = computed(() => {
  if (maxLeft === 0) return 0
  return Math.round((handleLeft.value / maxLeft) * 100)
})

// ============ Generate random puzzle ============
const PIECE_W = 44
const PIECE_H = 44

function randomInt(min: number, max: number) {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

function generatePuzzle() {
  if (!canvasRef.value || !canvasWrapRef.value) return

  const canvas = canvasRef.value
  const wrap = canvasWrapRef.value
  const wrapW = wrap.clientWidth
  const wrapH = wrap.clientHeight || 168
  canvas.width = wrapW
  canvas.height = wrapH

  const ctx = canvas.getContext('2d')!
  ctx.clearRect(0, 0, wrapW, wrapH)

  // Random position for the puzzle gap (avoid edges)
  initTrackSizes()

  const gapX = randomInt(Math.min(88, wrapW - PIECE_W - 40), wrapW - PIECE_W - 24)
  const gapY = randomInt(18, wrapH - PIECE_H - 18)
  targetLeft.value = gapX
  targetTop.value = gapY

  // Draw background pattern (geometric lines + dots)
  const bg = ctx.createLinearGradient(0, 0, wrapW, wrapH)
  bg.addColorStop(0, '#fbfaf7')
  bg.addColorStop(0.52, '#f1ece3')
  bg.addColorStop(1, '#e9dfd0')
  ctx.fillStyle = bg
  ctx.fillRect(0, 0, wrapW, wrapH)

  // Subtle dots pattern
  ctx.fillStyle = 'rgba(130, 103, 70, 0.13)'
  for (let x = 0; x < wrapW; x += 16) {
    for (let y = 0; y < wrapH; y += 16) {
      ctx.beginPath()
      ctx.arc(x + 8, y + 8, 1, 0, Math.PI * 2)
      ctx.fill()
    }
  }

  // Random line patterns
  ctx.strokeStyle = 'rgba(100, 72, 44, 0.12)'
  ctx.lineWidth = 1
  for (let i = 0; i < 7; i++) {
    ctx.beginPath()
    const x1 = randomInt(0, wrapW)
    const y1 = randomInt(0, wrapH)
    ctx.moveTo(x1, y1)
    ctx.lineTo(x1 + randomInt(-90, 90), y1 + randomInt(-48, 48))
    ctx.stroke()
  }

  // Draw the gap (white area where piece was removed)
  ctx.save()
  ctx.fillStyle = 'rgba(20, 20, 19, 0.10)'
  paintPuzzlePath(ctx, gapX, gapY, 'fill')
  // Inner shadow effect
  ctx.strokeStyle = 'rgba(255,255,255,0.65)'
  ctx.lineWidth = 2
  paintPuzzlePath(ctx, gapX, gapY, 'stroke')
  ctx.restore()

  pieceLeft.value = 8
  pieceTop.value = gapY
}

function paintPuzzlePath(ctx: CanvasRenderingContext2D, x: number, y: number, mode: 'fill' | 'stroke') {
  const path = new Path2D(PUZZLE_PATH)
  ctx.save()
  ctx.translate(x, y)
  if (mode === 'fill') {
    ctx.fill(path)
  } else {
    ctx.stroke(path)
  }
  ctx.restore()
}

// ============ Drag logic ============
function initTrackSizes() {
  if (!trackRef.value) return
  trackWidth = trackRef.value.offsetWidth
  const handle = trackRef.value.querySelector('.puzzle-captcha__handle') as HTMLElement
  handleWidth = handle ? handle.offsetWidth : 48
  maxLeft = trackWidth - handleWidth - 4
}

function startDrag(e: MouseEvent | TouchEvent) {
  if (verified.value) return
  failed.value = false
  dragging.value = true
  initTrackSizes()
  const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX
  startX = clientX
  startLeft = handleLeft.value
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', endDrag)
  document.addEventListener('touchmove', onDrag, { passive: false })
  document.addEventListener('touchend', endDrag)
}

function onDrag(e: MouseEvent | TouchEvent) {
  if (!dragging.value || verified.value) return
  const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX
  const delta = clientX - startX
  let newLeft = startLeft + delta
  newLeft = Math.max(0, Math.min(newLeft, maxLeft))
  handleLeft.value = newLeft

  pieceLeft.value = newLeft
}

function endDrag() {
  dragging.value = false
  if (verified.value) {
    cleanupListeners()
    return
  }

  // Check if piece is aligned within tolerance
  if (trackWidth > 0) {
    const expectedPieceX = targetLeft.value
    if (Math.abs(pieceLeft.value - expectedPieceX) <= TOLERANCE) {
      verified.value = true
      failed.value = false
      handleLeft.value = targetLeft.value
      pieceLeft.value = targetLeft.value
      emit('verified')
    } else {
      // Animate back to start
      failed.value = true
      handleLeft.value = 0
      pieceLeft.value = 8
    }
  }

  cleanupListeners()
}

function resetPuzzle() {
  verified.value = false
  failed.value = false
  handleLeft.value = 0
  pieceLeft.value = 8
  generatePuzzle()
}

function cleanupListeners() {
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', endDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', endDrag)
}

onMounted(() => {
  nextTick(() => {
    generatePuzzle()
    window.addEventListener('resize', generatePuzzle)
  })
})

onUnmounted(() => {
  cleanupListeners()
  window.removeEventListener('resize', generatePuzzle)
})
</script>

<style scoped>
.puzzle-captcha {
  user-select: none;
  width: 100%;
}

.puzzle-captcha__canvas-wrap {
  position: relative;
  width: 100%;
  height: 168px;
  background: #f6f2ea;
  border: 1px solid rgba(20, 20, 19, 0.08);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.70);
}
.puzzle-captcha__canvas {
  display: block;
  width: 100%;
  height: 100%;
}
.puzzle-captcha__target {
  position: absolute;
  width: 44px;
  height: 44px;
  overflow: visible;
  filter: drop-shadow(0 1px 1px rgba(20, 20, 19, 0.10));
  pointer-events: none;
  opacity: 0.92;
}
.puzzle-captcha__target path {
  fill: rgba(20, 20, 19, 0.10);
  stroke: rgba(255,255,255,0.72);
  stroke-width: 2;
}
.puzzle-captcha__refresh {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 30px;
  height: 30px;
  border: 1px solid rgba(20, 20, 19, 0.08);
  border-radius: 999px;
  background: rgba(255,255,255,0.72);
  color: rgba(20, 20, 19, 0.58);
  font-size: 17px;
  line-height: 1;
  cursor: pointer;
  backdrop-filter: blur(10px);
  transition: transform 0.18s ease, color 0.18s ease, background 0.18s ease;
}
.puzzle-captcha__refresh:hover {
  color: #141413;
  background: rgba(255,255,255,0.95);
  transform: rotate(35deg);
}

.puzzle-captcha__track {
  position: relative;
  height: 48px;
  margin-top: 12px;
  background: #f3f0ea;
  border: 1px solid rgba(20, 20, 19, 0.08);
  border-radius: 999px;
  overflow: hidden;
  display: flex;
  align-items: center;
}
.puzzle-captcha--verified .puzzle-captcha__track {
  background: #eef8ee;
  border-color: rgba(34, 197, 94, 0.24);
}
.puzzle-captcha--failed .puzzle-captcha__track {
  background: #fff3ef;
  border-color: rgba(239, 68, 68, 0.22);
}
.puzzle-captcha__progress {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background: linear-gradient(90deg, rgba(217, 117, 10, 0.22), rgba(217, 117, 10, 0.08));
  border-radius: inherit;
  transition: width 0.18s ease;
}
.puzzle-captcha--verified .puzzle-captcha__progress {
  background: linear-gradient(90deg, rgba(34, 197, 94, 0.30), rgba(34, 197, 94, 0.12));
}

.puzzle-captcha__handle {
  position: absolute;
  top: 4px;
  left: 4px;
  width: 44px;
  height: 40px;
  background: #fff;
  border: 1px solid rgba(20, 20, 19, 0.10);
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  z-index: 3;
  box-shadow: 0 8px 18px rgba(20, 20, 19, 0.12);
  transition: box-shadow 0.18s ease, background 0.18s ease, transform 0.18s ease;
}
.puzzle-captcha__handle--dragging {
  box-shadow: 0 12px 24px rgba(20, 20, 19, 0.16);
  cursor: grabbing;
  transform: scale(1.03);
}
.puzzle-captcha--verified .puzzle-captcha__handle {
  cursor: default;
  background: #22c55e;
  border-color: #22c55e;
}

.puzzle-captcha__piece {
  position: absolute;
  width: 44px;
  height: 44px;
  overflow: visible;
  filter:
    drop-shadow(0 10px 14px rgba(20, 20, 19, 0.13))
    drop-shadow(0 1px 0 rgba(255,255,255,0.70));
  z-index: 10;
  pointer-events: none;
  transition: left 0.18s ease;
}
.puzzle-captcha__piece path {
  fill: url("#puzzlePieceFill");
  stroke: rgba(84, 62, 40, 0.20);
  stroke-width: 1.2;
}
.puzzle-captcha--dragging .puzzle-captcha__piece {
  transition: none;
}
.puzzle-captcha--verified .puzzle-captcha__piece {
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.18), 0 12px 22px rgba(20, 20, 19, 0.10);
}

.puzzle-captcha__arrow {
  font-size: 18px;
  color: #7c6b58;
}
.puzzle-captcha__check {
  font-size: 18px;
  color: #fff;
}
.puzzle-captcha__track-text {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding-left: 46px;
  padding-right: 16px;
  color: rgba(20, 20, 19, 0.46);
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0;
  pointer-events: none;
}
.puzzle-captcha--verified .puzzle-captcha__track-text {
  color: rgba(22, 101, 52, 0.72);
}
.puzzle-captcha--failed .puzzle-captcha__track-text {
  color: rgba(185, 28, 28, 0.72);
}
</style>
