import { ref, onMounted } from 'vue'
import { usePaperKnowledgeBase, buildContextBlock } from '@/modules/paper-kb'
import type { PaperTaskType, ContextChunk } from '@/modules/paper-kb'

export interface KbSettings {
  topK: number
  autoRetrieve: boolean
}

export interface KbRetrievalHints {
  focusText?: string
  notes?: string
  sourceText?: string
  annotations?: string[]
}

const MAX_QUERY_CHARS = 900
const MAX_HINT_CHARS = 500

/**
 * 公共知识库面板逻辑，三个论文工具页面共用。
 * 返回面板状态、处理器、以及检索增强方法。
 */
export function usePaperKbPanel(taskType: PaperTaskType) {
  const {
    papers,
    isLoading,
    importingFile,
    loadPapers,
    importFile,
    deletePaper,
    clearAll,
    getRelevantContext,
    doExportBackup,
    doImportBackup,
  } = usePaperKnowledgeBase()

  const showKbPanel = ref(false)
  const settings = ref<KbSettings>({ topK: 5, autoRetrieve: true })
  const lastRetrievedCount = ref(0)
  const lastRetrievedPaperCount = ref(0)
  const lastRetrievedChunks = ref<ContextChunk[]>([])
  const error = ref('')

  onMounted(() => loadPapers())

  async function handleImport(files: File[]) {
    error.value = ''
    for (const f of files) {
      try {
        await importFile(f)
      } catch (e: unknown) {
        const message = e instanceof Error ? e.message : String(e)
        error.value = `文件解析失败: ${message || '导入失败'}`
        return
      }
    }
  }

  async function handleDelete(id: number) {
    error.value = ''
    await deletePaper(id)
  }

  async function handleClearAll() {
    error.value = ''
    await clearAll()
  }

  async function handleBackup() {
    await doExportBackup()
  }

  async function handleRestore(file: File) {
    error.value = ''
    try {
      await doImportBackup(file)
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : String(e)
      error.value = `备份恢复失败: ${message || '文件格式不正确'}`
    }
  }

  /**
   * 从知识库检索相关上下文。
   * 返回格式化好的上下文文本块，检索失败时返回空字符串。
   */
  async function retrieveAndFormat(queryText: string, hints?: KbRetrievalHints): Promise<string> {
    const chunks = await retrieveRaw(queryText, hints)
    if (chunks.length === 0) return ''
    return buildContextBlock(chunks)
  }

  /**
   * 从知识库检索相关上下文，返回原始 chunk 数组。
   * 用于需要将 raw chunks 发送给后端的场景（AiReduce / PlagiarismReduce）。
   */
  async function retrieveRaw(queryText: string, hints?: KbRetrievalHints): Promise<ContextChunk[]> {
    if (!settings.value.autoRetrieve || papers.value.length === 0) {
      lastRetrievedCount.value = 0
      lastRetrievedPaperCount.value = 0
      lastRetrievedChunks.value = []
      return []
    }

    lastRetrievedCount.value = 0
    lastRetrievedPaperCount.value = 0
    lastRetrievedChunks.value = []

    try {
      const chunks = await getRelevantContext(
        buildRetrievalQuery(queryText, hints),
        taskType,
        settings.value.topK
      )

      lastRetrievedCount.value = chunks.length
      lastRetrievedPaperCount.value = new Set(chunks.map(c => c.paperTitle)).size
      lastRetrievedChunks.value = chunks
      return chunks
    } catch {
      lastRetrievedCount.value = 0
      lastRetrievedPaperCount.value = 0
      lastRetrievedChunks.value = []
      return []
    }
  }

  return {
    papers,
    isLoading,
    importingFile,
    showKbPanel,
    settings,
    error,
    lastRetrievedCount,
    lastRetrievedPaperCount,
    lastRetrievedChunks,
    handleImport,
    handleDelete,
    handleClearAll,
    handleBackup,
    handleRestore,
    retrieveAndFormat,
    retrieveRaw,
  }
}

function buildRetrievalQuery(queryText: string, hints?: KbRetrievalHints): string {
  const hintParts = [
    hints?.focusText,
    hints?.sourceText,
    hints?.annotations?.join('\n'),
    hints?.notes,
  ]
    .map(part => normalizeQueryText(part).slice(0, MAX_HINT_CHARS))
    .filter(Boolean)
    .join('\n')
    .slice(0, MAX_HINT_CHARS)

  const body = pickRepresentativeText(queryText)
  const query = `${hintParts}\n${body}`.trim()
  return query.length > 0 ? query.slice(0, MAX_QUERY_CHARS) : normalizeQueryText(queryText).slice(0, MAX_QUERY_CHARS)
}

function pickRepresentativeText(text: string): string {
  const normalized = normalizeQueryText(text)
  const segments = normalized
    .split(/(?:\n{2,}|[。！？!?；;])/)
    .map(part => part.trim())
    .filter(part => part.length >= 12)

  if (segments.length === 0) return normalized.slice(0, MAX_QUERY_CHARS)

  return segments
    .slice(0, 8)
    .join('\n')
    .slice(0, MAX_QUERY_CHARS)
}

function normalizeQueryText(text?: string): string {
  if (!text) return ''
  return text
    .replace(/\[P\d+]\s*/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}
