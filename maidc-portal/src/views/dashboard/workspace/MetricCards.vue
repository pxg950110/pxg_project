<template>
  <a-row :gutter="[16, 16]">
    <a-col v-for="item in cards" :key="item.title" :span="6">
      <MetricCard
        :title="item.title"
        :value="item.value"
        :suffix="item.suffix"
        :icon="item.icon"
        :loading="loading"
      />
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ExperimentOutlined, RocketOutlined, ThunderboltOutlined, AuditOutlined } from '@ant-design/icons-vue'
import MetricCard from '@/components/MetricCard/index.vue'
import type { MetricsInfo } from '@/api/workspace'

const props = defineProps<{
  metrics: MetricsInfo | null
  loading: boolean
}>()

const cards = computed(() => [
  { title: '模型总数', value: props.metrics?.modelCount ?? 0, suffix: '个', icon: ExperimentOutlined },
  { title: '活跃部署', value: props.metrics?.activeDeployments ?? 0, suffix: '个', icon: RocketOutlined },
  { title: '今日推理', value: props.metrics?.dailyInferences ?? 0, suffix: '次', icon: ThunderboltOutlined },
  { title: '待审批', value: props.metrics?.pendingApprovals ?? 0, suffix: '项', icon: AuditOutlined },
])
</script>
