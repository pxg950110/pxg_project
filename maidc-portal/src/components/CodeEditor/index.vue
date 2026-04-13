<template>
  <div class="code-editor" :class="{ readonly: readOnly }">
    <div v-if="language" class="editor-lang">{{ language }}</div>
    <textarea
      ref="textareaRef"
      :value="modelValue"
      :readonly="readOnly"
      class="editor-textarea"
      spellcheck="false"
      @input="handleInput"
      @keydown="handleKeydown"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  modelValue: string
  language?: string
  readOnly?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: string): void
}

const props = withDefaults(defineProps<Props>(), {
  readOnly: false,
})
const emit = defineEmits<Emits>()

const textareaRef = ref<HTMLTextAreaElement | null>(null)

function handleInput(event: Event) {
  const target = event.target as HTMLTextAreaElement
  emit('update:modelValue', target.value)
}

function handleKeydown(event: KeyboardEvent) {
  const textarea = event.target as HTMLTextAreaElement

  // Support Tab key to insert spaces
  if (event.key === 'Tab') {
    event.preventDefault()
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const value = textarea.value
    const newValue = value.substring(0, start) + '  ' + value.substring(end)
    textarea.value = newValue
    textarea.selectionStart = textarea.selectionEnd = start + 2
    emit('update:modelValue', newValue)
  }
}
</script>

<style scoped>
.code-editor {
  position: relative;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  overflow: hidden;
  background: #1e1e1e;
}
.code-editor.readonly {
  background: #f5f5f5;
}
.editor-lang {
  position: absolute;
  top: 4px;
  right: 8px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.08);
  padding: 1px 6px;
  border-radius: 3px;
  z-index: 1;
  pointer-events: none;
}
.readonly .editor-lang {
  color: rgba(0, 0, 0, 0.25);
  background: rgba(0, 0, 0, 0.04);
}
.editor-textarea {
  display: block;
  width: 100%;
  min-height: 200px;
  padding: 12px 16px;
  border: none;
  outline: none;
  resize: vertical;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.6;
  tab-size: 2;
  background: transparent;
  color: #d4d4d4;
  box-sizing: border-box;
}
.readonly .editor-textarea {
  color: rgba(0, 0, 0, 0.85);
  cursor: default;
}
.editor-textarea:focus {
  outline: none;
}
</style>
