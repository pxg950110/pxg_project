<template>
  <a-tag :color="tagColor" class="version-tag">
    {{ version }}
    <span v-if="isLatest" class="latest-badge">Latest</span>
  </a-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  version: string
  isLatest?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isLatest: false,
})

const tagColor = computed(() => {
  if (props.isLatest) return 'green'
  // Determine color by version prefix
  const v = props.version.replace(/^v/i, '')
  const parts = v.split('.')
  if (parts.length >= 1 && parts[0] === '0') return 'orange'
  return 'blue'
})
</script>

<style scoped>
.version-tag {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
}
.latest-badge {
  margin-left: 4px;
  font-size: 10px;
  padding: 0 4px;
  border-radius: 3px;
  background: rgba(82, 196, 26, 0.15);
}
</style>
