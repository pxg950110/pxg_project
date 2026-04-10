<template>
  <PageContainer title="特征字典">
    <template #extra>
      <a-button type="primary" @click="openCreate()">
        <PlusOutlined /> 新增特征
      </a-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      @change="handleTableChange"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'data_type'">
          <a-tag :color="dataTypeColorMap[record.data_type] || 'default'">{{ record.data_type }}</a-tag>
        </template>
        <template v-if="column.key === 'category'">
          <a-tag color="blue">{{ record.category }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="openEdit(record)">编辑</a>
            <a-popconfirm title="确认删除该特征？" @confirm="handleDelete(record.id)">
              <a class="danger-link">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- Add/Edit Feature Modal -->
    <a-modal
      v-model:open="featureModal.visible"
      :title="isEdit ? '编辑特征' : '新增特征'"
      @ok="handleSubmit"
      :confirm-loading="submitting"
      width="600px"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="特征编码" required>
              <a-input v-model:value="featureForm.feature_code" placeholder="如: patient_age" :disabled="isEdit" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="特征名称" required>
              <a-input v-model:value="featureForm.feature_name" placeholder="如: 患者年龄" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="数据类型" required>
              <a-select v-model:value="featureForm.data_type" placeholder="选择数据类型">
                <a-select-option value="INTEGER">整数 (INTEGER)</a-select-option>
                <a-select-option value="FLOAT">浮点数 (FLOAT)</a-select-option>
                <a-select-option value="VARCHAR">字符串 (VARCHAR)</a-select-option>
                <a-select-option value="DATE">日期 (DATE)</a-select-option>
                <a-select-option value="DATETIME">日期时间 (DATETIME)</a-select-option>
                <a-select-option value="BOOLEAN">布尔 (BOOLEAN)</a-select-option>
                <a-select-option value="TEXT">文本 (TEXT)</a-select-option>
                <a-select-option value="JSON">JSON</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="分类" required>
              <a-select v-model:value="featureForm.category" placeholder="选择分类">
                <a-select-option value="DEMOGRAPHIC">人口学</a-select-option>
                <a-select-option value="CLINICAL">临床</a-select-option>
                <a-select-option value="LABORATORY">检验</a-select-option>
                <a-select-option value="IMAGING">影像</a-select-option>
                <a-select-option value="MEDICATION">用药</a-select-option>
                <a-select-option value="OUTCOME">结局</a-select-option>
                <a-select-option value="DERIVED">衍生</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="来源表">
          <a-input v-model:value="featureForm.source_table" placeholder="如: cdr_patient" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="featureForm.description" :rows="3" placeholder="特征描述说明" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import request from '@/utils/request'

defineOptions({ name: 'FeatureDictionary' })

const featureModal = useModal()
const submitting = ref(false)
const isEdit = computed(() => !!featureModal.currentRecord?.value)

const dataTypeColorMap: Record<string, string> = {
  INTEGER: 'blue',
  FLOAT: 'cyan',
  VARCHAR: 'green',
  DATE: 'orange',
  DATETIME: 'orange',
  BOOLEAN: 'purple',
  TEXT: 'default',
  JSON: 'magenta',
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

const columns = [
  { title: '特征编码', dataIndex: 'feature_code', key: 'feature_code', width: 160 },
  { title: '特征名称', dataIndex: 'feature_name', key: 'feature_name', width: 160 },
  { title: '数据类型', dataIndex: 'data_type', key: 'data_type', width: 110 },
  { title: '来源表', dataIndex: 'source_table', key: 'source_table', width: 140 },
  { title: '分类', dataIndex: 'category', key: 'category', width: 100 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '操作', key: 'action', width: 120 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => request.get('/rdr/features', { params: { page: params.page, page_size: params.pageSize } })
)

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
    message.warning('请填写特征编码和名称')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      const record = featureModal.currentRecord!.value!
      await request.put(`/rdr/features/${record.id}`, featureForm)
      message.success('特征更新成功')
    } else {
      await request.post('/rdr/features', featureForm)
      message.success('特征创建成功')
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
    message.success('特征已删除')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

onMounted(() => fetchData())
</script>

<style scoped>
.danger-link {
  color: #ff4d4f;
}
.danger-link:hover {
  color: #ff7875;
}
</style>
