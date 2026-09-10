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
      message="建档后将按随访方案全量生成 4 个阶段任务（基线 / 3月 / 6月 / 12月），任务到期日 = 建档日 + 阶段偏移天数"
    />
    <a-form layout="vertical">
      <a-form-item label="患者" required>
        <a-select
          v-model:value="form.patientId"
          placeholder="仅显示队列内且无活跃档案的患者"
          show-search
          :options="patientOptions"
          :field-names="{ label: 'patientName', value: 'patientId' }"
          option-filter-prop="patientName"
        />
      </a-form-item>
      <a-form-item label="随访方案">
        <a-input value="CRS 慢性鼻窦炎随访方案 v2（PUBLISHED）" disabled />
      </a-form-item>
      <a-form-item label="负责医生" required>
        <a-select v-model:value="form.doctorName" :options="doctorOptions" />
      </a-form-item>
      <a-form-item label="随访护士">
        <a-select v-model:value="form.nurseName" :options="nurseOptions" placeholder="可空 = 暂未分配" allow-clear />
      </a-form-item>
      <a-form-item label="建档日期" required>
        <a-date-picker v-model:value="form.enrollDate" style="width: 100%" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import { cohortPatients } from '../mock'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'created'): void
}>()

const patientOptions = computed(() => cohortPatients.filter(p => !p.followed))
const doctorOptions = [{ value: '陈志远' }, { value: '吴静怡' }]
const nurseOptions = [{ value: '刘敏' }, { value: '孙萍' }]

const form = reactive<{ patientId: number | null; doctorName: string; nurseName?: string; enrollDate: Dayjs | null }>({
  patientId: null,
  doctorName: '陈志远',
  nurseName: '刘敏',
  enrollDate: dayjs('2026-09-09'),
})

const submitting = ref(false)

function handleOk() {
  if (!form.patientId) { message.warning('请选择患者'); return }
  if (!form.doctorName) { message.warning('请选择负责医生'); return }
  if (!form.enrollDate) { message.warning('请选择建档日期'); return }
  submitting.value = true
  setTimeout(() => {
    submitting.value = false
    message.success('建档成功：已按方案生成 4 个阶段任务')
    emit('update:open', false)
    emit('created')
    form.patientId = null
  }, 600)
}
</script>
