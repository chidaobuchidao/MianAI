import type { RetrievedChunk, ContextChunk, PaperTaskType } from './types'
import { searchChunks } from './paperSearchIndex'

const TASK_KEYWORDS: Record<PaperTaskType, string[]> = {
  paper_polish: ['论文', '润色', '学术', '写作', '表达'],
  ai_reduce: ['降重', '重复', '相似', '改写', 'AI率'],
  plagiarism_reduce: ['查重', '抄袭', '重复率', '原创', '改写'],
  paper_qa: ['问题', '回答', '解释', '分析', '方法'],
}

export async function retrieveContext(
  question: string,
  taskType: PaperTaskType,
  topK = 5
): Promise<RetrievedChunk[]> {
  const taskKeywords = TASK_KEYWORDS[taskType]
  const enhancedQuery = `${question} ${taskKeywords.join(' ')}`

  return searchChunks(enhancedQuery, topK)
}

export function toContextChunks(chunks: RetrievedChunk[]): ContextChunk[] {
  return chunks.map(c => ({
    paperTitle: c.paperTitle,
    section: c.section,
    content: c.content,
    score: c.score,
    keywords: c.keywords,
    chunkId: c.chunkId,
    paperId: c.paperId,
    chunkIndex: c.chunkIndex,
  }))
}
