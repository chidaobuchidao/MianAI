export interface DimItem {
  name: string
  score: number
  comment: string
}

/**
 * Removes the [面试结束] marker and everything after it from interview content.
 */
export function sanitizeEndMarker(text: string): string {
  return text.replace(/\[面试结束\]\s*\{.*\}/s, '').trim()
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
