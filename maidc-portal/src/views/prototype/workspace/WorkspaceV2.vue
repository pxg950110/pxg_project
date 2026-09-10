<template>
  <PageContainer title="首页工作台 v2 · 页面原型" :breadcrumb="[{ title: '原型' }, { title: '首页工作台' }]">
    <!-- 角色视角切换：仅评审演示用，正式实现由登录用户角色驱动 -->
    <a-card size="small" style="margin-bottom: 16px">
      <a-space :size="16">
        <span class="dim">角色视角（评审用）：</span>
        <a-radio-group v-model:value="group" size="small" button-style="solid">
          <a-radio-button v-for="opt in roleGroupOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio-button>
        </a-radio-group>
        <a-tag color="purple">{{ mock.roleKey }}</a-tag>
        <span class="dim">同一骨架，指标卡 / 待办源 / 快捷入口按角色组差异化</span>
      </a-space>
    </a-card>

    <!-- 欢迎栏 + 快捷患者检索（P1） -->
    <a-card :bordered="false" style="margin-bottom: 16px">
      <div class="welcome-bar">
        <div>
          <div class="hello">
            {{ mock.welcome.greeting }}，{{ mock.userName }}
            <a-tag color="blue" style="margin-left: 8px">{{ mock.roleName }}</a-tag>
          </div>
          <div class="dim">{{ mock.welcome.date }} · {{ mock.welcome.orgName }}</div>
        </div>
        <a-input-search
          v-model:value="patientKeyword"
          placeholder="快捷患者检索：姓名 / 住院号，回车直达患者列表"
          style="width: 360px"
          allow-clear
          enter-button
          @search="onPatientSearch"
        />
      </div>
    </a-card>

    <!-- 指标卡：按角色组返回 cards 数组，前端通用渲染 -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col v-for="card in mock.cards" :key="card.key" :span="6">
        <a-card hoverable :bordered="false" @click="go(card.route)">
          <div class="metric-card">
            <a-statistic
              :title="card.label"
              :value="card.value"
              :suffix="card.suffix"
              :value-style="{ color: toneColor(card.tone), fontWeight: 600 }"
            />
            <component :is="iconMap[card.icon]" class="metric-icon" :style="{ color: toneColor(card.tone) }" />
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 待办中心（60%） + 通知 / 队列动态（40%） -->
    <a-row :gutter="16">
      <a-col :span="14">
        <a-card :bordered="false">
          <template #title>
            待办中心
            <a-badge :count="stats.total" :number-style="{ backgroundColor: '#1677ff' }" style="margin-left: 8px" />
          </template>
          <template #extra>
            <a-space>
              <a-tag color="blue">今日到期 {{ stats.today }}</a-tag>
              <a-tag :color="stats.overdue > 0 ? 'error' : 'default'">已超期 {{ stats.overdue }}</a-tag>
              <a-button size="small" :loading="refreshing" @click="refresh">
                <template #icon><ReloadOutlined /></template>
                刷新
              </a-button>
            </a-space>
          </template>

          <a-tabs v-model:activeKey="todoTab" size="small">
            <a-tab-pane key="ALL">
              <template #tab>全部 <a-badge :count="stats.total" size="small" style="margin-left: 2px" /></template>
            </a-tab-pane>
            <a-tab-pane v-if="hasFollowup" key="FOLLOWUP">
              <template #tab>随访 <a-badge :count="stats.followup" size="small" style="margin-left: 2px" /></template>
            </a-tab-pane>
            <a-tab-pane key="APPROVAL">
              <template #tab>审批 <a-badge :count="stats.approval" size="small" style="margin-left: 2px" /></template>
            </a-tab-pane>
            <a-tab-pane key="LABELING">
              <template #tab>标注 <a-badge :count="stats.labeling" size="small" style="margin-left: 2px" /></template>
            </a-tab-pane>
            <a-tab-pane key="OTHER">
              <template #tab>其他 <a-badge :count="stats.other" size="small" style="margin-left: 2px" /></template>
            </a-tab-pane>
          </a-tabs>

          <a-empty v-if="filteredTodos.length === 0" description="暂无待办任务" style="padding: 32px 0" />

          <a-list v-else :data-source="filteredTodos" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <!-- 随访待办：患者 + 阶段 + 应评量表 + 超期状态，入口跳随访工作台执行 -->
                <a-list-item-meta v-if="item.taskType === 'FOLLOWUP'">
                  <template #title>
                    <a @click="goFollowup">{{ item.patientName }} · {{ item.stageName }}随访</a>
                    <a-tag v-if="item.overdueDays" color="error" style="margin-left: 8px">
                      超期 {{ item.overdueDays }} 天
                    </a-tag>
                    <a-tooltip v-if="(item.overdueDays ?? 0) > 7" title="超期 > 7 天已升级通知负责医生">
                      <WarningOutlined style="color: #faad14; margin-left: 4px" />
                    </a-tooltip>
                  </template>
                  <template #description>
                    <a-tag v-for="s in item.scales" :key="s" color="red" style="margin-bottom: 2px">{{ s }}</a-tag>
                    <span class="dim" style="margin-left: 8px">到期：{{ item.dueDate }}</span>
                  </template>
                  <template #avatar>
                    <a-tag :color="priorityColor(item.priority)">{{ item.priority }}</a-tag>
                  </template>
                </a-list-item-meta>

                <!-- 通用待办：与 v1 行为一致 -->
                <a-list-item-meta v-else>
                  <template #title>{{ item.title }}</template>
                  <template #description>
                    <span>{{ typeLabel(item.taskType) }}</span>
                    <span class="dim" style="margin-left: 12px">截止：{{ item.dueDate }}</span>
                  </template>
                  <template #avatar>
                    <a-tag :color="priorityColor(item.priority)">{{ item.priority }}</a-tag>
                  </template>
                </a-list-item-meta>

                <template #actions>
                  <a-button v-if="item.taskType === 'FOLLOWUP'" type="primary" size="small" @click="goFollowup">
                    开始随访
                  </a-button>
                  <a-button v-else type="link" size="small" @click="completeTodo(item)">完成</a-button>
                </template>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>

      <a-col :span="10">
        <a-card title="消息通知" :bordered="false" style="margin-bottom: 16px">
          <template #extra>
            <a-button type="link" size="small" @click="markAllRead">全部已读</a-button>
          </template>
          <a-list :data-source="localNotifications" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <div class="notify-item" :class="{ unread: !item.isRead }" @click="markRead(item)">
                  <a-badge :status="notifyStatus(item.type)" />
                  <span class="notify-title">{{ item.title }}</span>
                  <span class="dim notify-time">{{ item.createdAt }}</span>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>

        <!-- 队列动态：临床 / 科研角色组显示（P1） -->
        <a-card v-if="mock.cohortDigest" title="专病队列动态" :bordered="false">
          <a-list :data-source="mock.cohortDigest" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-space>
                  <a-tag :color="digestColor(item.type)">{{ digestLabel(item.type) }}</a-tag>
                  <span>{{ item.title }}</span>
                </a-space>
                <span class="dim">{{ item.time }}</span>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>

    <!-- 快捷入口：服务端按角色下发，前端 hasPermission 兜底过滤 -->
    <a-card title="快捷操作" :bordered="false" style="margin-top: 16px">
      <a-row :gutter="[16, 16]">
        <a-col v-for="action in mock.quickActions" :key="action.key" :span="6">
          <a-button type="primary" ghost block size="large" @click="go(action.route)">
            <template #icon>
              <component :is="iconMap[action.icon]" />
            </template>
            {{ action.label }}
          </a-button>
        </a-col>
      </a-row>
    </a-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ScheduleOutlined, AlertOutlined, TeamOutlined, CheckCircleOutlined, DatabaseOutlined,
  AuditOutlined, AppstoreOutlined, ExperimentOutlined, RocketOutlined, ThunderboltOutlined,
  SafetyOutlined, SearchOutlined, ProfileOutlined, BookOutlined, MedicineBoxOutlined,
  ProjectOutlined, SyncOutlined, PlusOutlined, UserOutlined, FileSearchOutlined,
  DashboardOutlined, ReloadOutlined, WarningOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { workspaceMock, roleGroupOptions } from './mock'
import type { RoleGroup, TodoItemVO, NotificationVO } from './mock'

const router = useRouter()

/* 角色视角（原型评审用） */
const group = ref<RoleGroup>('CLINICAL')
const mock = computed(() => workspaceMock[group.value])

/* 本地可变副本：模拟完成待办 / 已读交互 */
const localTodos = ref<TodoItemVO[]>([])
const localNotifications = ref<NotificationVO[]>([])
watch(() => group.value, (g) => {
  localTodos.value = [...workspaceMock[g].todos]
  localNotifications.value = workspaceMock[g].notifications.map(n => ({ ...n }))
}, { immediate: true })

const patientKeyword = ref('')
const todoTab = ref('ALL')
const refreshing = ref(false)

const iconMap: Record<string, any> = {
  schedule: ScheduleOutlined,
  alert: AlertOutlined,
  team: TeamOutlined,
  'check-circle': CheckCircleOutlined,
  database: DatabaseOutlined,
  audit: AuditOutlined,
  appstore: AppstoreOutlined,
  experiment: ExperimentOutlined,
  rocket: RocketOutlined,
  thunderbolt: ThunderboltOutlined,
  shield: SafetyOutlined,
  search: SearchOutlined,
  profile: ProfileOutlined,
  book: BookOutlined,
  'medicine-box': MedicineBoxOutlined,
  project: ProjectOutlined,
  sync: SyncOutlined,
  plus: PlusOutlined,
  user: UserOutlined,
  'file-search': FileSearchOutlined,
  dashboard: DashboardOutlined,
}

const toneColor = (tone?: string) =>
  ({ primary: '#1677ff', danger: '#cf1322', success: '#389e0d', warning: '#fa8c16' } as any)[tone ?? ''] ?? 'rgba(0, 0, 0, 0.88)'
const priorityColor = (p: string) => ({ HIGH: 'red', MEDIUM: 'orange', LOW: 'blue' } as any)[p] ?? 'default'
const typeLabel = (t: string) =>
  ({ APPROVAL: '审批', LABELING: '标注', DATA_QUERY: '数据查询', FOLLOWUP: '随访', OTHER: '其他' } as any)[t] ?? t
const digestLabel = (t: string) => ({ SYNC_DONE: '同步', AI_SUGGEST: 'AI建议', KB_UPDATE: '知识库' } as any)[t] ?? t
const digestColor = (t: string) => ({ SYNC_DONE: 'cyan', AI_SUGGEST: 'purple', KB_UPDATE: 'geekblue' } as any)[t] ?? 'default'
const notifyStatus = (t: string) => ({ ALERT: 'error', APPROVAL: 'warning', COHORT: 'processing' } as any)[t] ?? 'default'

const hasFollowup = computed(() => localTodos.value.some(t => t.taskType === 'FOLLOWUP'))
const stats = computed(() => ({
  total: localTodos.value.length,
  today: localTodos.value.filter(t => t.dueDate === '2026-09-09').length,
  overdue: localTodos.value.filter(t => (t.overdueDays ?? 0) > 0).length,
  followup: localTodos.value.filter(t => t.taskType === 'FOLLOWUP').length,
  approval: localTodos.value.filter(t => t.taskType === 'APPROVAL').length,
  labeling: localTodos.value.filter(t => t.taskType === 'LABELING').length,
  other: localTodos.value.filter(t => t.taskType === 'OTHER' || t.taskType === 'DATA_QUERY').length,
}))

const filteredTodos = computed(() => {
  if (todoTab.value === 'ALL') return localTodos.value
  if (todoTab.value === 'OTHER') return localTodos.value.filter(t => t.taskType === 'OTHER' || t.taskType === 'DATA_QUERY')
  return localTodos.value.filter(t => t.taskType === todoTab.value)
})

function go(route?: string) {
  if (route) router.push(route)
}

function goFollowup() {
  // 正式实现后跳随访工作台（路由随 CRS 前端平移确定），原型期间联动 CRS 原型页
  router.push('/proto/crs/workbench')
}

function onPatientSearch(value: string) {
  if (!value.trim()) return
  // 正式实现：透传 keyword 到患者列表；命中唯一患者号时直达患者 360
  router.push({ path: '/data/cdr/patients', query: { keyword: value.trim() } })
}

function completeTodo(item: TodoItemVO) {
  localTodos.value = localTodos.value.filter(t => t.id !== item.id)
  message.success(`已完成：${item.title}`)
}

function markRead(item: NotificationVO) {
  item.isRead = true
}

function markAllRead() {
  localNotifications.value.forEach(n => { n.isRead = true })
}

function refresh() {
  refreshing.value = true
  setTimeout(() => {
    refreshing.value = false
    message.success('工作台数据已刷新')
  }, 600)
}
</script>

<style scoped>
.dim { color: #999; font-size: 13px; }

.welcome-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.hello { font-size: 20px; font-weight: 600; }

.metric-card { position: relative; }
.metric-icon {
  position: absolute;
  top: 4px;
  right: 0;
  font-size: 28px;
  opacity: 0.85;
}

.notify-item {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
  cursor: pointer;
  padding-left: 8px;
  border-left: 3px solid transparent;
}
.notify-item.unread { border-left-color: #1677ff; }
.notify-title { flex: 1; }
.notify-time { flex-shrink: 0; }
</style>
