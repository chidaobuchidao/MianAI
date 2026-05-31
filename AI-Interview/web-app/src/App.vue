<template>
  <router-view v-slot="{ Component, route }">
    <transition name="page" mode="out-in">
      <component :is="Component" :key="route.path" />
    </transition>
  </router-view>
  <footer class="icp-footer">
    <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">湘ICP备2026019447号</a>
  </footer>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

onMounted(() => {
  userStore.restoreToken()
})
</script>

<style>
#app {
  min-height: 100vh;
  background: var(--bg-canvas);
}

.icp-footer {
  text-align: center;
  padding: 12px 0;
  font-size: 12px;
  color: var(--text-tertiary, #999);
}

.icp-footer a {
  color: inherit;
  text-decoration: none;
}

.icp-footer a:hover {
  text-decoration: underline;
}
</style>
