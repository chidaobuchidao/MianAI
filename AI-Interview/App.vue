<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app';
import { getToken } from '@/utils/request';

onLaunch(() => {
  const token = getToken();
  // 如果没有 token，跳转登录页
  if (!token) {
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
