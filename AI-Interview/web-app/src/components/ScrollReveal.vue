<template>
  <div
    ref="containerRef"
    class="scroll-reveal"
    :style="{ '--sr-delay': `${delay}ms`, '--sr-duration': `${duration}ms`, '--sr-stagger': `${stagger}s` }"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useScrollReveal } from '@/composables/useScrollReveal'

interface Props {
  y?: number
  x?: number
  opacity?: number
  scale?: number
  duration?: number
  delay?: number
  stagger?: number
  ease?: string
  start?: string
  once?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  y: 30,
  x: 0,
  opacity: 0,
  scale: 1,
  duration: 0.8,
  delay: 0,
  stagger: 0.08,
  ease: 'power3.out',
  start: 'top 85%',
  once: true
})

const containerRef = ref<HTMLElement>()

useScrollReveal(containerRef, {
  y: props.y,
  x: props.x,
  opacity: props.opacity,
  scale: props.scale,
  duration: props.duration,
  delay: props.delay,
  stagger: props.stagger,
  ease: props.ease,
  start: props.start,
  once: props.once
})
</script>

<style scoped>
.scroll-reveal {
  will-change: transform, opacity;
}

/* CSS-only fallback when JS/GSAP not available */
@supports (animation-timeline: view()) {
  .scroll-reveal > * {
    animation: sr-enter var(--sr-duration) var(--ease-out-expo) forwards;
    animation-timeline: view();
    animation-range: entry 0% entry 100%;
    animation-delay: calc(var(--sr-stagger) * var(--child-index, 0));
  }
}
</style>
