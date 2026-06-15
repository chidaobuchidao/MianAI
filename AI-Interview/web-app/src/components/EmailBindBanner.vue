<template>
  <div v-if="visible" class="email-bind-banner">
    <span class="email-bind-banner__icon">⚠️</span>
    <span class="email-bind-banner__text">您的账号尚未绑定邮箱，14 天后未绑定的账号将被清除，请尽快绑定。</span>
    <button class="email-bind-banner__btn" @click="$emit('bindEmail')">去绑定</button>
  </div>
</template>

<script setup lang="ts">
defineProps<{ visible: boolean }>()
defineEmits<{ bindEmail: [] }>()
</script>

<style scoped>
.email-bind-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: rgba(249, 115, 22, 0.1);
  border: 1px solid rgba(249, 115, 22, 0.3);
  border-radius: var(--radius-md);
  margin-bottom: 16px;
  font-size: 13px;
  color: var(--text-main);
}
.email-bind-banner__icon {
  font-size: 16px;
  flex-shrink: 0;
}
.email-bind-banner__text {
  flex: 1;
  color: var(--text-light);
}
.email-bind-banner__btn {
  flex-shrink: 0;
  padding: 4px 12px;
  font-size: 12px;
  background: var(--bg-dark);
  color: #fff;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  white-space: nowrap;
}
.email-bind-banner__btn:active {
  transform: scale(0.97);
}
</style>
