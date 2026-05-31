import Dexie, { type Table } from 'dexie'
import type { LocalPaper, LocalPaperChunk } from './types'

class PaperKnowledgeDb extends Dexie {
  papers!: Table<LocalPaper, number>
  chunks!: Table<LocalPaperChunk, number>

  constructor() {
    super('MainAI_PaperKnowledgeBase')
    this.version(1).stores({
      papers: '++id, title, fingerprint, uploadTime, fileType',
      chunks: '++id, paperId, order, section, *keywords',
    })
  }
}

export const paperKnowledgeDb = new PaperKnowledgeDb()
