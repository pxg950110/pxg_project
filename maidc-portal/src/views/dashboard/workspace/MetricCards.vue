<template>
  <a-row :gutter="[16, 16]">
    <a-col v-for="card in displayCards" :key="card.key" :span="6">
      <a-card hoverable :bordered="false" :loading="loading" @click="handleClick(card)">
        <div class="metric-card">
          <a-statistic
            :title="card.label"
            :value="card.value"
            :suffix="card.suffix"
            :value-style="{ color: toneColorMap[card.tone ?? ''] ?? 'rgba(0, 0, 0, 0.88)', fontWeight: 600 }"
          />
          <component
            :is="workspaceIconMap[card.icon]"
            class="metric-icon"
            :style="{ color: toneColorMap[card.tone ?? ''] ?? 'rgba(0, 0, 0, 0.45)' }"
          />
        </div>
      </a-card>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { workspaceIconMap, toneColorMap } from './icons'
import type { MetricCard, MetricsInfo } from '@/api/workspace'

const props = defineProps<{
  /** v2 角色化卡片，服务端按角色组下发 */
  cards?: MetricCard[] | null
  /** 旧响应（无 cards）时回退渲染 v1 模型四卡 */
  metrics?: MetricsInfo | null
  loading: boolean
}>()

const router = useRouter()

const legacyCards = (metrics?: MetricsInfo | null): MetricCard[] => [
  { key: 'model_count', label: '模型总数', value: metrics?.modelCount ?? 0, suffix: '个', icon: 'experiment' },
  { key: 'active_deployments', label: '活跃部署', value: metrics?.activeDeployments ?? 0, suffix: '个', icon: 'rocket' },
  { key: 'daily_inferences', label: '今日推理', value: metrics?.dailyInferences ?? 0, suffix: '次', icon: 'thunderbolt' },
  { key: 'pending_approvals', label: '待审批', value: metrics?.pendingApprovals ?? 0, suffix: '项', icon: 'audit' },
]

const displayCards = computed(() =>
  props.cards && props.cards.length > 0 ? props.cards : legacyCards(props.metrics))

function handleClick(card: MetricCard) {
  if (card.route) router.push(card.route)
}
</script>

<style scoped lang="scss">
.metric-card {
  position: relative;
}

.metric-icon {
  position: absolute;
  top: 4px;
  right: 0;
  font-size: 28px;
  opacity: 0.85;
}
</style>
