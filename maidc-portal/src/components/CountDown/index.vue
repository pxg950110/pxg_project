<template>
  <span class="countdown">{{ display }}</span>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import dayjs from 'dayjs'

interface Props {
  targetTime: string | Date
  format?: string
}

interface Emits {
  (e: 'finish'): void
}

const props = withDefaults(defineProps<Props>(), {
  format: 'HH:mm:ss',
})
const emit = defineEmits<Emits>()

const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null

const target = computed(() => {
  const t = props.targetTime
  return dayjs(typeof t === 'string' ? t : t).valueOf()
})

const diff = computed(() => {
  const d = target.value - now.value
  return d > 0 ? d : 0
})

const display = computed(() => {
  const d = diff.value
  if (d <= 0) return '00:00:00'

  const hours = Math.floor(d / 3600000)
  const minutes = Math.floor((d % 3600000) / 60000)
  const seconds = Math.floor((d % 60000) / 1000)

  const fmt = props.format
  return fmt
    .replace(/DD/g, String(Math.floor(d / 86400000)).padStart(2, '0'))
    .replace(/HH/g, String(hours).padStart(2, '0'))
    .replace(/mm/g, String(minutes).padStart(2, '0'))
    .replace(/ss/g, String(seconds).padStart(2, '0'))
})

onMounted(() => {
  timer = setInterval(() => {
    now.value = Date.now()
    if (diff.value <= 0) {
      if (timer) clearInterval(timer)
      emit('finish')
    }
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.countdown {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-variant-numeric: tabular-nums;
}
</style>
