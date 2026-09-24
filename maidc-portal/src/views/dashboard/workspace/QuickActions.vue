<template>
  <el-card shadow="never" class="rounded-xl">
    <template #header>
      <span class="text-base font-semibold text-slate-800">快捷操作</span>
    </template>
    <div class="grid grid-cols-2 gap-3 md:grid-cols-4">
      <button
        v-for="action in allowedActions"
        :key="action.key"
        type="button"
        class="quick-action-btn"
        @click="handleClick(action)"
      >
        <el-icon :size="20">
          <component :is="workspaceIconMap[action.icon] ?? workspaceIconMap['appstore']" />
        </el-icon>
        <span>{{ action.label }}</span>
      </button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { usePermissionStore } from '@/stores/permission'
import { workspaceIconMap } from './icons'
import type { QuickAction } from '@/api/workspace'

const props = defineProps<{ actions: QuickAction[] }>()
const router = useRouter()
const permissionStore = usePermissionStore()

// 服务端已按角色组过滤；permission 字段为前端 hasPermission 兜底（角色缓存与后端不一致时防御）
const allowedActions = computed(() =>
  props.actions.filter(a => !a.permission || permissionStore.hasPermission(a.permission)))

function handleClick(action: QuickAction) {
  router.push(action.route)
}
</script>

<style scoped>
.quick-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 48px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-action-btn:hover {
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
</style>
