<template>
  <div class="patient-card">
    <div class="patient-info">
      <div class="patient-avatar">{{ avatarInitial }}</div>
      <div class="patient-details">
        <h2 class="patient-name">{{ patientInfo.patientName }}</h2>
        <div class="patient-meta">
          <span>性别: {{ formattedGender }}</span>
          <span class="separator">|</span>
          <span>年龄: {{ patientInfo.age }}岁</span>
          <span class="separator">|</span>
          <span>住院号: {{ patientInfo.patientNo }}</span>
          <span class="separator">|</span>
          <span>身份证: {{ desensitizedIdCard }}</span>
        </div>
      </div>
    </div>
    <div v-if="hasAllergy" class="allergy-warning">
      过敏史: {{ patientInfo.allergyHistory }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface PatientInfo {
  patientName: string
  gender: string
  age: number
  patientNo: string
  idCard: string
  phone?: string
  allergyHistory?: string
  familyHistory?: string
}

interface Props {
  patientInfo: PatientInfo
}

const props = defineProps<Props>()

// Avatar initial - first character of patient name
const avatarInitial = computed(() => {
  return props.patientInfo.patientName?.charAt(0) || '?'
})

// Format gender display
const formattedGender = computed(() => {
  const gender = props.patientInfo.gender
  if (gender === '男' || gender === 'M' || gender === 'male' || gender === '1') {
    return '男'
  }
  if (gender === '女' || gender === 'F' || gender === 'female' || gender === '2') {
    return '女'
  }
  return gender || '未知'
})

// Check if allergy history exists
const hasAllergy = computed(() => {
  return props.patientInfo.allergyHistory && props.patientInfo.allergyHistory.trim() !== ''
})

// Desensitize ID card - show first 3 and last 4 digits
const desensitizedIdCard = computed(() => {
  const idCard = props.patientInfo.idCard
  if (!idCard || idCard.length < 7) {
    return idCard || ''
  }
  // Show first 3 digits + asterisks + last 4 digits
  const firstPart = idCard.substring(0, 3)
  const lastPart = idCard.substring(idCard.length - 4)
  const middleStars = '***********'
  return `${firstPart}${middleStars}${lastPart}`
})
</script>

<style scoped>
.patient-card {
  background: white;
  margin: 20px;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.patient-info {
  display: flex;
  gap: 30px;
  align-items: center;
}

.patient-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 32px;
  font-weight: bold;
  flex-shrink: 0;
}

.patient-details {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.patient-name {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  color: rgba(0, 0, 0, 0.88);
}

.patient-meta {
  display: flex;
  gap: 20px;
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
  align-items: center;
}

.separator {
  color: rgba(0, 0, 0, 0.25);
}

.allergy-warning {
  background: #fff3e0;
  border: 1px solid #ff9800;
  padding: 8px 16px;
  border-radius: 4px;
  color: #f57c00;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.allergy-warning::before {
  content: "\26A0";
  font-size: 18px;
}
</style>