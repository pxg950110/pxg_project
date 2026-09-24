<template>
  <PageContainer title="编码映射">
    <!-- Filter bar -->
    <el-card shadow="never" style="margin-bottom: 16px">
      <div class="flex items-center gap-4 flex-wrap">
        <div class="flex min-w-0 flex-1 items-center gap-2">
          <span class="shrink-0 text-sm">源编码体系:</span>
          <el-select v-model="sourceSystemId" placeholder="选择源体系" clearable class="min-w-0 flex-1" @change="fetchMappings">
            <el-option v-for="cs in codeSystems" :key="cs.id" :value="cs.id" :label="cs.name" />
          </el-select>
        </div>
        <div class="flex min-w-0 flex-1 items-center gap-2">
          <span class="shrink-0 text-sm">目标编码体系:</span>
          <el-select v-model="targetSystemId" placeholder="选择目标体系" clearable class="min-w-0 flex-1" @change="fetchMappings">
            <el-option v-for="cs in codeSystems" :key="cs.id" :value="cs.id" :label="cs.name" />
          </el-select>
        </div>
        <div class="ml-auto">
          <el-button type="primary" :disabled="!sourceSystemId || !targetSystemId" @click="handleCreateMapping">
            <el-icon class="mr-1"><Plus /></el-icon>
            新增映射
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- Mappings table -->
    <el-table :data="mappings" v-loading="loading" row-key="id">
      <el-table-column label="源概念编码" prop="sourceConceptCode" width="150" />
      <el-table-column label="源概念名称" prop="sourceConceptName" width="180" show-overflow-tooltip />
      <el-table-column label="目标概念编码" prop="targetConceptCode" width="150" />
      <el-table-column label="目标概念名称" prop="targetConceptName" width="180" show-overflow-tooltip />
      <el-table-column label="映射类型" width="120">
        <template #default="{ row }">
          <el-tag :type="mappingTypeTagType(row.mappingType)" :style="mappingTypeTagStyle(row.mappingType)">{{ row.mappingType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="置信度" prop="confidence" width="80">
        <template #default="{ row }">
          {{ row.confidence != null ? `${(row.confidence * 100).toFixed(0)}%` : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-popconfirm title="确定删除此映射？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button link type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="mt-4 justify-end"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="pagination.total"
      :current-page="pagination.current"
      :page-size="pagination.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />

    <!-- Create mapping modal -->
    <el-dialog v-model="modalVisible" title="新增映射" width="600px" :destroy-on-close="true">
      <el-form ref="formRef" :model="formState" :rules="formRules" label-width="100px">
        <el-form-item label="源概念ID" prop="sourceConceptId">
          <el-input-number v-model="formState.sourceConceptId" placeholder="输入源概念ID" class="w-full" />
        </el-form-item>
        <el-form-item label="目标概念ID" prop="targetConceptId">
          <el-input-number v-model="formState.targetConceptId" placeholder="输入目标概念ID" class="w-full" />
        </el-form-item>
        <el-form-item label="映射类型" prop="mappingType">
          <el-select v-model="formState.mappingType" placeholder="选择映射类型" class="w-full">
            <el-option value="SAME_AS" label="相同 (SAME_AS)" />
            <el-option value="BROADER_THAN" label="更宽 (BROADER_THAN)" />
            <el-option value="NARROWER_THAN" label="更窄 (NARROWER_THAN)" />
            <el-option value="CLOSE_ENOUGH" label="近似 (CLOSE_ENOUGH)" />
          </el-select>
        </el-form-item>
        <el-form-item label="置信度" prop="confidence">
          <el-slider v-model="formState.confidence" :min="0" :max="100" :step="5"
            :marks="{ 0: '0%', 50: '50%', 100: '100%' }" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleModalCancel">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { type FormInstance, type FormItemRule } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import { getCodeSystems, getConceptMappings, createMapping, deleteMapping } from '@/api/masterdata'

defineOptions({ name: 'MappingManager' })

type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'
const PURPLE_TAG_STYLE = { color: '#8b5cf6', background: '#f5f3ff', borderColor: '#ddd6fe' }

const codeSystems = ref<any[]>([])
const sourceSystemId = ref<number | undefined>(undefined)
const targetSystemId = ref<number | undefined>(undefined)

const loading = ref(false)
const mappings = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

function mappingTypeTagType(type: string): TagType {
  const types: Record<string, TagType> = { SAME_AS: 'success', BROADER_THAN: 'primary', NARROWER_THAN: 'warning', CLOSE_ENOUGH: 'info' }
  return types[type] || 'info'
}

function mappingTypeTagStyle(type: string) {
  return type === 'CLOSE_ENOUGH' ? PURPLE_TAG_STYLE : undefined
}

async function fetchCodeSystems() {
  try {
    const res = await getCodeSystems()
    codeSystems.value = res.data.data || []
  } catch { /* ignore */ }
}

async function fetchMappings() {
  if (!sourceSystemId.value || !targetSystemId.value) {
    mappings.value = []
    return
  }
  loading.value = true
  try {
    // Fetch concepts for source system and then their mappings
    const res = await getConceptMappings(sourceSystemId.value, {
      targetCodeSystemId: targetSystemId.value,
      page: pagination.current,
      pageSize: pagination.pageSize,
    })
    mappings.value = res.data.data || []
    // Note: if the API returns a page, adjust accordingly
  } catch {
    mappings.value = []
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  pagination.current = page
  fetchMappings()
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.current = 1
  fetchMappings()
}

// Modal
const modalVisible = ref(false)
const formRef = ref<FormInstance>()

const formState = reactive({
  sourceConceptId: undefined as number | undefined,
  targetConceptId: undefined as number | undefined,
  mappingType: 'SAME_AS',
  confidence: 100,
})

const formRules: Record<string, FormItemRule[]> = {
  sourceConceptId: [{ required: true, message: '请输入源概念ID' }],
  targetConceptId: [{ required: true, message: '请输入目标概念ID' }],
  mappingType: [{ required: true, message: '请选择映射类型' }],
}

function handleCreateMapping() {
  Object.assign(formState, { sourceConceptId: undefined, targetConceptId: undefined, mappingType: 'SAME_AS', confidence: 100 })
  modalVisible.value = true
}

function handleModalCancel() {
  formRef.value?.resetFields()
  modalVisible.value = false
}

async function handleSubmit() {
  await formRef.value?.validate()
  await createMapping({
    sourceConceptId: formState.sourceConceptId,
    targetConceptId: formState.targetConceptId,
    mappingType: formState.mappingType,
    confidence: formState.confidence / 100,
    sourceCodeSystemId: sourceSystemId.value,
    targetCodeSystemId: targetSystemId.value,
  })
  ElMessage.success('映射创建成功')
  handleModalCancel()
  fetchMappings()
}

async function handleDelete(record: any) {
  await deleteMapping(record.id)
  ElMessage.success('删除成功')
  fetchMappings()
}

onMounted(() => fetchCodeSystems())
</script>
