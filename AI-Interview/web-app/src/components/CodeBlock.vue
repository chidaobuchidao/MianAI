<template>
  <div class="code-block">
    <div class="code-block__header">
      <div class="code-block__dots">
        <span class="code-block__dot code-block__dot--red" />
        <span class="code-block__dot code-block__dot--yellow" />
        <span class="code-block__dot code-block__dot--green" />
      </div>
      <span class="code-block__filename" v-if="filename">{{ filename }}</span>
      <span class="code-block__lang" v-if="language">{{ language }}</span>
    </div>
    <div class="code-block__body no-scrollbar">
      <pre><code :class="language ? `language-${language}` : ''"><slot /></code></pre>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  filename?: string
  language?: string
}

defineProps<Props>()
</script>

<style scoped>
.code-block {
  background: #141413;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0,0,0,0.18);
  font-family: var(--font-mono);
}

.code-block__header {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background: rgba(255,255,255,0.04);
  border-bottom: 1px solid rgba(255,255,255,0.05);
}

.code-block__dots {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.code-block__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.code-block__dot--red    { background: #ED6A5E; }
.code-block__dot--yellow { background: #F4BF4F; }
.code-block__dot--green  { background: #61C554; }

.code-block__filename {
  margin-left: 12px;
  font-size: 11px;
  color: #888;
  font-family: var(--font-mono);
}

.code-block__lang {
  margin-left: auto;
  font-size: 10px;
  color: #666;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.code-block__body {
  padding: 16px 20px;
  overflow-x: auto;
}

.code-block__body pre {
  margin: 0;
  font-family: var(--font-mono);
  font-size: 13px;
  line-height: 1.7;
  color: #E4E4E4;
}

.code-block__body code {
  font-family: inherit;
  tab-size: 4;
}
</style>
