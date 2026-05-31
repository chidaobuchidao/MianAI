import type { PaperCitation } from './types'

/**
 * 从论文文本中提取引用元数据（作者、年份、期刊）。
 * 纯函数，无外部依赖。提取不到返回 null。
 */
export function extractCitation(text: string): PaperCitation | null {
  if (!text || text.trim().length < 50) return null

  const first500 = text.slice(0, 500)
  const refLine = extractFirstReferenceLine(text)

  const authors = extractAuthors(first500) ?? (refLine ? extractAuthorsFromRef(refLine) : [])
  const year = extractYear(first500) ?? (refLine ? extractYear(refLine) : undefined)
  const journal = extractJournal(refLine ?? first500)

  if (authors.length === 0 && !year && !journal) return null

  return {
    authors,
    year: year ?? undefined,
    journal: journal ?? undefined,
    rawReference: refLine ?? undefined,
  }
}

/**
 * 提取参考文献第一行。
 */
function extractFirstReferenceLine(text: string): string | null {
  const refPatterns = [
    /\[(\d+)\]\s*(.+)/,
    /^(\d+)\.\s+(.+)/m,
  ]

  for (const pattern of refPatterns) {
    const match = text.match(pattern)
    if (match && match[2]) {
      const line = match[2].trim()
      if (/\b(?:19|20)\d{2}\b/.test(line) || /\[J\]|\[M\]|\[C\]/.test(line)) {
        return line
      }
    }
  }
  return null
}

/**
 * 从论文首页提取作者。
 */
function extractAuthors(text: string): string[] | null {
  const cnMatch = text.match(/[一-鿿]{2,4}(?:[,，、\s]+[一-鿿]{2,4})+/)
  if (cnMatch) {
    const names = cnMatch[0].split(/[,，、\s]+/).filter(n => n.length >= 2 && n.length <= 4)
    if (names.length >= 1 && names.length <= 10) return names
  }

  const enMatch = text.match(/[A-Z]\.\s*(?:[A-Z]\.\s*)?[A-Z][a-z]+(?:\s+et al\.?)?/)
  if (enMatch) {
    return [enMatch[0].trim()]
  }

  return null
}

/**
 * 从参考文献行提取作者。
 */
function extractAuthorsFromRef(refLine: string): string[] {
  const dotIndex = refLine.indexOf('.')
  if (dotIndex > 0 && dotIndex < 80) {
    const authorPart = refLine.slice(0, dotIndex).trim()
    if (authorPart.length >= 2) {
      const cnNames = authorPart.match(/[一-鿿]{2,4}/g)
      if (cnNames && cnNames.length >= 1) return cnNames
      return [authorPart]
    }
  }
  return []
}

/**
 * 提取年份。
 */
function extractYear(text: string): number | undefined {
  const match = text.match(/\b((?:19|20)\d{2})\b/)
  if (match) {
    const year = parseInt(match[1], 10)
    if (year >= 1900 && year <= 2099) return year
  }
  return undefined
}

/**
 * 提取期刊名。
 */
function extractJournal(text: string): string | null {
  const jMatch = text.match(/\[J\]\s*[.。]?\s*(.+?)(?:[,，]|\s*\d{4}|\s*$)/)
  if (jMatch && jMatch[1]) {
    const journal = jMatch[1].trim()
    if (journal.length >= 2 && journal.length <= 60) return journal
  }

  const journalKeywords = text.match(/([一-鿿]*(?:学报|科学|研究|通讯|通报|杂志)[一-鿿]*)/)
  if (journalKeywords && journalKeywords[1]) {
    return journalKeywords[1].trim()
  }

  return null
}
