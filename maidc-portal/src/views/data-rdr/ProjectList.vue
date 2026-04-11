<template>
  <PageContainer title="研究项目">
    <template #extra>
      <a-button type="primary" @click="projectModal.open()">
        <PlusOutlined /> 新建项目
      </a-button>
    </template>

    <!-- Search & Filter Bar -->
    <div class="filter-bar">
      <a-input
        v-model:value="searchKeyword"
        placeholder="搜索项目..."
        allow-clear
        style="width: 280px"
        @change="handleFilter"
      >
        <template #prefix>
          <SearchOutlined style="color: rgba(0,0,0,0.25)" />
        </template>
      </a-input>
      <a-select
        v-model:value="filterStatus"
        placeholder="项目状态"
        allow-clear
        style="width: 160px"
        @change="handleFilter"
      >
        <a-select-option value="ACTIVE">进行中</a-select-option>
        <a-select-option value="PLANNED">计划中</a-select-option>
        <a-select-option value="COMPLETED">已完成</a-select-option>
        <a-select-option value="SUSPENDED">已暂停</a-select-option>
      </a-select>
      <a-select
        v-model:value="filterCategory"
        placeholder="研究领域"
        allow-clear
        style="width: 160px"
        @change="handleFilter"
      >
        <a-select-option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</a-select-option>
      </a-select>
    </div>

    <!-- Card Grid -->
    <a-row :gutter="[16, 16]">
      <a-col v-for="project in pagedProjects" :key="project.id" :span="8">
        <a-card hoverable class="project-card" @click="router.push(`/data/rdr/projects/${project.id}`)">
          <!-- Card Header -->
          <div class="card-header">
            <span class="project-name">{{ project.name }}</span>
            <a-tag :color="statusColor(project.status)">{{ statusLabel(project.status) }}</a-tag>
          </div>

          <!-- PI -->
          <div class="card-info-row">
            <UserOutlined class="info-icon" />
            <span class="info-label">负责人:</span>
            <span>{{ project.pi }}</span>
          </div>

          <!-- Research Field -->
          <div class="card-info-row">
            <span class="info-label">研究领域:</span>
            <a-tag color="blue" size="small">{{ project.field }}</a-tag>
          </div>

          <!-- Timeline -->
          <div class="card-info-row">
            <CalendarOutlined class="info-icon" />
            <span class="timeline-text">{{ project.startDate }} ~ {{ project.endDate }}</span>
          </div>

          <!-- Team Size -->
          <div class="card-info-row">
            <TeamOutlined class="info-icon" />
            <span>{{ project.teamSize }} 人</span>
          </div>

          <!-- Progress -->
          <div class="card-progress">
            <div class="progress-label">
              <span>招募进度</span>
              <span class="progress-percent">{{ project.progress }}%</span>
            </div>
            <a-progress
              :percent="project.progress"
              :stroke-color="progressColor(project.progress)"
              :show-info="false"
              size="small"
            />
          </div>

          <!-- Action Link -->
          <div class="card-action">
            <a @click.stop="router.push(`/data/rdr/projects/${project.id}`)">
              <EyeOutlined /> 查看详情 ->
            </a>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- Empty State -->
    <a-empty v-if="filteredProjects.length === 0" description="暂无匹配项目" style="margin-top: 48px" />

    <!-- Pagination -->
    <div class="pagination-wrapper">
      <a-pagination
        v-model:current="currentPage"
        :total="filteredProjects.length"
        :page-size="pageSize"
        show-quick-jumper
        @change="onPageChange"
      />
    </div>

    <!-- Create Project Modal -->
    <a-modal v-model:open="projectModal.visible" title="新建研究项目" @ok="handleCreate" :confirm-loading="submitting" width="600px">
      <a-form layout="vertical">
        <a-form-item label="项目名称" required><a-input v-model:value="projectForm.name" /></a-form-item>
        <a-form-item label="研究类型"><a-select v-model:value="projectForm.research_type">
          <a-select-option value="CLINICAL">临床研究</a-select-option>
          <a-select-option value="EPIDEMIOLOGICAL">流行病学研究</a-select-option>
          <a-select-option value="BASIC">基础研究</a-select-option>
        </a-select></a-form-item>
        <a-form-item label="描述"><a-textarea v-model:value="projectForm.description" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>

    <!-- Invite Modal -->
    <a-modal v-model:open="inviteVisible" title="邀请成员" @ok="handleInvite" :confirm-loading="inviting">
      <UserSelect v-model:value="inviteUserId" placeholder="选择用户" />
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  PlusOutlined,
  SearchOutlined,
  TeamOutlined,
  CalendarOutlined,
  UserOutlined,
  EyeOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import UserSelect from '@/components/UserSelect/index.vue'
import { useModal } from '@/hooks/useModal'
import request from '@/utils/request'

const router = useRouter()
const projectModal = useModal()
const submitting = ref(false)
const inviteVisible = ref(false)
const inviting = ref(false)
const inviteUserId = ref<string>()
let invitingProjectId = 0

// ---- Mock Data ----
const mockProjects = ref([
  {
    id: 1,
    name: '肺癌早筛多中心研究',
    status: 'ACTIVE',
    pi: '张医生',
    field: '肿瘤学',
    startDate: '2025-03-01',
    endDate: '2026-12-31',
    teamSize: 12,
    progress: 68,
  },
  {
    id: 2,
    name: '糖尿病并发症预测',
    status: 'ACTIVE',
    pi: '李医生',
    field: '内分泌',
    startDate: '2025-06-15',
    endDate: '2027-06-15',
    teamSize: 8,
    progress: 45,
  },
  {
    id: 3,
    name: '心血管风险评估队列',
    status: 'PLANNED',
    pi: '王医生',
    field: '心血管',
    startDate: '2026-01-01',
    endDate: '2028-12-31',
    teamSize: 15,
    progress: 0,
  },
  {
    id: 4,
    name: '影像AI辅助诊断验证',
    status: 'COMPLETED',
    pi: '赵医生',
    field: '影像医学',
    startDate: '2024-01-01',
    endDate: '2025-12-31',
    teamSize: 20,
    progress: 100,
  },
  {
    id: 5,
    name: '基因组学罕见病研究',
    status: 'ACTIVE',
    pi: '刘医生',
    field: '遗传学',
    startDate: '2025-09-01',
    endDate: '2027-09-01',
    teamSize: 6,
    progress: 32,
  },
  {
    id: 6,
    name: 'NLP病历质控研究',
    status: 'SUSPENDED',
    pi: '陈医生',
    field: 'NLP',
    startDate: '2025-04-01',
    endDate: '2026-10-01',
    teamSize: 10,
    progress: 60,
  },
])

// ---- Filters ----
const searchKeyword = ref('')
const filterStatus = ref<string | undefined>(undefined)
const filterCategory = ref<string | undefined>(undefined)

const categories = computed(() => {
  const set = new Set(mockProjects.value.map((p) => p.field))
  return Array.from(set)
})

const filteredProjects = computed(() => {
  return mockProjects.value.filter((p) => {
    const matchKeyword = !searchKeyword.value || p.name.includes(searchKeyword.value)
    const matchStatus = !filterStatus.value || p.status === filterStatus.value
    const matchCategory = !filterCategory.value || p.field === filterCategory.value
    return matchKeyword && matchStatus && matchCategory
  })
})

function handleFilter() {
  currentPage.value = 1
}

// ---- Pagination ----
const currentPage = ref(1)
const pageSize = 6

const pagedProjects = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredProjects.value.slice(start, start + pageSize)
})

function onPageChange(page: number) {
  currentPage.value = page
}

// ---- Status helpers ----
function statusColor(status: string): string {
  const map: Record<string, string> = {
    ACTIVE: 'green',
    PLANNED: 'blue',
    COMPLETED: 'default',
    SUSPENDED: 'red',
  }
  return map[status] || 'default'
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    ACTIVE: '进行中',
    PLANNED: '计划中',
    COMPLETED: '已完成',
    SUSPENDED: '已暂停',
  }
  return map[status] || status
}

function progressColor(percent: number): string {
  if (percent >= 80) return '#52c41a'
  if (percent >= 40) return '#1890ff'
  if (percent > 0) return '#faad14'
  return '#d9d9d9'
}

// ---- Create Project ----
const projectForm = reactive({ name: '', research_type: 'CLINICAL', description: '' })

async function handleCreate() {
  submitting.value = true
  try {
    message.success('项目创建成功')
    projectModal.close()
  } finally {
    submitting.value = false
  }
}

// ---- Invite ----
function openInvite(record: any) {
  invitingProjectId = record.id
  inviteVisible.value = true
}

async function handleInvite() {
  inviting.value = true
  try {
    await request.post(`/rdr/projects/${invitingProjectId}/members`, { user_id: inviteUserId.value })
    message.success('邀请成功')
    inviteVisible.value = false
  } finally {
    inviting.value = false
  }
}
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  align-items: center;
}

.project-card {
  border-radius: 8px;
  transition: box-shadow 0.3s, transform 0.2s;
}

.project-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
}

.project-name {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  line-height: 1.4;
  flex: 1;
  margin-right: 8px;
}

.card-info-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}

.info-icon {
  color: #1890ff;
  font-size: 14px;
}

.info-label {
  color: rgba(0, 0, 0, 0.45);
  white-space: nowrap;
}

.timeline-text {
  font-size: 13px;
}

.card-progress {
  margin-top: 12px;
  margin-bottom: 8px;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.progress-percent {
  color: rgba(0, 0, 0, 0.88);
  font-weight: 500;
}

.card-action {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  text-align: right;
}

.card-action a {
  color: #1890ff;
  font-size: 13px;
}

.card-action a:hover {
  color: #40a9ff;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
</style>
