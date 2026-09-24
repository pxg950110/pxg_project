<template>
  <PageContainer :title="project?.name || '项目详情'" :loading="loading">
    <template #extra>
      <el-button @click="router.back()">返回</el-button>
    </template>

    <template v-if="project">
      <!-- Basic Info -->
      <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm mb-4">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="项目名称">{{ project.name }}</el-descriptions-item>
          <el-descriptions-item label="负责人 (PI)">{{ project.pi_name }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="project.status === 'ACTIVE' ? 'success' : 'info'">{{ project.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatDateTime(project.start_date) }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ formatDateTime(project.end_date) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(project.created_at) }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="3">{{ project.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- Tabs -->
      <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
        <el-tabs v-model="activeTab">
          <!-- Members Tab -->
          <el-tab-pane label="成员" name="members">
            <div class="mb-4">
              <el-button type="primary" @click="inviteModal.open()">
                <el-icon class="mr-1"><Plus /></el-icon> 邀请成员
              </el-button>
            </div>
            <el-table :data="members" v-loading="membersLoading" size="small" row-key="id">
              <el-table-column label="姓名" prop="user_name" />
              <el-table-column label="项目角色" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.role === 'PI' ? 'primary' : row.role === 'RESEARCHER' ? 'success' : 'info'">
                    {{ row.role }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="加入时间" width="170">
                <template #default="{ row }">{{ formatDateTime(row.joined_at) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <el-popconfirm title="确认移除该成员？" @confirm="handleRemoveMember(row.id)">
                    <template #reference>
                      <el-button link type="danger">移除</el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- Datasets Tab -->
          <el-tab-pane label="数据集" name="datasets">
            <el-table :data="datasets" v-loading="datasetsLoading" size="small" row-key="id">
              <el-table-column label="数据集名称" prop="name" />
              <el-table-column label="样本数" prop="sample_count" width="100" />
              <el-table-column label="版本数" prop="version_count" width="80" />
              <el-table-column label="创建人" prop="creator_name" width="100" />
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <el-button link type="primary" @click="router.push(`/data/rdr/datasets/${row.id}`)">详情</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- Cohorts Tab -->
          <el-tab-pane label="队列" name="cohorts">
            <el-table :data="cohorts" v-loading="cohortsLoading" size="small" row-key="id">
              <el-table-column label="队列名称" prop="name" />
              <el-table-column label="纳入标准" prop="criteria_summary" />
              <el-table-column label="患者数" prop="patient_count" width="100" />
              <el-table-column label="创建时间" width="170">
                <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- Activity Tab -->
          <el-tab-pane label="活动记录" name="activity">
            <el-timeline>
              <el-timeline-item
                v-for="(item, index) in activities"
                :key="index"
                :color="activityColorMap[item.type] || '#0ea5e9'"
              >
                <div class="activity-item">
                  <span class="activity-title">{{ item.title }}</span>
                  <span class="activity-desc">{{ item.description }}</span>
                  <span class="activity-time">{{ formatDateTime(item.created_at) }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-if="!activities.length" description="暂无活动记录" :image-size="60" />
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </template>

    <!-- Invite Member Dialog -->
    <el-dialog
      v-model="inviteModal.visible"
      title="邀请成员"
      width="480px"
    >
      <el-form label-position="top">
        <el-form-item label="选择用户" required>
          <UserSelect v-model="inviteForm.user_id" placeholder="搜索用户" />
        </el-form-item>
        <el-form-item label="项目角色" required>
          <el-select v-model="inviteForm.role" placeholder="选择角色">
            <el-option value="RESEARCHER" label="研究员" />
            <el-option value="ANALYST" label="分析员" />
            <el-option value="COLLABORATOR" label="协作人" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inviteModal.close()">取消</el-button>
        <el-button type="primary" :loading="inviting" @click="handleInvite">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
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
  CREATE: '#10b981',
  UPDATE: '#0ea5e9',
  MEMBER_ADD: '#06b6d4',
  MEMBER_REMOVE: '#f97316',
  DATASET_LINK: '#8b5cf6',
  WARNING: '#ef4444',
}

const inviteForm = reactive({ user_id: undefined as any, role: 'RESEARCHER' })

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
    ElMessage.success('成员邀请成功')
    inviteModal.close()
    loadMembers()
  } finally {
    inviting.value = false
  }
}

async function handleRemoveMember(memberId: number) {
  try {
    await request.delete(`/rdr/projects/${route.params.id}/members/${memberId}`)
    ElMessage.success('成员已移除')
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
  color: #0f172a;
}
.activity-desc {
  font-size: 13px;
  color: #64748b;
}
.activity-time {
  font-size: 12px;
  color: #94a3b8;
}
</style>
