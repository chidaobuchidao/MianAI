export { usePaperKnowledgeBase } from './usePaperKnowledgeBase'
export { paperKnowledgeDb } from './paperKnowledgeDb'
export { parsePaperFile } from './paperParser'
export { chunkPaper } from './paperChunker'
export { searchChunks, ensureSearchIndex } from './paperSearchIndex'
export { retrieveContext, toContextChunks } from './paperRetriever'
export { buildQuestionWithContext, buildContextBlock } from './paperPromptBuilder'
export { exportBackup, importBackup, downloadBackupFile } from './paperKbBackup'

export type {
  LocalPaper,
  LocalPaperChunk,
  RetrievedChunk,
  KbBackup,
  PaperTaskType,
  ContextChunk,
} from './types'
