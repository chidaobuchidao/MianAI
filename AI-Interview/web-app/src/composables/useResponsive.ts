import { ref, computed, onMounted, onUnmounted } from 'vue'

/**
 * Reactive viewport width detection.
 * Provides isDesktop / isWide computed refs that update on window resize.
 *
 * Breakpoints (synced with tokens.css):
 *   >= 768px  → isDesktop
 *   >= 1024px → isWide
 */
export function useResponsive() {
  const width = ref(window.innerWidth)

  const isDesktop = computed(() => width.value >= 768)
  const isWide = computed(() => width.value >= 1024)

  function onResize() {
    width.value = window.innerWidth
  }

  onMounted(() => {
    window.addEventListener('resize', onResize)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', onResize)
  })

  return { width, isDesktop, isWide }
}
