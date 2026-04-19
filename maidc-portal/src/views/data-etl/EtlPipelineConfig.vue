<template>
  <PageContainer :loading="pageLoading" :breadcrumb="breadcrumb">
    <template #title>
      <span>{{ isEdit ? '编辑管道' : '新建管道' }}</span>
    </template>
    <template #extra>
      <a-space>
        <a-button @click="handlePreview" :disabled="!pipelineId">
          <template #icon><EyeOutlined /></template>
          预览YAML
        </a-button>
        <a-button v-if="isEdit" @click="handleValidate">
          <template #icon><CheckCircleOutlined /></template>
          校验
        </a-button>
        <a-button type="primary" :loading="saveLoading" @click="handleSave">
          <template #icon><SaveOutlined /></template>
          保存
        </a-button>
      </a-space>
    </template>

    <!-- Pipeline Basic Info -->
    <a-card title="基本信息" :bordered="false" style="margin-bottom: 12px" size="small">
      <a-form layout="inline" :model="pipelineForm">
        <a-form-item label="管道名称">
          <a-input v-model:value="pipelineForm.name" placeholder="请输入管道名称" style="width: 240px" size="small" />
        </a-form-item>
        <a-form-item label="数据源">
          <a-select v-model:value="pipelineForm.sourceId" placeholder="请选择数据源" :loading="dataSourceLoading" :disabled="isEdit" style="width: 200px" size="small">
            <a-select-option v-for="ds in dataSourceOptions" :key="ds.id" :value="ds.id">{{ ds.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="同步模式">
          <a-radio-group v-model:value="pipelineForm.syncMode" size="small">
            <a-radio-button value="MANUAL">手动</a-radio-button>
            <a-radio-button value="INCREMENTAL">增量</a-radio-button>
            <a-radio-button value="FULL">全量</a-radio-button>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- Visual Designer -->
    <EtlDesigner
      :nodes="graph.nodes.value"
      :edges="graph.edges.value"
      @update:nodes="(n: any) => graph.nodes.value = n"
      @update:edges="(e: any) => graph.edges.value = e"
    />
  </PageContainer>

  <!-- YAML Preview Modal -->
  <a-modal v-model:open="previewVisible" title="Embulk YAML 预览" :width="640" :footer="null">
    <a-spin :spinning="previewLoading">
      <pre class="yaml-preview">{{ previewYaml }}</pre>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { SaveOutlined, CheckCircleOutlined, EyeOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import EtlDesigner from './components/EtlDesigner.vue'
import { useDesignerGraph } from './composables/useDesignerGraph'
import {
  getEtlPipeline, createEtlPipeline, updateEtlPipeline,
  validateEtlPipeline, getEtlPipelineGraph, saveEtlPipelineGraph, previewEtlYaml,
} from '@/api/etl'
import { getDataSources } from '@/api/data'

defineOptions({ name: 'EtlPipelineConfig' })

const route = useRoute()
const router = useRouter()
const graph = useDesignerGraph()

const pageLoading = ref(false)
const saveLoading = ref(false)
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewYaml = ref('')

const pipelineId = computed(() => {
  const id = route.params.id
  return id && id !== 'new' ? Number(id) : null
})
const isEdit = computed(() => pipelineId.value !== null)

const breadcrumb = computed(() => [
  { title: '管道管理', path: '/etl/pipelines' },
  { title: isEdit.value ? '编辑管道' : '新建管道' },
])

const pipelineForm = reactive({
  name: '',
  sourceId: undefined as number | undefined,
  syncMode: 'MANUAL' as string,
  engineType: 'EMBULK' as string,
  description: '',
})

const dataSourceOptions = ref<any[]>([])
const dataSourceLoading = ref(false)

async function loadDataSources() {
  dataSourceLoading.value = true
  try {
    const res = await getDataSources({ page: 1, page_size: 200 })
    dataSourceOptions.value = res.data?.data?.items || []
  } catch {} finally { dataSourceLoading.value = false }
}

async function loadPipelineData() {
  if (!pipelineId.value) return
  pageLoading.value = true
  try {
    const res = await getEtlPipeline(pipelineId.value)
    const data = res.data?.data
    if (!data) return
    Object.assign(pipelineForm, {
      name: data.pipelineName || data.name || '', sourceId: data.sourceId,
      syncMode: data.syncMode || 'MANUAL', engineType: data.engineType || 'EMBULK',
      description: data.description || '',
    })

    const graphRes = await getEtlPipelineGraph(pipelineId.value)
    const graphData = graphRes.data?.data
    if (graphData) graph.deserialize(graphData)
  } catch {} finally { pageLoading.value = false }
}

async function handleSave() {
  if (!pipelineForm.name) { message.warning('请输入管道名称'); return }
  if (!pipelineForm.sourceId) { message.warning('请选择数据源'); return }

  saveLoading.value = true
  try {
    const pipelineData: Record<string, any> = {
      pipelineName: pipelineForm.name, sourceId: pipelineForm.sourceId,
      syncMode: pipelineForm.syncMode, engineType: pipelineForm.engineType,
      description: pipelineForm.description,
    }

    let currentId = pipelineId.value
    if (currentId) {
      await updateEtlPipeline(currentId, pipelineData)
    } else {
      const createRes = await createEtlPipeline(pipelineData)
      currentId = createRes.data?.data?.id
      if (currentId) router.replace({ name: 'EtlPipelineConfig', params: { id: currentId } })
    }

    if (!currentId) { message.error('管道保存失败：未获取到ID'); return }

    graph.refreshAllNodeStatuses()
    await saveEtlPipelineGraph(currentId, graph.serialize())
    message.success('保存成功')
  } catch {} finally { saveLoading.value = false }
}

async function handleValidate() {
  if (!pipelineId.value) { message.warning('请先保存管道后再校验'); return }
  const hide = message.loading('正在校验管道配置...', 0)
  try {
    const res = await validateEtlPipeline(pipelineId.value)
    hide()
    const errors = res.data?.data
    if (errors && errors.length > 0) message.warning(`校验发现 ${errors.length} 个问题`)
    else message.success('校验通过')
  } catch { hide() }
}

async function handlePreview() {
  if (!pipelineId.value) return
  previewVisible.value = true
  previewLoading.value = true
  try {
    const res = await previewEtlYaml(pipelineId.value)
    previewYaml.value = res.data?.data || 'No preview available'
  } catch { previewYaml.value = 'Failed to generate preview' }
  finally { previewLoading.value = false }
}

onMounted(async () => {
  await loadDataSources()
  if (isEdit.value) await loadPipelineData()
})
</script>

<style scoped>
.yaml-preview {
  background: #1e1e1e; color: #d4d4d4; padding: 16px; border-radius: 6px;
  font-family: 'Consolas', 'Monaco', monospace; font-size: 13px; line-height: 1.6;
  max-height: 500px; overflow: auto; white-space: pre-wrap;
}
</style>
