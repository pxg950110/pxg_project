<template>
  <PageContainer :title="cohort.name || '专病详情'" :breadcrumb="breadcrumb">
    <template #extra>
      <a-button @click="handleSync" :loading="syncing">手动同步</a-button>
      <a-button @click="router.back()">返回</a-button>
    </template>

    <a-spin :spinning="loading">
      <!-- 基本信息 -->
      <a-card title="基本信息" style="margin-bottom: 16px">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="专病名称">{{ cohort.name }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="cohort.status === 'ACTIVE' ? 'blue' : 'default'">
              {{ cohort.status === 'ACTIVE' ? '已启用' : '未启用' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="自动同步">
            <a-switch :checked="cohort.autoSync" @change="toggleAutoSync" />
          </a-descriptions-item>
          <a-descriptions-item label="最后同步">{{ cohort.lastSyncAt || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ cohort.createdAt }}</a-descriptions-item>
          <a-descriptions-item label="描述" :span="2">{{ cohort.description || '-' }}</a-descriptions-item>
        </a-descriptions>
        <div class="rules-summary" v-if="parsedRules.length">
          <div class="rules-title">纳入规则</div>
          <div v-for="(group, gi) in parsedRules" :key="gi" class="rule-line">
            <a-tag size="small" :color="domainColor(group.domain)">{{ domainLabel(group.domain) }}</a-tag>
            <span>{{ groupSummary(group) }}</span>
          </div>
        </div>
      </a-card>

      <!-- 统计指标 -->
      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :span="6">
          <a-card><a-statistic title="患者总数" :value="stats.totalPatients || 0" /></a-card>
        </a-col>
        <a-col :span="6">
          <a-card><a-statistic title="男性占比" :value="stats.maleRatio || 0" suffix="%" :precision="1" /></a-card>
        </a-col>
        <a-col :span="6">
          <a-card><a-statistic title="平均年龄" :value="stats.avgAge || 0" suffix="岁" :precision="1" /></a-card>
        </a-col>
        <a-col :span="6">
          <a-card><a-statistic title="近30天新增" :value="stats.recentCount || 0" /></a-card>
        </a-col>
      </a-row>

      <!-- 患者列表 -->
      <a-card title="患者列表">
        <template #extra>
          <a-button size="small" @click="addModalVisible = true">手动添加</a-button>
          <a-button size="small" @click="handleExport">导出</a-button>
        </template>
        <a-table
          :columns="patientColumns"
          :data-source="patients"
          :loading="patientsLoading"
          :pagination="{ current: patientPage, pageSize: 20, total: patientTotal }"
          row-key="id"
          @change="onPatientTableChange"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'matchSource'">
              <a-tag :color="record.matchSource === 'AUTO' ? 'blue' : 'orange'">
                {{ record.matchSource === 'AUTO' ? '自动' : '手动' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-popconfirm title="确认移除？" @confirm="handleRemovePatient(record.patientId)" v-if="record.matchSource === 'MANUAL'">
                <a-button type="link" danger size="small">移除</a-button>
              </a-popconfirm>
              <span v-else class="text-gray">-</span>
            </template>
          </template>
        </a-table>
      </a-card>
    </a-spin>

    <!-- 手动添加弹窗 -->
    <a-modal v-model:open="addModalVisible" title="手动添加患者" @ok="handleAddPatient" :confirm-loading="adding">
      <a-form layout="vertical">
        <a-form-item label="患者ID">
          <a-input-number v-model:value="addPatientId" placeholder="输入患者ID" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import {
  getDiseaseCohort, syncDiseaseCohort, getDiseaseCohortPatients,
  removeDiseaseCohortPatient, addDiseaseCohortPatient, getDiseaseCohortStatistics,
  exportDiseaseCohort, updateDiseaseCohort,
} from '@/api/data'

const route = useRoute()
const router = useRouter()
const cohortId = Number(route.params.id)

const loading = ref(false)
const syncing = ref(false)
const cohort = ref<any>({})
const stats = ref<any>({})

const patients = ref<any[]>([])
const patientsLoading = ref(false)
const patientPage = ref(1)
const patientTotal = ref(0)

const addModalVisible = ref(false)
const addPatientId = ref<number | null>(null)
const adding = ref(false)

const breadcrumb = [
  { title: '数据管理' },
  { title: '专病管理', path: '/data/cdr/disease' },
  { title: cohort.value.name || '详情' },
]

const domainLabels: Record<string, string> = {
  DIAGNOSIS: '诊断', LAB: '检验', MEDICATION: '用药',
  IMAGING: '影像', SURGERY: '手术', PATHOLOGY: '病理',
}
const domainColors: Record<string, string> = {
  DIAGNOSIS: 'blue', LAB: 'green', MEDICATION: 'orange',
  IMAGING: 'purple', SURGERY: 'red', PATHOLOGY: 'cyan',
}
const domainLabel = (d: string) => domainLabels[d] || d
const domainColor = (d: string) => domainColors[d] || 'default'

const parsedRules = computed(() => {
  try {
    const rules = typeof cohort.value.inclusionRules === 'string'
      ? JSON.parse(cohort.value.inclusionRules)
      : cohort.value.inclusionRules
    return rules?.groups || []
  } catch { return [] }
})

function groupSummary(group: any) {
  return (group.conditions || [])
    .map((c: any) => `${c.field} ${c.operator} ${Array.isArray(c.value) ? c.value.join(',') : c.value}`)
    .join(` ${group.logic} `)
}

const patientColumns = [
  { title: '患者姓名', dataIndex: 'patientName', key: 'patientName' },
  { title: '性别', dataIndex: 'gender', key: 'gender', width: 60 },
  { title: '年龄', dataIndex: 'age', key: 'age', width: 60 },
  { title: '匹配来源', dataIndex: 'matchSource', key: 'matchSource', width: 100 },
  { title: '匹配时间', dataIndex: 'matchedAt', key: 'matchedAt', width: 180 },
  { title: '操作', key: 'action', width: 80 },
]

async function loadCohort() {
  loading.value = true
  try {
    const res = await getDiseaseCohort(cohortId)
    cohort.value = res.data?.data || {}
  } catch (e: any) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await getDiseaseCohortStatistics(cohortId)
    stats.value = res.data?.data || {}
  } catch { /* ignore */ }
}

async function loadPatients() {
  patientsLoading.value = true
  try {
    const res = await getDiseaseCohortPatients(cohortId, { page: patientPage.value, page_size: 20 })
    const data = res.data?.data || {}
    patients.value = data.items || []
    patientTotal.value = data.total || 0
  } catch { /* ignore */ }
  finally { patientsLoading.value = false }
}

function onPatientTableChange(pagination: any) {
  patientPage.value = pagination.current
  loadPatients()
}

async function handleSync() {
  syncing.value = true
  try {
    await syncDiseaseCohort(cohortId)
    message.success('同步完成')
    loadCohort()
    loadStats()
    loadPatients()
  } catch (e: any) {
    message.error('同步失败')
  } finally { syncing.value = false }
}

async function toggleAutoSync(val: boolean) {
  try {
    await updateDiseaseCohort(cohortId, { autoSync: val })
    cohort.value.autoSync = val
  } catch { message.error('更新失败') }
}

async function handleRemovePatient(patientId: number) {
  try {
    await removeDiseaseCohortPatient(cohortId, patientId)
    message.success('已移除')
    loadPatients()
    loadStats()
  } catch (e: any) {
    message.error(e.response?.data?.message || '移除失败')
  }
}

async function handleAddPatient() {
  if (!addPatientId.value) { message.warning('请输入患者ID'); return }
  adding.value = true
  try {
    await addDiseaseCohortPatient(cohortId, addPatientId.value)
    message.success('添加成功')
    addModalVisible.value = false
    addPatientId.value = null
    loadPatients()
    loadStats()
  } catch (e: any) {
    message.error(e.response?.data?.message || '添加失败')
  } finally { adding.value = false }
}

async function handleExport() {
  try {
    const res = await exportDiseaseCohort(cohortId)
    const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${cohort.value.name || 'export'}_patients.csv`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch { message.error('导出失败') }
}

onMounted(() => {
  loadCohort()
  loadStats()
  loadPatients()
})
</script>

<style scoped>
.rules-summary { margin-top: 12px; padding-top: 12px; border-top: 1px solid #f0f0f0; }
.rules-title { font-weight: 600; margin-bottom: 8px; }
.rule-line { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.text-gray { color: #ccc; }
</style>
