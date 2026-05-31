<template>
  <span ref="containerRef" class="decrypt-wrapper" :style="{ display: 'inline-block', whiteSpace: 'pre-wrap' }" v-bind="triggerProps">
    <span class="sr-only">{{ displayText }}</span>
    <span aria-hidden="true">
      <span
        v-for="(char, idx) in displayChars"
        :key="idx"
        :class="isRevealed(idx) ? '' : 'decrypt-char'"
      >{{ char }}</span>
    </span>
  </span>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

interface Props {
  text: string
  speed?: number
  maxIterations?: number
  sequential?: boolean
  revealDirection?: 'start' | 'end' | 'center'
  characters?: string
  animateOn?: 'view' | 'hover' | 'click'
  clickMode?: 'once' | 'toggle'
  delay?: number
}

const props = withDefaults(defineProps<Props>(), {
  speed: 50,
  maxIterations: 10,
  sequential: true,
  revealDirection: 'center',
  characters: 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!@#$%^&*()_+',
  animateOn: 'view',
  clickMode: 'once',
  delay: 0
})

const containerRef = ref<HTMLElement>()
const displayText = ref(props.text)
const displayChars = computed(() => displayText.value.split(''))
const revealedIndices = ref<Set<number>>(new Set())
const isAnimating = ref(false)
const isDecrypted = ref(props.animateOn !== 'click')
const hasAnimated = ref(false)
const direction = ref<'forward' | 'reverse'>('forward')

let intervalId: ReturnType<typeof setInterval> | null = null

const availableChars = computed(() => {
  return props.characters.split('')
})

function shuffleText(original: string, revealed: Set<number>): string {
  return original.split('').map((char, i) => {
    if (char === ' ') return ' '
    if (revealed.has(i)) return original[i]
    return availableChars.value[Math.floor(Math.random() * availableChars.value.length)]
  }).join('')
}

function isRevealed(idx: number): boolean {
  return revealedIndices.value.has(idx) || (!isAnimating.value && isDecrypted.value)
}

function computeOrder(len: number): number[] {
  if (len <= 0) return []
  if (props.revealDirection === 'start') {
    return Array.from({ length: len }, (_, i) => i)
  }
  if (props.revealDirection === 'end') {
    return Array.from({ length: len }, (_, i) => len - 1 - i)
  }
  // center
  const order: number[] = []
  const middle = Math.floor(len / 2)
  let offset = 0
  while (order.length < len) {
    if (offset % 2 === 0) {
      const idx = middle + offset / 2
      if (idx >= 0 && idx < len) order.push(idx)
    } else {
      const idx = middle - Math.ceil(offset / 2)
      if (idx >= 0 && idx < len) order.push(idx)
    }
    offset++
  }
  return order.slice(0, len)
}

function startDecrypt() {
  if (isAnimating.value) return
  if (props.animateOn === 'click' && props.clickMode === 'once' && isDecrypted.value) return

  const doStart = () => {
    if (isAnimating.value) return
    direction.value = 'forward'
    revealedIndices.value = new Set()
    displayText.value = shuffleText(props.text, new Set())
    isAnimating.value = true

    const order = props.sequential ? computeOrder(props.text.length) : []
    let pointer = 0
    let iteration = 0

    intervalId = setInterval(() => {
    if (props.sequential) {
      if (pointer < order.length) {
        const next = new Set(revealedIndices.value)
        next.add(order[pointer++])
        revealedIndices.value = next
        displayText.value = shuffleText(props.text, next)
      } else {
        clearInterval(intervalId!)
        isAnimating.value = false
        isDecrypted.value = true
        displayText.value = props.text
      }
    } else {
      iteration++
      if (iteration >= props.maxIterations) {
        clearInterval(intervalId!)
        isAnimating.value = false
        isDecrypted.value = true
        displayText.value = props.text
      } else {
        displayText.value = shuffleText(props.text, revealedIndices.value)
      }
    }
  }, props.speed)
  }

  if (props.delay > 0) {
    setTimeout(doStart, props.delay)
  } else {
    doStart()
  }
}

function resetToPlain() {
  if (intervalId) clearInterval(intervalId)
  isAnimating.value = false
  revealedIndices.value = new Set()
  displayText.value = props.text
  isDecrypted.value = true
}

function encryptInstantly() {
  revealedIndices.value = new Set()
  displayText.value = shuffleText(props.text, new Set())
  isDecrypted.value = false
}

const triggerProps = computed(() => {
  if (props.animateOn === 'hover') {
    return {
      onMouseenter: startDecrypt,
      onMouseleave: resetToPlain
    }
  }
  if (props.animateOn === 'click') {
    return { onClick: startDecrypt }
  }
  return {}
})

// Initial state
onMounted(() => {
  if (props.animateOn === 'view') {
    // Start scrambled, wait for scroll
    encryptInstantly()
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting && !hasAnimated.value) {
            hasAnimated.value = true
            startDecrypt()
          }
        })
      },
      { threshold: 0.1 }
    )
    const el = containerRef.value
    if (el) observer.observe(el)
    onUnmounted(() => { if (el) observer.unobserve(el) })
  } else if (props.animateOn === 'click') {
    encryptInstantly()
  } else {
    displayText.value = props.text
    isDecrypted.value = true
  }
})

onUnmounted(() => {
  if (intervalId) clearInterval(intervalId)
})
</script>

<style scoped>
.sr-only {
  position: absolute;
  width: 1px; height: 1px;
  padding: 0; margin: -1px;
  overflow: hidden; clip: rect(0, 0, 0, 0);
  border: 0;
}

.decrypt-char {
  color: var(--text-light);
  font-family: var(--font-mono);
  opacity: 0.35;
}
</style>
