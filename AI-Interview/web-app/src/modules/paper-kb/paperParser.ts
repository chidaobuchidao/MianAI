export interface ParsedPaper {
  title: string
  fullText: string
  fileType: 'pdf' | 'docx' | 'txt' | 'md'
  wordCount: number
  fingerprint: string
}

function countWords(text: string): number {
  if (!text.trim()) return 0
  // Chinese: count each CJK character as a word
  const cjkChars = text.match(/[一-鿿㐀-䶿]/g)
  const cjkCount = cjkChars ? cjkChars.length : 0
  // Non-CJK: count space-separated tokens
  const nonCjk = text.replace(/[一-鿿㐀-䶿]/g, ' ')
  const latinTokens = nonCjk.split(/\s+/).filter(w => w.length > 0)
  return cjkCount + latinTokens.length
}

function computeFingerprint(file: File): string {
  return `${file.name}::${file.size}::${file.lastModified}`
}

async function parsePdf(file: File): Promise<ParsedPaper> {
  const arrayBuffer = await file.arrayBuffer()
  const data = new Uint8Array(arrayBuffer)
  const pdfjsLib = await import('pdfjs-dist/legacy/build/pdf.mjs')
  const pdf = await pdfjsLib.getDocument({
    data,
    disableWorker: true,
    useWorkerFetch: false,
  } as any).promise

  const pageTexts: string[] = []
  try {
    for (let i = 1; i <= pdf.numPages; i++) {
      const page = await pdf.getPage(i)
      const content = await page.getTextContent()
      const text = content.items
        .map(item => ('str' in item ? item.str : ''))
        .join(' ')
      pageTexts.push(text)
      page.cleanup()
    }
  } finally {
    await pdf.destroy()
  }

  const fullText = normalizeParsedText(pageTexts.join('\n\n'))
  if (fullText.length < 10) {
    throw new Error('PDF未提取到有效文本，可能是扫描版图片或加密文档')
  }

  return {
    title: file.name.replace(/\.pdf$/i, ''),
    fullText,
    fileType: 'pdf',
    wordCount: countWords(fullText),
    fingerprint: computeFingerprint(file),
  }
}

function normalizeParsedText(text: string): string {
  return text
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n[ \t]+/g, '\n')
    .replace(/[ \t]{2,}/g, ' ')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

async function parseDocx(file: File): Promise<ParsedPaper> {
  const mammoth = await import('mammoth')
  const arrayBuffer = await file.arrayBuffer()
  const result = await mammoth.extractRawText({ arrayBuffer })

  return {
    title: file.name.replace(/\.docx$/i, ''),
    fullText: result.value,
    fileType: 'docx',
    wordCount: countWords(result.value),
    fingerprint: computeFingerprint(file),
  }
}

async function parseText(file: File): Promise<ParsedPaper> {
  const text = await file.text()
  return {
    title: file.name.replace(/\.(txt|md)$/i, ''),
    fullText: text,
    fileType: file.name.endsWith('.md') ? 'md' : 'txt',
    wordCount: countWords(text),
    fingerprint: computeFingerprint(file),
  }
}

export async function parsePaperFile(file: File): Promise<ParsedPaper> {
  const ext = file.name.toLowerCase()

  if (ext.endsWith('.pdf')) {
    return parsePdf(file)
  }
  if (ext.endsWith('.docx')) {
    return parseDocx(file)
  }
  if (ext.endsWith('.txt') || ext.endsWith('.md')) {
    return parseText(file)
  }

  throw new Error(`不支持的文件格式: ${file.name}`)
}
