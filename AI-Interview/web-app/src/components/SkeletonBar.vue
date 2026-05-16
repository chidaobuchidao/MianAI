<template>
  <div class="skeleton">
    <div
      class="skeleton-bar"
      v-for="i in lines"
      :key="i"
      :style="{ width: widths[i - 1] || '100%', height, marginBottom: gap }"
    />
    <slot />
  </div>
</template>

<script setup lang="ts">
interface Props {
  lines?: number
  widths?: string[]
  height?: string
  gap?: string
}

withDefaults(defineProps<Props>(), {
  lines: 3,
  width: () => ['80%', '55%', '65%'],
  height: '12px',
  gap: '8px'
})
</script>

<style scoped>
.skeleton {
  padding: 8px 0;
}

.skeleton-bar {
  background: linear-gradient(90deg, #F0F0F0 25%, #E4E4E4 50%, #F0F0F0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite linear;
  border-radius: 6px;
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
</style>
