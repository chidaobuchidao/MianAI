<template>
  <div class="voice-panel" :class="{ 'voice-panel--recording': recording, 'voice-panel--dark': dark }">
    <template v-if="!recording">
      <!-- Idle: input bar -->
      <div class="voice-panel__bar">
        <button class="voice-panel__keyboard" aria-label="键盘输入" @click="$emit('keyboard')">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="2" y="4" width="20" height="16" rx="2" ry="2"/>
            <line x1="6" y1="8" x2="18" y2="8"/>
            <line x1="6" y1="12" x2="18" y2="12"/>
            <line x1="8" y1="16" x2="16" y2="16"/>
          </svg>
        </button>
        <div class="voice-panel__hint" @click="$emit('start')">{{ hint }}</div>
        <button
          class="voice-panel__mic"
          :class="{ 'voice-panel__mic--dark': dark }"
          aria-label="开始录音"
          @click="$emit('start')"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3z"/>
            <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
            <line x1="12" y1="19" x2="12" y2="23"/>
            <line x1="8" y1="23" x2="16" y2="23"/>
          </svg>
        </button>
      </div>
    </template>

    <template v-else>
      <!-- Recording: waveform + actions -->
      <div class="voice-panel__recording">
        <button class="voice-panel__cancel" @click="$emit('cancel')">取消</button>
        <div class="voice-panel__waves">
          <span class="wave-bar" v-for="n in 5" :key="n" />
        </div>
        <button class="voice-panel__send" @click="$emit('stop')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="12 5 19 12 12 19"/>
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
        </button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
interface Props {
  recording?: boolean
  dark?: boolean
  hint?: string
}

withDefaults(defineProps<Props>(), {
  recording: false,
  dark: false,
  hint: '轻触说话...'
})

defineEmits<{
  start: []
  stop: []
  cancel: []
  keyboard: []
}>()
</script>

<style scoped>
.voice-panel {
  padding: 12px 20px 28px;
}

.voice-panel__bar {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: 24px;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: var(--shadow-lg);
}

.voice-panel__keyboard {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  flex-shrink: 0;
}

.voice-panel__hint {
  flex: 1;
  padding: 0 8px;
  color: var(--text-light);
  font-size: 14px;
  cursor: pointer;
}

.voice-panel__mic {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: var(--bg-dark);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  flex-shrink: 0;
}
.voice-panel__mic--dark {
  background: var(--accent);
}

/* Recording state */
.voice-panel--recording {
  padding: 12px 20px 28px;
}

.voice-panel__recording {
  background: var(--bg-dark);
  border-radius: 24px;
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: var(--shadow-lg);
}

.voice-panel__cancel {
  color: #888;
  font-size: 14px;
  font-weight: 500;
  border: none;
  background: none;
  cursor: pointer;
}

.voice-panel__waves {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 24px;
}

.voice-panel__send {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #141413;
  cursor: pointer;
}
</style>
