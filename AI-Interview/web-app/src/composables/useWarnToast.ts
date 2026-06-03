import { ref, onUnmounted } from 'vue'

export function useWarnToast(duration = 5000) {
  const warnToast = ref('')
  let warnTimer: ReturnType<typeof setTimeout> | null = null

  function showWarn(msg: string) {
    warnToast.value = msg
    if (warnTimer) clearTimeout(warnTimer)
    warnTimer = setTimeout(() => { warnToast.value = '' }, duration)
  }

  onUnmounted(() => {
    if (warnTimer) {
      clearTimeout(warnTimer)
      warnTimer = null
    }
  })

  return { warnToast, showWarn }
}

export type UseWarnToastReturn = ReturnType<typeof useWarnToast>
