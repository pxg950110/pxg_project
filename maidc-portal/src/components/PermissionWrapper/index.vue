<template>
  <slot v-if="permitted" />
  <slot v-else name="fallback" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { usePermissionStore } from '@/stores/permission'

interface Props {
  code: string
}

const props = defineProps<Props>()

const permissionStore = usePermissionStore()
const permitted = computed(() => permissionStore.hasPermission(props.code))
</script>
