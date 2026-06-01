import { ref, onMounted } from 'vue'
import { usePaperKnowledgeBase, formatCitedContext } from '@/modules/paper-kb'
import type { PaperTaskType, ContextChunk, CitedChunk } from '@/modules/paper-kb'
import { authFetch } from '@/utils/authFetch'
import { readJsonResponse } from '@/utils/httpResponse'

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
const KB_DENY_MESSAGE = '知识库需要配置自己的 AI API Key，或由管理员单独开放后才能使用。'

/**
 * 公共知识库面板逻辑，三个论文工具页面共用。
 * 返回面板状态、处理器、以及检索增强方法。
 */
export function usePaperKbPanel(taskType: PaperTaskType, options?: { canUse?: () => boolean }) {
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

  function canUseKnowledgeBase(): boolean {
    return options?.canUse ? options.canUse() : true
  }

  function denyKnowledgeBase(): boolean {
    if (canUseKnowledgeBase()) return false
    error.value = KB_DENY_MESSAGE
    lastRetrievedCount.value = 0
    lastRetrievedPaperCount.value = 0
    lastRetrievedChunks.value = []
    return true
  }

  async function handleImport(files: File[]) {
    error.value = ''
    if (denyKnowledgeBase()) return
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
    if (denyKnowledgeBase()) return
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
  async function retrieveAndFormat(queryText: string, hints?: KbRetrievalHints, model?: string): Promise<string> {
    if (denyKnowledgeBase()) return ''
    const query = buildRetrievalQuery(queryText, hints)
    const chunks = await retrieveCandidates(query)
    if (chunks.length === 0) return ''

    if (taskType === 'paper_polish') {
      const evidenceChunks = await classifyEvidence(query, chunks, hints, model)
      lastRetrievedChunks.value = evidenceChunks
      lastRetrievedCount.value = evidenceChunks.length
      lastRetrievedPaperCount.value = new Set(evidenceChunks.map(c => c.paperTitle)).size
      return formatEvidenceContext(evidenceChunks)
    }

    lastRetrievedChunks.value = chunks
    lastRetrievedCount.value = chunks.length
    lastRetrievedPaperCount.value = new Set(chunks.map(c => c.paperTitle)).size
    // 将 ContextChunk 转为 CitedChunk（带索引和 citation 元数据）
    const citedChunks: CitedChunk[] = chunks.map((c, i) => ({
      index: i + 1,
      paperTitle: c.paperTitle,
      section: c.section,
      content: c.content,
    }))
    return formatCitedContext(citedChunks)
  }

  /**
   * 从知识库检索相关上下文，返回原始 chunk 数组。
   * 用于需要将 raw chunks 发送给后端的场景（AiReduce / PlagiarismReduce）。
   */
  async function retrieveRaw(queryText: string, hints?: KbRetrievalHints): Promise<ContextChunk[]> {
    if (denyKnowledgeBase()) return []
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
      const chunks = await retrieveCandidates(buildRetrievalQuery(queryText, hints))
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

  async function retrieveCandidates(query: string): Promise<ContextChunk[]> {
    return getRelevantContext(query, taskType, settings.value.topK)
  }

  async function classifyEvidence(
    query: string,
    chunks: ContextChunk[],
    hints?: KbRetrievalHints,
    model?: string
  ): Promise<ContextChunk[]> {
    try {
      const res = await authFetch('/api/paper-kb/evidence', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          queryText: query,
          focusText: [hints?.focusText, hints?.notes].filter(Boolean).join('\n'),
          model,
          chunks: chunks.map((chunk, i) => ({ ...chunk, index: i + 1 })),
        }),
      })
      const data = await readJsonResponse(res, '证据筛选失败')
      if (!res.ok) throw new Error(data.error || data.message || '证据筛选失败')
      const evidences = Array.isArray(data.evidences) ? data.evidences : []
      return assignCitationIndexes(evidences.length ? evidences : chunks)
    } catch (e) {
      error.value = e instanceof Error ? e.message : '证据筛选失败'
      return assignCitationIndexes(chunks.map((chunk, index) => ({
        ...chunk,
        supportLevel: (chunk.score ?? 0) >= 0.42 ? 'background_only' : 'irrelevant',
        confidence: 'low',
        reason: '证据筛选接口不可用或请求失败，已按安全策略降级为背景参考，不会自动生成正文引用。',
        index: index + 1,
      } as ContextChunk & { index: number })))
    }
  }

  function assignCitationIndexes(chunks: ContextChunk[]): ContextChunk[] {
    let citationIndex = 1
    return chunks.map(chunk => {
      const supportLevel = normalizeSupportLevel(chunk.supportLevel)
      const allowCitation = supportLevel === 'direct_support' && chunk.confidence !== 'low'
      return {
        ...chunk,
        supportLevel,
        confidence: normalizeConfidence(chunk.confidence),
        citationIndex: allowCitation ? citationIndex++ : undefined,
      }
    })
  }

  function formatEvidenceContext(chunks: ContextChunk[]): string {
    const directChunks = chunks.filter(c => c.citationIndex != null)
    if (!directChunks.length) {
      return [
        '知识库证据筛选结果：未找到可直接支撑当前观点的文献片段。',
        '请不要为了添加引用而使用仅背景相关或弱相关文献；可以继续润色，但不要新增文献引用标记。',
      ].join('\n')
    }

    const parts = directChunks.map(c => {
      const section = c.section ? `《${c.section}》` : ''
      const claim = c.supportedClaim ? `可支撑观点：${c.supportedClaim}\n` : ''
      const reason = c.reason ? `证据判断：${c.reason}\n` : ''
      return `[${c.citationIndex}] ${c.paperTitle}${section}\n${claim}${reason}证据片段：${c.content}`
    })

    return [
      '以下文献片段已通过证据筛选。只有这些片段允许在正文中标注对应的 [N] 引用：',
      '',
      ...parts,
      '',
      '引用规则：',
      '- 只有当句子观点被对应证据片段直接支持时，才在句末标注 [N]',
      '- 不要引用背景相关、弱相关或未列出的文献',
      '- 不要为了增加引用而改写、扩写或制造证据片段中没有的结论',
    ].join('\n')
  }
}

function normalizeSupportLevel(value?: string): 'direct_support' | 'background_only' | 'irrelevant' {
  return value === 'direct_support' || value === 'background_only' || value === 'irrelevant'
    ? value
    : 'background_only'
}

function normalizeConfidence(value?: string): 'high' | 'medium' | 'low' {
  return value === 'high' || value === 'medium' || value === 'low' ? value : 'low'
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
