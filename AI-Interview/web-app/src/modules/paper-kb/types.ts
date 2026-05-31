import type { PaperCitation } from './citation/types'

export interface LocalPaper {
  id?: number
  title: string
  fingerprint: string
  fileType: 'pdf' | 'docx' | 'txt' | 'md'
  uploadTime: number
  wordCount: number
  chunkCount: number
  citation?: PaperCitation
}

export interface LocalPaperChunk {
  id?: number
  paperId: number
  order: number
  section?: string
  content: string
  tokenCount: number
  keywords: string[]
}

export interface RetrievedChunk {
  chunkId: number
  paperId: number
  paperTitle: string
  chunkIndex: number
  section?: string
  content: string
  score: number
  keywords: string[]
}

export interface KbBackup {
  version: 1
  exportedAt: string
  papers: LocalPaper[]
  chunks: LocalPaperChunk[]
}

export type PaperTaskType =
  | 'paper_polish'
  | 'ai_reduce'
  | 'plagiarism_reduce'
  | 'paper_qa'

export interface ContextChunk {
  paperTitle: string
  section?: string
  content: string
  score?: number
  keywords?: string[]
  chunkId?: number
  paperId?: number
  chunkIndex?: number
}
