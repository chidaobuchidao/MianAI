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
page {
  background-color: #f5f6fa;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.global-text-primary {
  color: #2b6ff2;
}

.global-bg-primary {
  background-color: #2b6ff2;
}
</style>
