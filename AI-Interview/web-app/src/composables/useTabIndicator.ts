import { ref, onMounted, onUnmounted, watch, nextTick, type Ref } from 'vue'

export function useTabIndicator(activeValue: Ref<string>) {
  const tabRefs: Record<string, HTMLElement | null> = {}
  const tabIndicator = ref<HTMLElement | null>(null)
  let resizeObs: ResizeObserver | null = null

  function moveIndicator(value?: string) {
    const val = value || activeValue.value
    const indicator = tabIndicator.value
    const tab = tabRefs[val]
    if (!indicator || !tab) return
    const parent = indicator.parentElement
    if (!parent) return
    const pr = parent.getBoundingClientRect()
    const tr = tab.getBoundingClientRect()
    indicator.style.width = `${tr.width}px`
    indicator.style.transition = 'all 0.4s var(--spring-bounce)'
    indicator.style.transform = `translateX(${tr.left - pr.left}px)`
  }

  function setTab(value: string) {
    activeValue.value = value
    nextTick(() => moveIndicator(value))
  }

  function setTabRef(key: string, el: unknown) {
    tabRefs[key] = el as HTMLElement | null
  }

  onMounted(() => {
    nextTick(() => moveIndicator())
    resizeObs = new ResizeObserver(() => moveIndicator())
    const parent = tabIndicator.value?.parentElement
    if (parent) resizeObs.observe(parent)
  })

  onUnmounted(() => {
    resizeObs?.disconnect()
  })

  watch(activeValue, () => nextTick(() => moveIndicator()))

  return { tabRefs, tabIndicator, moveIndicator, setTab, setTabRef }
}

export type UseTabIndicatorReturn = ReturnType<typeof useTabIndicator>
