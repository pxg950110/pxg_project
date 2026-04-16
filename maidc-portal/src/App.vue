<template>
  <a-config-provider :theme="themeConfig">
    <router-view />
  </a-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()

const themeConfig = computed(() => ({
  token: {
    colorPrimary: '#' + uiStore.primaryColor,
  },
}))
</script>
