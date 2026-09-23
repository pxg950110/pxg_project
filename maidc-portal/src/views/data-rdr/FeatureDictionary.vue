<template>
  <PageContainer title="特征字典">
    <template #extra>
      <el-button type="primary" @click="openCreate()">
        <el-icon class="mr-1"><Plus /></el-icon> 新增特征
      </el-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <el-table
      :data="tableData"
      v-loading="loading"
      row-key="id"
      size="default"
    >
      <el-table-column label="特征编码" prop="feature_code" width="160" />
      <el-table-column label="特征名称" prop="feature_name" width="160" />
      <el-table-column label="数据类型" width="110">
        <template #default="{ row }">
          <el-tag :style="dataTypeColorMap[row.data_type] || defaultTagStyle">{{ row.data_type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源表" prop="source_table" width="140" />
      <el-table-column label="分类" width="100">
        <template #default="{ row }">
          <el-tag :style="categoryColorMap[row.category] || defaultTagStyle">{{ categoryLabelMap[row.category] || row.category }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="描述" prop="description" show-overflow-tooltip />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <div class="flex items-center gap-2">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除该特征？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
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

    <!-- Add/Edit Feature Dialog -->
    <el-dialog
      v-model="featureModal.visible"
      :title="isEdit ? '编辑特征' : '新增特征'"
      width="600px"
    >
      <el-form label-position="top">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <el-form-item label="特征编码" required>
            <el-input v-model="featureForm.feature_code" placeholder="如: patient_age" :disabled="isEdit" />
          </el-form-item>
          <el-form-item label="特征名称" required>
            <el-input v-model="featureForm.feature_name" placeholder="如: 患者年龄" />
          </el-form-item>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <el-form-item label="数据类型" required>
            <el-select v-model="featureForm.data_type" placeholder="选择数据类型">
              <el-option value="INTEGER" label="整数 (INTEGER)" />
              <el-option value="FLOAT" label="浮点数 (FLOAT)" />
              <el-option value="VARCHAR" label="字符串 (VARCHAR)" />
              <el-option value="DATE" label="日期 (DATE)" />
              <el-option value="DATETIME" label="日期时间 (DATETIME)" />
              <el-option value="BOOLEAN" label="布尔 (BOOLEAN)" />
              <el-option value="TEXT" label="文本 (TEXT)" />
              <el-option value="JSON" label="JSON" />
            </el-select>
          </el-form-item>
          <el-form-item label="分类" required>
            <el-select v-model="featureForm.category" placeholder="选择分类">
              <el-option value="DEMOGRAPHIC" label="人口学" />
              <el-option value="CLINICAL" label="临床" />
              <el-option value="LABORATORY" label="检验" />
              <el-option value="IMAGING" label="影像" />
              <el-option value="MEDICATION" label="用药" />
              <el-option value="OUTCOME" label="结局" />
              <el-option value="DERIVED" label="衍生" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="来源表">
          <el-input v-model="featureForm.source_table" placeholder="如: cdr_patient" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="featureForm.description" type="textarea" :rows="3" placeholder="特征描述说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="featureModal.close()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import request from '@/utils/request'

defineOptions({ name: 'FeatureDictionary' })

const featureModal = useModal()
const submitting = ref(false)
const isEdit = computed(() => !!featureModal.currentRecord?.value)

const defaultTagStyle = { background: '#f8fafc', borderColor: '#e2e8f0', color: '#64748b' }

const dataTypeColorMap: Record<string, Record<string, string>> = {
  INTEGER: { background: '#f0f9ff', borderColor: '#bae6fd', color: '#0ea5e9' },
  FLOAT: { background: '#cffafe', borderColor: '#a5f3fc', color: '#06b6d4' },
  VARCHAR: { background: '#ecfdf5', borderColor: '#a7f3d0', color: '#10b981' },
  DATE: { background: '#fff7ed', borderColor: '#fed7aa', color: '#f97316' },
  DATETIME: { background: '#fff7ed', borderColor: '#fed7aa', color: '#f97316' },
  BOOLEAN: { background: '#f5f3ff', borderColor: '#ddd6fe', color: '#8b5cf6' },
  TEXT: defaultTagStyle,
  JSON: { background: '#fdf2f8', borderColor: '#fbcfe8', color: '#ec4899' },
}

const categoryColorMap: Record<string, Record<string, string>> = {
  DEMOGRAPHIC: { background: '#f0f9ff', borderColor: '#bae6fd', color: '#0ea5e9' },
  CLINICAL: { background: '#ecfdf5', borderColor: '#a7f3d0', color: '#10b981' },
  LABORATORY: { background: '#fff7ed', borderColor: '#fed7aa', color: '#f97316' },
  IMAGING: { background: '#f5f3ff', borderColor: '#ddd6fe', color: '#8b5cf6' },
  DIAGNOSIS: { background: '#cffafe', borderColor: '#a5f3fc', color: '#06b6d4' },
  MEDICATION: { background: '#eef2ff', borderColor: '#c7d2fe', color: '#6366f1' },
  GENOMIC: { background: '#fdf2f8', borderColor: '#fbcfe8', color: '#ec4899' },
}

const categoryLabelMap: Record<string, string> = {
  DEMOGRAPHIC: '人口统计学',
  CLINICAL: '生命体征',
  LABORATORY: '实验室检查',
  IMAGING: '影像特征',
  DIAGNOSIS: '诊断',
  MEDICATION: '用药',
  GENOMIC: '基因组',
}

const searchFields = [
  { name: 'keyword', label: '关键词', type: 'input', placeholder: '特征编码或名称' },
  { name: 'category', label: '分类', type: 'select', options: [
    { label: '人口学', value: 'DEMOGRAPHIC' },
    { label: '临床', value: 'CLINICAL' },
    { label: '检验', value: 'LABORATORY' },
    { label: '影像', value: 'IMAGING' },
    { label: '用药', value: 'MEDICATION' },
    { label: '结局', value: 'OUTCOME' },
    { label: '衍生', value: 'DERIVED' },
  ]},
  { name: 'data_type', label: '数据类型', type: 'select', options: [
    { label: '整数', value: 'INTEGER' },
    { label: '浮点数', value: 'FLOAT' },
    { label: '字符串', value: 'VARCHAR' },
    { label: '日期', value: 'DATE' },
    { label: '布尔', value: 'BOOLEAN' },
  ]},
]

const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => request.get('/rdr/features', { params: { page: params.page, page_size: params.pageSize } })
)

function handlePageChange(page: number) {
  pagination.current = page
  fetchData({ page })
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.current = 1
  fetchData({ page: 1, pageSize: size })
}

const featureForm = reactive({
  feature_code: '',
  feature_name: '',
  data_type: 'VARCHAR',
  source_table: '',
  description: '',
  category: 'CLINICAL',
})

function resetForm() {
  featureForm.feature_code = ''
  featureForm.feature_name = ''
  featureForm.data_type = 'VARCHAR'
  featureForm.source_table = ''
  featureForm.description = ''
  featureForm.category = 'CLINICAL'
}

function openCreate() {
  resetForm()
  featureModal.open()
}

function openEdit(record: any) {
  Object.assign(featureForm, {
    feature_code: record.feature_code,
    feature_name: record.feature_name,
    data_type: record.data_type,
    source_table: record.source_table,
    description: record.description,
    category: record.category,
  })
  featureModal.open(record)
}

function handleSearch() { fetchData() }
function handleReset() { fetchData() }

async function handleSubmit() {
  if (!featureForm.feature_code || !featureForm.feature_name) {
    ElMessage.warning('请填写特征编码和名称')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      const record = featureModal.currentRecord!.value!
      await request.put(`/rdr/features/${record.id}`, featureForm)
      ElMessage.success('特征更新成功')
    } else {
      await request.post('/rdr/features', featureForm)
      ElMessage.success('特征创建成功')
    }
    featureModal.close()
    resetForm()
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await request.delete(`/rdr/features/${id}`)
    ElMessage.success('特征已删除')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

onMounted(() => fetchData())
</script>
