/**
 * 剥离文本中的 [N] 引用标记。
 * 纯函数，用于导出前清洗，确保 DOCX 写回的是干净文本。
 */
export function stripCitationMarkers(text: string): string {
  if (!text) return text
  return text.replace(/\[\d+\]/g, '')
}
