<template>
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()

// Sync primaryColor to CSS variable for custom styles (logo, hover effects, etc.)
watch(
  () => uiStore.primaryColor,
  (color) => {
    if (color) {
      document.documentElement.style.setProperty('--el-color-primary', '#' + color)
    }
  },
  { immediate: true },
)
</script>
