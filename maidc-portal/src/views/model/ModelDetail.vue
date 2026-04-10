<template>
  <PageContainer :title="model?.model_name || '模型详情'" :loading="loading">
    <template #extra>
      <a-space>
        <a-button @click="router.back()">返回</a-button>
      </a-space>
    </template>

    <template v-if="model">
      <!-- Basic Info -->
      <a-card title="基本信息" style="margin-bottom: 16px">
        <a-descriptions :column="3" bordered size="small">
          <a-descriptions-item label="模型编码">{{ model.model_code }}</a-descriptions-item>
          <a-descriptions-item label="模型类型">
            <a-tag>{{ model.model_type }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="框架">
            <a-tag color="blue">{{ model.framework }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <StatusBadge :status="model.status" type="model" />
          </a-descriptions-item>
          <a-descriptions-item label="最新版本">{{ model.latest_version || '-' }}</a-descriptions-item>
          <a-descriptions-item label="版本数">{{ model.version_count }}</a-descriptions-item>
          <a-descriptions-item label="负责人">{{ model.owner_name }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatDateTime(model.created_at) }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ formatDateTime(model.updated_at) }}</a-descriptions-item>
          <a-descriptions-item label="描述" :span="3">{{ model.description || '-' }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- Tabs -->
      <a-card>
        <a-tabs v-model:activeKey="activeTab">
          <a-tab-pane key="versions" tab="版本管理">
            <VersionList :model-id="model.id" />
          </a-tab-pane>
          <a-tab-pane key="schema" tab="数据Schema">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-card title="输入 Schema" size="small">
                  <SchemaViewer :schema="model.input_schema" mode="input" />
                </a-card>
              </a-col>
              <a-col :span="12">
                <a-card title="输出 Schema" size="small">
                  <SchemaViewer :schema="model.output_schema" mode="output" />
                </a-card>
              </a-col>
            </a-row>
          </a-tab-pane>
          <a-tab-pane key="stats" tab="统计信息">
            <a-empty description="暂无统计数据" />
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '@/components/PageContainer/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import SchemaViewer from '@/components/SchemaViewer/index.vue'
import { getModel } from '@/api/model'
import { formatDateTime } from '@/utils/date'

const route = useRoute()
const router = useRouter()
const model = ref<any>(null)
const loading = ref(false)
const activeTab = ref('versions')

async function loadModel() {
  loading.value = true
  try {
    const res = await getModel(Number(route.params.id))
    model.value = res.data.data
  } finally {
    loading.value = false
  }
}

onMounted(loadModel)
</script>
