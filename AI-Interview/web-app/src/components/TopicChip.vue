<template>
  <span class="chip" :class="{ 'chip--active': active }" @click="$emit('click')">
    <slot />
  </span>
</template>

<script setup lang="ts">
interface Props {
  active?: boolean
}

withDefaults(defineProps<Props>(), {
  active: false
})

defineEmits<{
  click: []
}>()
</script>

<style scoped>
.chip {
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--border-light);
  padding: 8px 18px;
  border-radius: var(--radius-full);
  font-size: 14px;
  color: var(--text-main);
  background: var(--bg-paper);
  transition: all 0.15s var(--ease-out-expo);
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
}
.chip:hover {
  background: var(--bg-surface);
  border-color: var(--text-main);
}
.chip--active {
  background: var(--bg-dark);
  color: #fff;
  border-color: var(--bg-dark);
}
.chip--active:hover {
  background: var(--bg-dark);
  border-color: var(--bg-dark);
}
.chip:active {
  transform: scale(0.97);
}
</style>
