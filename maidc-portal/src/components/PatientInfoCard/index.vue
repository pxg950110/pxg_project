<template>
  <a-card class="patient-info-card" :bordered="false" size="small">
    <div class="patient-header">
      <UserOutlined class="patient-avatar" />
      <div class="patient-main">
        <div class="patient-name">{{ desensitize ? maskName(patient.name) : patient.name }}</div>
        <div class="patient-id">
          ID: {{ desensitize ? maskId(patient.id) : patient.id }}
        </div>
      </div>
      <a-switch
        v-model:checked="desensitize"
        checked-children="脱敏"
        un-checked-children="原文"
        size="small"
      />
    </div>
    <a-descriptions :column="2" size="small" class="patient-details">
      <a-descriptions-item label="性别">{{ patient.gender }}</a-descriptions-item>
      <a-descriptions-item label="年龄">{{ patient.age }}岁</a-descriptions-item>
      <a-descriptions-item label="诊断" :span="2">
        {{ patient.diagnosis }}
      </a-descriptions-item>
    </a-descriptions>
  </a-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { UserOutlined } from '@ant-design/icons-vue'

interface PatientInfo {
  id: string
  name: string
  gender: string
  age: number
  diagnosis: string
}

interface Props {
  patient: PatientInfo
}

defineProps<Props>()

const desensitize = ref(true)

function maskName(name: string): string {
  if (!name) return ''
  if (name.length <= 1) return name
  if (name.length === 2) return name[0] + '*'
  return name[0] + '*'.repeat(name.length - 2) + name[name.length - 1]
}

function maskId(id: string): string {
  if (!id || id.length <= 4) return '****'
  return id.slice(0, 2) + '****' + id.slice(-2)
}
</script>

<style scoped>
.patient-info-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}
.patient-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.patient-avatar {
  font-size: 36px;
  color: #1677ff;
  background: rgba(22, 119, 255, 0.08);
  width: 48px;
  height: 48px;
  line-height: 48px;
  text-align: center;
  border-radius: 50%;
}
.patient-main {
  flex: 1;
}
.patient-name {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
}
.patient-id {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 2px;
}
</style>
