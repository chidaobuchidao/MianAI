<template>
  <component
    :is="tag"
    ref="textRef"
    class="split-text"
    :class="{ 'split-text--animated': revealed }"
    :style="{ textAlign }"
  >
    {{ text }}
  </component>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useSplitReveal } from '@/composables/useScrollReveal'

interface Props {
  text: string
  tag?: string
  textAlign?: 'left' | 'center' | 'right'
  delay?: number
  duration?: number
  ease?: string
  y?: number
  stagger?: number
}

const props = withDefaults(defineProps<Props>(), {
  tag: 'p',
  textAlign: 'center',
  duration: 1.2,
  ease: 'power3.out',
  y: 40,
  stagger: 0.03
})

const textRef = ref<HTMLElement>()
const revealed = ref(false)

useSplitReveal(textRef, {
  y: props.y,
  duration: props.duration,
  stagger: props.stagger,
  ease: props.ease
})

// Mark as revealed once GSAP runs (handled in composable)
</script>

<style scoped>
.split-text {
  overflow: hidden;
  will-change: transform, opacity;
}

.split-text :deep(span) {
  display: inline-block;
}
</style>
