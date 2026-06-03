import { computed, type Ref } from 'vue'
import { renderCitations } from '@/modules/paper-kb'
import type { CitedChunk } from '@/modules/paper-kb'

interface RetrievedChunk {
  citationIndex?: number | null
  paperTitle: string
  section?: string
  content: string
}

export interface CitationChipsOptions {
  /** The chunks from knowledge base retrieval */
  chunks: Ref<RetrievedChunk[]>
  /** The text to scan for citation markers */
  sourceText: Ref<string>
  /**
   * Map chunks to CitedChunk[]. Defaults to sequential (i+1) indexing.
   * Pass 'citationIndex' to use each chunk's citationIndex field instead.
   */
  indexStrategy?: 'sequential' | 'citationIndex'
}

export function useCitationChips(options: CitationChipsOptions) {
  const { chunks, sourceText, indexStrategy = 'sequential' } = options

  function applyCitationChips(html: string): string {
    return html.replace(/\[(\d+)\]/g, (match, numStr) => {
      const index = parseInt(numStr, 10)
      if (index < 1 || index > 50) return match
      return `<span class="cite-chip" data-cite-index="${index}">${index}</span>`
    })
  }

  const citedReferences = computed(() => {
    if (!chunks.value.length) return []

    let citedChunks: CitedChunk[]

    if (indexStrategy === 'citationIndex') {
      citedChunks = chunks.value
        .filter(c => c.citationIndex != null)
        .map(c => ({
          index: c.citationIndex!,
          paperTitle: c.paperTitle,
          section: c.section,
          content: c.content,
        }))
    } else {
      citedChunks = chunks.value.map((c, i) => ({
        index: i + 1,
        paperTitle: c.paperTitle,
        section: c.section,
        content: c.content,
      }))
    }

    const { references } = renderCitations(sourceText.value, citedChunks)
    return references
  })

  return { applyCitationChips, citedReferences }
}

export type UseCitationChipsReturn = ReturnType<typeof useCitationChips>
