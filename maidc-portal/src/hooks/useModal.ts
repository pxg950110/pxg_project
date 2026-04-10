import { ref } from 'vue'

export function useModal<T = any>() {
  const visible = ref(false)
  const currentRecord = ref<T | null>(null)

  function open(record?: T) {
    currentRecord.value = record ?? null
    visible.value = true
  }

  function close() {
    visible.value = false
    currentRecord.value = null
  }

  return { visible, currentRecord, open, close }
}
