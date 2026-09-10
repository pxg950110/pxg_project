<template>
  <a-card title="快捷操作" :bordered="false">
    <a-row :gutter="[16, 16]">
      <a-col v-for="action in allowedActions" :key="action.key" :span="6">
        <a-button type="primary" ghost block size="large" @click="handleClick(action)">
          <template #icon>
            <component :is="workspaceIconMap[action.icon] ?? workspaceIconMap['appstore']" />
          </template>
          {{ action.label }}
        </a-button>
      </a-col>
    </a-row>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { usePermissionStore } from '@/stores/permission'
import { workspaceIconMap } from './icons'
import type { QuickAction } from '@/api/workspace'

const props = defineProps<{
  actions: QuickAction[]
}>()

const router = useRouter()
const permissionStore = usePermissionStore()

// 服务端已按角色组过滤；permission 字段为前端 hasPermission 兜底（角色缓存与后端不一致时防御）
const allowedActions = computed(() =>
  props.actions.filter(a => !a.permission || permissionStore.hasPermission(a.permission)))

function handleClick(action: QuickAction) {
  router.push(action.route)
}
</script>
