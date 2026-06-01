import { ref } from 'vue'
import { get } from '@/utils/request'
import { useUserStore } from '@/stores/user'

export interface QuotaInfo {
  hasApiKey: boolean
  isAdmin: boolean
  knowledgeBaseEnabled: boolean
  unlimited: boolean
  dailyQuota: number
  quotaUsed: number
  quotaRemaining: number
}

type RawQuotaInfo = Omit<QuotaInfo, 'unlimited' | 'knowledgeBaseEnabled'> & {
  unlimited?: boolean
  knowledgeBaseEnabled?: boolean
}

const cached = ref<QuotaInfo | null>(null)
let lastFetch = 0

export function useQuota() {
  async function fetchQuota(force = false): Promise<QuotaInfo> {
    if (!force && cached.value && Date.now() - lastFetch < 30_000) return cached.value
    const res = await get<RawQuotaInfo>('/api/user/quota')
    if (res.data) {
      const quota = normalizeQuota(res.data)
      cached.value = quota
      lastFetch = Date.now()
      // Keep userStore.isAdmin in sync across all quota consumers
      const userStore = useUserStore()
      userStore.setAdmin(quota.isAdmin === true)
      return quota
    }
    return normalizeQuota({ hasApiKey: false, isAdmin: false, knowledgeBaseEnabled: false, dailyQuota: 10, quotaUsed: 0, quotaRemaining: 10 })
  }

  /** Returns remaining count, or -1 if admin/has own key (unlimited) */
  function checkQuota(needed: number, msg?: string): { ok: boolean; msg?: string; remaining?: number } {
    const q = cached.value
    if (!q) return { ok: false, msg: '配额信息加载中，请稍后再试' }
    if (q.isAdmin || q.hasApiKey) return { ok: true }
    if (q.quotaRemaining < needed) {
      return {
        ok: false,
        msg: msg || `免费次数不足（剩余 ${q.quotaRemaining} 次），请配置 AI API Key 后继续使用`,
        remaining: q.quotaRemaining
      }
    }
    return { ok: true, remaining: q.quotaRemaining }
  }

  function invalidateQuota() {
    lastFetch = 0
  }

  return { fetchQuota, checkQuota, cached, invalidateQuota }
}

function normalizeQuota(quota: RawQuotaInfo): QuotaInfo {
  return {
    ...quota,
    knowledgeBaseEnabled: quota.knowledgeBaseEnabled ?? (quota.isAdmin === true || quota.hasApiKey === true),
    unlimited: quota.unlimited ?? (quota.isAdmin === true || quota.hasApiKey === true),
  }
}
