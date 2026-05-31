<template>
  <div
    class="glare-card"
    :class="{
      'glare-card--hover': isHovering,
      'glare-card--play-once': playOnce,
      'glare-card--played': sweepPlayed
    }"
    :style="{ ...cardVars, '--mouse-x': mouseX, '--mouse-y': mouseY }"
    @mousemove="onMouseMove"
    @mouseenter="onMouseEnter"
    @mouseleave="onMouseLeave"
  >
    <!-- Sweep glare beam (diagonal) -->
    <span class="glare-card__sweep" :style="sweepStyle" />
    <!-- Radial mouse-follow shine -->
    <span class="glare-card__shine" :style="shineStyle" />
    <!-- Animated border glow -->
    <span class="glare-card__border-glow" :style="borderGlowStyle" />
    <!-- Content -->
    <span class="glare-card__content">
      <slot />
    </span>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface Props {
  width?: string
  height?: string
  background?: string
  borderRadius?: string
  borderColor?: string
  glareColor?: string
  glareOpacity?: number
  glareAngle?: number
  glareSize?: number
  transitionDuration?: number
  playOnce?: boolean
  tiltAmount?: number
}

const props = withDefaults(defineProps<Props>(), {
  width: '100%',
  height: 'auto',
  background: '#141413',
  borderRadius: '20px',
  borderColor: 'rgba(255,255,255,0.12)',
  glareColor: '#ffffff',
  glareOpacity: 0.5,
  glareAngle: -45,
  glareSize: 250,
  transitionDuration: 650,
  playOnce: false,
  tiltAmount: 8
})

const isHovering = ref(false)
const sweepPlayed = ref(false)
const mouseX = ref(0.5)
const mouseY = ref(0.5)

function onMouseMove(e: MouseEvent) {
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  mouseX.value = (e.clientX - rect.left) / rect.width
  mouseY.value = (e.clientY - rect.top) / rect.height
}

function onMouseEnter() {
  isHovering.value = true
  if (props.playOnce) {
    sweepPlayed.value = true
  }
}

function onMouseLeave() {
  isHovering.value = false
  if (!props.playOnce) {
    sweepPlayed.value = false
  }
}

function hexToRgba(hex: string, opacity: number): string {
  const clean = hex.replace('#', '')
  if (/^[0-9A-Fa-f]{6}$/.test(clean)) {
    const r = parseInt(clean.slice(0, 2), 16)
    const g = parseInt(clean.slice(2, 4), 16)
    const b = parseInt(clean.slice(4, 6), 16)
    return `rgba(${r}, ${g}, ${b}, ${opacity})`
  }
  if (/^[0-9A-Fa-f]{3}$/.test(clean)) {
    const r = parseInt(clean[0] + clean[0], 16)
    const g = parseInt(clean[1] + clean[1], 16)
    const b = parseInt(clean[2] + clean[2], 16)
    return `rgba(${r}, ${g}, ${b}, ${opacity})`
  }
  return hex
}

const glareRgba = computed(() => hexToRgba(props.glareColor, props.glareOpacity))

const cardVars = computed(() => ({
  width: props.width,
  height: props.height,
  background: props.background,
  borderRadius: props.borderRadius,
  '--gh-angle': `${props.glareAngle}deg`,
  '--gh-duration': `${props.transitionDuration}ms`,
  '--gh-size': `${props.glareSize}%`,
  '--gh-rgba': glareRgba.value,
  '--gh-border': props.borderColor,
  '--gh-tilt': `${props.tiltAmount}px`
}))

const shineStyle = computed(() => ({
  background: `radial-gradient(circle ${props.glareSize}px at ${mouseX.value * 100}% ${mouseY.value * 100}%, ${glareRgba.value} 0%, transparent 100%)`,
  opacity: isHovering.value ? 1 : 0,
  transition: `opacity ${props.transitionDuration}ms var(--ease-out-expo)`
}))

const sweepStyle = computed(() => ({
  background: `linear-gradient(var(--gh-angle), transparent 0%, ${glareRgba.value} 45%, ${glareRgba.value} 55%, transparent 100%)`,
  opacity: sweepPlayed.value ? 1 : 0
}))

const borderGlowStyle = computed(() => ({
  background: `radial-gradient(circle 300px at ${mouseX.value * 100}% ${mouseY.value * 100}%, ${props.borderColor} 0%, transparent 70%)`,
  opacity: isHovering.value ? 1 : 0,
  transition: `opacity ${props.transitionDuration}ms var(--ease-out-expo)`
}))
</script>

<style scoped>
.glare-card {
  position: relative;
  overflow: hidden;
  transform: translateZ(0);
  will-change: transform;
  cursor: pointer;
}

/* ===== Sweep Glare Beam ===== */
.glare-card__sweep {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  border-radius: inherit;
  opacity: 0;
  transform: translateX(-120%);
}

.glare-card--hover .glare-card__sweep,
.glare-card--play-once.glare-card--played .glare-card__sweep {
  animation: glare-sweep var(--gh-duration) var(--ease-out-expo) forwards;
}

@keyframes glare-sweep {
  0% {
    transform: translateX(-120%);
    opacity: 0;
  }
  20% {
    opacity: 1;
  }
  80% {
    opacity: 1;
  }
  100% {
    transform: translateX(120%);
    opacity: 0;
  }
}

/* ===== Radial Mouse-Follow Shine ===== */
.glare-card__shine {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  border-radius: inherit;
}

/* ===== Border Glow ===== */
.glare-card__border-glow {
  position: absolute;
  inset: -1px;
  z-index: 0;
  pointer-events: none;
  border-radius: inherit;
  opacity: 0;
}

/* ===== Content ===== */
.glare-card__content {
  position: relative;
  z-index: 3;
  height: 100%;
  display: block;
}

/* ===== Tilt Effect (desktop only) ===== */
@media (min-width: 769px) {
  .glare-card--hover {
    transition: transform 0.4s var(--ease-out-expo);
    transform: perspective(1200px)
      rotateX(calc((0.5 - var(--mouse-y, 0.5)) * 8deg))
      rotateY(calc((var(--mouse-x, 0.5) - 0.5) * 8deg))
      scale(1.02);
  }
}
</style>
