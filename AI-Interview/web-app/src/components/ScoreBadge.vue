<template>
  <div class="score-badge" :class="colorClass" :style="{ width: size, height: size, borderRadius: radius }">
    <span class="score-badge__num">{{ displayScore }}</span>
    <span class="score-badge__max">/{{ max }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  score: number | string
  max?: number
  size?: string
  radius?: string
}

const props = withDefaults(defineProps<Props>(), {
  max: 100,
  size: '80px',
  radius: '24px'
})

const displayScore = computed(() => {
  if (props.score === '' || props.score === null || props.score === undefined) return '--'
  return String(props.score)
})

const numScore = computed(() => Number(props.score))

const colorClass = computed(() => {
  if (isNaN(numScore.value)) return 'score-badge--muted'
  if (numScore.value >= 8) return 'score-badge--high'
  if (numScore.value >= 6) return 'score-badge--mid'
  if (numScore.value >= 4) return 'score-badge--low'
  return 'score-badge--poor'
})
</script>

<style scoped>
.score-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-family: var(--font-sans);
}

.score-badge__num {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -1px;
  line-height: 1;
  margin-bottom: 2px;
}

.score-badge__max {
  font-size: 11px;
  font-weight: 500;
}

/* Color variants */
.score-badge--high {
  background: #F0FDF4;
  border: 1px solid #BBF7D0;
}
.score-badge--high .score-badge__num { color: #16A34A; }
.score-badge--high .score-badge__max { color: #15803D; }

.score-badge--mid {
  background: #FFFBEB;
  border: 1px solid #FDE68A;
}
.score-badge--mid .score-badge__num { color: #D97706; }
.score-badge--mid .score-badge__max { color: #B45309; }

.score-badge--low {
  background: #FFF7ED;
  border: 1px solid #FED7AA;
}
.score-badge--low .score-badge__num { color: #EA580C; }
.score-badge--low .score-badge__max { color: #C2410C; }

.score-badge--poor {
  background: #FEF2F2;
  border: 1px solid #FECACA;
}
.score-badge--poor .score-badge__num { color: #DC2626; }
.score-badge--poor .score-badge__max { color: #B91C1C; }

.score-badge--muted {
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
}
.score-badge--muted .score-badge__num { color: var(--text-light); }
.score-badge--muted .score-badge__max { color: var(--text-light); }
</style>
