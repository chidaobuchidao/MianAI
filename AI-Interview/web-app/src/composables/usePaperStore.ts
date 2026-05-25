/**
 * Shared document state across Polish / AiReduce / PlagiarismReduce views.
 * Survives router.replace() navigation between the three paper tools.
 */

interface PaperDoc {
  sourceText: string
  paragraphs: any[]
  fileName: string
  timestamp: number
}

const KEY = 'paper_doc'

export function usePaperStore() {
  function save(doc: PaperDoc) {
    try {
      sessionStorage.setItem(KEY, JSON.stringify(doc))
    } catch { /* quota exceeded — ignore */ }
  }

  function load(): PaperDoc | null {
    try {
      const raw = sessionStorage.getItem(KEY)
      if (!raw) return null
      return JSON.parse(raw) as PaperDoc
    } catch {
      return null
    }
  }

  function clear() {
    try { sessionStorage.removeItem(KEY) } catch { /* ignore */ }
  }

  return { save, load, clear }
}
