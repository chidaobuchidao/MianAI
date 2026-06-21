<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app';
import { useUserStore } from '@/store/user';

onLaunch(() => {
  const userStore = useUserStore();
  // 恢复持久化的登录状态
  userStore.restoreToken();
  // 如果没有 token，跳转登录页（只在非 tab 页时跳转）
  if (!userStore.token) {
    uni.reLaunch({ url: '/pages/login/login' });
  }
});
</script>

<style lang="scss">
@import "@/styles/tokens.scss";

@import "@/styles/tokens.css";

page {
  background-color: $bg-canvas;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Segoe UI', Roboto, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.global-text-primary {
  color: $accent;
}

.global-bg-primary {
  background-color: $bg-dark;
}
</style>
