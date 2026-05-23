export interface DimItem {
  name: string
  score: number
  comment: string
}

/** Extract a balanced JSON object from text starting at a given offset. Returns the JSON string and end index, or null. */
export function extractBalancedJson(text: string, start: number): { json: string; end: number } | null {
  let i = start
  while (i < text.length && /\s/.test(text[i])) i++
  if (i >= text.length || text[i] !== '{') return null
  let depth = 0
  const begin = i
  for (; i < text.length; i++) {
    if (text[i] === '{') depth++
    else if (text[i] === '}') {
      depth--
      if (depth === 0) return { json: text.slice(begin, i + 1), end: i + 1 }
    }
  }
  return null
}

/** Remove a marker and its JSON payload from text, returning cleaned content. */
export function stripMarker(text: string, marker: string): string {
  const idx = text.indexOf(marker)
  if (idx === -1) return text.trim()
  const result = extractBalancedJson(text, idx + marker.length)
  if (result) return (text.slice(0, idx) + text.slice(result.end)).trim()
  return text.slice(0, idx).trim()
}

export function sanitizeEndMarker(text: string): string {
  return stripMarker(text, '[面试结束]')
}

/**
 * Fixes common AI-generated JSON issues such as trailing commas before } or ].
 */
export function fixJsonString(json: string): string {
  return json
    .replace(/,\s*}/g, '}')
    .replace(/,\s*\]/g, ']')
}

/**
 * Safely parses dimension data from a raw API response.
 * Accepts an array, a JSON string, or malformed JSON from AI output.
 */
export function scoreToVerdict(score: number): string {
  if (score >= 8) return '优秀'
  if (score >= 6) return '良好'
  if (score >= 4) return '一般'
  return '需提升'
}

export function scoreToRingClass(score: number): string {
  if (score >= 8) return 'score-ring__fg--high'
  if (score >= 6) return 'score-ring__fg--mid'
  if (score >= 4) return 'score-ring__fg--low'
  return 'score-ring__fg--muted'
}

export function safeParseDims(raw: unknown): DimItem[] {
  if (Array.isArray(raw)) return raw as DimItem[]
  if (typeof raw === 'string') {
    try {
      return JSON.parse(fixJsonString(raw)) as DimItem[]
    } catch {
      return []
    }
  }
  return []
}
