import { ref } from 'vue'
import { paperKnowledgeDb } from './paperKnowledgeDb'
import { parsePaperFile } from './paperParser'
import { chunkPaper } from './paperChunker'
import { ensureSearchIndex } from './paperSearchIndex'
import { retrieveContext, toContextChunks } from './paperRetriever'
import { buildQuestionWithContext } from './paperPromptBuilder'
import { exportBackup, importBackup, downloadBackupFile } from './paperKbBackup'
import { extractCitation } from './citation'
import type { LocalPaper, PaperTaskType, ContextChunk, KbBackup } from './types'

export function usePaperKnowledgeBase() {
  const papers = ref<LocalPaper[]>([])
  const isLoading = ref(false)
  const importingFile = ref<string | null>(null)

  async function loadPapers(): Promise<void> {
    papers.value = await paperKnowledgeDb.papers.orderBy('uploadTime').reverse().toArray()
  }

  async function importFile(file: File): Promise<void> {
    importingFile.value = file.name
    isLoading.value = true

    try {
      const parsed = await parsePaperFile(file)
      const existing = await paperKnowledgeDb.papers
        .where('fingerprint')
        .equals(parsed.fingerprint)
        .first()

      if (existing) {
        throw new Error(`已存在相同文件: ${parsed.title}`)
      }

      const chunks = chunkPaper(parsed.fullText)
      const citation = extractCitation(parsed.fullText)
      const wordCount = parsed.wordCount
      const chunkCount = chunks.length

      const paperId = await paperKnowledgeDb.papers.add({
        title: parsed.title,
        fingerprint: parsed.fingerprint,
        fileType: parsed.fileType,
        uploadTime: Date.now(),
        wordCount,
        chunkCount,
        citation: citation ?? undefined,
      })

      await paperKnowledgeDb.chunks.bulkAdd(
        chunks.map(c => ({
          paperId: paperId as number,
          order: c.order,
          section: c.section,
          content: c.content,
          tokenCount: c.tokenCount,
          keywords: c.keywords,
        }))
      )

      await ensureSearchIndex()
      await loadPapers()
    } finally {
      isLoading.value = false
      importingFile.value = null
    }
  }

  async function deletePaper(paperId: number): Promise<void> {
    await paperKnowledgeDb.transaction('rw', paperKnowledgeDb.papers, paperKnowledgeDb.chunks, async () => {
      await paperKnowledgeDb.chunks.where('paperId').equals(paperId).delete()
      await paperKnowledgeDb.papers.delete(paperId)
    })

    await ensureSearchIndex()
    await loadPapers()
  }

  async function clearAll(): Promise<void> {
    await paperKnowledgeDb.transaction('rw', paperKnowledgeDb.papers, paperKnowledgeDb.chunks, async () => {
      await paperKnowledgeDb.papers.clear()
      await paperKnowledgeDb.chunks.clear()
    })

    await ensureSearchIndex()
    await loadPapers()
  }

  async function getRelevantContext(
    question: string,
    taskType: PaperTaskType,
    topK = 5
  ): Promise<ContextChunk[]> {
    const chunks = await retrieveContext(question, taskType, topK)
    return toContextChunks(chunks)
  }

  function buildAugmentedQuestion(
    question: string,
    contextChunks: ContextChunk[],
    taskType: PaperTaskType
  ): string {
    return buildQuestionWithContext(question, contextChunks, taskType)
  }

  async function doExportBackup(): Promise<void> {
    const backup = await exportBackup()
    downloadBackupFile(backup)
  }

  async function doImportBackup(file: File): Promise<{ paperCount: number; chunkCount: number }> {
    const text = await file.text()
    const backup: KbBackup = JSON.parse(text)
    const result = await importBackup(backup)
    await ensureSearchIndex()
    await loadPapers()
    return result
  }

  return {
    papers,
    isLoading,
    importingFile,
    loadPapers,
    importFile,
    deletePaper,
    clearAll,
    getRelevantContext,
    buildAugmentedQuestion,
    doExportBackup,
    doImportBackup,
  }
}
