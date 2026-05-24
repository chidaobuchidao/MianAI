import { ref, nextTick, type Ref } from 'vue'
import { sanitizeEndMarker, fixJsonString, extractBalancedJson, type DimItem } from '@/utils/sanitize'

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

export interface CodeProblem {
  type: 'complete' | 'algorithm'
  title: string
  description: string
  template: string
  language: string
  templates?: Record<string, string>
  testCases?: TestCase[]
}

export interface TestCase {
  input: string
  expected: string
}

export interface CodingReview {
  score: number
  feedback: string
  dimensions?: DimItem[]
  suggestion?: string
}

interface UseInterviewStreamOptions {
  sessionId: Ref<number>
  messages: Ref<InterviewMessage[]>
  loading: Ref<boolean>
  onFinish: (data: ReportData) => void
  onCodeProblem?: (data: CodeProblem) => void
  onCodingInvite?: () => void
  onCodingFinish?: (data: CodingReview) => void
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
  const { sessionId, messages, loading, onFinish, onCodeProblem, onCodingInvite, onCodingFinish } = options
  const reportScore = ref(0)

  let codingReviewSeen = false

  function buildFinishData(json: Record<string, unknown>): ReportData {
    return {
      score: json.score != null ? Number(json.score) : undefined,
      feedback: typeof json.feedback === 'string' ? json.feedback : '',
      dimensions: Array.isArray(json.dimensions) ? (json.dimensions as DimItem[]) : undefined,
      suggestion: typeof json.suggestion === 'string' ? json.suggestion : undefined
    }
  }

  /**
   * Generic marker parser: locate a marker label, extract balanced JSON after it,
   * then delegate to `build` to construct the result. Returns null if parsing fails.
   */
  function tryParseMarker<T>(
    content: string,
    marker: string,
    build: (json: Record<string, unknown>) => T | null
  ): { data: T; cleanContent: string } | null {
    const idx = content.indexOf(marker)
    if (idx === -1) return null
    const result = extractBalancedJson(content, idx + marker.length)
    if (!result) return null
    try {
      const json = JSON.parse(fixJsonString(result.json))
      const data = build(json)
      if (!data) return null
      return { data, cleanContent: (content.slice(0, idx) + content.slice(result.end)).trim() }
    } catch { return null }
  }

  function tryParseEndMarker(content: string): { data: ReportData; cleanContent: string } | null {
    const result = tryParseMarker(content, '[面试结束]', json => {
      if (json.score != null) reportScore.value = Number(json.score)
      return buildFinishData(json)
    })
    if (result) return result
    // Marker exists without JSON → signal end with empty data
    if (content.includes('[面试结束]')) {
      return { data: buildFinishData({}), cleanContent: sanitizeEndMarker(content) }
    }
    return null
  }

  function tryParseCodeMarker(content: string): { data: CodeProblem; cleanContent: string } | null {
    return tryParseMarker(content, '[编程题目]', json => {
      if (!json.title || !json.template) return null
      return {
        type: (json.type === 'algorithm' ? 'algorithm' : 'complete') as CodeProblem['type'],
        title: String(json.title || ''),
        description: String(json.description || ''),
        template: String(json.template || ''),
        language: String(json.language || 'java'),
        templates: json.templates || undefined,
        testCases: Array.isArray(json.testCases) ? json.testCases : undefined
      } as CodeProblem
    })
  }

  function tryParseCodingMarker(content: string): { data: CodingReview; cleanContent: string } | null {
    const result = tryParseMarker(content, '[笔试结束]', json => ({
      score: json.score != null ? Number(json.score) : 0,
      feedback: String(json.feedback || ''),
      dimensions: Array.isArray(json.dimensions) ? json.dimensions as DimItem[] : undefined,
      suggestion: String(json.suggestion || '')
    } as CodingReview))
    if (result) codingReviewSeen = true
    return result
  }

  function updateMessage(idx: number, content: string): void {
    const updated = [...messages.value]
    updated[idx] = { ...updated[idx], role: 'ai', content }
    messages.value = updated
  }

  interface SendAnswerOptions {
    text: string
    code?: string
    codeLang?: string
    codeFile?: string
  }

  async function sendAnswer(textOrOptions: string | SendAnswerOptions): Promise<void> {
    const opts = typeof textOrOptions === 'string'
      ? { text: textOrOptions }
      : textOrOptions

    if (!opts.text || loading.value) return

    messages.value = [...messages.value, { role: 'user', content: opts.text }]
    loading.value = true

    try {
      const token = localStorage.getItem('token') || ''
      const body: Record<string, string> = { answer: opts.text }
      if (opts.code) {
        body.code = opts.code
        body.codeLang = opts.codeLang || 'java'
        body.codeFile = opts.codeFile || 'Solution.java'
      }
      const response = await fetch(`/api/interview/${sessionId.value}/answer/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(body)
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

          // Detect [笔试邀请] marker
          if (streamState.aiContent.includes('[笔试邀请]') && onCodingInvite) {
            streamState.aiContent = streamState.aiContent.replace(/\[笔试邀请\]/g, '').trim()
            updateMessage(aiMsgIdx, streamState.aiContent || '...')
            onCodingInvite()
            return
          }

          // Detect [编程题目] marker (before end marker check)
          const codeResult = tryParseCodeMarker(streamState.aiContent)
          if (codeResult && onCodeProblem) {
            streamState.aiContent = codeResult.cleanContent
            // 若标记前后无文字，移除占位气泡避免空白对话框
            if (!codeResult.cleanContent || codeResult.cleanContent.trim().length === 0) {
              messages.value = messages.value.filter((_, i) => i !== aiMsgIdx)
            } else {
              updateMessage(aiMsgIdx, codeResult.cleanContent)
            }
            onCodeProblem(codeResult.data)
            return
          }

          // Detect [笔试结束] marker — coding review result from AI
          const codingResult = tryParseCodingMarker(streamState.aiContent)
          if (codingResult && onCodingFinish) {
            streamState.aiContent = codingResult.cleanContent
            // 有前置反馈文字则显示，否则直接展示审查完成卡片
            if (codingResult.cleanContent && codingResult.cleanContent.trim().length > 0) {
              updateMessage(aiMsgIdx, codingResult.cleanContent)
            } else {
              updateMessage(aiMsgIdx, '__CODING_REVIEW_DONE__')
            }
            onCodingFinish(codingResult.data)
            return
          }

          const endResult = tryParseEndMarker(streamState.aiContent)
          if (endResult) {
            // During coding review flow, interview evaluation is deferred to codingReviewDone event
            if (codingReviewSeen) {
              updateMessage(aiMsgIdx, endResult.cleanContent || '面试已结束')
              return
            }
            updateMessage(aiMsgIdx, endResult.cleanContent || '面试已结束')
            loading.value = false
            onFinish(endResult.data)
            streamState.endDetected = true
            return
          }

          // Strip markers from display content — user never sees raw JSON
          const display = streamState.aiContent
            .replace(/\[笔试结束\][\s\S]*/g, '')
            .replace(/\[面试结束\][\s\S]*/g, '')
            .replace(/\[编程题目\][\s\S]*/g, '')
            .trim()
          updateMessage(aiMsgIdx, display)
        } else if (event === 'finish') {
          try {
            const json = JSON.parse(data)
            if (json.report?.score != null) {
              reportScore.value = Number(json.report.score)
            }
            // Coding problem delivered — stop loading, let user write code
            if (json.coding) {
              loading.value = false
              streamState.endDetected = true
              return
            }
            // Code review done — interview report was already saved before coding started.
            if (json.codingReviewDone) {
              loading.value = false
              onFinish({})
              streamState.endDetected = true
              return
            }
            if (json.finished) {
              loading.value = false
              // Try [面试结束] marker from accumulated content
              const endResult = tryParseEndMarker(streamState.aiContent)
              if (endResult) {
                updateMessage(aiMsgIdx, endResult.cleanContent || '面试已结束')
                onFinish(endResult.data)
              } else {
                updateMessage(aiMsgIdx, sanitizeEndMarker(streamState.aiContent) || '面试已结束')
                onFinish(json.report ? buildFinishData(json.report) : {})
              }
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
