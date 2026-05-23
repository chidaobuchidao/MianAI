<template>
  <div class="code-editor">
    <div class="code-editor__head">
      <div class="code-editor__dots">
        <span class="code-editor__dot code-editor__dot--red" />
        <span class="code-editor__dot code-editor__dot--yellow" />
        <span class="code-editor__dot code-editor__dot--green" />
      </div>
      <span class="code-editor__filename" v-if="filename">{{ filename }}</span>

      <!-- Language dropdown -->
      <div class="lang-dropdown" v-if="languages && languages.length > 0" ref="dropdownRef">
        <button class="lang-dropdown__trigger" @click="toggleDropdown">
          <span class="lang-dropdown__dot" :class="'lang-dropdown__dot--' + language" />
          {{ langLabel(language) }}
          <svg
            class="lang-dropdown__chevron"
            :class="{ 'lang-dropdown__chevron--open': dropdownOpen }"
            width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
          ><polyline points="6 9 12 15 18 9"/></svg>
        </button>
        <transition name="dropdown-slide">
          <div class="lang-dropdown__menu" v-if="dropdownOpen">
            <button
              v-for="lang in languages"
              :key="lang"
              class="lang-dropdown__item"
              :class="{ 'lang-dropdown__item--active': lang === language }"
              @click="selectLanguage(lang)"
            >
              <span class="lang-dropdown__dot" :class="'lang-dropdown__dot--' + lang" />
              {{ langLabel(lang) }}
            </button>
          </div>
        </transition>
      </div>

      <span class="code-editor__lang" v-else-if="language">{{ language }}</span>
    </div>
    <div class="code-editor__body" ref="editorHost" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted, onBeforeUnmount } from 'vue'
import { EditorView, keymap, lineNumbers, highlightActiveLine } from '@codemirror/view'
import { EditorState, Compartment } from '@codemirror/state'
import { oneDark } from '@codemirror/theme-one-dark'
import { java } from '@codemirror/lang-java'
import { javascript } from '@codemirror/lang-javascript'
import { python } from '@codemirror/lang-python'
import { cpp } from '@codemirror/lang-cpp'
import { defaultKeymap, history, indentWithTab } from '@codemirror/commands'

const LANGS: Record<string, string> = {
  java: 'Java',
  python: 'Python',
  javascript: 'JavaScript',
  cpp: 'C++',
  go: 'Go'
}

interface Props {
  modelValue?: string
  filename?: string
  language?: string
  languages?: string[]
  readonly?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  language: 'java',
  readonly: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'update:language': [value: string]
}>()

const editorHost = ref<HTMLElement>()
const dropdownRef = ref<HTMLElement>()
let view: EditorView | null = null
const languageConf = new Compartment()

const dropdownOpen = ref(false)

function langLabel(lang: string): string {
  return LANGS[lang] || lang
}

function getLanguage(lang: string) {
  switch (lang) {
    case 'python': return python()
    case 'javascript': return javascript()
    case 'cpp': return cpp()
    default: return java()
  }
}

function toggleDropdown() {
  dropdownOpen.value = !dropdownOpen.value
}

function selectLanguage(lang: string) {
  dropdownOpen.value = false
  if (lang === props.language) return
  emit('update:language', lang)
}

function onDocumentClick(e: MouseEvent) {
  if (dropdownRef.value && !dropdownRef.value.contains(e.target as Node)) {
    dropdownOpen.value = false
  }
}

onMounted(() => {
  if (!editorHost.value) return

  document.addEventListener('click', onDocumentClick)

  const updateListener = EditorView.updateListener.of(update => {
    if (update.docChanged) {
      emit('update:modelValue', update.state.doc.toString())
    }
  })

  view = new EditorView({
    state: EditorState.create({
      doc: props.modelValue,
      extensions: [
        lineNumbers(),
        highlightActiveLine(),
        oneDark,
        languageConf.of(getLanguage(props.language)),
        history(),
        keymap.of([...defaultKeymap, indentWithTab]),
        updateListener,
        EditorState.readOnly.of(props.readonly),
        EditorView.editable.of(!props.readonly)
      ]
    }),
    parent: editorHost.value
  })
})

watch(() => props.modelValue, (val) => {
  if (!view) return
  const current = view.state.doc.toString()
  if (val !== current) {
    view.dispatch({
      changes: { from: 0, to: current.length, insert: val }
    })
  }
})

watch(() => props.language, (lang) => {
  if (!view) return
  view.dispatch({
    effects: languageConf.reconfigure(getLanguage(lang))
  })
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
})

onUnmounted(() => {
  view?.destroy()
})
</script>

<style scoped>
.code-editor {
  display: flex; flex-direction: column;
  height: 100%;
  background: #141413;
  overflow: hidden;
}

.code-editor__head {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px;
  background: rgba(255,255,255,0.05);
  border-bottom: 1px solid rgba(255,255,255,0.05);
  flex-shrink: 0;
}

.code-editor__dots { display: flex; gap: 6px; }
.code-editor__dot { width: 10px; height: 10px; border-radius: 50%; }
.code-editor__dot--red    { background: #ED6A5E; }
.code-editor__dot--yellow { background: #F4BF4F; }
.code-editor__dot--green  { background: #61C554; }

.code-editor__filename {
  font-size: 11px; color: #888;
  font-family: var(--font-mono);
}

.code-editor__lang {
  margin-left: auto;
  font-size: 10px; color: #666;
  text-transform: uppercase; letter-spacing: 0.5px;
}

/* ===== Language Dropdown ===== */
.lang-dropdown {
  margin-left: auto;
  position: relative;
}
.lang-dropdown__trigger {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 10px;
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 5px;
  background: rgba(255,255,255,0.04);
  color: #aaa;
  font-size: 11px; font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}
.lang-dropdown__trigger:hover {
  border-color: rgba(255,255,255,0.18);
  color: #fff;
}
.lang-dropdown__dot {
  width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0;
}
.lang-dropdown__dot--java       { background: #ED6A5E; }
.lang-dropdown__dot--python     { background: #61C554; }
.lang-dropdown__dot--javascript { background: #F4BF4F; }
.lang-dropdown__dot--cpp        { background: #79C0FF; }
.lang-dropdown__dot--go         { background: #79C0FF; }

.lang-dropdown__chevron {
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.lang-dropdown__chevron--open {
  transform: rotate(180deg);
}

.lang-dropdown__menu {
  position: absolute; top: calc(100% + 4px); right: 0;
  min-width: 140px;
  background: #1e1e1c;
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 6px;
  padding: 4px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.5);
  z-index: 50;
  overflow: hidden;
}
.lang-dropdown__item {
  display: flex; align-items: center; gap: 8px;
  width: 100%; padding: 7px 12px;
  border: none; border-radius: 4px;
  background: transparent;
  color: #aaa;
  font-size: 12px; font-weight: 500;
  cursor: pointer;
  transition: all 0.12s;
  text-align: left;
}
.lang-dropdown__item:hover {
  background: rgba(255,255,255,0.06);
  color: #fff;
}
.lang-dropdown__item--active {
  background: rgba(255,255,255,0.08);
  color: #fff;
}

/* Dropdown slide animation */
.dropdown-slide-enter-active {
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}
.dropdown-slide-leave-active {
  transition: all 0.15s cubic-bezier(0.4, 0, 1, 1);
}
.dropdown-slide-enter-from {
  opacity: 0;
  transform: translateY(-8px) scaleY(0.85);
}
.dropdown-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px) scaleY(0.9);
}

.code-editor__body {
  flex: 1;
  overflow-y: auto;
  font-size: 13px;
}

/* Override CM theme to match Warm Tech */
:deep(.cm-editor) { height: 100%; }
:deep(.cm-editor.cm-focused) { outline: none; }
:deep(.cm-scroller) { overflow: auto; }
:deep(.cm-gutters) {
  background: #1a1a18;
  border-right: 1px solid rgba(255,255,255,0.06);
  color: #555;
}
:deep(.cm-activeLineGutter) { background: rgba(255,255,255,0.03); }
:deep(.cm-activeLine) { background: rgba(255,255,255,0.03); }
</style>
