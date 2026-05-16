import { ref, nextTick, type Ref } from 'vue'
import { post } from '@/utils/request'
import { sanitizeEndMarker, fixJsonString, type DimItem } from '@/utils/sanitize'

export interface InterviewMessage {
  role: 'ai' | 'user'
  content: string
  code?: string
  codeFile?: string
  codeLang?: string
}

export interface ReportData {
  score?: number
  feedback?: string
  dimensions?: DimItem[]
  suggestion?: string
}

interface UseInterviewStreamOptions {
  sessionId: Ref<number>
  messages: Ref<InterviewMessage[]>
  loading: Ref<boolean>
  onFinish: (data: ReportData) => void
}

interface SSEParseResult {
  buffer: string
  currentEvent: string
}

/**
 * Parses a raw SSE buffer, calling the handler for each complete event.
 * Returns the remaining buffer and current event for the next iteration.
 */
function parseSSEBuffer(
  buffer: string,
  currentEvent: string,
  handler: (event: string, data: string) => void
): SSEParseResult {
  const parts = buffer.split('\n\n')
  const remaining = parts.pop() || ''

  for (const part of parts) {
    const lines = part.split('\n')
    for (const line of lines) {
      if (line.startsWith('event:')) {
        currentEvent = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        handler(currentEvent, line.slice(5).trim())
      }
    }
  }

  return { buffer: remaining, currentEvent }
}

export function useInterviewStream(options: UseInterviewStreamOptions) {
  const { sessionId, messages, loading, onFinish } = options
  const reportScore = ref(0)

  function buildFinishData(json: Record<string, unknown>): ReportData {
    return {
      score: json.score != null ? Number(json.score) : undefined,
      feedback: typeof json.feedback === 'string' ? json.feedback : '',
      dimensions: Array.isArray(json.dimensions) ? (json.dimensions as DimItem[]) : undefined,
      suggestion: typeof json.suggestion === 'string' ? json.suggestion : undefined
    }
  }

  /**
   * Try to parse [面试结束] inline marker from accumulated AI content.
   * Returns parsed report data if the marker is found, null otherwise.
   */
  function tryParseEndMarker(content: string): { data: ReportData; cleanContent: string } | null {
    if (!content.includes('[面试结束]')) return null

    const match = content.match(/\[面试结束\]\s*(\{.*\})/s)
    if (!match) return null

    let json: Record<string, unknown> = {}
    try {
      json = JSON.parse(fixJsonString(match[1]))
    } catch {
      /* AI output may have malformed JSON */
    }

    if (json.score != null) {
      reportScore.value = Number(json.score)
    }

    return {
      data: buildFinishData(json),
      cleanContent: sanitizeEndMarker(content)
    }
  }

  function updateMessage(idx: number, content: string): void {
    const updated = [...messages.value]
    updated[idx] = { ...updated[idx], role: 'ai', content }
    messages.value = updated
  }

  async function sendAnswer(text: string): Promise<void> {
    if (!text || loading.value) return

    messages.value = [...messages.value, { role: 'user', content: text }]
    loading.value = true

    try {
      const token = localStorage.getItem('token') || ''
      const response = await fetch(`/api/interview/${sessionId.value}/answer/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ answer: text })
      })

      const reader = response.body?.getReader()
      if (!reader) throw new Error('No stream')

      const decoder = new TextDecoder()

      // Mutable stream state (read/written by SSE handler)
      const streamState = {
        buffer: '',
        currentEvent: '',
        aiContent: '',
        endDetected: false
      }

      // Add placeholder AI message for progressive rendering
      const aiMsgIdx = messages.value.length
      messages.value = [...messages.value, { role: 'ai', content: '' }]

      function handleSSEEvent(event: string, data: string): void {
        if (streamState.endDetected) return

        if (event === 'token') {
          streamState.aiContent += data

          const endResult = tryParseEndMarker(streamState.aiContent)
          if (endResult) {
            updateMessage(aiMsgIdx, endResult.cleanContent || '面试已结束')
            loading.value = false
            post(`/api/interview/${sessionId.value}/end`).catch(() => {})
            onFinish(endResult.data)
            streamState.endDetected = true
            return
          }

          const display = streamState.aiContent.replace(/\[面试结束\].*/s, '').trim()
          updateMessage(aiMsgIdx, display)
        } else if (event === 'finish') {
          try {
            const json = JSON.parse(data)
            if (json.report?.score != null) {
              reportScore.value = Number(json.report.score)
            }
            if (json.finished) {
              loading.value = false
              post(`/api/interview/${sessionId.value}/end`).catch(() => {})
              updateMessage(aiMsgIdx, sanitizeEndMarker(streamState.aiContent) || '面试已结束')

              const finishData: ReportData = json.report
                ? buildFinishData(json.report)
                : {}
              onFinish(finishData)
              streamState.endDetected = true
            }
          } catch {
            /* ignore malformed JSON */
          }
        } else if (event === 'error') {
          messages.value = [...messages.value, { role: 'ai', content: '⚠️ ' + data }]
        }
      }

      // Main read loop
      while (true) {
        if (streamState.endDetected) break

        const { done, value } = await reader.read()
        if (done) break

        streamState.buffer += decoder.decode(value, { stream: true })

        const result = parseSSEBuffer(
          streamState.buffer,
          streamState.currentEvent,
          handleSSEEvent
        )
        streamState.buffer = result.buffer
        streamState.currentEvent = result.currentEvent

        await nextTick()
      }

      // Flush remaining buffer
      if (!streamState.endDetected) {
        const result = parseSSEBuffer(
          streamState.buffer,
          streamState.currentEvent,
          handleSSEEvent
        )
        streamState.buffer = result.buffer
        streamState.currentEvent = result.currentEvent
      }
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : '请求失败'
      messages.value = [...messages.value, { role: 'ai', content: '抱歉，请求出错了：' + msg }]
    } finally {
      loading.value = false
    }
  }

  return { sendAnswer, reportScore }
}
