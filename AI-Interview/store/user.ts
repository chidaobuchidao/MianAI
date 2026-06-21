import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { getToken, setToken, removeToken } from '@/utils/request';
import { post } from '@/utils/request';

interface UserInfo {
  token: string;
  userId: number;
  nickname: string;
  avatarUrl: string;
  email?: string;
  needBindEmail?: boolean;
  isAdmin?: boolean;
  role?: number | string;
}


function isAdminInfo(info: UserInfo): boolean {
  const role = info.role;
  return info.isAdmin === true || role === 1 || role === '1' || role === 'admin' || role === "管理员";
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken());
  const userId = ref<number>(0);
  const nickname = ref<string>('');
  const avatarUrl = ref<string>('');
  const isAdmin = ref<boolean>(false);
  const email = ref<string>('');
  const needBindEmail = ref<boolean>(false);

  const isLogin = computed(() => !!token.value && userId.value > 0);

  function setUser(info: UserInfo) {
    token.value = info.token;
    userId.value = info.userId;
    nickname.value = info.nickname;
    avatarUrl.value = info.avatarUrl || '';
    email.value = info.email || '';
    needBindEmail.value = info.needBindEmail || false;
    setAdmin(isAdminInfo(info));
    setToken(info.token);
    uni.setStorageSync('mianmiantong_userId', String(info.userId));
    uni.setStorageSync('mianmiantong_nickname', info.nickname);
    uni.setStorageSync('mianmiantong_avatarUrl', info.avatarUrl || '');
    uni.setStorageSync('mianmiantong_email', info.email || '');
  }

  function setAdmin(admin: boolean) {
    isAdmin.value = admin;
    uni.setStorageSync('mianmiantong_isAdmin', admin ? '1' : '0');
  }

  function clearUser() {
    token.value = '';
    userId.value = 0;
    nickname.value = '';
    avatarUrl.value = '';
    isAdmin.value = false;
    email.value = '';
    needBindEmail.value = false;
    removeToken();
    uni.removeStorageSync('mianmiantong_userId');
    uni.removeStorageSync('mianmiantong_nickname');
    uni.removeStorageSync('mianmiantong_avatarUrl');
    uni.removeStorageSync('mianmiantong_isAdmin');
    uni.removeStorageSync('mianmiantong_email');
  }

  function restoreToken() {
    const saved = getToken();
    if (saved) {
      token.value = saved;
      userId.value = Number(uni.getStorageSync('mianmiantong_userId')) || 0;
      nickname.value = uni.getStorageSync('mianmiantong_nickname') || '';
      avatarUrl.value = uni.getStorageSync('mianmiantong_avatarUrl') || '';
      isAdmin.value = uni.getStorageSync('mianmiantong_isAdmin') === '1';
      email.value = uni.getStorageSync('mianmiantong_email') || '';
    }
  }

  async function login(code: string) {
    const res = await post<UserInfo>('/api/auth/login', { code });
    setUser(res.data);
  }

  function devLogin() {
    const devToken = 'dev-token-' + Date.now();
    setUser({
      token: devToken,
      userId: 1,
      nickname: '开发测试',
      avatarUrl: '',
    });
  }

  function logout() {
    clearUser();
    uni.reLaunch({ url: '/pages/login/login' });
  }

  return {
    token,
    userId,
    nickname,
    avatarUrl,
    isAdmin,
    email,
    needBindEmail,
    isLogin,
    login,
    devLogin,
    logout,
    setUser,
    setAdmin,
    clearUser,
    restoreToken,
  };
});
