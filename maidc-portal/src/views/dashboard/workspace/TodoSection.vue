<template>
  <a-card title="待办任务" :bordered="false" :loading="loading">
    <template #extra>
      <a-radio-group v-model:value="activeFilter" size="small" button-style="solid">
        <a-radio-button value="ALL">全部</a-radio-button>
        <a-radio-button value="APPROVAL">审批</a-radio-button>
        <a-radio-button value="LABELING">标注</a-radio-button>
        <a-radio-button value="OTHER">其他</a-radio-button>
      </a-radio-group>
    </template>

    <div v-if="filteredTodos.length === 0" class="empty-state">
      <a-empty description="暂无待办任务" />
    </div>

    <a-list v-else :data-source="filteredTodos" size="small">
      <template #renderItem="{ item }">
        <a-list-item>
          <a-list-item-meta>
            <template #title>
              <a class="todo-title" @click="handleNavigate(item)">{{ item.title }}</a>
            </template>
            <template #description>
              <span>{{ formatType(item.taskType) }}</span>
              <span v-if="item.dueDate" style="margin-left: 12px; color: #ff4d4f">
                截止: {{ item.dueDate }}
              </span>
            </template>
            <template #avatar>
              <a-tag :color="priorityColor(item.priority)">{{ item.priority }}</a-tag>
            </template>
          </a-list-item-meta>
          <template #actions>
            <a-button type="link" size="small" @click="handleComplete(item)">完成</a-button>
          </template>
        </a-list-item>
      </template>
    </a-list>
  </a-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import type { PersonalTaskVO } from '@/api/workspace'

const props = defineProps<{
  todos: PersonalTaskVO[]
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'complete', id: number): void
}>()

const router = useRouter()
const activeFilter = ref('ALL')

const filteredTodos = computed(() => {
  if (activeFilter.value === 'ALL') return props.todos
  return props.todos.filter(t => t.taskType === activeFilter.value)
})

function formatType(type: string) {
  const map: Record<string, string> = { APPROVAL: '审批', LABELING: '标注', DATA_QUERY: '数据查询', OTHER: '其他' }
  return map[type] ?? type
}

function priorityColor(priority: string) {
  const map: Record<string, string> = { HIGH: 'red', MEDIUM: 'orange', LOW: 'blue' }
  return map[priority] ?? 'default'
}

function handleNavigate(item: PersonalTaskVO) {
  const routeMap: Record<string, string> = {
    APPROVAL: `/model/approvals/${item.sourceId}`,
    LABELING: `/label/workspace/${item.sourceId}`,
  }
  const route = routeMap[item.sourceType]
  if (route) router.push(route)
}

function handleComplete(item: PersonalTaskVO) {
  emit('complete', item.id)
}
</script>

<style scoped lang="scss">
.empty-state {
  padding: 32px 0;
}

.todo-title {
  cursor: pointer;
  &:hover { color: #1890ff; }
}
</style>
