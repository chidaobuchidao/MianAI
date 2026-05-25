<template>
  <div class="page">
    <div class="page__inner">
      <div class="page-head">
        <button class="back-btn" @click="$router.push('/profile')">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>
        <span class="page-head__title">管理后台</span>
        <span class="page-head__role">Admin</span>
      </div>

      <!-- Stats -->
      <div class="stats-row">
        <div class="stat-card">
          <span class="stat-card__num">{{ status.totalUsers }}</span>
          <span class="stat-card__label">总用户</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__num">{{ status.totalSessions }}</span>
          <span class="stat-card__label">总面试</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__num">{{ status.usersWithKey }}</span>
          <span class="stat-card__label">已配置Key</span>
        </div>
        <div class="stat-card" :class="{ 'stat-card--ok': status.hasSystemKey, 'stat-card--warn': !status.hasSystemKey }">
          <span class="stat-card__num">{{ status.hasSystemKey ? '已配置' : '未配置' }}</span>
          <span class="stat-card__label">系统Key</span>
        </div>
      </div>

      <!-- User Table -->
      <div class="section">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px;gap:10px;flex-wrap:wrap">
          <h3 class="section__title" style="margin-bottom:0">用户列表 ({{ userTotal }})</h3>
          <input class="search-input" v-model="userKeyword" placeholder="搜索昵称/用户名..." @input="debounceLoadUsers" style="max-width:200px" />
        </div>
        <div class="table-wrap">
          <table class="tbl">
            <thead>
              <tr>
                <th>ID</th><th>昵称</th><th>角色</th><th>Key</th><th>日配额</th><th>面试</th><th>时间</th><th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in users" :key="u.id">
                <td>{{ u.id }}</td>
                <td>{{ u.nickname }}</td>
                <td><span class="tag" :class="u.role === '管理员' ? 'tag--admin' : 'tag--user'">{{ u.role }}</span></td>
                <td>{{ u.hasApiKey ? '已配置' : '未配置' }}</td>
                <td>
                  <span class="quota-cell" @click="startEditQuota(u)" title="点击设置剩余次数">{{ remaining(u) }}</span>
                </td>
                <td>{{ u.interviewCount }}</td>
                <td class="tbl__time">{{ formatTime(u.createTime) }}</td>
                <td class="tbl__actions">
                  <button class="mini-btn" @click="startEditQuota(u)">剩余</button>
                  <button class="mini-btn" @click="startEditLimit(u)" :title="'日上限：' + (u.dailyQuota ?? 10)">上限</button>
                  <button class="mini-btn" @click="toggleAdmin(u)">{{ u.role === '管理员' ? '降级' : '升管' }}</button>
                  <button class="mini-btn mini-btn--danger" @click="deleteUser(u)">删除</button>
                </td>
              </tr>
              <tr v-if="users.length === 0"><td colspan="8" class="tbl__empty">暂无用户</td></tr>
            </tbody>
          </table>
        </div>
        <div class="pager" v-if="userTotal > userPageSize">
          <button :disabled="userPage <= 1" @click="userPage--; loadUsers()">上一页</button>
          <span>{{ userPage }} / {{ Math.ceil(userTotal / userPageSize) }}</span>
          <button :disabled="userPage >= Math.ceil(userTotal / userPageSize)" @click="userPage++; loadUsers()">下一页</button>
        </div>
      </div>

      <!-- Sessions Table -->
      <div class="section">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px;gap:10px;flex-wrap:wrap">
          <h3 class="section__title" style="margin-bottom:0">面试记录 ({{ sessionTotal }})</h3>
          <input class="search-input" v-model="sessionKeyword" placeholder="搜索岗位..." @input="debounceLoadSessions" style="max-width:200px" />
        </div>
        <div class="table-wrap">
          <table class="tbl">
            <thead>
              <tr>
                <th>ID</th><th>用户</th><th>岗位</th><th>分数</th><th>状态</th><th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in sessions" :key="s.id">
                <td>{{ s.id }}</td>
                <td>{{ s.userName }}</td>
                <td>{{ s.position }}</td>
                <td>{{ s.score ?? '-' }}</td>
                <td><span class="tag" :class="s.status === '已结束' ? 'tag--ok' : 'tag--warn'">{{ s.status }}</span></td>
                <td class="tbl__time">{{ formatTime(s.createTime) }}</td>
              </tr>
              <tr v-if="sessions.length === 0"><td colspan="6" class="tbl__empty">暂无记录</td></tr>
            </tbody>
          </table>
        </div>
        <div class="pager" v-if="sessionTotal > sessionPageSize">
          <button :disabled="sessionPage <= 1" @click="sessionPage--; loadSessions()">上一页</button>
          <span>{{ sessionPage }} / {{ Math.ceil(sessionTotal / sessionPageSize) }}</span>
          <button :disabled="sessionPage >= Math.ceil(sessionTotal / sessionPageSize)" @click="sessionPage++; loadSessions()">下一页</button>
        </div>
      </div>

      <!-- Announcements -->
      <div class="section">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px">
          <h3 class="section__title" style="margin-bottom:0">公告管理</h3>
          <button class="btn btn--dark" style="padding:6px 16px;font-size:12px" @click="showAnnEditor = true; editingAnn = null">新建公告</button>
        </div>
        <div class="table-wrap">
          <table class="tbl">
            <thead>
              <tr><th>ID</th><th>标题</th><th>状态</th><th>时间</th><th>操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="a in announcements" :key="a.id">
                <td>{{ a.id }}</td>
                <td>{{ a.title }}</td>
                <td><span class="tag" :class="a.isPublished ? 'tag--ok' : 'tag--warn'">{{ a.isPublished ? '已发布' : '已下架' }}</span></td>
                <td class="tbl__time">{{ formatTime(a.createTime) }}</td>
                <td class="tbl__actions">
                  <button class="mini-btn" @click="editAnnouncement(a)">编辑</button>
                  <button class="mini-btn" @click="toggleAnnPublish(a)">{{ a.isPublished ? '下架' : '发布' }}</button>
                  <button class="mini-btn mini-btn--danger" @click="deleteAnnouncement(a)">删除</button>
                </td>
              </tr>
              <tr v-if="announcements.length === 0"><td colspan="5" class="tbl__empty">暂无公告</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Announcement editor modal -->
      <div class="modal-overlay" v-if="showAnnEditor" @click.self="showAnnEditor = false">
        <div class="modal-card animate-scale-in" style="width:560px;max-height:80vh;overflow-y:auto">
          <span class="modal-title">{{ editingAnn ? '编辑公告' : '新建公告' }}</span>
          <input class="modal-input" v-model="annForm.title" placeholder="公告标题" style="margin-bottom:8px" />
          <textarea class="modal-input" v-model="annForm.content" placeholder="公告内容（支持Markdown/HTML/图片链接）" rows="10" style="resize:vertical;font-family:monospace;font-size:13px"></textarea>
          <div class="modal-actions">
            <button class="btn btn--ghost" @click="showAnnEditor = false">取消</button>
            <button class="btn btn--dark" @click="saveAnnouncement">保存</button>
          </div>
        </div>
      </div>

      <!-- Quota edit modal -->
      <div class="modal-overlay" v-if="editingQuota" @click.self="editingQuota = null">
        <div class="modal-card animate-scale-in">
          <span class="modal-title">{{ editingQuota.mode === 'remaining' ? '设置剩余次数' : '设置每日上限' }} — {{ editingQuota.user.nickname }}</span>
          <p style="font-size:12px;color:var(--text-light);margin-bottom:12px" v-if="editingQuota.mode === 'remaining'">日配额上限 {{ editingQuota.user.dailyQuota ?? 10 }} 次</p>
          <input class="modal-input" v-model="editingQuota.value" type="number" min="0" @keydown.enter="saveQuota" />
          <div class="modal-actions">
            <button class="btn btn--ghost" @click="editingQuota = null">取消</button>
            <button class="btn btn--dark" @click="saveQuota">保存</button>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="section">
        <h3 class="section__title">操作</h3>
        <div class="actions">
          <button class="act-btn act-btn--danger" @click="clearSessions">清空面试记录</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { get, post, put, del } from '@/utils/request'

interface Status { totalUsers: number; totalSessions: number; hasSystemKey: boolean; usersWithKey: number }
interface UserRow { id: number; nickname: string; role: string; hasApiKey: boolean; dailyQuota: number; quotaUsed: number; createTime: string; interviewCount: number }
interface SessionRow { id: number; userName: string; position: string; score: number | null; status: string; createTime: string }

const router = useRouter()
const userStore = useUserStore()

const status = ref<Status>({ totalUsers: 0, totalSessions: 0, hasSystemKey: false, usersWithKey: 0 })
const users = ref<UserRow[]>([])
const sessions = ref<SessionRow[]>([])
const userTotal = ref(0); const userPage = ref(1); const userPageSize = 20; const userKeyword = ref('')
const sessionTotal = ref(0); const sessionPage = ref(1); const sessionPageSize = 20; const sessionKeyword = ref('')
let userTimer: any = null, sessionTimer: any = null

function debounceLoadUsers() {
  clearTimeout(userTimer); userTimer = setTimeout(() => { userPage.value = 1; loadUsers() }, 300)
}
function debounceLoadSessions() {
  clearTimeout(sessionTimer); sessionTimer = setTimeout(() => { sessionPage.value = 1; loadSessions() }, 300)
}

async function loadUsers() {
  const res = await get<any>(`/api/admin/users?page=${userPage.value}&pageSize=${userPageSize}&keyword=${encodeURIComponent(userKeyword.value)}`)
  if (res.data) { users.value = res.data.list || []; userTotal.value = res.data.total || 0 }
}
async function loadSessions() {
  const res = await get<any>(`/api/admin/sessions?page=${sessionPage.value}&pageSize=${sessionPageSize}&keyword=${encodeURIComponent(sessionKeyword.value)}`)
  if (res.data) { sessions.value = res.data.list || []; sessionTotal.value = res.data.total || 0 }
}

onMounted(async () => {
  if (!userStore.isAdmin) {
    router.replace('/profile')
    return
  }
  try {
    const [s, u, ss] = await Promise.all([
      get<Status>('/api/admin/status'),
      get<any>(`/api/admin/users?page=1&pageSize=${userPageSize}`),
      get<any>(`/api/admin/sessions?page=1&pageSize=${sessionPageSize}`)
    ])
    if (s.data) status.value = s.data
    if (u.data) { users.value = u.data.list || []; userTotal.value = u.data.total || 0 }
    if (ss.data) { sessions.value = ss.data.list || []; sessionTotal.value = ss.data.total || 0 }
    loadAnnouncements()
  } catch {
    router.replace('/profile')
  }
})

interface AnnRow { id: number; title: string; content: string; isPublished: number; createTime: string }
const announcements = ref<AnnRow[]>([])
const showAnnEditor = ref(false)
const editingAnn = ref<AnnRow | null>(null)
const annForm = ref({ title: '', content: '' })

async function loadAnnouncements() {
  try { const r = await get<AnnRow[]>('/api/admin/announcements'); if (r.data) announcements.value = r.data } catch {}
}
function editAnnouncement(a: AnnRow) { editingAnn.value = a; annForm.value = { title: a.title, content: a.content }; showAnnEditor.value = true }
async function saveAnnouncement() {
  if (!annForm.value.title.trim()) return
  if (editingAnn.value) {
    await put(`/api/admin/announcement/${editingAnn.value.id}`, annForm.value)
  } else {
    await post('/api/admin/announcement', annForm.value)
  }
  showAnnEditor.value = false
  await loadAnnouncements()
}
async function toggleAnnPublish(a: AnnRow) {
  await post(`/api/admin/announcement/${a.id}/publish`)
  a.isPublished = a.isPublished ? 0 : 1
}
async function deleteAnnouncement(a: AnnRow) {
  if (!confirm(`确定删除公告「${a.title}」？`)) return
  await del(`/api/admin/announcement/${a.id}`)
  announcements.value = announcements.value.filter(x => x.id !== a.id)
}

function formatTime(t: string): string {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
}

const editingQuota = ref<{ user: UserRow; value: string; mode: 'remaining' | 'limit' } | null>(null)

function remaining(u: UserRow): number {
  return Math.max(0, (u.dailyQuota ?? 10) - (u.quotaUsed ?? 0))
}

function startEditQuota(u: UserRow) {
  editingQuota.value = { user: u, value: String(remaining(u)), mode: 'remaining' }
}

function startEditLimit(u: UserRow) {
  editingQuota.value = { user: u, value: String(u.dailyQuota ?? 10), mode: 'limit' }
}

async function saveQuota() {
  if (!editingQuota.value) return
  const { user, value, mode } = editingQuota.value
  if (!/^\d+$/.test(value)) return
  const num = parseInt(value)
  if (mode === 'remaining') {
    await post('/api/admin/set-quota', { userId: user.id, remaining: num })
    user.quotaUsed = Math.max(0, (user.dailyQuota ?? 10) - num)
  } else {
    await post('/api/admin/set-limit', { userId: user.id, limit: num })
    user.dailyQuota = num
  }
  editingQuota.value = null
}

async function toggleAdmin(u: UserRow) {
  if (!confirm(`确定${u.role === '管理员' ? '取消' : '设为'}管理员：${u.nickname}？`)) return
  await post('/api/admin/toggle-admin', { userId: u.id })
  u.role = u.role === '管理员' ? '普通用户' : '管理员'
}

async function deleteUser(u: UserRow) {
  if (!confirm(`确定删除用户：${u.nickname}？此操作不可撤销！`)) return
  await post('/api/admin/delete-user', { userId: u.id })
  await loadUsers()
  const s = await get<Status>('/api/admin/status'); if (s.data) status.value = s.data
}

async function clearSessions() {
  if (!confirm('确定清空所有面试记录？')) return
  await post('/api/admin/clear-sessions')
  status.value.totalSessions = 0
  sessionTotal.value = 0; sessions.value = []
  await loadSessions()
}
</script>

<style scoped>
.page { min-height: 100vh; background: var(--bg-canvas); }
.page__inner { max-width: 800px; margin: 0 auto; padding: 0 20px; }
.page-head {
  display: flex; align-items: center; gap: 12px;
  padding: 16px 0 24px;
}
.back-btn {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-main); flex-shrink: 0;
}
.page-head__title {
  font-family: var(--font-serif); font-size: 20px; font-weight: 600; flex: 1;
}
.page-head__role {
  font-size: 11px; font-weight: 600;
  padding: 3px 10px; border-radius: 100px;
  background: rgba(217,117,10,0.1); color: var(--accent);
}

.stats-row { display: grid; grid-template-columns: 1fr 1fr 1fr 1fr; gap: 10px; margin-bottom: 28px; }
.stat-card {
  background: var(--bg-paper); border: 1px solid var(--border-light);
  border-radius: var(--radius-lg); padding: 16px; text-align: center;
}
.stat-card__num { font-size: 22px; font-weight: 700; display: block; margin-bottom: 2px; }
.stat-card__label { font-size: 12px; color: var(--text-light); }
.stat-card--ok .stat-card__num { color: var(--color-success); }
.stat-card--warn .stat-card__num { color: var(--color-danger); }

.section { margin-bottom: 24px; }
.section__title {
  font-family: var(--font-serif); font-size: 16px; font-weight: 600; margin-bottom: 10px;
}
.table-wrap {
  background: var(--bg-paper); border: 1px solid var(--border-light);
  border-radius: var(--radius-lg); overflow-x: auto;
}
.tbl { width: 100%; border-collapse: collapse; font-size: 13px; }
.tbl th {
  text-align: left; padding: 10px 14px;
  font-weight: 600; color: var(--text-light); font-size: 11px;
  text-transform: uppercase; letter-spacing: 0.5px;
  border-bottom: 1px solid var(--border-light); background: var(--bg-surface);
}
.tbl td { padding: 9px 14px; border-bottom: 1px solid var(--border-light); color: var(--text-muted); }
.tbl tr:last-child td { border-bottom: none; }
.tbl__time { font-size: 12px; color: var(--text-light); white-space: nowrap; }
.tbl__empty { text-align: center; padding: 32px !important; color: var(--text-light); }

.tag { font-size: 11px; padding: 2px 8px; border-radius: 100px; font-weight: 500; }
.tag--admin { background: rgba(217,117,10,0.1); color: var(--accent); }
.tag--user { background: var(--bg-surface); color: var(--text-muted); }
.tag--ok { background: rgba(34,197,94,0.08); color: var(--color-success); }
.tag--warn { background: rgba(217,117,10,0.08); color: var(--accent); }
.quota-cell { cursor: pointer; border-bottom: 1px dashed var(--border-medium); }
.quota-cell:hover { color: var(--accent); }
.tbl__actions { display: flex; gap: 4px; white-space: nowrap; }
.mini-btn {
  padding: 3px 8px; border-radius: 4px; border: 1px solid var(--border-light);
  background: var(--bg-surface); font-size: 11px; cursor: pointer;
  color: var(--text-muted); transition: all 0.15s;
}
.mini-btn:hover { border-color: var(--accent); color: var(--accent); }
.mini-btn--danger:hover { border-color: var(--color-danger); color: var(--color-danger); }

.actions { display: flex; gap: 10px; }
.act-btn {
  padding: 8px 18px; border-radius: 100px; border: none; cursor: pointer;
  font-size: 13px; font-weight: 500; transition: opacity 0.15s;
}
.act-btn:hover { opacity: 0.85; }
.act-btn--danger { background: var(--color-danger); color: #fff; }

.modal-overlay {
  position: fixed; inset: 0; z-index: 200;
  background: rgba(20,20,19,0.5);
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(4px);
}
.modal-card {
  background: var(--bg-paper); border-radius: var(--radius-xl);
  padding: 28px 24px; max-width: 360px; width: calc(100% - 40px);
  box-shadow: var(--shadow-xl);
}
.modal-title {
  font-family: var(--font-serif); font-size: 16px; font-weight: 600;
  display: block; margin-bottom: 16px;
}
.modal-input {
  width: 100%; padding: 12px 14px; margin-bottom: 16px;
  border: 1px solid var(--border-light); border-radius: var(--radius-md);
  font-size: 15px; outline: none; font-family: inherit;
  background: var(--bg-surface);
}
.modal-input:focus { border-color: var(--text-main); }
.modal-actions { display: flex; gap: 10px; justify-content: flex-end; }
.modal-actions .btn { padding: 10px 24px; border-radius: 100px; font-size: 14px; cursor: pointer; border: none; }
.modal-actions .btn--ghost { background: var(--bg-surface); color: var(--text-muted); }
.modal-actions .btn--dark { background: var(--bg-dark); color: #fff; }

.search-input {
  padding: 6px 12px; border: 1px solid var(--border-medium); border-radius: 8px;
  font-size: 13px; font-family: inherit; outline: none; background: var(--bg-paper);
  color: var(--text-main); width: 100%;
}
.search-input:focus { border-color: var(--accent); }
.search-input::placeholder { color: var(--text-light); }

.pager {
  display: flex; align-items: center; justify-content: center; gap: 12px;
  margin-top: 12px; font-size: 13px; color: var(--text-muted);
}
.pager button {
  padding: 4px 14px; border: 1px solid var(--border-medium); border-radius: 6px;
  background: var(--bg-paper); color: var(--text-main); font-size: 13px;
  font-family: inherit; cursor: pointer;
}
.pager button:hover:not(:disabled) { border-color: var(--accent); color: var(--accent); }
.pager button:disabled { opacity: 0.3; cursor: not-allowed; }

@media (max-width: 600px) {
  .stats-row { grid-template-columns: 1fr 1fr; }
}
</style>
