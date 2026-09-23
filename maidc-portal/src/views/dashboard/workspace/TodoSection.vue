<template>
  <div class="bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm flex flex-col h-full">
    <!-- 头部与筛选 -->
    <div class="flex items-center justify-between pb-3 mb-2 border-b border-slate-100 flex-wrap gap-2">
      <div class="flex items-center gap-2">
        <span class="w-1 h-3.5 bg-sky-500 rounded-full" />
        <h3 class="text-sm font-semibold text-slate-900 m-0">待办事项</h3>
        <span
          v-if="stats.total > 0"
          class="inline-flex items-center justify-center px-2 py-0.5 text-xs font-bold bg-sky-100 text-sky-700 rounded-full"
        >
          {{ stats.total }}
        </span>
      </div>

      <!-- 右侧统计与分类筛选 -->
      <div class="flex items-center gap-2 flex-wrap">
        <span class="text-xs px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 font-medium">
          今日 {{ stats.today }}
        </span>
        <span
          v-if="stats.overdue > 0"
          class="text-xs px-2 py-0.5 rounded-full bg-rose-100 text-rose-700 font-medium"
        >
          超期 {{ stats.overdue }}
        </span>

        <!-- 分类筛选单选按钮组 -->
        <el-radio-group v-model="activeFilter" size="small">
          <el-radio-button label="ALL">全部</el-radio-button>
          <el-radio-button v-if="hasFollowup" label="FOLLOWUP">随访</el-radio-button>
          <el-radio-button label="APPROVAL">审批</el-radio-button>
          <el-radio-button label="OTHER">其他</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 列表内容区 -->
    <div v-loading="loading" class="flex-1 overflow-y-auto min-h-[280px]">
      <div v-if="filteredTodos.length === 0" class="py-12 flex flex-col items-center justify-center text-slate-400">
        <el-icon :size="40" class="text-slate-300 mb-2"><CircleCheck /></el-icon>
        <p class="text-xs font-medium">暂无进行中的待办任务</p>
      </div>

      <div v-else class="divide-y divide-slate-100">
        <div
          v-for="item in filteredTodos"
          :key="item.id"
          class="py-3 px-2 flex items-center justify-between gap-4 hover:bg-slate-50/80 rounded-lg transition-colors group"
        >
          <!-- 左侧优先级徽标与标题信息 -->
          <div class="flex items-start gap-3 flex-1 min-w-0">
            <!-- 优先级微徽标 -->
            <span
              class="mt-0.5 px-1.5 py-0.5 rounded text-[11px] font-semibold select-none flex-shrink-0"
              :class="priorityClasses(item.priority)"
            >
              {{ item.priority || 'NORMAL' }}
            </span>

            <div class="min-w-0 flex-1">
              <!-- 随访待办 -->
              <div v-if="item.taskType === 'FOLLOWUP'" class="space-y-1">
                <div class="flex items-center gap-2 flex-wrap">
                  <span
                    class="text-xs font-semibold text-slate-800 hover:text-sky-600 transition-colors cursor-pointer"
                    @click="goFollowupWorkbench(item)"
                  >
                    {{ item.patientName ?? item.title }}
                  </span>
                  <span v-if="item.stageName" class="text-xs text-slate-500">
                    · {{ item.stageName }}随访
                  </span>

                  <!-- 超期警报 -->
                  <span
                    v-if="(item.overdueDays ?? 0) > 0"
                    class="inline-flex items-center gap-1 text-[11px] font-semibold text-rose-600 bg-rose-50 border border-rose-200 px-1.5 py-0.2 rounded"
                  >
                    超期 {{ item.overdueDays }} 天
                  </span>
                  <el-tooltip v-if="(item.overdueDays ?? 0) > 7" content="超期 > 7 天已升级通知负责医生" placement="top">
                    <el-icon :size="13" class="text-amber-500"><WarningFilled /></el-icon>
                  </el-tooltip>
                </div>

                <!-- 量表与截止时间 -->
                <div class="flex items-center gap-2 flex-wrap text-xs text-slate-400">
                  <span
                    v-for="scale in item.scales ?? []"
                    :key="scale"
                    class="px-1.5 py-0.5 rounded bg-rose-50 text-rose-600 text-[11px]"
                  >
                    {{ scale }}
                  </span>
                  <span class="text-slate-400">到期：{{ item.dueDate }}</span>
                </div>
              </div>

              <!-- 通用常规待办 -->
              <div v-else class="space-y-1">
                <div class="flex items-center gap-2">
                  <span
                    class="text-xs font-semibold text-slate-800 hover:text-sky-600 transition-colors cursor-pointer"
                    @click="handleNavigate(item)"
                  >
                    {{ item.title }}
                  </span>
                  <span class="text-[11px] px-1.5 py-0.5 rounded bg-slate-100 text-slate-600">
                    {{ formatType(item.taskType) }}
                  </span>
                </div>
                <div v-if="item.dueDate" class="text-xs text-slate-400">
                  截止日期：
                  <span :class="{ 'text-rose-500 font-semibold': (item.overdueDays ?? 0) > 0 }">
                    {{ item.dueDate }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧动作操作 -->
          <div class="flex-shrink-0 flex items-center gap-2">
            <el-button
              v-if="item.taskType === 'FOLLOWUP'"
              type="primary"
              size="small"
              class="!rounded-md"
              @click="goFollowupWorkbench(item)"
            >
              开始随访
            </el-button>
            <el-button
              v-else
              link
              type="primary"
              size="small"
              class="!text-xs"
              @click="handleComplete(item)"
            >
              完成
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { CircleCheck, WarningFilled } from '@element-plus/icons-vue'
import type { TodoStats, WorkspaceTodo } from '@/api/workspace'

const props = defineProps<{
  todos: WorkspaceTodo[]
  stats?: TodoStats | null
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'complete', id: number): void
}>()

const router = useRouter()
const activeFilter = ref('ALL')

const hasFollowup = computed(() => props.todos.some((t) => t.taskType === 'FOLLOWUP'))

const localStats = computed<TodoStats>(() => {
  const today = new Date().toISOString().slice(0, 10)
  return {
    today: props.todos.filter((t) => t.dueDate === today).length,
    overdue: props.todos.filter((t) => (t.overdueDays ?? 0) > 0).length,
    total: props.todos.length,
  }
})

const stats = computed(() => (props.stats && props.stats.total > 0 ? props.stats : localStats.value))

const filteredTodos = computed(() => {
  if (activeFilter.value === 'ALL') return props.todos
  if (activeFilter.value === 'OTHER') {
    return props.todos.filter((t) => t.taskType === 'OTHER' || t.taskType === 'DATA_QUERY')
  }
  return props.todos.filter((t) => t.taskType === activeFilter.value)
})

function formatType(type: string) {
  const map: Record<string, string> = {
    APPROVAL: '审批',
    LABELING: '标注',
    DATA_QUERY: '数据查询',
    FOLLOWUP: '随访',
    OTHER: '其他',
  }
  return map[type] ?? type
}

function priorityClasses(priority?: string) {
  const p = (priority || '').toUpperCase()
  if (p === 'HIGH' || p === 'CRITICAL') {
    return 'bg-rose-100 text-rose-700'
  }
  if (p === 'MEDIUM') {
    return 'bg-amber-100 text-amber-700'
  }
  return 'bg-sky-100 text-sky-700'
}

function handleNavigate(item: WorkspaceTodo) {
  const routeMap: Record<string, string> = {
    APPROVAL: `/model/approvals/${item.sourceId}`,
    LABELING: `/label/workspace/${item.sourceId}`,
  }
  const route = item.sourceType ? routeMap[item.sourceType] : undefined
  if (route) router.push(route)
}

function goFollowupWorkbench(item: WorkspaceTodo) {
  router.push({
    path: '/followup/workbench',
    query: {
      ...(item.patientId != null ? { patientId: String(item.patientId) } : {}),
      ...(item.sourceId != null ? { taskId: String(item.sourceId) } : {}),
    },
  })
}

function handleComplete(item: WorkspaceTodo) {
  emit('complete', item.id)
}
</script>
