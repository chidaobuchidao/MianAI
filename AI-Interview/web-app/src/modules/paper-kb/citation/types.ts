/**
 * 论文引用元数据。
 * 上传时从论文文本中提取，存储在 LocalPaper.citation。
 */
export interface PaperCitation {
  authors: string[]
  year?: number
  journal?: string
  doi?: string
  rawReference?: string
}

/**
 * 带引用索引的检索 chunk。
 * 用于 citationFormatter 生成带 [N] 标记的 context 块。
 */
export interface CitedChunk {
  index: number
  paperTitle: string
  citation?: PaperCitation
  section?: string
  content: string
}
