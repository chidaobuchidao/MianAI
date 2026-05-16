<template>
  <div class="code-editor">
    <div class="code-editor__head">
      <div class="code-editor__dots">
        <span class="code-editor__dot code-editor__dot--red" />
        <span class="code-editor__dot code-editor__dot--yellow" />
        <span class="code-editor__dot code-editor__dot--green" />
      </div>
      <span class="code-editor__filename" v-if="filename">{{ filename }}</span>
      <span class="code-editor__lang" v-if="language">{{ language }}</span>
    </div>
    <div class="code-editor__body" ref="editorHost" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { EditorView, keymap, lineNumbers, highlightActiveLine } from '@codemirror/view'
import { EditorState } from '@codemirror/state'
import { oneDark } from '@codemirror/theme-one-dark'
import { java } from '@codemirror/lang-java'
import { javascript } from '@codemirror/lang-javascript'
import { python } from '@codemirror/lang-python'
import { defaultKeymap, indentWithTab } from '@codemirror/commands'

interface Props {
  modelValue?: string
  filename?: string
  language?: string
  readonly?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  language: 'java',
  readonly: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const editorHost = ref<HTMLElement>()
let view: EditorView | null = null

function getLanguage(lang: string) {
  switch (lang) {
    case 'python': return python()
    case 'javascript': return javascript()
    default: return java()
  }
}

onMounted(() => {
  if (!editorHost.value) return

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
        getLanguage(props.language),
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
    effects: EditorState.reconfigure.of([
      getLanguage(lang)
    ])
  })
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
