<template>
  <Teleport to="body">
    <Transition name="kb-overlay">
      <div v-if="visible" class="kb-overlay" @click.self="$emit('close')" />
    </Transition>
    <Transition name="kb-panel">
      <div v-if="visible" class="kb-panel">
        <div class="kb-header">
          <div class="kb-title-row">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
            <span class="kb-title">本地知识库</span>
            <span class="kb-count" v-if="papers.length">{{ papers.length }} 篇</span>
          </div>
          <button class="kb-close" @click="$emit('close')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>

        <!-- Import area -->
        <div
          class="kb-dropzone"
          :class="{ 'kb-dropzone--active': isDragOver }"
          @dragover.prevent="isDragOver = true"
          @dragleave="isDragOver = false"
          @drop.prevent="handleDrop"
          @click="triggerFileInput"
        >
          <input
            ref="fileInputRef"
            type="file"
            accept=".pdf,.docx,.txt,.md"
            multiple
            style="display: none"
            @change="handleFileSelect"
          />
          <div v-if="isLoading" class="kb-dropzone-loading">
            <div class="kb-spinner" />
            <span>正在解析: {{ importingFile }}</span>
          </div>
          <div v-else class="kb-dropzone-idle">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--text-light)" stroke-width="1.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
            <span>拖拽或点击上传论文</span>
            <span class="kb-dropzone-hint">支持 PDF / DOCX / TXT / MD</span>
          </div>
        </div>
        <div v-if="error" class="kb-error">{{ error }}</div>

        <!-- Paper list -->
        <div class="kb-list" v-if="papers.length > 0">
          <div v-for="paper in papers" :key="paper.id" class="kb-paper-item">
            <div class="kb-paper-icon">
              <svg v-if="paper.fileType === 'pdf'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#E53E3E" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              <svg v-else-if="paper.fileType === 'docx'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#3182CE" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#718096" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            </div>
            <div class="kb-paper-info">
              <div class="kb-paper-title">{{ paper.title }}</div>
              <div class="kb-paper-meta">{{ paper.chunkCount }} 段 · {{ formatWords(paper.wordCount) }}</div>
            </div>
            <button class="kb-paper-delete" @click="handleDelete(paper.id!)" title="删除">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            </button>
          </div>
        </div>

        <!-- Empty state -->
        <div v-else-if="!isLoading" class="kb-empty">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--text-light)" stroke-width="1" opacity="0.4"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
          <span>暂无论文，上传后 AI 可参考知识库内容</span>
        </div>

        <!-- Settings -->
        <div class="kb-settings" v-if="papers.length > 0">
          <div class="kb-setting-row">
            <label class="kb-setting-label">检索数量 (Top-K)</label>
            <div class="kb-setting-control">
              <input
                type="range"
                :value="settings.topK"
                min="1"
                max="10"
                step="1"
                class="kb-range"
                @input="$emit('update:settings', { ...settings, topK: +($event.target as HTMLInputElement).value })"
              />
              <span class="kb-setting-value">{{ settings.topK }}</span>
            </div>
          </div>
          <label class="kb-checkbox-row">
            <input
              type="checkbox"
              :checked="settings.autoRetrieve"
              @change="$emit('update:settings', { ...settings, autoRetrieve: ($event.target as HTMLInputElement).checked })"
            />
            <span>自动检索知识库</span>
          </label>
        </div>

        <!-- Actions -->
        <div class="kb-actions">
          <button class="kb-action-btn" @click="$emit('backup')">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
            导出备份
          </button>
          <button class="kb-action-btn" @click="triggerRestoreInput">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
            恢复备份
          </button>
          <input ref="restoreInputRef" type="file" accept=".json" style="display: none" @change="handleRestore" />
          <button v-if="papers.length > 0" class="kb-action-btn kb-action-danger" @click="handleClearAll">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            清空全部
          </button>
        </div>

        <!-- Privacy notice -->
        <div class="kb-privacy">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
          <span>资料仅保存在当前浏览器本地，清除浏览器数据或更换设备后需要重新导入或恢复备份。</span>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { LocalPaper } from '@/modules/paper-kb'

export interface KbSettings {
  topK: number
  autoRetrieve: boolean
}

defineProps<{
  visible: boolean
  papers: LocalPaper[]
  isLoading: boolean
  importingFile: string | null
  settings: KbSettings
  error?: string | null
}>()

const emit = defineEmits<{
  close: []
  import: [files: File[]]
  delete: [paperId: number]
  clearAll: []
  backup: []
  restore: [file: File]
  'update:settings': [settings: KbSettings]
}>()

const isDragOver = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const restoreInputRef = ref<HTMLInputElement | null>(null)

function triggerFileInput() {
  fileInputRef.value?.click()
}

function triggerRestoreInput() {
  restoreInputRef.value?.click()
}

function handleDrop(e: DragEvent) {
  isDragOver.value = false
  const files = Array.from(e.dataTransfer?.files ?? [])
  if (files.length > 0) {
    emit('import', files)
  }
}

function handleFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  if (files.length > 0) {
    emit('import', files)
  }
  input.value = ''
}

function handleDelete(paperId: number) {
  emit('delete', paperId)
}

function handleClearAll() {
  if (confirm('确定清空全部知识库数据？此操作不可撤销。')) {
    emit('clearAll')
  }
}

function handleRestore(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    emit('restore', file)
  }
  input.value = ''
}

function formatWords(count: number): string {
  if (count >= 10000) return `${(count / 10000).toFixed(1)}万字`
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k字`
  return `${count}字`
}
</script>

<style scoped>
.kb-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 999;
  backdrop-filter: blur(2px);
}

.kb-panel {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 380px;
  max-width: 100vw;
  background: var(--bg-paper, #FDFCFB);
  box-shadow: -8px 0 32px rgba(0, 0, 0, 0.12);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.kb-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light, rgba(0,0,0,0.06));
  flex-shrink: 0;
}

.kb-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.kb-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main, #141413);
}

.kb-count {
  font-size: 11px;
  color: var(--text-light, #999);
  background: var(--bg-surface, #F5F4F1);
  padding: 2px 8px;
  border-radius: 100px;
}

.kb-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: none;
  color: var(--text-muted, #555);
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.15s;
}
.kb-close:hover { background: var(--bg-surface, #F5F4F1); }

/* Dropzone */
.kb-dropzone {
  margin: 16px 20px 0;
  border: 2px dashed var(--border-medium, rgba(0,0,0,0.12));
  border-radius: 12px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  min-height: 80px;
}
.kb-dropzone:hover { border-color: var(--accent, #D9750A); background: rgba(217,117,10,0.03); }
.kb-dropzone--active { border-color: var(--accent, #D9750A); background: rgba(217,117,10,0.06); }

.kb-error {
  margin: 10px 20px 0;
  padding: 10px 12px;
  border: 1px solid rgba(220, 38, 38, 0.18);
  border-radius: 8px;
  background: rgba(220, 38, 38, 0.06);
  color: #DC2626;
  font-size: 12px;
  line-height: 1.5;
  word-break: break-word;
}

.kb-dropzone-idle {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-muted, #555);
}

.kb-dropzone-hint {
  font-size: 11px;
  color: var(--text-light, #999);
}

.kb-dropzone-loading {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--accent, #D9750A);
}

.kb-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid var(--border-medium, rgba(0,0,0,0.12));
  border-top-color: var(--accent, #D9750A);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Paper list */
.kb-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 20px;
}

.kb-paper-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  transition: background 0.15s;
}
.kb-paper-item:hover { background: var(--bg-surface, #F5F4F1); }

.kb-paper-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-surface, #F5F4F1);
  border-radius: 6px;
}

.kb-paper-info {
  flex: 1;
  min-width: 0;
}

.kb-paper-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-main, #141413);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kb-paper-meta {
  font-size: 11px;
  color: var(--text-light, #999);
  margin-top: 2px;
}

.kb-paper-delete {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: none;
  color: var(--text-light, #999);
  cursor: pointer;
  border-radius: 6px;
  opacity: 0;
  transition: all 0.15s;
}
.kb-paper-item:hover .kb-paper-delete { opacity: 1; }
.kb-paper-delete:hover { background: rgba(220, 38, 38, 0.08); color: #DC2626; }

/* Empty state */
.kb-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 13px;
  color: var(--text-light, #999);
  padding: 40px 20px;
}

/* Settings */
.kb-settings {
  padding: 12px 20px;
  border-top: 1px solid var(--border-light, rgba(0,0,0,0.06));
  flex-shrink: 0;
}

.kb-setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.kb-setting-label {
  font-size: 12px;
  color: var(--text-muted, #555);
  font-weight: 500;
}

.kb-setting-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.kb-range {
  width: 80px;
  accent-color: var(--accent, #D9750A);
}

.kb-setting-value {
  font-size: 12px;
  font-weight: 600;
  color: var(--accent, #D9750A);
  min-width: 16px;
  text-align: center;
}

.kb-checkbox-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-muted, #555);
  cursor: pointer;
}
.kb-checkbox-row input[type="checkbox"] {
  accent-color: var(--accent, #D9750A);
}

/* Actions */
.kb-actions {
  display: flex;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid var(--border-light, rgba(0,0,0,0.06));
  flex-shrink: 0;
  flex-wrap: wrap;
}

.kb-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid var(--border-medium, rgba(0,0,0,0.12));
  border-radius: 8px;
  background: var(--bg-paper, #FDFCFB);
  color: var(--text-muted, #555);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.kb-action-btn:hover { background: var(--bg-surface, #F5F4F1); color: var(--text-main, #141413); }
.kb-action-danger:hover { background: rgba(220, 38, 38, 0.06); color: #DC2626; border-color: rgba(220, 38, 38, 0.25); }

/* Privacy */
.kb-privacy {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 12px 20px;
  font-size: 11px;
  color: var(--text-light, #999);
  line-height: 1.5;
  border-top: 1px solid var(--border-light, rgba(0,0,0,0.06));
  flex-shrink: 0;
}
.kb-privacy svg { flex-shrink: 0; margin-top: 1px; }

/* Transitions */
.kb-overlay-enter-active, .kb-overlay-leave-active { transition: opacity 0.25s ease; }
.kb-overlay-enter-from, .kb-overlay-leave-to { opacity: 0; }

.kb-panel-enter-active { transition: transform 0.3s cubic-bezier(0.25, 1, 0.4, 1); }
.kb-panel-leave-active { transition: transform 0.2s ease-in; }
.kb-panel-enter-from { transform: translateX(100%); }
.kb-panel-leave-to { transform: translateX(100%); }

@media (max-width: 480px) {
  .kb-panel { width: 100vw; }
}
</style>
