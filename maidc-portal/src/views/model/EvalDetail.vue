<template>
  <PageContainer title="评估详情" :loading="loading">
    <template #extra>
      <el-button @click="router.back()">返回</el-button>
    </template>

    <template v-if="evaluation">
      <el-card shadow="never" class="mb-4 !rounded-xl !border-slate-200/80 shadow-clinical-sm">
        <template #header>
          <span class="font-semibold text-slate-900">基本信息</span>
        </template>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="模型">{{ evaluation.model_name }}</el-descriptions-item>
          <el-descriptions-item label="版本">{{ evaluation.version_no }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusBadge :status="evaluation.status" type="eval" />
          </el-descriptions-item>
          <el-descriptions-item label="数据集">{{ evaluation.dataset_name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="样本数">{{ evaluation.sample_count || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(evaluation.created_at) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <template v-if="evaluation.status === 'COMPLETED'">
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
          <MetricCard title="Accuracy" :value="(evaluation.metrics?.accuracy * 100).toFixed(2)" suffix="%" />
          <MetricCard title="Precision" :value="(evaluation.metrics?.precision * 100).toFixed(2)" suffix="%" />
          <MetricCard title="Recall" :value="(evaluation.metrics?.recall * 100).toFixed(2)" suffix="%" />
          <MetricCard title="F1 Score" :value="(evaluation.metrics?.f1_score * 100).toFixed(2)" suffix="%" />
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
            <template #header>
              <span class="font-semibold text-slate-900 text-sm">混淆矩阵</span>
            </template>
            <ConfusionMatrix v-if="evaluation.metrics?.confusion_matrix" :matrix="evaluation.metrics.confusion_matrix" />
            <el-empty v-else :image-size="60" />
          </el-card>
          <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
            <template #header>
              <span class="font-semibold text-slate-900 text-sm">ROC 曲线</span>
            </template>
            <RocCurve v-if="evaluation.metrics?.roc_data" :data="evaluation.metrics.roc_data" :auc="evaluation.metrics.auc" />
            <el-empty v-else :image-size="60" />
          </el-card>
        </div>
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
