<template>
  <a-card hoverable class="disease-card" @click="emit('detail')">
    <div class="card-header">
      <span class="card-name">{{ data.name }}</span>
      <a-tag :color="data.status === 'ACTIVE' ? 'blue' : 'default'">
        {{ data.status === 'ACTIVE' ? '已启用' : '未启用' }}
      </a-tag>
    </div>
    <div class="card-patient-count">
      <span class="count-number">{{ data.patientCount || 0 }}</span>
      <span class="count-label">名患者</span>
    </div>
    <div class="card-rules">
      <template v-for="(group, gi) in parsedRules" :key="gi">
        <div class="rule-line">
          <a-tag size="small" :color="domainColor(group.domain)">{{ domainLabel(group.domain) }}</a-tag>
          <span class="rule-text">{{ groupSummary(group) }}</span>
        </div>
      </template>
    </div>
    <div class="card-actions" @click.stop>
      <a-button type="link" size="small" @click="emit('edit')">编辑</a-button>
      <a-button type="link" size="small" @click="emit('detail')">详情</a-button>
      <a-button type="link" size="small" @click="emit('sync')">同步</a-button>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ data: any }>()
const emit = defineEmits<{ edit: []; detail: []; sync: [] }>()

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
    const rules = typeof props.data.inclusionRules === 'string'
      ? JSON.parse(props.data.inclusionRules)
      : props.data.inclusionRules
    return rules?.groups || []
  } catch {
    return []
  }
})

function groupSummary(group: any) {
  return (group.conditions || [])
    .map((c: any) => `${c.field} ${c.operator} ${Array.isArray(c.value) ? c.value.join(',') : c.value}`)
    .join(` ${group.logic} `)
}
</script>

<style scoped>
.disease-card { cursor: pointer; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.card-name { font-weight: 600; font-size: 16px; }
.card-patient-count { margin-bottom: 12px; }
.count-number { font-size: 28px; font-weight: 700; color: #1890ff; }
.count-label { margin-left: 4px; color: #999; }
.card-rules { margin-bottom: 8px; }
.rule-line { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.rule-text { font-size: 12px; color: #666; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-actions { display: flex; justify-content: flex-end; border-top: 1px solid #f0f0f0; padding-top: 8px; }
</style>
