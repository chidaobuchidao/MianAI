<template>
  <div class="diag-card">
    <div class="diag-card__indicator" :class="indicatorClass" />
    <span class="diag-card__label">{{ label }}</span>
    <div class="diag-card__body">
      <div class="diag-card__title">{{ title }}</div>
      <div class="diag-card__original" v-if="original">
        <span class="diag-card__original-label">原文：</span>
        {{ original }}
      </div>
      <div class="diag-card__rewrite" v-if="rewrite">
        <div class="diag-card__rewrite-label">AI 建议重写为：</div>
        <div class="diag-card__rewrite-text">{{ rewrite }}</div>
      </div>
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  label: string
  title: string
  original?: string
  rewrite?: string
  variant?: 'danger' | 'warning' | 'info'
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'danger'
})

const indicatorClass = computed(() => `diag-card__indicator--${props.variant}`)
</script>

<style scoped>
.diag-card {
  padding: 16px 0;
}

.diag-card__indicator {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
}
.diag-card__indicator--danger  { background: var(--color-danger); }
.diag-card__indicator--warning { background: var(--accent); }
.diag-card__indicator--info    { background: #3B82F6; }

.diag-card__label {
  font-size: 13px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-main);
}

.diag-card__body {
  margin-top: 12px;
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 16px;
  box-shadow: var(--shadow-sm);
}

.diag-card__title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-main);
  margin-bottom: 8px;
}

.diag-card__original {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
  margin-bottom: 12px;
}

.diag-card__original-label {
  font-size: 11px;
  color: var(--text-light);
}

.diag-card__rewrite {
  background: var(--bg-surface);
  border-radius: var(--radius-sm);
  padding: 12px;
  border-left: 3px solid var(--color-success);
}

.diag-card__rewrite-label {
  font-size: 11px;
  color: var(--text-light);
  margin-bottom: 6px;
}

.diag-card__rewrite-text {
  font-size: 13px;
  color: var(--text-main);
  line-height: 1.6;
}
</style>
