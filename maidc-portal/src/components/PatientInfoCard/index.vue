<template>
  <el-card class="patient-info-card !rounded-xl !border-slate-200/80 shadow-clinical-sm" shadow="hover">
    <div class="patient-header">
      <div class="patient-avatar">
        <el-icon :size="24"><UserFilled /></el-icon>
      </div>
      <div class="patient-main">
        <div class="patient-name">{{ desensitize ? maskName(patient.name) : patient.name }}</div>
        <div class="patient-id">
          ID: {{ desensitize ? maskId(patient.id) : patient.id }}
        </div>
      </div>
      <el-switch
        v-model="desensitize"
        active-text="脱敏"
        inactive-text="原文"
        inline-prompt
        size="small"
      />
    </div>
    <el-descriptions :column="2" size="small" border class="patient-details mt-3">
      <el-descriptions-item label="性别">{{ patient.gender }}</el-descriptions-item>
      <el-descriptions-item label="年龄">{{ patient.age }}岁</el-descriptions-item>
      <el-descriptions-item label="诊断" :span="2">
        {{ patient.diagnosis }}
      </el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { UserFilled } from '@element-plus/icons-vue'

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
  border-radius: 12px;
}
.patient-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.patient-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #f0f9ff;
  color: #0ea5e9;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.patient-main {
  flex: 1;
}
.patient-name {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}
.patient-id {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
</style>
