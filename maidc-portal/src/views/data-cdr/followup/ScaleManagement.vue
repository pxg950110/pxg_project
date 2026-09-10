<template>
  <PageContainer title="量表管理" :breadcrumb="[{ title: '数据管理' }, { title: '量表管理' }]">
    <template #extra>
      <a-button v-if="hasPermission('disease:scale:manage')" type="primary" @click="goDesign()">+ 新建量表</a-button>
    </template>

    <a-card>
      <a-input-search v-model:value="keyword" placeholder="搜索量表名称 / 编码" style="width: 280px; margin-bottom: 16px" allow-clear @search="load" />
      <a-table :columns="columns" :data-source="filtered" :loading="loading" row-key="id" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <b>{{ record.name }}</b>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'ACTIVE' ? 'green' : 'default'">
              {{ record.status === 'ACTIVE' ? '启用' : '已停用' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'maxScore'">
            {{ parseDef(record).maxScore ?? '-' }}
          </template>
          <template v-if="column.key === 'mcid'">
            {{ parseDef(record).mcid ?? '–' }}
          </template>
          <template v-if="column.key === 'action'">
            <a-button size="small" type="link" @click="previewScale(record)">预览</a-button>
            <a-button v-if="hasPermission('disease:scale:manage')" size="small" type="link" @click="goDesign(record.scaleCode)">设计</a-button>
            <a-popconfirm v-if="hasPermission('disease:scale:manage') && record.status === 'ACTIVE'"
              title="停用后不可被新方案引用，确认停用？" @confirm="handleDisable(record)">
              <a-button size="small" type="link" danger>停用</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 预览弹窗：受试者视角 -->
    <a-modal v-model:open="previewOpen" :title="`量表预览 · ${previewRow?.name || ''}（受试者视角）`" width="760" footer="null">
      <ScaleFillPanel v-if="previewRow" :scale-code="previewRow.scaleCode" :definition="parseDef(previewRow)" />
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import ScaleFillPanel from './components/ScaleFillPanel.vue'
import { getScales, updateScaleStatus } from '@/api/followup'
import { usePermissionStore } from '@/stores/permission'

const router = useRouter()
const permissionStore = usePermissionStore()
const hasPermission = (code: string) => permissionStore.hasPermission(code)

const keyword = ref('')
const loading = ref(false)
const scales = ref<any[]>([])

const filtered = computed(() =>
  keyword.value
    ? scales.value.filter(s => s.name.includes(keyword.value) || s.scaleCode.toLowerCase().includes(keyword.value.toLowerCase()))
    : scales.value)

const columns = [
  { title: '编码', dataIndex: 'scaleCode', key: 'scaleCode', width: 160 },
  { title: '名称', key: 'name' },
  { title: '版本', dataIndex: 'version', key: 'version', width: 70 },
  { title: '状态', key: 'status', width: 90 },
  { title: '满分', key: 'maxScore', width: 70 },
  { title: 'MCID', key: 'mcid', width: 70 },
  { title: '操作', key: 'action', width: 200 },
]

async function load() {
  loading.value = true
  try {
    const res = await getScales()
    scales.value = res.data?.data || []
  } catch { message.error('量表加载失败') }
  finally { loading.value = false }
}

const previewOpen = ref(false)
const previewRow = ref<any>(null)
function previewScale(record: any) {
  previewRow.value = record
  previewOpen.value = true
}

function parseDef(record: any) {
  const d = record.definition
  return typeof d === 'string' ? JSON.parse(d || '{}') : d || {}
}

function goDesign(scaleCode?: string) {
  router.push({ name: 'ScaleDesigner', query: scaleCode ? { scaleCode } : {} })
}

async function handleDisable(record: any) {
  try {
    await updateScaleStatus(record.id, 'DISABLED')
    message.success(`已停用 ${record.scaleCode}`)
    load()
  } catch (e: any) {
    message.error(e.response?.data?.message || '停用失败')
  }
}

onMounted(load)
</script>
