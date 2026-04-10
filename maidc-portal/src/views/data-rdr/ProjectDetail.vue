<template>
  <PageContainer :title="project?.name || '项目详情'" :loading="loading">
    <template #extra>
      <a-button @click="router.back()">返回</a-button>
    </template>

    <template v-if="project">
      <!-- Basic Info -->
      <a-card style="margin-bottom: 16px">
        <a-descriptions :column="3" bordered size="small">
          <a-descriptions-item label="项目名称">{{ project.name }}</a-descriptions-item>
          <a-descriptions-item label="负责人 (PI)">{{ project.pi_name }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="project.status === 'ACTIVE' ? 'green' : 'default'">{{ project.status }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="开始时间">{{ formatDateTime(project.start_date) }}</a-descriptions-item>
          <a-descriptions-item label="结束时间">{{ formatDateTime(project.end_date) }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatDateTime(project.created_at) }}</a-descriptions-item>
          <a-descriptions-item label="描述" :span="3">{{ project.description || '-' }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- Tabs -->
      <a-card>
        <a-tabs v-model:activeKey="activeTab">
          <!-- Members Tab -->
          <a-tab-pane key="members" tab="成员">
            <div style="margin-bottom: 16px">
              <a-button type="primary" @click="inviteModal.open()">
                <PlusOutlined /> 邀请成员
              </a-button>
            </div>
            <a-table :columns="memberColumns" :data-source="members" :loading="membersLoading" size="small" row-key="id">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'role'">
                  <a-tag :color="record.role === 'PI' ? 'blue' : record.role === 'RESEARCHER' ? 'green' : 'default'">
                    {{ record.role }}
                  </a-tag>
                </template>
                <template v-if="column.key === 'joined_at'">
                  {{ formatDateTime(record.joined_at) }}
                </template>
                <template v-if="column.key === 'action'">
                  <a-popconfirm title="确认移除该成员？" @confirm="handleRemoveMember(record.id)">
                    <a class="danger-link">移除</a>
                  </a-popconfirm>
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <!-- Datasets Tab -->
          <a-tab-pane key="datasets" tab="数据集">
            <a-table :columns="datasetColumns" :data-source="datasets" :loading="datasetsLoading" size="small" row-key="id">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'action'">
                  <a @click="router.push(`/data/rdr/datasets/${record.id}`)">详情</a>
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <!-- Cohorts Tab -->
          <a-tab-pane key="cohorts" tab="队列">
            <a-table :columns="cohortColumns" :data-source="cohorts" :loading="cohortsLoading" size="small" row-key="id">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'created_at'">
                  {{ formatDateTime(record.created_at) }}
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <!-- Activity Tab -->
          <a-tab-pane key="activity" tab="活动记录">
            <a-timeline>
              <a-timeline-item
                v-for="(item, index) in activities"
                :key="index"
                :color="activityColorMap[item.type] || 'blue'"
              >
                <div class="activity-item">
                  <span class="activity-title">{{ item.title }}</span>
                  <span class="activity-desc">{{ item.description }}</span>
                  <span class="activity-time">{{ formatDateTime(item.created_at) }}</span>
                </div>
              </a-timeline-item>
            </a-timeline>
            <a-empty v-if="!activities.length" description="暂无活动记录" />
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </template>

    <!-- Invite Member Modal -->
    <a-modal
      v-model:open="inviteModal.visible"
      title="邀请成员"
      @ok="handleInvite"
      :confirm-loading="inviting"
      width="480px"
    >
      <a-form layout="vertical">
        <a-form-item label="选择用户" required>
          <UserSelect v-model:value="inviteForm.user_id" placeholder="搜索用户" />
        </a-form-item>
        <a-form-item label="项目角色" required>
          <a-select v-model:value="inviteForm.role" placeholder="选择角色">
            <a-select-option value="RESEARCHER">研究员</a-select-option>
            <a-select-option value="ANALYST">分析员</a-select-option>
            <a-select-option value="COLLABORATOR">协作人</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import UserSelect from '@/components/UserSelect/index.vue'
import { useModal } from '@/hooks/useModal'
import { getProject } from '@/api/data'
import request from '@/utils/request'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'ProjectDetail' })

const route = useRoute()
const router = useRouter()
const inviteModal = useModal()
const inviting = ref(false)

const project = ref<any>(null)
const loading = ref(false)
const activeTab = ref('members')

// Members
const members = ref<any[]>([])
const membersLoading = ref(false)
// Datasets
const datasets = ref<any[]>([])
const datasetsLoading = ref(false)
// Cohorts
const cohorts = ref<any[]>([])
const cohortsLoading = ref(false)
// Activities
const activities = ref<any[]>([])

const activityColorMap: Record<string, string> = {
  CREATE: 'green',
  UPDATE: 'blue',
  MEMBER_ADD: 'cyan',
  MEMBER_REMOVE: 'orange',
  DATASET_LINK: 'purple',
  WARNING: 'red',
}

const inviteForm = reactive({ user_id: undefined as any, role: 'RESEARCHER' })

const memberColumns = [
  { title: '姓名', dataIndex: 'user_name', key: 'user_name' },
  { title: '项目角色', dataIndex: 'role', key: 'role', width: 120 },
  { title: '加入时间', dataIndex: 'joined_at', key: 'joined_at', width: 170 },
  { title: '操作', key: 'action', width: 80 },
]

const datasetColumns = [
  { title: '数据集名称', dataIndex: 'name', key: 'name' },
  { title: '样本数', dataIndex: 'sample_count', key: 'sample_count', width: 100 },
  { title: '版本数', dataIndex: 'version_count', key: 'version_count', width: 80 },
  { title: '创建人', dataIndex: 'creator_name', key: 'creator_name', width: 100 },
  { title: '操作', key: 'action', width: 80 },
]

const cohortColumns = [
  { title: '队列名称', dataIndex: 'name', key: 'name' },
  { title: '纳入标准', dataIndex: 'criteria_summary', key: 'criteria_summary' },
  { title: '患者数', dataIndex: 'patient_count', key: 'patient_count', width: 100 },
  { title: '创建时间', dataIndex: 'created_at', key: 'created_at', width: 170 },
]

async function loadProject() {
  loading.value = true
  try {
    const res = await getProject(route.params.id as string)
    project.value = res.data.data
  } finally {
    loading.value = false
  }
}

async function loadMembers() {
  membersLoading.value = true
  try {
    const res = await request.get(`/rdr/projects/${route.params.id}/members`)
    members.value = res.data.data?.items || res.data.data || []
  } finally {
    membersLoading.value = false
  }
}

async function loadDatasets() {
  datasetsLoading.value = true
  try {
    const res = await request.get(`/rdr/projects/${route.params.id}/datasets`)
    datasets.value = res.data.data?.items || res.data.data || []
  } finally {
    datasetsLoading.value = false
  }
}

async function loadCohorts() {
  cohortsLoading.value = true
  try {
    const res = await request.get(`/rdr/projects/${route.params.id}/cohorts`)
    cohorts.value = res.data.data?.items || res.data.data || []
  } finally {
    cohortsLoading.value = false
  }
}

async function loadActivities() {
  try {
    const res = await request.get(`/rdr/projects/${route.params.id}/activities`)
    activities.value = res.data.data?.items || res.data.data || []
  } catch {
    activities.value = []
  }
}

watch(activeTab, (tab) => {
  if (tab === 'members' && !members.value.length) loadMembers()
  else if (tab === 'datasets' && !datasets.value.length) loadDatasets()
  else if (tab === 'cohorts' && !cohorts.value.length) loadCohorts()
  else if (tab === 'activity' && !activities.value.length) loadActivities()
})

async function handleInvite() {
  inviting.value = true
  try {
    await request.post(`/rdr/projects/${route.params.id}/members`, inviteForm)
    message.success('成员邀请成功')
    inviteModal.close()
    loadMembers()
  } finally {
    inviting.value = false
  }
}

async function handleRemoveMember(memberId: number) {
  try {
    await request.delete(`/rdr/projects/${route.params.id}/members/${memberId}`)
    message.success('成员已移除')
    loadMembers()
  } catch {
    // error handled by request interceptor
  }
}

onMounted(async () => {
  await loadProject()
  loadMembers()
})
</script>

<style scoped>
.activity-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.activity-title {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
}
.activity-desc {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
}
.activity-time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
}
.danger-link {
  color: #ff4d4f;
}
.danger-link:hover {
  color: #ff7875;
}
</style>
