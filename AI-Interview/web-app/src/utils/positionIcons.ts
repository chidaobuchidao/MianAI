export const POSITION_ICONS: Record<string, string> = {
  'Java 后端开发':
    '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#D9750A" stroke-width="1.5"><path d="M18 8h1a4 4 0 0 1 0 8h-1"/><path d="M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z"/><line x1="6" y1="1" x2="6" y2="4"/><line x1="10" y1="1" x2="10" y2="4"/><line x1="14" y1="1" x2="14" y2="4"/></svg>',
  '前端开发工程师':
    '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#4A4A4A" stroke-width="1.5"><circle cx="12" cy="12" r="2"/><path d="M16.24 7.76a6 6 0 0 1 0 8.49m-8.48 0a6 6 0 0 1 0-8.49m11.31-2.82a10 10 0 0 1 0 14.14m-14.14 0a10 10 0 0 1 0-14.14"/></svg>',
  '算法工程师':
    '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#4A4A4A" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/><line x1="8" y1="6" x2="8" y2="3"/></svg>',
  '数据分析师':
    '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#4A4A4A" stroke-width="1.5"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>',
  'DevOps 工程师':
    '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#4A4A4A" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>'
}

const FALLBACK_KEY = 'DevOps 工程师'

export function getPosIcon(position: string): string {
  return POSITION_ICONS[position] || POSITION_ICONS[FALLBACK_KEY]
}
