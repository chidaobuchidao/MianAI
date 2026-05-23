import { ref, onUnmounted } from 'vue'

export function useStreamPolish() {
  const output = ref('')
  const isStreaming = ref(false)
  const error = ref('')
  let abortController: AbortController | null = null

  async function startStream(url: string, body: Record<string, unknown>) {
    output.value = ''
    error.value = ''
    isStreaming.value = true
    abortController = new AbortController()

    try {
      const token = localStorage.getItem('token') || ''
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify(body),
        signal: abortController.signal,
      })

      if (!response.ok) {
        error.value = `请求失败: ${response.status}`
        isStreaming.value = false
        return
      }

      const reader = response.body?.getReader()
      if (!reader) { isStreaming.value = false; return }

      const decoder = new TextDecoder()
      let buffer = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })

        // Parse SSE event stream
        const lines = buffer.split('\n')
        buffer = lines.pop() || '' // Keep incomplete line in buffer

        for (const line of lines) {
          if (line.startsWith('event:') || line.startsWith('id:') || line.startsWith('retry:')) {
            continue // skip SSE metadata lines
          }
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (data === 'finish' || data === '[DONE]') {
              break
            }
            if (data) {
              output.value += data
            }
          }
        }
      }
    } catch (e: unknown) {
      if (e instanceof DOMException && e.name === 'AbortError') return
      error.value = `流式连接中断: ${String(e)}`
    } finally {
      isStreaming.value = false
    }
  }

  function stopStream() {
    abortController?.abort()
    isStreaming.value = false
  }

  onUnmounted(() => stopStream())

  return { output, isStreaming, error, startStream, stopStream }
}
