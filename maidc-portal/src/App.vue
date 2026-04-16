<template>
  <a-config-provider :theme="themeConfig">
    <router-view />
  </a-config-provider>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()

const themeConfig = computed(() => ({
  token: {
    colorPrimary: '#' + uiStore.primaryColor,
  },
}))

// Sync primaryColor to CSS variable for custom styles (logo, hover effects, etc.)
watch(
  () => uiStore.primaryColor,
  (color) => {
    document.documentElement.style.setProperty('--ant-color-primary', '#' + color)
  },
  { immediate: true },
)
</script>
