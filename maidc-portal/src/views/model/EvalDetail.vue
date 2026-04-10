<template>
  <PageContainer title="评估详情" :loading="loading">
    <template #extra>
      <a-button @click="router.back()">返回</a-button>
    </template>

    <template v-if="evaluation">
      <a-card title="基本信息" style="margin-bottom: 16px">
        <a-descriptions :column="3" bordered size="small">
          <a-descriptions-item label="模型">{{ evaluation.model_name }}</a-descriptions-item>
          <a-descriptions-item label="版本">{{ evaluation.version_no }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <StatusBadge :status="evaluation.status" type="eval" />
          </a-descriptions-item>
          <a-descriptions-item label="数据集">{{ evaluation.dataset_name || '-' }}</a-descriptions-item>
          <a-descriptions-item label="样本数">{{ evaluation.sample_count || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatDateTime(evaluation.created_at) }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <template v-if="evaluation.status === 'COMPLETED'">
        <a-row :gutter="16" style="margin-bottom: 16px">
          <a-col :span="6"><MetricCard title="Accuracy" :value="(evaluation.metrics?.accuracy * 100).toFixed(2)" suffix="%" /></a-col>
          <a-col :span="6"><MetricCard title="Precision" :value="(evaluation.metrics?.precision * 100).toFixed(2)" suffix="%" /></a-col>
          <a-col :span="6"><MetricCard title="Recall" :value="(evaluation.metrics?.recall * 100).toFixed(2)" suffix="%" /></a-col>
          <a-col :span="6"><MetricCard title="F1 Score" :value="(evaluation.metrics?.f1_score * 100).toFixed(2)" suffix="%" /></a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-card title="混淆矩阵" size="small">
              <ConfusionMatrix v-if="evaluation.metrics?.confusion_matrix" :matrix="evaluation.metrics.confusion_matrix" />
              <a-empty v-else />
            </a-card>
          </a-col>
          <a-col :span="12">
            <a-card title="ROC 曲线" size="small">
              <RocCurve v-if="evaluation.metrics?.roc_data" :data="evaluation.metrics.roc_data" :auc="evaluation.metrics.auc" />
              <a-empty v-else />
            </a-card>
          </a-col>
        </a-row>
      </template>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '@/components/PageContainer/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import ConfusionMatrix from '@/components/ConfusionMatrix/index.vue'
import RocCurve from '@/components/RocCurve/index.vue'
import { getEvaluation } from '@/api/model'
import { formatDateTime } from '@/utils/date'

const route = useRoute()
const router = useRouter()
const evaluation = ref<any>(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getEvaluation(Number(route.params.id))
    evaluation.value = res.data.data
  } finally { loading.value = false }
})
</script>
