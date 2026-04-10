<template>
  <a-badge :color="meta.color" :text="meta.text" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { getStatusMeta, type StatusType } from './statusMap'

interface Props {
  status: string
  type: StatusType
}

const props = defineProps<Props>()

const meta = computed(() => getStatusMeta(props.status, props.type))
</script>
