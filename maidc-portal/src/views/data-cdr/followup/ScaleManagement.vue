<template>
  <PageContainer title="量表管理" :breadcrumb="[{ title: '数据管理' }, { title: '量表管理' }]">
    <template #extra>
      <el-button v-if="hasPermission('disease:scale:manage')" type="primary" @click="goDesign()">
        <el-icon class="mr-1"><Plus /></el-icon>
        新建量表
      </el-button>
    </template>

    <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
      <el-input
        v-model="keyword"
        placeholder="搜索量表名称 / 编码"
        clearable
        style="width: 280px; margin-bottom: 16px"
        @keyup.enter="load"
      />
      <el-table :data="filtered" v-loading="loading" row-key="id" size="small">
        <el-table-column label="编码" prop="scaleCode" width="160" />
        <el-table-column label="名称" min-width="160">
          <template #default="{ row }">
            <b>{{ row.name }}</b>
          </template>
        </el-table-column>
        <el-table-column label="版本" prop="version" width="70" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '启用' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="满分" width="70">
          <template #default="{ row }">{{ parseDef(row).maxScore ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="MCID" width="70">
          <template #default="{ row }">{{ parseDef(row).mcid ?? '–' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="previewScale(row)">预览</el-button>
            <el-button v-if="hasPermission('disease:scale:manage')" size="small" link type="primary" @click="goDesign(row.scaleCode)">设计</el-button>
            <el-popconfirm
              v-if="hasPermission('disease:scale:manage') && row.status === 'ACTIVE'"
              title="停用后不可被新方案引用，确认停用？"
              @confirm="handleDisable(row)"
            >
              <template #reference>
                <el-button size="small" link type="danger">停用</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 预览弹窗：受试者视角 -->
    <el-dialog v-model="previewOpen" :title="`量表预览 · ${previewRow?.name || ''}（受试者视角）`" width="760px">
      <ScaleFillPanel v-if="previewRow" :scale-code="previewRow.scaleCode" :definition="parseDef(previewRow)" />
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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

async function load() {
  loading.value = true
  try {
    const res = await getScales()
    scales.value = res.data?.data || []
  } catch { ElMessage.error('量表加载失败') }
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
    ElMessage.success(`已停用 ${record.scaleCode}`)
    load()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '停用失败')
  }
}

onMounted(load)
</script>
