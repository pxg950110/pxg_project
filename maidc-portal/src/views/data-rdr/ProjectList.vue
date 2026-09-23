<template>
  <PageContainer title="研究项目">
    <template #extra>
      <el-button type="primary" @click="projectModal.open()">
        <el-icon class="mr-1"><Plus /></el-icon> 创建项目
      </el-button>
    </template>

    <!-- Search & Filter Bar -->
    <div class="filter-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索项目..."
        clearable
        style="width: 280px"
        @input="handleFilter"
      >
        <template #prefix>
          <el-icon style="color: #94a3b8"><Search /></el-icon>
        </template>
      </el-input>
      <el-select
        v-model="filterStatus"
        placeholder="项目状态"
        clearable
        style="width: 160px"
        @change="handleFilter"
      >
        <el-option value="ACTIVE" label="进行中" />
        <el-option value="PLANNED" label="计划中" />
        <el-option value="COMPLETED" label="已完成" />
        <el-option value="SUSPENDED" label="已暂停" />
      </el-select>
      <el-select
        v-model="filterCategory"
        placeholder="研究领域"
        clearable
        style="width: 160px"
        @change="handleFilter"
      >
        <el-option v-for="cat in categories" :key="cat" :value="cat" :label="cat" />
      </el-select>
    </div>

    <!-- Card Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <el-card
        v-for="project in pagedProjects"
        :key="project.id"
        shadow="hover"
        class="project-card cursor-pointer !rounded-xl !border-slate-200/80"
        @click="router.push(`/data/rdr/projects/${project.id}`)"
      >
        <!-- Card Header -->
        <div class="card-header">
          <span class="project-name">{{ project.name }}</span>
          <el-tag :type="statusColor(project.status)">{{ statusLabel(project.status) }}</el-tag>
        </div>

        <!-- PI -->
        <div class="card-info-row">
          <el-icon class="info-icon"><User /></el-icon>
          <span class="info-label">负责人:</span>
          <span>{{ project.pi }}</span>
        </div>

        <!-- Research Field -->
        <div class="card-info-row">
          <span class="info-label">研究领域:</span>
          <el-tag type="primary" size="small">{{ project.field }}</el-tag>
        </div>

        <!-- Timeline -->
        <div class="card-info-row">
          <el-icon class="info-icon"><Calendar /></el-icon>
          <span class="timeline-text">{{ project.startDate }} ~ {{ project.endDate }}</span>
        </div>

        <!-- Team Size -->
        <div class="card-info-row">
          <el-icon class="info-icon"><User /></el-icon>
          <span>{{ project.teamSize }} 人</span>
        </div>

        <!-- Progress -->
        <div class="card-progress">
          <div class="progress-label">
            <span>招募进度</span>
            <span class="progress-percent">{{ project.progress }}%</span>
          </div>
          <el-progress
            :percentage="project.progress"
            :color="progressColor(project.progress)"
            :stroke-width="6"
            :show-text="false"
          />
        </div>

        <!-- Action Link -->
        <div class="card-action">
          <el-button link type="primary" @click.stop="router.push(`/data/rdr/projects/${project.id}`)">
            <el-icon class="mr-1"><View /></el-icon> 查看详情 -&gt;
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- Empty State -->
    <el-empty v-if="filteredProjects.length === 0" description="暂无匹配项目" :image-size="60" style="margin-top: 48px" />

    <!-- Pagination -->
    <div class="pagination-wrapper">
      <el-pagination
        background
        layout="prev, pager, next, jumper"
        :total="filteredProjects.length"
        :current-page="currentPage"
        :page-size="pageSize"
        @current-change="onPageChange"
      />
    </div>

    <!-- Create Project Dialog -->
    <el-dialog v-model="projectModal.visible" title="新建研究项目" width="600px">
      <el-form label-position="top">
        <el-form-item label="项目名称" required><el-input v-model="projectForm.name" /></el-form-item>
        <el-form-item label="研究类型"><el-select v-model="projectForm.research_type">
          <el-option value="CLINICAL" label="临床研究" />
          <el-option value="EPIDEMIOLOGICAL" label="流行病学研究" />
          <el-option value="BASIC" label="基础研究" />
        </el-select></el-form-item>
        <el-form-item label="描述"><el-input v-model="projectForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="projectModal.close()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- Invite Dialog -->
    <el-dialog v-model="inviteVisible" title="邀请成员" width="520px">
      <UserSelect v-model="inviteUserId" placeholder="选择用户" />
      <template #footer>
        <el-button @click="inviteVisible = false">取消</el-button>
        <el-button type="primary" :loading="inviting" @click="handleInvite">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Plus,
  Search,
  User,
  Calendar,
  View,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import UserSelect from '@/components/UserSelect/index.vue'
import { useModal } from '@/hooks/useModal'
import { useTable } from '@/hooks/useTable'
import { getProjects, createProject } from '@/api/data'
import request from '@/utils/request'

const router = useRouter()
const projectModal = useModal()
const submitting = ref(false)
const inviteVisible = ref(false)
const inviting = ref(false)
const inviteUserId = ref<string>()
let invitingProjectId = 0

// ---- Table Data from API ----
const { tableData: projects, loading, fetchData } = useTable<any>(
  (params) => getProjects({ page: params.page, page_size: params.pageSize }),
)

// ---- Filters ----
const searchKeyword = ref('')
const filterStatus = ref<string | undefined>(undefined)
const filterCategory = ref<string | undefined>(undefined)

const categories = computed(() => {
  const set = new Set(projects.value.map((p: any) => p.field))
  return Array.from(set)
})

const filteredProjects = computed(() => {
  return projects.value.filter((p: any) => {
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
    ACTIVE: 'success',
    PLANNED: 'primary',
    COMPLETED: 'info',
    SUSPENDED: 'danger',
  }
  return map[status] || 'info'
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
  if (percent >= 80) return '#10b981'
  if (percent >= 40) return '#0ea5e9'
  if (percent > 0) return '#f59e0b'
  return '#e2e8f0'
}

// ---- Create Project ----
const projectForm = reactive({ name: '', research_type: 'CLINICAL', description: '' })

async function handleCreate() {
  submitting.value = true
  try {
    await createProject(projectForm)
    ElMessage.success('项目创建成功')
    projectModal.close()
    fetchData()
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
    ElMessage.success('邀请成功')
    inviteVisible.value = false
  } finally {
    inviting.value = false
  }
}

onMounted(() => fetchData())
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
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.12);
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
  color: #0f172a;
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
  color: #64748b;
}

.info-icon {
  color: #0ea5e9;
  font-size: 14px;
}

.info-label {
  color: #94a3b8;
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
  color: #94a3b8;
}

.progress-percent {
  color: #0f172a;
  font-weight: 500;
}

.card-action {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
  text-align: right;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}
</style>
