const LATIN_SEGMENT_RE = /[A-Za-z][A-Za-z0-9\s,.;:()/%+\-/]*[A-Za-z0-9]/g
const CJK_SPACED_SEGMENT_RE = /[\u4e00-\u9fff](?:[ \t\u00A0]+[\u4e00-\u9fff]){1,}/g
const CONTROL_CHARS_RE = /[\x00-\x08\x0B\x0C\x0E-\x1F]/g
const SOURCE_WORD_RE = /[A-Za-z]+(?:['-][A-Za-z]+)?|\d+(?:\.\d+)?/g
const COLLAPSED_LATIN_TOKEN_RE = /[A-Za-z][A-Za-z0-9']{11,}/g
const LATIN_PUNCTUATION_JOIN_RE = /([A-Za-z0-9][,.;:!?])(?=[A-Za-z])/g

interface SpacingSegment {
  original: string
  compact: string
}

interface SourceWord {
  text: string
  normalized: string
}

/**
 * Restores spacing that already existed in the source paragraph.
 * This guards against LLM outputs that collapse English word spaces
 * or deliberate title spacing in CJK titles.
 */
export function preserveOriginalSpacing(originalText: string, candidateText: string): string {
  if (!candidateText) return candidateText

  let restored = cleanCandidateText(candidateText)
  if (!originalText) return restored

  const segments = collectSpacingSegments(originalText)
  for (const segment of segments) {
    const pattern = compactToFlexiblePattern(segment.compact)
    restored = restored.replace(pattern, match => {
      const compactMatch = compactWhitespace(match)
      return compactMatch === segment.compact ? segment.original : match
    })
  }

  restored = restoreCollapsedLatinWords(originalText, restored)
  return restoreLatinPunctuationSpacing(restored)
}

function cleanCandidateText(text: string): string {
  return text.replace(CONTROL_CHARS_RE, '')
}

function restoreLatinPunctuationSpacing(text: string): string {
  return text.replace(LATIN_PUNCTUATION_JOIN_RE, '$1 ')
}

function restoreCollapsedLatinWords(originalText: string, candidateText: string): string {
  const sourceWords = collectSourceWords(originalText)
  if (sourceWords.length < 2) return candidateText

  return candidateText.replace(COLLAPSED_LATIN_TOKEN_RE, token => {
    const restored = splitCollapsedTokenBySourceWords(token, sourceWords)
    return restored ?? token
  })
}

function collectSourceWords(text: string): SourceWord[] {
  return Array.from(text.matchAll(SOURCE_WORD_RE))
    .map(match => ({
      text: match[0],
      normalized: normalizeLatinWord(match[0]),
    }))
    .filter(word => word.normalized.length > 0)
}

function splitCollapsedTokenBySourceWords(token: string, sourceWords: SourceWord[]): string | null {
  const tokenNormalized = normalizeLatinWord(token)
  if (tokenNormalized.length < 12) return null

  for (let start = 0; start < sourceWords.length - 1; start++) {
    let combined = ''
    const parts: string[] = []

    for (let end = start; end < sourceWords.length; end++) {
      combined += sourceWords[end].normalized
      parts.push(sourceWords[end].text)

      if (combined.length > tokenNormalized.length) break
      if (combined === tokenNormalized && parts.length > 1) {
        return parts.join(' ')
      }
    }
  }

  return null
}

function normalizeLatinWord(value: string): string {
  return value.replace(/[^A-Za-z0-9]/g, '').toLowerCase()
}

function collectSpacingSegments(text: string): SpacingSegment[] {
  const segments: SpacingSegment[] = []

  for (const match of text.matchAll(LATIN_SEGMENT_RE)) {
    const value = match[0].trim()
    const words = value.match(/[A-Za-z][A-Za-z0-9-]*/g) ?? []
    if (words.length < 2 || !/\s/.test(value)) continue
    segments.push(toSegment(value))
  }

  for (const match of text.matchAll(CJK_SPACED_SEGMENT_RE)) {
    segments.push(toSegment(match[0]))
  }

  return dedupeSegments(segments)
    .filter(segment => segment.compact.length >= 4)
    .sort((a, b) => b.compact.length - a.compact.length)
}

function toSegment(original: string): SpacingSegment {
  return {
    original,
    compact: compactWhitespace(original),
  }
}

function dedupeSegments(segments: SpacingSegment[]): SpacingSegment[] {
  const seen = new Set<string>()
  const result: SpacingSegment[] = []
  for (const segment of segments) {
    if (seen.has(segment.compact)) continue
    seen.add(segment.compact)
    result.push(segment)
  }
  return result
}

function compactWhitespace(value: string): string {
  return value.replace(/[\s\u00A0]+/g, '')
}

function compactToFlexiblePattern(compact: string): RegExp {
  const chars = Array.from(compact).map(escapeRegExp)
  return new RegExp(chars.join('[\\s\\u00A0]*'), 'g')
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}
