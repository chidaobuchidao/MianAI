import MiniSearch from 'minisearch'
import type { LocalPaperChunk, RetrievedChunk } from './types'
import { paperKnowledgeDb } from './paperKnowledgeDb'
import { isReferenceLikeChunk } from './paperChunker'

let searchIndex: MiniSearch | null = null
let indexedChunkIds = new Set<number>()

function buildIndex(chunks: LocalPaperChunk[]): MiniSearch {
  const searchableChunks = chunks.filter(c => !isReferenceLikeChunk(c.content, c.section))
  const ms = new MiniSearch<{ id: number; content: string; keywords: string }>({
    fields: ['content', 'keywords'],
    storeFields: [],
    searchOptions: {
      boost: { keywords: 2 },
      prefix: true,
      fuzzy: 0.2,
    },
  })

  ms.addAll(
    searchableChunks.map(c => ({
      id: c.id!,
      content: c.content,
      keywords: c.keywords.join(' '),
    }))
  )

  return ms
}

export async function ensureSearchIndex(): Promise<void> {
  const allChunks = await paperKnowledgeDb.chunks.toArray()
  const searchableChunks = allChunks.filter(c => !isReferenceLikeChunk(c.content, c.section))
  const currentIds = new Set(searchableChunks.filter(c => c.id != null).map(c => c.id!))

  const idsChanged =
    currentIds.size !== indexedChunkIds.size ||
    [...currentIds].some(id => !indexedChunkIds.has(id))

  if (!searchIndex || idsChanged) {
    searchIndex = buildIndex(searchableChunks)
    indexedChunkIds = currentIds
  }
}

export async function searchChunks(query: string, topK = 5): Promise<RetrievedChunk[]> {
  await ensureSearchIndex()

  if (!searchIndex || !query.trim()) return []

  const results = searchIndex.search(query).slice(0, topK * 2)

  if (results.length === 0) return []

  const chunkIds = results.map(r => r.id as number)
  const chunks = await paperKnowledgeDb.chunks
    .where('id')
    .anyOf(chunkIds)
    .toArray()
    .then(items => items.filter(c => !isReferenceLikeChunk(c.content, c.section)))

  const chunkMap = new Map(chunks.map(c => [c.id, c]))

  const paperIds = [...new Set(chunks.map(c => c.paperId))]
  const papers = await paperKnowledgeDb.papers
    .where('id')
    .anyOf(paperIds)
    .toArray()

  const paperMap = new Map(papers.map(p => [p.id, p]))

  const maxScore = results[0]?.score ?? 1

  const retrieved: RetrievedChunk[] = []
  for (const r of results.slice(0, topK)) {
    const chunk = chunkMap.get(r.id as number)
    if (!chunk) continue
    retrieved.push({
      chunkId: chunk.id!,
      paperId: chunk.paperId,
      paperTitle: paperMap.get(chunk.paperId)?.title ?? '未知论文',
      chunkIndex: chunk.order,
      ...(chunk.section ? { section: chunk.section } : {}),
      content: chunk.content,
      score: r.score / maxScore,
      keywords: chunk.keywords,
    })
  }
  return retrieved
}
