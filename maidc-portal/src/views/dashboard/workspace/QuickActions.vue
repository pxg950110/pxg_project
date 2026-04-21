<template>
  <a-card title="快捷操作" :bordered="false">
    <a-row :gutter="[16, 16]">
      <a-col v-for="action in actions" :key="action.key" :span="6">
        <a-button type="primary" ghost block size="large" @click="handleClick(action)">
          <template #icon>
            <component :is="iconMap[action.icon]" />
          </template>
          {{ action.label }}
        </a-button>
      </a-col>
    </a-row>
  </a-card>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { PlusOutlined, SearchOutlined, ExperimentOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import type { QuickAction } from '@/api/workspace'

defineProps<{
  actions: QuickAction[]
}>()

const router = useRouter()

const iconMap: Record<string, any> = {
  'plus-outlined': PlusOutlined,
  'search-outlined': SearchOutlined,
  'experiment-outlined': ExperimentOutlined,
  'thunderbolt-outlined': ThunderboltOutlined,
}

function handleClick(action: QuickAction) {
  router.push(action.route)
}
</script>
