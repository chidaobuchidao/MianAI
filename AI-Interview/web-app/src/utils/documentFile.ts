export function isDocxFile(file?: File | null): boolean {
  return !!file && file.name.toLowerCase().endsWith('.docx')
}

export function isPdfFile(file?: File | null): boolean {
  return !!file && file.name.toLowerCase().endsWith('.pdf')
}
