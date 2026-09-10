<template>
  <a-card :bordered="false" :loading="loading">
    <template #title>
      待办任务
      <a-badge
        v-if="stats.total > 0"
        :count="stats.total"
        :number-style="{ backgroundColor: '#1677ff' }"
        style="margin-left: 8px"
      />
    </template>
    <template #extra>
      <a-space :size="8">
        <a-tag color="blue">今日 {{ stats.today }}</a-tag>
        <a-tag :color="stats.overdue > 0 ? 'error' : 'default'">超期 {{ stats.overdue }}</a-tag>
        <a-radio-group v-model:value="activeFilter" size="small" button-style="solid">
          <a-radio-button value="ALL">全部</a-radio-button>
          <a-radio-button v-if="hasFollowup" value="FOLLOWUP">随访</a-radio-button>
          <a-radio-button value="APPROVAL">审批</a-radio-button>
          <a-radio-button value="LABELING">标注</a-radio-button>
          <a-radio-button value="OTHER">其他</a-radio-button>
        </a-radio-group>
      </a-space>
    </template>

    <div v-if="filteredTodos.length === 0" class="empty-state">
      <a-empty description="暂无待办任务" />
    </div>

    <a-list v-else :data-source="filteredTodos" size="small">
      <template #renderItem="{ item }">
        <a-list-item>
          <!-- 随访待办：患者/阶段/必评量表/超期状态，跳随访工作台执行（不在工作台直接完成） -->
          <a-list-item-meta v-if="item.taskType === 'FOLLOWUP'">
            <template #title>
              <a class="todo-title" @click="goFollowupWorkbench">
                {{ item.patientName ?? item.title }}
                <span v-if="item.stageName" class="dim">· {{ item.stageName }}随访</span>
              </a>
              <a-tag v-if="(item.overdueDays ?? 0) > 0" color="error" style="margin-left: 8px">
                超期 {{ item.overdueDays }} 天
              </a-tag>
              <a-tooltip v-if="(item.overdueDays ?? 0) > 7" title="超期 > 7 天已升级通知负责医生">
                <WarningOutlined style="color: #faad14; margin-left: 4px" />
              </a-tooltip>
            </template>
            <template #description>
              <a-tag v-for="scale in item.scales ?? []" :key="scale" color="red" style="margin-bottom: 2px">
                {{ scale }}
              </a-tag>
              <span class="dim" style="margin-left: 8px">到期：{{ item.dueDate }}</span>
            </template>
            <template #avatar>
              <a-tag :color="priorityColor(item.priority)">{{ item.priority }}</a-tag>
            </template>
          </a-list-item-meta>

          <!-- 通用待办：与 v1 行为一致 -->
          <a-list-item-meta v-else>
            <template #title>
              <a class="todo-title" @click="handleNavigate(item)">{{ item.title }}</a>
            </template>
            <template #description>
              <span>{{ formatType(item.taskType) }}</span>
              <span v-if="item.dueDate" style="margin-left: 12px" :class="{ 'overdue-text': (item.overdueDays ?? 0) > 0 }">
                截止: {{ item.dueDate }}
              </span>
            </template>
            <template #avatar>
              <a-tag :color="priorityColor(item.priority)">{{ item.priority }}</a-tag>
            </template>
          </a-list-item-meta>

          <template #actions>
            <a-button v-if="item.taskType === 'FOLLOWUP'" type="primary" size="small" @click="goFollowupWorkbench">
              开始随访
            </a-button>
            <a-button v-else type="link" size="small" @click="handleComplete(item)">完成</a-button>
          </template>
        </a-list-item>
      </template>
    </a-list>
  </a-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { WarningOutlined } from '@ant-design/icons-vue'
import type { TodoStats, WorkspaceTodo } from '@/api/workspace'

const props = defineProps<{
  todos: WorkspaceTodo[]
  /** 服务端统计；缺失时按 todos 本地计算 */
  stats?: TodoStats | null
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'complete', id: number): void
}>()

const router = useRouter()
const activeFilter = ref('ALL')

const hasFollowup = computed(() => props.todos.some(t => t.taskType === 'FOLLOWUP'))

const localStats = computed<TodoStats>(() => {
  const today = new Date().toISOString().slice(0, 10)
  return {
    today: props.todos.filter(t => t.dueDate === today).length,
    overdue: props.todos.filter(t => (t.overdueDays ?? 0) > 0).length,
    total: props.todos.length,
  }
})

const stats = computed(() => (props.stats && props.stats.total > 0 ? props.stats : localStats.value))

const filteredTodos = computed(() => {
  if (activeFilter.value === 'ALL') return props.todos
  if (activeFilter.value === 'OTHER') {
    return props.todos.filter(t => t.taskType === 'OTHER' || t.taskType === 'DATA_QUERY')
  }
  return props.todos.filter(t => t.taskType === activeFilter.value)
})

function formatType(type: string) {
  const map: Record<string, string> = { APPROVAL: '审批', LABELING: '标注', DATA_QUERY: '数据查询', FOLLOWUP: '随访', OTHER: '其他' }
  return map[type] ?? type
}

function priorityColor(priority: string) {
  const map: Record<string, string> = { HIGH: 'red', MEDIUM: 'orange', LOW: 'blue' }
  return map[priority] ?? 'default'
}

function handleNavigate(item: WorkspaceTodo) {
  const routeMap: Record<string, string> = {
    APPROVAL: `/model/approvals/${item.sourceId}`,
    LABELING: `/label/workspace/${item.sourceId}`,
  }
  const route = item.sourceType ? routeMap[item.sourceType] : undefined
  if (route) router.push(route)
}

function goFollowupWorkbench() {
  // TODO: 随访工作台正式路由随 CRS 前端平移确定，暂指专病管理
  router.push('/data/cdr/disease')
}

function handleComplete(item: WorkspaceTodo) {
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

.dim {
  color: #999;
  font-size: 13px;
}

.overdue-text {
  color: #ff4d4f;
}
</style>
