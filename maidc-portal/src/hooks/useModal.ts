import { ref, computed } from 'vue'

export function useModal<T = any>() {
  const visibleRef = ref(false)
  const currentRecord = ref<T | null>(null)

  const visible = computed({
    get: () => visibleRef.value,
    set: (val: boolean) => { visibleRef.value = val }
  })

  function open(record?: T) {
    currentRecord.value = record ?? null
    visibleRef.value = true
  }

  function close() {
    visibleRef.value = false
    currentRecord.value = null
  }

  return { visible, currentRecord, open, close }
}
