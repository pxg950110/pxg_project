<template>
  <el-dialog
    :model-value="open"
    title="患者建档"
    width="520px"
    @update:model-value="(v: boolean) => emit('update:open', v)"
  >
    <el-alert
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
      title="建档后将按随访方案全量生成阶段任务（基线 / 3月 / 6月 / 12月），任务到期日 = 建档日 + 阶段偏移天数"
    />
    <el-form label-position="top">
      <el-form-item label="患者" required>
        <el-select
          v-model="form.patientId"
          placeholder="仅显示队列内患者"
          filterable
          :loading="loadingPatients"
          style="width: 100%"
        >
          <el-option v-for="p in patientOptions" :key="p.patientId" :value="p.patientId" :label="p.patientName" />
        </el-select>
      </el-form-item>
      <el-form-item label="随访方案">
        <el-input :model-value="protocolLabel" disabled />
      </el-form-item>
      <el-form-item label="负责医生" required>
        <el-select v-model="form.doctorId" :loading="loadingUsers" placeholder="选择医生" style="width: 100%">
          <el-option v-for="o in doctorOptions" :key="o.value" :value="o.value" :label="o.label" />
        </el-select>
      </el-form-item>
      <el-form-item label="随访护士">
        <el-select v-model="form.nurseId" :loading="loadingUsers" placeholder="可空 = 暂未分配" clearable style="width: 100%">
          <el-option v-for="o in nurseOptions" :key="o.value" :value="o.value" :label="o.label" />
        </el-select>
      </el-form-item>
      <el-form-item label="建档日期" required>
        <el-date-picker v-model="form.enrollDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:open', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleOk">确认建档</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getUsers } from '@/api/system'
import { getDiseaseCohortPatients } from '@/api/data'
import { enrollFollowup, getLatestProtocol } from '@/api/followup'

const props = defineProps<{ open: boolean; cohortId: number | string }>()
const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'created'): void
}>()

const patientOptions = ref<{ patientId: number; patientName: string }[]>([])
const doctorOptions = ref<{ value: number; label: string }[]>([])
const nurseOptions = ref<{ value: number; label: string }[]>([])
const loadingPatients = ref(false)
const loadingUsers = ref(false)
const protocolLabel = ref('—')

const form = reactive<{ patientId: number | null; doctorId: number | null; nurseId?: number | null; enrollDate: string | null }>({
  patientId: null,
  doctorId: null,
  nurseId: null,
  enrollDate: dayjs().format('YYYY-MM-DD'),
})

const submitting = ref(false)

watch(() => props.open, async (open) => {
  if (!open) return
  form.patientId = null
  form.enrollDate = dayjs().format('YYYY-MM-DD')
  await Promise.all([loadPatients(), loadUsers(), loadProtocol()])
})

async function loadPatients() {
  loadingPatients.value = true
  try {
    const res = await getDiseaseCohortPatients(Number(props.cohortId), { page: 1, page_size: 500 })
    patientOptions.value = (res.data?.data?.items || []).map((p: any) => ({
      patientId: p.patientId, patientName: `${p.patientName ?? '患者#' + p.patientId}（${p.gender ?? '?'}）`,
    }))
  } catch { ElMessage.error('队列患者加载失败') }
  finally { loadingPatients.value = false }
}

async function loadUsers() {
  loadingUsers.value = true
  try {
    const res = await getUsers({ page: 1, page_size: 200, status: 'ACTIVE' })
    const users = (res.data?.data?.items || res.data?.data?.content || []).map((u: any) => ({
      value: u.id, label: u.realName || u.username, roles: u.roles || [],
    }))
    doctorOptions.value = users.filter((u: any) => u.roles.includes('doctor') || u.roles.includes('admin'))
    nurseOptions.value = users.filter((u: any) => u.roles.includes('nurse'))
  } catch { ElMessage.error('用户列表加载失败') }
  finally { loadingUsers.value = false }
}

async function loadProtocol() {
  try {
    const res = await getLatestProtocol(Number(props.cohortId))
    const p = res.data?.data
    protocolLabel.value = p ? `${p.name} v${p.version}（PUBLISHED）` : '暂无已发布方案'
  } catch { protocolLabel.value = '暂无已发布方案' }
}

async function handleOk() {
  if (!form.patientId) { ElMessage.warning('请选择患者'); return }
  if (!form.doctorId) { ElMessage.warning('请选择负责医生'); return }
  if (!form.enrollDate) { ElMessage.warning('请选择建档日期'); return }
  submitting.value = true
  try {
    const res = await enrollFollowup(Number(props.cohortId), {
      patientId: form.patientId,
      doctorId: form.doctorId,
      nurseId: form.nurseId ?? undefined,
      enrollDate: form.enrollDate,
    })
    const d = res.data?.data
    ElMessage.success(`建档成功：已按方案 v${d?.protocolVersion} 生成 ${d?.taskCount} 个阶段任务`)
    emit('update:open', false)
    emit('created')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '建档失败')
  } finally { submitting.value = false }
}
</script>
