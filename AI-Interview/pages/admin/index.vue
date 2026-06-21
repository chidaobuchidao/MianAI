<template>
  <view class="admin">
    <view v-if="checkingAdmin" class="admin-guard"><text>正在校验管理员权限...</text></view>
    <block v-else-if="authorized">
      <view class="page-header">
        <text class="page-title">管理后台</text>
        <text class="page-sub">系统状态、用户额度、内容运营配置</text>
      </view>

      <view class="tabs">
        <view v-for="t in tabs" :key="t.id" class="tab" :class="{ active: activeTab === t.id }" @click="activeTab = t.id">{{ t.label }}</view>
      </view>

      <view v-if="activeTab === 'status'">
        <view class="stat-row">
          <view class="stat-card"><text class="stat-num">{{ status.totalUsers || stats.totalUsers }}</text><text class="stat-label">用户</text></view>
          <view class="stat-card"><text class="stat-num">{{ status.totalSessions || stats.totalInterviews }}</text><text class="stat-label">会话</text></view>
          <view class="stat-card"><text class="stat-num">{{ status.totalQuestions || stats.totalQuestions }}</text><text class="stat-label">题目</text></view>
        </view>
        <view class="danger-btn" @click="clearSessions">清空面试会话</view>
      </view>

      <view v-if="activeTab === 'users'" class="section">
        <view class="search-row"><input class="search-input" v-model="userKeyword" placeholder="搜索用户" /><view class="btn-sm" @click="loadUsers">查询</view></view>
        <view class="item" v-for="u in users" :key="u.id">
          <view class="item-body">
            <text class="item-title">{{ u.nickname || ('用户 #' + u.id) }}</text>
            <view class="item-meta">
              <text class="tag" :class="u.role === '管理员' ? 'diff-2' : ''">{{ u.role || '用户' }}</text>
              <text class="tag">剩余 {{ remaining(u) }}</text>
              <text class="tag" v-if="u.hasApiKey">API Key</text>
              <text class="tag" v-if="u.knowledgeBaseEnabled">知识库</text>
            </view>
          </view>
          <view class="action-grid">
            <text @click="setQuota(u)">配额</text>
            <text @click="setLimit(u)">限制</text>
            <text @click="toggleAdmin(u)">角色</text>
            <text @click="toggleKb(u)">知识库</text>
            <text class="danger" @click="deleteUser(u)">删除</text>
          </view>
        </view>
      </view>

      <view v-if="activeTab === 'sessions'" class="section">
        <view class="search-row"><input class="search-input" v-model="sessionKeyword" placeholder="搜索会话" /><view class="btn-sm" @click="loadSessions">查询</view></view>
        <view class="item" v-for="s in sessions" :key="s.id">
          <view class="item-body">
            <text class="item-title">{{ s.position || ('会话 #' + s.id) }}</text>
            <view class="item-meta"><text class="tag">用户 {{ s.userId || '-' }}</text><text class="tag">{{ s.createTime || '' }}</text></view>
          </view>
        </view>
      </view>

      <view v-if="activeTab === 'announcements'" class="section">
        <view class="section-head"><text class="section-title">公告管理</text><view class="btn-sm" @click="openAnnouncement()">新建</view></view>
        <view class="item" v-for="a in announcements" :key="a.id">
          <view class="item-body"><text class="item-title">{{ a.title }}</text><text class="item-desc">{{ a.content }}</text></view>
          <view class="action-grid"><text @click="openAnnouncement(a)">编辑</text><text @click="publishAnnouncement(a)">发布</text><text class="danger" @click="removeAnnouncement(a)">删除</text></view>
        </view>
      </view>

      <view v-if="activeTab === 'questions'" class="section">
        <view class="section-head"><text class="section-title">题库</text><view class="btn-sm" @click="showAddModal = true">新增题目</view></view>
        <view class="item" v-for="q in questions" :key="q.id">
          <view class="item-body"><text class="item-title">{{ q.title }}</text><view class="item-meta"><text class="tag">{{ q.categoryName }}</text><text class="tag" :class="'diff-' + q.difficulty">{{ difficultyLabel(q.difficulty) }}</text></view></view>
          <view class="item-actions"><text class="item-action" @click="deleteQuestion(q.id)">删除</text></view>
        </view>
      </view>

      <view class="modal-mask" v-if="showAnnModal" @click="showAnnModal = false">
        <view class="modal-card" @click.stop>
          <text class="modal-title">公告</text>
          <input class="field-input" v-model="annForm.title" placeholder="标题" />
          <textarea class="field-textarea" v-model="annForm.content" placeholder="请输入公告内容" />
          <view class="modal-btns"><button class="mbtn cancel" @click="showAnnModal = false">取消</button><button class="mbtn save" @click="saveAnnouncement">保存</button></view>
        </view>
      </view>

      <!-- 新增题目 -->
      <view class="modal-mask" v-if="showAddModal" @click="showAddModal = false">
        <view class="modal-card" @click.stop>
          <text class="modal-title">新增题目</text>
          <view class="field"><text class="field-label">分类</text><picker mode="selector" :range="categoryNames" :value="selectedCategoryIdx" @change="onCategoryChange"><view class="field-picker">{{ categoryNames[selectedCategoryIdx] || '选择分类' }}</view></picker></view>
          <view class="field"><text class="field-label">类型</text><view class="type-row"><view v-for="(t, i) in questionTypes" :key="i" class="type-chip" :class="{ active: form.type === (i + 1) }" @click="form.type = (i + 1)"><text>{{ t }}</text></view></view></view>
          <view class="field"><text class="field-label">难度</text><view class="type-row"><view v-for="(d, i) in ['简单', '中等', '困难']" :key="i" class="type-chip" :class="{ active: form.difficulty === (i + 1) }" @click="form.difficulty = (i + 1)"><text>{{ d }}</text></view></view></view>
          <view class="field"><text class="field-label">题目</text><textarea class="field-textarea" v-model="form.title" placeholder="请输入题目内容..." /></view>
          <view class="field" v-if="form.type <= 2"><text class="field-label">选项（每行一个，如 A. 开头）</text><textarea class="field-textarea" v-model="form.optionsText" placeholder="A. 选项A&#10;B. 选项B" /></view>
          <view class="field"><text class="field-label">答案</text><input class="field-input" v-model="form.answer" placeholder="答案" /></view>
          <view class="field"><text class="field-label">解析</text><textarea class="field-textarea" v-model="form.analysis" placeholder="解析" /></view>
          <view class="modal-btns"><button class="mbtn cancel" @click="showAddModal = false">取消</button><button class="mbtn save" @click="addQuestion" :disabled="submitting">添加</button></view>
        </view>
      </view>
    </block>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad, onShow } from '@dcloudio/uni-app';
import { get, post, put, del } from '@/utils/request';
import { useUserStore } from '@/store/user';
import { useQuota } from '@/composables/useQuota';

interface Category { id: number; name: string; }
interface QuestionItem { id: number; title: string; categoryName: string; categoryId: number; difficulty: number; type: number; }
interface AdminStats { totalQuestions: number; totalUsers: number; totalInterviews: number; }
interface Status { totalUsers?: number; totalSessions?: number; totalQuestions?: number; }
interface UserRow { id: number; nickname?: string; role?: string; hasApiKey?: boolean; knowledgeBaseEnabled?: boolean; dailyQuota?: number; quotaUsed?: number; }
interface SessionRow { id: number; userId?: number; position?: string; createTime?: string; }
interface AnnRow { id: number; title: string; content: string; published?: boolean; }

const tabs = [{ id: 'status', label: "状态" }, { id: 'users', label: "用户" }, { id: 'sessions', label: "会话" }, { id: 'announcements', label: "公告" }, { id: 'questions', label: "题库" }];
const activeTab = ref('status');
const stats = ref<AdminStats>({ totalQuestions: 0, totalUsers: 0, totalInterviews: 0 });
const status = ref<Status>({});
const users = ref<UserRow[]>([]);
const sessions = ref<SessionRow[]>([]);
const announcements = ref<AnnRow[]>([]);
const questions = ref<QuestionItem[]>([]);
const categories = ref<Category[]>([]);
const categoryNames = ref<string[]>([]);
const selectedCategoryIdx = ref(0);
const userKeyword = ref('');
const sessionKeyword = ref('');
const showAnnModal = ref(false);
const showAddModal = ref(false);
const submitting = ref(false);
const editingAnn = ref<AnnRow | null>(null);
const annForm = ref({ title: '', content: '' });
const questionTypes = ['', "单选", "多选", "判断", "问答"];
const form = ref({ categoryId: 0, type: 1, difficulty: 1, title: '', optionsText: '', answer: '', analysis: '' });
const checkingAdmin = ref(true);
const authorized = ref(false);
let adminLoaded = false;
let loadingAdmin = false;
let authCheckSeq = 0;

function difficultyLabel(d: number): string { return d === 3 ? "困难" : d === 2 ? "中等" : "简单"; }
function remaining(u: UserRow): number { return Math.max(0, (u.dailyQuota ?? 10) - (u.quotaUsed ?? 0)); }
function rows<T>(data: unknown): T[] { const d = data as any; return d?.records || d?.list || d?.rows || (Array.isArray(d) ? d : []); }

function withTimeout<T>(promise: Promise<T>, ms: number, message: string): Promise<T> {
  let timer: ReturnType<typeof setTimeout> | undefined;
  return new Promise((resolve, reject) => {
    timer = setTimeout(() => reject(new Error(message)), ms);
    promise.then(
      (value) => {
        if (timer) clearTimeout(timer);
        resolve(value);
      },
      (error) => {
        if (timer) clearTimeout(timer);
        reject(error);
      },
    );
  });
}

async function loadStatus() { const r = await get<Status>('/api/admin/status'); status.value = r.data || {}; }
async function loadUsers() { const r = await get<unknown>('/api/admin/users', { page: 1, pageSize: 50, keyword: userKeyword.value }); users.value = rows<UserRow>(r.data); }
async function loadSessions() { const r = await get<unknown>('/api/admin/sessions', { page: 1, pageSize: 50, keyword: sessionKeyword.value }); sessions.value = rows<SessionRow>(r.data); }
async function loadAnnouncements() { const r = await get<AnnRow[]>('/api/admin/announcements'); announcements.value = r.data || []; }
async function loadQuestions() { const r = await get<{ records: QuestionItem[] }>('/api/questions', { size: 100 }); questions.value = r.data?.records || []; }
async function loadCategories() { const r = await get<Category[]>('/api/questions/categories'); categories.value = r.data || []; categoryNames.value = categories.value.map(c => c.name); }

function onCategoryChange(e: { detail: { value: number } }) { selectedCategoryIdx.value = e.detail.value; form.value.categoryId = categories.value[e.detail.value]?.id || 0; }
function askNumber(title: string, cb: (n: number) => void) { uni.showModal({ title, editable: true, placeholderText: "请输入数字", success: (r) => { const n = Number(r.content); if (r.confirm && Number.isFinite(n)) cb(n); } }); }
function setQuota(u: UserRow) { askNumber("设置剩余次数", async (n) => { await post('/api/admin/set-quota', { userId: u.id, remaining: n }); await loadUsers(); }); }
function setLimit(u: UserRow) { askNumber("设置每日限制", async (n) => { await post('/api/admin/set-limit', { userId: u.id, limit: n }); await loadUsers(); }); }
async function toggleAdmin(u: UserRow) { await post('/api/admin/toggle-admin', { userId: u.id }); await loadUsers(); }
async function toggleKb(u: UserRow) { await post('/api/admin/toggle-knowledge-base', { userId: u.id }); await loadUsers(); }
function deleteUser(u: UserRow) { uni.showModal({ title: "删除用户", content: "确认删除该用户？", success: async (r) => { if (r.confirm) { await post('/api/admin/delete-user', { userId: u.id }); await loadUsers(); } } }); }
function clearSessions() { uni.showModal({ title: "清空会话", content: "确认清空全部面试会话？", success: async (r) => { if (r.confirm) { await post('/api/admin/clear-sessions'); await loadStatus(); await loadSessions(); } } }); }
function openAnnouncement(a?: AnnRow) { editingAnn.value = a || null; annForm.value = { title: a?.title || '', content: a?.content || '' }; showAnnModal.value = true; }
async function saveAnnouncement() { if (editingAnn.value) await put(`/api/admin/announcement/${editingAnn.value.id}`, annForm.value); else await post('/api/admin/announcement', annForm.value); showAnnModal.value = false; await loadAnnouncements(); }
async function publishAnnouncement(a: AnnRow) { await post(`/api/admin/announcement/${a.id}/publish`); await loadAnnouncements(); }
async function removeAnnouncement(a: AnnRow) { await del(`/api/admin/announcement/${a.id}`); await loadAnnouncements(); }

async function deleteQuestion(id: number) { await del(`/api/questions/${id}`); await loadQuestions(); }
async function addQuestion() {
  if (!form.value.title.trim() || !form.value.answer.trim()) { uni.showToast({ title: "请填写题目和答案", icon: 'none' }); return; }
  submitting.value = true;
  try {
    let options = '';
    if (form.value.type <= 2 && form.value.optionsText.trim()) {
      options = JSON.stringify(form.value.optionsText.trim().split('\n').map((line) => {
        const m = line.match(/^([A-D])[.\u3001\s]\s*(.+)/); return m ? { label: m[1], content: m[2] } : null;
      }).filter(Boolean));
    }
    await post('/api/questions', { categoryId: form.value.categoryId, type: form.value.type, difficulty: form.value.difficulty, title: form.value.title.trim(), options, answer: form.value.answer.trim(), analysis: form.value.analysis.trim() || undefined });
    showAddModal.value = false;
    form.value = { categoryId: 0, type: 1, difficulty: 1, title: '', optionsText: '', answer: '', analysis: '' };
    await loadQuestions();
  } finally { submitting.value = false; }
}

async function ensureAdmin(background = false): Promise<boolean> {
  const checkId = ++authCheckSeq;
  let checkFailed = false;
  if (!background) checkingAdmin.value = true;
  userStore.restoreToken();
  if (!userStore.isLogin) {
    authorized.value = false;
    if (!background) checkingAdmin.value = false;
    uni.reLaunch({ url: '/pages/login/login' });
    return false;
  }

  if (userStore.isAdmin) {
    authorized.value = true;
    if (!background) checkingAdmin.value = false;
    void withTimeout(fetchQuota(true), 8_000, '管理员权限校验超时')
      .then((quota) => {
        if (quota.isAdmin !== true) {
          authorized.value = false;
          userStore.setAdmin(false);
          uni.showToast({ title: "无管理员权限", icon: 'none' });
          setTimeout(() => uni.switchTab({ url: '/pages/profile/profile' }), 500);
        }
      })
      .catch((error) => {
        console.warn('[admin] 后台权限复核失败', error);
      });
    return true;
  }

  try {
    const quota = await withTimeout(fetchQuota(true), 8_000, '管理员权限校验超时');
    authorized.value = quota.isAdmin === true;
  } catch (error) {
    console.warn('[admin] 后台权限校验失败', error);
    checkFailed = true;
    authorized.value = false;
    uni.showToast({ title: "权限校验失败，请稍后重试", icon: 'none' });
  } finally {
    if (checkId === authCheckSeq && !background) checkingAdmin.value = false;
  }
  if (!authorized.value) {
    userStore.setAdmin(false);
    if (!checkFailed) uni.showToast({ title: "无管理员权限", icon: 'none' });
    setTimeout(() => uni.switchTab({ url: '/pages/profile/profile' }), 500);
  }
  return authorized.value;
}

async function loadAdminData() {
  if (loadingAdmin) return;
  loadingAdmin = true;
  try {
    if (!(await ensureAdmin())) return;
    adminLoaded = true;
    await Promise.allSettled([loadStatus(), loadUsers(), loadSessions(), loadAnnouncements(), loadQuestions(), loadCategories()]);
  } finally {
    loadingAdmin = false;
  }
}

onLoad(loadAdminData);
onShow(async () => {
  if (adminLoaded) {
    await ensureAdmin(true);
    return;
  }
  await loadAdminData();
});
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";
.admin { min-height: 100vh; background: $bg-canvas; padding: 40rpx 28rpx; }.admin-guard { min-height: 60vh; display: flex; align-items: center; justify-content: center; color: $text-muted; font-size: 26rpx; }
.page-header { margin-bottom: 28rpx; }.page-title { font-family: Georgia, serif; font-size: 40rpx; font-weight: 600; color: $text-main; display: block; margin-bottom: 8rpx; }.page-sub { font-size: 24rpx; color: $text-muted; }
.tabs { display: flex; gap: 8rpx; overflow-x: auto; margin-bottom: 26rpx; }.tab { flex-shrink: 0; padding: 14rpx 24rpx; border-radius: $radius-full; background: $bg-paper; border: 1px solid $border-light; color: $text-light; font-size: 24rpx; }.tab.active { background: $bg-dark; color: #fff; }
.stat-row { display: flex; gap: 14rpx; margin-bottom: 24rpx; }.stat-card { flex: 1; background: $bg-paper; border: 1px solid $border-light; border-radius: $radius-lg; padding: 28rpx 0; display: flex; flex-direction: column; align-items: center; box-shadow: $shadow-sm; }.stat-num { font-family: Georgia, serif; font-size: 40rpx; font-weight: 600; color: $text-main; }.stat-label { font-size: 22rpx; color: $text-light; margin-top: 6rpx; }
.section { margin-bottom: 32rpx; }.section-head, .search-row { display: flex; justify-content: space-between; align-items: center; gap: 14rpx; margin-bottom: 18rpx; }.section-title { font-size: 28rpx; font-weight: 600; color: $text-main; }.search-input { flex: 1; height: 70rpx; background: $bg-paper; border: 1px solid $border-light; border-radius: $radius-md; padding: 0 20rpx; font-size: 24rpx; }
.btn-sm, .danger-btn { background: $bg-dark; color: #fff; font-size: 24rpx; font-weight: 600; padding: 14rpx 24rpx; border-radius: $radius-sm; }.danger-btn { text-align: center; background: $color-danger; }
.item { background: $bg-paper; border: 1px solid $border-light; border-radius: $radius-md; padding: 24rpx; margin-bottom: 12rpx; box-shadow: $shadow-sm; }.item-title { font-size: 26rpx; color: $text-main; line-height: 1.5; display: block; margin-bottom: 8rpx; }.item-desc { font-size: 22rpx; color: $text-light; line-height: 1.5; display: block; }.item-meta { display: flex; gap: 8rpx; flex-wrap: wrap; }.tag { font-size: 20rpx; padding: 4rpx 12rpx; border-radius: $radius-full; background: $bg-surface; color: $text-light; }.tag.diff-1 { background: rgba(34,197,94,0.1); color: $color-success; }.tag.diff-2 { background: rgba(217,117,10,0.1); color: $accent; }.tag.diff-3 { background: rgba(239,68,68,0.1); color: $color-danger; }
.item-actions, .action-grid { display: flex; gap: 18rpx; flex-wrap: wrap; margin-top: 14rpx; }.item-action, .action-grid text { font-size: 22rpx; color: $accent; }.action-grid .danger, .item-action { color: $color-danger; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 999; display: flex; align-items: center; justify-content: center; padding: 40rpx; }.modal-card { background: $bg-paper; border-radius: $radius-xl; padding: 40rpx 32rpx; width: 100%; max-width: 620rpx; max-height: 80vh; overflow-y: auto; }.modal-title { font-size: 30rpx; font-weight: 600; color: $text-main; margin-bottom: 24rpx; display: block; }
.field { margin-bottom: 20rpx; }.field-label { font-size: 24rpx; font-weight: 600; color: $text-main; margin-bottom: 10rpx; display: block; }.field-input, .field-picker { width: 100%; height: 72rpx; background: $bg-surface; border: 1px solid $border-medium; border-radius: $radius-md; padding: 0 20rpx; font-size: 26rpx; box-sizing: border-box; }.field-textarea { width: 100%; min-height: 120rpx; background: $bg-surface; border: 1px solid $border-medium; border-radius: $radius-md; padding: 16rpx 20rpx; font-size: 26rpx; box-sizing: border-box; }
.type-row { display: flex; gap: 12rpx; flex-wrap: wrap; }.type-chip { padding: 10rpx 24rpx; border-radius: $radius-full; background: $bg-surface; font-size: 24rpx; color: $text-light; border: 1px solid $border-light; }.type-chip.active { background: rgba(217,117,10,0.06); border-color: $accent; color: $accent; font-weight: 600; }
.modal-btns { display: flex; gap: 16rpx; margin-top: 28rpx; }.mbtn { flex: 1; height: 80rpx; border: none; border-radius: $radius-xl; font-size: 28rpx; font-weight: 600; }.mbtn.cancel { background: $bg-surface; color: $text-muted; }.mbtn.save { background: $bg-dark; color: #fff; }
@media (min-width: 1025px) { .admin { max-width: 900px; margin: 0 auto; } }
</style>
