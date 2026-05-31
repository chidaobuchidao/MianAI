import type { CitedChunk } from './types'

export interface CitationRenderResult {
  html: string
  references: string[]
  citedIndices: number[]
}

/**
 * 解析 AI 输出中的 [N] 引用标记，生成带 chip 的 HTML 和参考文献列表。
 * 纯函数。
 */
export function renderCitations(text: string, citedChunks: CitedChunk[]): CitationRenderResult {
  const citedIndices: number[] = []
  const chunkMap = new Map(citedChunks.map(c => [c.index, c]))

  // 替换 [N] 为 chip HTML
  const html = text.replace(/\[(\d+)\]/g, (match, numStr: string) => {
    const index = parseInt(numStr, 10)
    if (!chunkMap.has(index)) return match
    if (!citedIndices.includes(index)) citedIndices.push(index)
    return `<span class="cite-chip" data-cite-index="${index}">${index}</span>`
  })

  // 生成参考文献列表（按引用顺序）
  citedIndices.sort((a, b) => a - b)
  const references = citedIndices.map(index => {
    const chunk = chunkMap.get(index)
    if (!chunk) return `[${index}] 未知来源`
    return formatReference(index, chunk)
  })

  return { html, references, citedIndices }
}

/**
 * 格式化单条参考文献。
 */
function formatReference(index: number, chunk: CitedChunk): string {
  const { citation, paperTitle, section } = chunk
  if (!citation) return `[${index}] ${paperTitle}`

  const parts: string[] = [`[${index}]`]

  if (citation.authors.length > 0) {
    const authorStr = citation.authors.length > 3
      ? `${citation.authors.slice(0, 3).join(', ')} et al.`
      : citation.authors.join(', ')
    parts.push(authorStr)
  }

  if (citation.year) {
    parts.push(`(${citation.year}).`)
  }

  parts.push(paperTitle + '.')

  if (citation.journal) {
    parts.push(citation.journal + '.')
  }

  if (section) {
    parts.push(`【${section}】`)
  }

  return parts.join(' ')
}
