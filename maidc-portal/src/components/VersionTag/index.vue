<template>
  <el-tag :type="tagType" size="small" class="version-tag">
    {{ version }}
    <span v-if="isLatest" class="latest-badge">Latest</span>
  </el-tag>
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

const tagType = computed<'success' | 'warning' | 'primary'>(() => {
  if (props.isLatest) return 'success'
  // Determine color by version prefix
  const v = props.version.replace(/^v/i, '')
  const parts = v.split('.')
  if (parts.length >= 1 && parts[0] === '0') return 'warning'
  return 'primary'
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
  background: rgba(103, 194, 58, 0.15);
}
</style>
