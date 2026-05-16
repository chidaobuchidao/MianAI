<template>
  <div class="page">
    <div class="page__inner">
      <div class="page-head">
        <button class="back-btn" @click="$router.push('/')">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>
        <span class="page-head__title">简历优化</span>
        <div style="width:36px" />
      </div>

      <p class="page-desc">上传简历，AI 深度诊断项目薄弱点，给出具体优化建议</p>

      <div class="model-bar">
        <span class="model-label">模型</span>
        <div class="capsule-toggle">
          <button class="capsule-opt" :class="{ active: resumeModel === 'deepseek-v4-flash' }" @click="resumeModel = 'deepseek-v4-flash'">Flash</button>
          <button class="capsule-opt" :class="{ active: resumeModel === 'deepseek-v4-pro' }" @click="resumeModel = 'deepseek-v4-pro'">Pro</button>
        </div>
      </div>

      <!-- Upload zone with drag & drop -->
      <GlareCard background="var(--bg-paper)" borderRadius="20px" glareColor="#D9750A" :glareOpacity="0.06"
        :glareSize="200">
        <div class="upload-zone" :class="{ 'upload-zone--has-file': !!file }" @click="triggerUpload" @dragover.prevent
          @drop.prevent="onDrop">
          <template v-if="!file">
            <div class="upload-zone__icon">
              <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="1.2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="17 8 12 3 7 8" />
                <line x1="12" y1="3" x2="12" y2="15" />
              </svg>
            </div>
            <span class="upload-zone__title">点击或拖拽上传简历</span>
            <span class="upload-zone__hint">支持 PDF、DOCX 格式，文件不超过 10MB</span>
          </template>
          <template v-else>
            <div class="upload-zone__file">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" :stroke="fileIconColor" stroke-width="1.5">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
              </svg>
              <div>
                <span class="upload-zone__filename">{{ file.name }}</span>
                <span class="upload-zone__filesize">{{ formatSize(file.size) }}</span>
              </div>
              <button class="upload-zone__remove" @click.stop="file = null" title="移除">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
          </template>
        </div>
      </GlareCard>

      <input ref="fileInput" type="file" accept=".pdf,.doc,.docx" style="display:none" @change="onFileChange" />

      <!-- JD input -->
      <div class="jd-section" v-if="file">
        <label class="jd-label">目标岗位 JD（可选）</label>
        <textarea class="jd-textarea" v-model="jobDescription" placeholder="粘贴目标岗位的职位描述，AI 将针对性优化你的简历..." rows="4" />
      </div>

      <!-- Submit -->
      <button class="submit-btn" :class="{ 'submit-btn--disabled': !file || submitting }"
        :disabled="!file || submitting" @click="submitResume">
        <svg v-if="submitting" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" class="spin">
          <path d="M21 12a9 9 0 1 1-6.219-8.56" />
        </svg>
        <span>{{ submitting ? 'AI 分析中...' : '开始分析' }}</span>
      </button>

      <!-- Folder history section -->
      <div class="history-section" v-if="hasHistory">
        <div class="history-divider" />
        <div class="history-folder">
          <Folder :color="'#D9750A'" :size="1.4" :items="recentPositions.length > 0 ? recentPositions : ['暂无记录']"
            @paper-click="onHistoryPaperClick" />
        </div>
        <p class="history-hint">点击文件夹查看分析历史</p>
        <button class="history-link" @click="$router.push('/resume/history')">查看全部记录 →</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { get, postForm } from '@/utils/request'
import GlareCard from '@/components/GlareCard.vue'
import Folder from '@/components/Folder.vue'

const router = useRouter()
const fileInput = ref<HTMLInputElement>()
const file = ref<File | null>(null)
const jobDescription = ref('')
const resumeModel = ref('deepseek-v4-flash')
const submitting = ref(false)
const hasHistory = ref(false)

const fileIconColor = computed(() => {
  if (!file.value) return 'var(--accent)'
  const ext = file.value.name.split('.').pop()?.toLowerCase()
  return ext === 'pdf' ? '#EF4444' : '#3B82F6'
})

interface RecentResume { id: number; position: string; score?: number }
const recentResumes = ref<RecentResume[]>([])
const recentPositions = ref<string[]>([])

onMounted(async () => {
  try {
    const r = await get<RecentResume[]>('/api/resume/list')
    if (Array.isArray(r.data) && r.data.length > 0) {
      hasHistory.value = true
      recentResumes.value = r.data.slice(0, 3)
      recentPositions.value = recentResumes.value.map(item => {
        const score = item.score != null ? ` · ${item.score}分` : ''
        return `${item.position || '简历'}${score}`
      })
    }
  } catch { }
})

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function triggerUpload() { fileInput.value?.click() }

function onFileChange(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (!f) return
  const ext = f.name.split('.').pop()?.toLowerCase()
  if (!['pdf', 'doc', 'docx'].includes(ext || '')) {
    alert('仅支持 PDF、DOC、DOCX 格式')
    return
  }
  if (f.size > 10 * 1024 * 1024) {
    alert('文件不能超过 10MB')
    return
  }
  file.value = f
}

function onDrop(e: DragEvent) {
  const f = e.dataTransfer?.files?.[0]
  if (!f) return
  const ext = f.name.split('.').pop()?.toLowerCase()
  if (!['pdf', 'doc', 'docx'].includes(ext || '')) {
    alert('仅支持 PDF、DOC、DOCX 格式')
    return
  }
  file.value = f
}

function onHistoryPaperClick(index: number) {
  const resume = recentResumes.value[index]
  if (resume) {
    router.push(`/resume/report?resumeId=${resume.id}`)
  }
}

async function submitResume() {
  if (!file.value || submitting.value) return
  submitting.value = true
  try {
    const fd = new FormData()
    fd.append('file', file.value)
    fd.append('jobDescription', jobDescription.value.trim() || '请分析这份简历')
    fd.append('model', resumeModel.value)

    const res = await postForm<{ resumeId: number }>('/api/resume/upload', fd)
    const code = (res as any).code
    if (code === 200 && res.data) {
      router.push(`/resume/report?resumeId=${res.data.resumeId}`)
    } else {
      alert('上传失败，请重试')
    }
  } catch {
    alert('上传失败，请检查网络')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--bg-canvas);
}

.page__inner {
  max-width: 640px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0 12px;
}

.back-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-main);
}

.page-head__title {
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 600;
}

.page-desc {
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.6;
  margin-bottom: 20px;
  padding: 0 4px;
}

.model-bar {
  display: flex; align-items: center; justify-content: center;
  gap: 10px; margin-bottom: 24px;
}
.model-label { font-size: 13px; color: var(--text-light); }
.capsule-toggle {
  display: inline-flex;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-full);
  overflow: hidden;
  background: var(--bg-surface);
}
.capsule-opt {
  padding: 6px 18px;
  font-size: 13px; font-weight: 500;
  border: none; background: transparent;
  color: var(--text-muted); cursor: pointer;
  transition: all 0.15s;
}
.capsule-opt.active {
  background: var(--bg-dark); color: #fff;
}

/* Upload zone */
.upload-zone {
  padding: 48px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.upload-zone--has-file {
  padding: 28px 24px;
  min-height: auto;
}

.upload-zone__icon {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  background: rgba(217, 117, 10, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.upload-zone__title {
  font-size: 17px;
  font-weight: 500;
  display: block;
  margin-bottom: 6px;
}

.upload-zone__hint {
  font-size: 13px;
  color: var(--text-light);
}

.upload-zone__file {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
}

.upload-zone__filename {
  font-size: 15px;
  font-weight: 500;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 280px;
}

.upload-zone__filesize {
  font-size: 12px;
  color: var(--text-light);
  display: block;
  margin-top: 2px;
}

.upload-zone__remove {
  margin-left: auto;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--border-light);
  background: var(--bg-paper);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-light);
  cursor: pointer;
  transition: all 0.15s;
}

.upload-zone__remove:hover {
  background: var(--color-danger);
  color: #fff;
  border-color: var(--color-danger);
}

/* JD */
.jd-section {
  margin: 32px 0 32px;
}

.jd-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  display: block;
  margin-bottom: 10px;
}

.jd-textarea {
  width: 100%;
  min-height: 100px;
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 14px 16px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-main);
  outline: none;
  resize: vertical;
  font-family: inherit;
  transition: border-color 0.15s;
}

.jd-textarea:focus {
  border-color: var(--text-main);
}

.jd-textarea::placeholder {
  color: var(--text-light);
}

/* Submit */
.submit-btn {
  width: 100%;
  padding: 16px;
  background: var(--bg-dark);
  color: #fff;
  border: none;
  border-radius: var(--radius-lg);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  box-shadow: var(--shadow-md);
  transition: all 0.15s;
  margin-top: 24px;
  margin-bottom: 24px;
}

.submit-btn:hover {
  background: #2a2a28;
}

.submit-btn:active {
  transform: scale(0.98);
}

.submit-btn--disabled {
  opacity: 0.4;
  cursor: default;
  pointer-events: none;
}

.submit-btn {
  margin-bottom: 48px;
}

/* History folder section */
.history-section {
  text-align: center;
  /* 在上方预留空间，避免展开动画推挤上方控件 */
  margin-top: 60px;
  padding: 20px 0 60px;
}

.history-divider {
  width: 40px;
  height: 2px;
  background: var(--border-medium);
  margin: 0 auto 32px;
}

.history-folder {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.history-hint {
  font-size: 13px;
  color: var(--text-light);
  margin-bottom: 12px;
}

.history-link {
  font-size: 14px;
  color: var(--accent);
  font-weight: 500;
  background: none;
  border: none;
  cursor: pointer;
}

.history-link:hover {
  text-decoration: underline;
}


/* Spin */
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.spin {
  animation: spin 1s linear infinite;
}
</style>
