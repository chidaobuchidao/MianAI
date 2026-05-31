const HEADER_RE = /^#{1,4}\s+.+|^(?:第[一二三四五六七八九十\d]+[章节部分篇]|[IVX]+\.\s)/m
const MAX_CHUNK = 800
const MIN_CHUNK = 50
const REFERENCE_HEADING_RE = /^(?:#{1,4}\s*)?(参考文献|参考资料|引用文献|参考书目|references|bibliography|literature cited|works cited)\s*[:：]?$/i
const INLINE_REFERENCE_HEADING_RE = /(?:^|\n)\s*(?:#{1,4}\s*)?(参考文献|参考资料|引用文献|参考书目|references|bibliography|literature cited|works cited)\s*[:：]?\s*(?=\n|$)/i
const LOOSE_REFERENCE_HEADING_RE = /(?:^|\n|\s)(参考文献|参考资料|引用文献|参考书目|references|bibliography|literature cited|works cited)\s*[:：]?\s+/i
const YEAR_RE = /\b(?:19|20)\d{2}\b/g
const JOURNAL_MARK_RE = /\[(?:J|M|D|C|R|P|EB\/OL|OL)\]/gi

export interface ChunkInput {
  content: string
  section?: string
}

function extractKeywords(text: string): string[] {
  const stopWords = new Set([
    '的', '了', '和', '是', '在', '有', '与', '及', '或', '为', '对', '中', '上', '下',
    'the', 'a', 'an', 'is', 'are', 'was', 'were', 'be', 'been', 'being',
    'have', 'has', 'had', 'do', 'does', 'did', 'will', 'would', 'could',
    'should', 'may', 'might', 'shall', 'can', 'to', 'of', 'in', 'for',
    'on', 'with', 'at', 'by', 'from', 'as', 'into', 'through', 'during',
    'before', 'after', 'above', 'below', 'between', 'this', 'that', 'these',
    'those', 'it', 'its', 'we', 'our', 'they', 'their', 'not', 'no', 'but',
    'if', 'or', 'so', 'and', 'also', 'than', 'then', 'just', 'more',
  ])

  const words = text
    .toLowerCase()
    .replace(/[^\w一-鿿]+/g, ' ')
    .split(/\s+/)
    .filter(w => w.length >= 2 && !stopWords.has(w))

  const freq = new Map<string, number>()
  for (const w of words) {
    freq.set(w, (freq.get(w) ?? 0) + 1)
  }

  return [...freq.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, 20)
    .map(([w]) => w)
}

function splitByHeaders(text: string): ChunkInput[] {
  const lines = text.split('\n')
  const sections: ChunkInput[] = []
  let currentSection: string | undefined
  let currentContent: string[] = []

  for (const line of lines) {
    if (HEADER_RE.test(line.trim())) {
      if (currentContent.length > 0) {
        const content = currentContent.join('\n').trim()
        if (content) {
          sections.push({ content, section: currentSection })
        }
      }
      currentSection = line.trim().replace(/^#+\s*/, '')
      currentContent = []
    } else {
      currentContent.push(line)
    }
  }

  if (currentContent.length > 0) {
    const content = currentContent.join('\n').trim()
    if (content) {
      sections.push({ content, section: currentSection })
    }
  }

  return sections
}

function stripReferenceSection(text: string): string {
  const inlineMatch = INLINE_REFERENCE_HEADING_RE.exec(text)
  if (inlineMatch) {
    const before = text.slice(0, inlineMatch.index).trim()
    const after = text.slice(inlineMatch.index).trim()
    if (isReferenceLikeText(after)) {
      return before
    }
  }

  const looseMatch = LOOSE_REFERENCE_HEADING_RE.exec(text)
  if (looseMatch) {
    const headingStart = looseMatch.index + looseMatch[0].indexOf(looseMatch[1])
    const before = text.slice(0, headingStart).trim()
    const after = text.slice(headingStart).trim()
    if (isReferenceLikeText(after)) {
      return before
    }
  }

  return text
}

function isReferenceLikeText(text: string): boolean {
  const normalized = text.replace(/\s+/g, ' ').trim()
  if (normalized.length < 80) return false

  const years = normalized.match(YEAR_RE)?.length ?? 0
  const journalMarks = normalized.match(JOURNAL_MARK_RE)?.length ?? 0
  const pageRanges = normalized.match(/\b\d+\s*[-–]\s*\d+\b/g)?.length ?? 0
  const authorSeparators = normalized.match(/(?:等|et al\.?|,|，|、)/gi)?.length ?? 0
  const citationTitles = normalized.match(/(?:学报|科学|生态|土壤|肥料|Journal|Science|Agriculture|Environment)/gi)?.length ?? 0

  return (
    (years >= 4 && (journalMarks >= 2 || pageRanges >= 3)) ||
    (years >= 5 && authorSeparators >= 12 && citationTitles >= 2)
  )
}

function isReferenceSection(section?: string): boolean {
  return Boolean(section && REFERENCE_HEADING_RE.test(section.trim()))
}

export function isReferenceLikeChunk(content: string, section?: string): boolean {
  if (isReferenceSection(section)) return true
  return isReferenceLikeText(content)
}

function splitLongChunk(text: string): string[] {
  if (text.length <= MAX_CHUNK) return [text]

  const parts: string[] = []
  const paragraphs = text.split(/\n{2,}/)
  let current = ''

  for (const para of paragraphs) {
    if (current.length + para.length + 2 > MAX_CHUNK && current.length >= MIN_CHUNK) {
      parts.push(current.trim())
      current = para
    } else {
      current = current ? `${current}\n\n${para}` : para
    }
  }

  if (current.trim()) {
    parts.push(current.trim())
  }

  return parts
}

export interface PaperChunk {
  order: number
  section?: string
  content: string
  tokenCount: number
  keywords: string[]
}

export function chunkPaper(fullText: string): PaperChunk[] {
  const sections = splitByHeaders(stripReferenceSection(fullText))
  const chunks: PaperChunk[] = []
  let order = 0

  for (const sec of sections) {
    if (isReferenceSection(sec.section)) continue

    const parts = splitLongChunk(sec.content)

    for (const part of parts) {
      if (isReferenceLikeChunk(part, sec.section)) continue

      chunks.push({
        order: order++,
        section: sec.section,
        content: part,
        tokenCount: Math.ceil(part.length / 2),
        keywords: extractKeywords(part),
      })
    }
  }

  return chunks
}
