import type { KbBackup } from './types'
import { paperKnowledgeDb } from './paperKnowledgeDb'

export async function exportBackup(): Promise<KbBackup> {
  const papers = await paperKnowledgeDb.papers.toArray()
  const chunks = await paperKnowledgeDb.chunks.toArray()

  return {
    version: 1,
    exportedAt: new Date().toISOString(),
    papers,
    chunks,
  }
}

export async function importBackup(backup: KbBackup): Promise<{ paperCount: number; chunkCount: number }> {
  if (backup.version !== 1) {
    throw new Error('不支持的备份版本')
  }

  await paperKnowledgeDb.transaction('rw', paperKnowledgeDb.papers, paperKnowledgeDb.chunks, async () => {
    await paperKnowledgeDb.papers.clear()
    await paperKnowledgeDb.chunks.clear()

    if (backup.papers.length > 0) {
      await paperKnowledgeDb.papers.bulkAdd(backup.papers)
    }
    if (backup.chunks.length > 0) {
      await paperKnowledgeDb.chunks.bulkAdd(backup.chunks)
    }
  })

  return {
    paperCount: backup.papers.length,
    chunkCount: backup.chunks.length,
  }
}

export function downloadBackupFile(backup: KbBackup): void {
  const json = JSON.stringify(backup, null, 2)
  const blob = new Blob([json], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `paper-kb-backup-${new Date().toISOString().slice(0, 10)}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
