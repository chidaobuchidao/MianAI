import { ref } from 'vue';
import { get } from '@/utils/request';
import { useUserStore } from '@/store/user';

export interface QuotaInfo {
  hasApiKey: boolean;
  isAdmin: boolean;
  dailyQuota: number;
  quotaUsed: number;
  quotaRemaining: number;
}

interface RawQuotaInfo {
  hasApiKey?: boolean;
  isAdmin?: boolean;
  dailyQuota?: number;
  quotaUsed?: number;
  quotaRemaining?: number;
}

const cached = ref<QuotaInfo | null>(null);
let lastFetch = 0;

const FALLBACK_QUOTA: QuotaInfo = {
  hasApiKey: false,
  isAdmin: false,
  dailyQuota: 10,
  quotaUsed: 0,
  quotaRemaining: 10,
};

export function useQuota() {
  async function fetchQuota(force = false): Promise<QuotaInfo> {
    if (!force && cached.value && Date.now() - lastFetch < 30_000) {
      return cached.value;
    }
    try {
      const res = await get<RawQuotaInfo>('/api/user/quota');
      const raw = res.data;
      if (raw && typeof raw === 'object') {
        const quota: QuotaInfo = {
          hasApiKey: raw.hasApiKey ?? false,
          isAdmin: raw.isAdmin ?? false,
          dailyQuota: raw.dailyQuota ?? 10,
          quotaUsed: raw.quotaUsed ?? 0,
          quotaRemaining: raw.quotaRemaining ?? 10,
        };
        cached.value = quota;
        lastFetch = Date.now();
        // 同步 isAdmin 到 userStore
        const userStore = useUserStore();
        userStore.setAdmin(quota.isAdmin);
        return quota;
      }
    } catch {
      // 配额接口失败时使用默认值
    }
    cached.value = FALLBACK_QUOTA;
    lastFetch = Date.now();
    return FALLBACK_QUOTA;
  }

  function checkQuota(needed: number, msg?: string): { ok: boolean; msg?: string; remaining?: number } {
    const q = cached.value;
    if (!q) {
      return { ok: false, msg: '配额信息加载中，请稍后再试' };
    }
    if (q.isAdmin || q.hasApiKey) {
      return { ok: true };
    }
    if (q.quotaRemaining < needed) {
      return {
        ok: false,
        msg: msg || `免费次数不足（剩余 ${q.quotaRemaining} 次），请在个人中心配置 AI API Key 后继续使用`,
        remaining: q.quotaRemaining,
      };
    }
    return { ok: true, remaining: q.quotaRemaining };
  }

  function invalidateQuota() {
    lastFetch = 0;
  }

  return { fetchQuota, checkQuota, cached, invalidateQuota };
}
