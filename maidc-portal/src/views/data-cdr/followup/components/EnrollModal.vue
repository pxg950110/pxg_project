<template>
  <a-modal
    :open="open"
    title="患者建档"
    :confirm-loading="submitting"
    ok-text="确认建档"
    @ok="handleOk"
    @cancel="$emit('update:open', false)"
  >
    <a-alert
      type="info"
      show-icon
      style="margin-bottom: 16px"
      message="建档后将按随访方案全量生成阶段任务（基线 / 3月 / 6月 / 12月），任务到期日 = 建档日 + 阶段偏移天数"
    />
    <a-form layout="vertical">
      <a-form-item label="患者" required>
        <a-select
          v-model:value="form.patientId"
          placeholder="仅显示队列内患者"
          show-search
          :options="patientOptions"
          :field-names="{ label: 'patientName', value: 'patientId' }"
          option-filter-prop="patientName"
          :loading="loadingPatients"
        />
      </a-form-item>
      <a-form-item label="随访方案">
        <a-input :value="protocolLabel" disabled />
      </a-form-item>
      <a-form-item label="负责医生" required>
        <a-select v-model:value="form.doctorId" :options="doctorOptions" :loading="loadingUsers" placeholder="选择医生" />
      </a-form-item>
      <a-form-item label="随访护士">
        <a-select v-model:value="form.nurseId" :options="nurseOptions" :loading="loadingUsers" placeholder="可空 = 暂未分配" allow-clear />
      </a-form-item>
      <a-form-item label="建档日期" required>
        <a-date-picker v-model:value="form.enrollDate" style="width: 100%" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
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

const form = reactive<{ patientId: number | null; doctorId: number | null; nurseId?: number | null; enrollDate: Dayjs | null }>({
  patientId: null,
  doctorId: null,
  nurseId: null,
  enrollDate: dayjs(),
})

const submitting = ref(false)

watch(() => props.open, async (open) => {
  if (!open) return
  form.patientId = null
  form.enrollDate = dayjs()
  await Promise.all([loadPatients(), loadUsers(), loadProtocol()])
})

async function loadPatients() {
  loadingPatients.value = true
  try {
    const res = await getDiseaseCohortPatients(Number(props.cohortId), { page: 1, page_size: 500 })
    patientOptions.value = (res.data?.data?.items || []).map((p: any) => ({
      patientId: p.patientId, patientName: `${p.patientName ?? '患者#' + p.patientId}（${p.gender ?? '?'}）`,
    }))
  } catch { message.error('队列患者加载失败') }
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
  } catch { message.error('用户列表加载失败') }
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
  if (!form.patientId) { message.warning('请选择患者'); return }
  if (!form.doctorId) { message.warning('请选择负责医生'); return }
  if (!form.enrollDate) { message.warning('请选择建档日期'); return }
  submitting.value = true
  try {
    const res = await enrollFollowup(Number(props.cohortId), {
      patientId: form.patientId,
      doctorId: form.doctorId,
      nurseId: form.nurseId ?? undefined,
      enrollDate: form.enrollDate.format('YYYY-MM-DD'),
    })
    const d = res.data?.data
    message.success(`建档成功：已按方案 v${d?.protocolVersion} 生成 ${d?.taskCount} 个阶段任务`)
    emit('update:open', false)
    emit('created')
  } catch (e: any) {
    message.error(e.response?.data?.message || '建档失败')
  } finally { submitting.value = false }
}
</script>
