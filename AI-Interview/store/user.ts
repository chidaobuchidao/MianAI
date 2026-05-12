import { defineStore } from 'pinia';
import { ref } from 'vue';
import { getToken, setToken, removeToken } from '@/utils/request';
import { post } from '@/utils/request';

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken());
  const userId = ref(0);
  const nickname = ref('');
  const avatarUrl = ref('');
  const isLogin = ref(false);

  async function login(code: string) {
    const res = await post<{
      token: string;
      userId: number;
      nickname: string;
      avatarUrl: string;
    }>('/api/auth/login', { code });

    token.value = res.data.token;
    userId.value = res.data.userId;
    nickname.value = res.data.nickname;
    avatarUrl.value = res.data.avatarUrl;
    isLogin.value = true;

    setToken(res.data.token);
  }

  function devLogin() {
    const devToken = 'dev-token-' + Date.now();
    token.value = devToken;
    userId.value = 1;
    nickname.value = '开发测试';
    avatarUrl.value = '';
    isLogin.value = true;
    setToken(devToken);
  }

  function logout() {
    token.value = '';
    userId.value = 0;
    nickname.value = '';
    avatarUrl.value = '';
    isLogin.value = false;
    removeToken();
    uni.reLaunch({ url: '/pages/login/login' });
  }

  return { token, userId, nickname, avatarUrl, isLogin, login, devLogin, logout };
});
