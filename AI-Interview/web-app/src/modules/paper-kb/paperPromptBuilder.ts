import type { ContextChunk, PaperTaskType } from './types'

const TASK_LABELS: Record<PaperTaskType, string> = {
  paper_polish: '论文润色',
  ai_reduce: 'AI降重',
  plagiarism_reduce: '降重改写',
  paper_qa: '论文问答',
}

export function buildContextBlock(chunks: ContextChunk[]): string {
  if (chunks.length === 0) return ''

  const parts = chunks.map((c, i) => {
    const header = c.section ? `【${c.section}】` : ''
    return `[参考${i + 1}] ${c.paperTitle}${header}\n${c.content}`
  })

  return `以下是从知识库中检索到的相关内容，请参考这些信息来完成任务：\n\n${parts.join('\n\n---\n\n')}\n\n---\n`
}

export function buildQuestionWithContext(
  question: string,
  contextChunks: ContextChunk[],
  taskType: PaperTaskType
): string {
  const contextBlock = buildContextBlock(contextChunks)
  const taskLabel = TASK_LABELS[taskType]

  if (!contextBlock) {
    return question
  }

  return `${contextBlock}\n用户${taskLabel}任务：\n${question}`
}
