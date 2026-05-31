import type { CitedChunk } from './types'

/**
 * 将带索引的 chunk 列表格式化为带引用标记的 prompt context 块。
 * 纯函数，替代 buildContextBlock 的引用场景。
 */
export function formatCitedContext(chunks: CitedChunk[]): string {
  if (chunks.length === 0) return ''

  const parts = chunks.map(c => {
    const citation = formatCitationLine(c)
    const header = c.section ? `【${c.section}】` : ''
    return `[${c.index}] ${citation}${header}\n${c.content}`
  })

  return [
    '以下是从知识库中检索到的相关内容，请参考并在引用时标注来源[N]：',
    '',
    ...parts,
    '',
    '---',
    '引用规则：',
    '- 在引用相关观点、数据、方法的句子后标注 [N]',
    '- 仅标注实际参考了的文献，不要无中生有',
    '- 一句话可标注多个来源，如 [1][2]',
    '- 未参考文献不要标注',
  ].join('\n')
}

/**
 * 格式化单条引用的标题行。
 */
function formatCitationLine(c: CitedChunk): string {
  const { citation, paperTitle } = c
  if (!citation) return `${paperTitle} `

  const parts: string[] = []

  if (citation.authors.length > 0) {
    const authorStr = citation.authors.length > 3
      ? `${citation.authors[0]}等`
      : citation.authors.join(', ')
    parts.push(authorStr)
  }

  if (citation.year) {
    parts.push(`(${citation.year})`)
  }

  parts.push(paperTitle)

  if (citation.journal) {
    parts.push(citation.journal)
  }

  return parts.join(' ') + ' '
}
