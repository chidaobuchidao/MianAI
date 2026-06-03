/**
 * Shared document state across Polish / AiReduce / PlagiarismReduce views.
 * Survives router.replace() navigation between the three paper tools.
 * File binary data is stored in IndexedDB (sessionStorage has ~5MB limit).
 */

interface PaperDoc {
  sourceText: string
  paragraphs: any[]
  fileName: string
  timestamp: number
}

const KEY = 'paper_doc'
const DB_NAME = 'paper_store'
const FILE_STORE = 'files'

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, 1)
    req.onupgradeneeded = () => req.result.createObjectStore(FILE_STORE)
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

async function saveFileBinary(fileName: string, data: ArrayBuffer): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(FILE_STORE, 'readwrite')
    tx.objectStore(FILE_STORE).put(data, fileName)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

async function loadFileBinary(fileName: string): Promise<File | null> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(FILE_STORE, 'readonly')
    const req = tx.objectStore(FILE_STORE).get(fileName)
    req.onsuccess = () => {
      if (req.result) {
        const ext = fileName.split('.').pop()?.toLowerCase() || 'bin'
        const mime = ext === 'docx' ? 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
          : ext === 'pdf' ? 'application/pdf' : 'application/octet-stream'
        resolve(new File([req.result], fileName, { type: mime }))
      } else {
        resolve(null)
      }
    }
    req.onerror = () => reject(req.error)
  })
}

async function clearFileBinary(fileName: string): Promise<void> {
  try {
    const db = await openDB()
    const tx = db.transaction(FILE_STORE, 'readwrite')
    tx.objectStore(FILE_STORE).delete(fileName)
  } catch { /* ignore */ }
}

export function usePaperStore() {
  async function save(doc: PaperDoc, file?: File | null) {
    try {
      sessionStorage.setItem(KEY, JSON.stringify(doc))
    } catch { /* quota exceeded — ignore */ }
    if (file) {
      try {
        const buf = await file.arrayBuffer()
        await saveFileBinary(doc.fileName, buf)
      } catch { /* ignore */ }
    }
  }

  function load(): PaperDoc | null {
    try {
      const raw = sessionStorage.getItem(KEY)
      if (!raw) return null
      return JSON.parse(raw) as PaperDoc
    } catch {
      return null
    }
  }

  async function loadFile(): Promise<File | null> {
    const doc = load()
    if (!doc) return null
    try {
      return await loadFileBinary(doc.fileName)
    } catch {
      return null
    }
  }

  function clear() {
    const doc = load()
    try { sessionStorage.removeItem(KEY) } catch { /* ignore */ }
    if (doc) clearFileBinary(doc.fileName)
  }

  return { save, load, loadFile, clear }
}
