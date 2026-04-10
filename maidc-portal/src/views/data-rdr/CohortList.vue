<template>
  <PageContainer title="队列管理">
    <template #extra>
      <a-button type="primary" @click="cohortModal.open()">
        <PlusOutlined /> 新建队列
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
        <template v-if="column.key === 'criteria_summary'">
          <a-tooltip :title="record.criteria_summary">
            <span class="text-ellipsis">{{ record.criteria_summary || '-' }}</span>
          </a-tooltip>
        </template>
        <template v-if="column.key === 'inclusion_rules'">
          <a-tag v-for="(rule, idx) in (record.inclusion_rules || []).slice(0, 3)" :key="idx" color="green">
            {{ rule.field }} {{ rule.operator }} {{ rule.value }}
          </a-tag>
          <span v-if="(record.inclusion_rules || []).length > 3"> +{{ record.inclusion_rules.length - 3 }}</span>
        </template>
        <template v-if="column.key === 'exclusion_rules'">
          <a-tag v-for="(rule, idx) in (record.exclusion_rules || []).slice(0, 2)" :key="idx" color="red">
            {{ rule.field }} {{ rule.operator }} {{ rule.value }}
          </a-tag>
          <span v-if="(record.exclusion_rules || []).length > 2"> +{{ record.exclusion_rules.length - 2 }}</span>
          <span v-if="!(record.exclusion_rules || []).length">-</span>
        </template>
        <template v-if="column.key === 'created_at'">
          {{ formatDateTime(record.created_at) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="viewCohort(record)">查看</a>
            <a-popconfirm title="确认删除该队列？" @confirm="handleDelete(record.id)">
              <a class="danger-link">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- Create/Edit Cohort Modal -->
    <a-modal
      v-model:open="cohortModal.visible"
      :title="isEdit ? '编辑队列' : '新建队列'"
      @ok="handleSubmit"
      :confirm-loading="submitting"
      width="700px"
    >
      <a-form layout="vertical">
        <a-form-item label="队列名称" required>
          <a-input v-model:value="cohortForm.name" placeholder="请输入队列名称" />
        </a-form-item>

        <a-divider orientation="left">纳入条件</a-divider>
        <div v-for="(condition, idx) in cohortForm.inclusion_rules" :key="'inc-' + idx" class="condition-row">
          <a-row :gutter="8" align="middle">
            <a-col :span="7">
              <a-input v-model:value="condition.field" placeholder="字段名" />
            </a-col>
            <a-col :span="5">
              <a-select v-model:value="condition.operator" placeholder="运算符">
                <a-select-option value="=">=</a-select-option>
                <a-select-option value="!=">!=</a-select-option>
                <a-select-option value=">">></a-select-option>
                <a-select-option value="<"><</a-select-option>
                <a-select-option value=">=">>=</a-select-option>
                <a-select-option value="<="><=</a-select-option>
                <a-select-option value="IN">IN</a-select-option>
                <a-select-option value="LIKE">LIKE</a-select-option>
              </a-select>
            </a-col>
            <a-col :span="8">
              <a-input v-model:value="condition.value" placeholder="值" />
            </a-col>
            <a-col :span="4">
              <a-button type="text" danger @click="removeCondition('inclusion', idx)">
                <DeleteOutlined />
              </a-button>
            </a-col>
          </a-row>
        </div>
        <a-button type="dashed" block @click="addCondition('inclusion')">
          <PlusOutlined /> 添加纳入条件
        </a-button>

        <a-divider orientation="left">排除条件</a-divider>
        <div v-for="(condition, idx) in cohortForm.exclusion_rules" :key="'exc-' + idx" class="condition-row">
          <a-row :gutter="8" align="middle">
            <a-col :span="7">
              <a-input v-model:value="condition.field" placeholder="字段名" />
            </a-col>
            <a-col :span="5">
              <a-select v-model:value="condition.operator" placeholder="运算符">
                <a-select-option value="=">=</a-select-option>
                <a-select-option value="!=">!=</a-select-option>
                <a-select-option value=">">></a-select-option>
                <a-select-option value="<"><</a-select-option>
                <a-select-option value=">=">>=</a-select-option>
                <a-select-option value="<="><=</a-select-option>
                <a-select-option value="IN">IN</a-select-option>
                <a-select-option value="LIKE">LIKE</a-select-option>
              </a-select>
            </a-col>
            <a-col :span="8">
              <a-input v-model:value="condition.value" placeholder="值" />
            </a-col>
            <a-col :span="4">
              <a-button type="text" danger @click="removeCondition('exclusion', idx)">
                <DeleteOutlined />
              </a-button>
            </a-col>
          </a-row>
        </div>
        <a-button type="dashed" block @click="addCondition('exclusion')">
          <PlusOutlined /> 添加排除条件
        </a-button>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import request from '@/utils/request'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'CohortList' })

const router = useRouter()
const cohortModal = useModal()
const submitting = ref(false)

const isEdit = computed(() => !!cohortModal.currentRecord?.value)

const searchFields = [
  { name: 'keyword', label: '关键词', type: 'input', placeholder: '队列名称' },
  { name: 'project_id', label: '所属项目', type: 'select', options: [] },
]

const columns = [
  { title: '队列名称', dataIndex: 'name', key: 'name', width: 180 },
  { title: '标准摘要', dataIndex: 'criteria_summary', key: 'criteria_summary', ellipsis: true },
  { title: '患者数', dataIndex: 'patient_count', key: 'patient_count', width: 90 },
  { title: '纳入规则', dataIndex: 'inclusion_rules', key: 'inclusion_rules', width: 200 },
  { title: '排除规则', dataIndex: 'exclusion_rules', key: 'exclusion_rules', width: 160 },
  { title: '创建时间', dataIndex: 'created_at', key: 'created_at', width: 170 },
  { title: '操作', key: 'action', width: 120 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => request.get('/rdr/cohorts', { params: { page: params.page, page_size: params.pageSize } })
)

interface Condition {
  field: string
  operator: string
  value: string
}

const cohortForm = reactive({
  name: '',
  inclusion_rules: [] as Condition[],
  exclusion_rules: [] as Condition[],
})

function addCondition(type: 'inclusion' | 'exclusion') {
  const condition: Condition = { field: '', operator: '=', value: '' }
  if (type === 'inclusion') {
    cohortForm.inclusion_rules.push(condition)
  } else {
    cohortForm.exclusion_rules.push(condition)
  }
}

function removeCondition(type: 'inclusion' | 'exclusion', index: number) {
  if (type === 'inclusion') {
    cohortForm.inclusion_rules.splice(index, 1)
  } else {
    cohortForm.exclusion_rules.splice(index, 1)
  }
}

function resetForm() {
  cohortForm.name = ''
  cohortForm.inclusion_rules = []
  cohortForm.exclusion_rules = []
}

function handleSearch() { fetchData() }
function handleReset() { fetchData() }

async function handleSubmit() {
  if (!cohortForm.name) {
    message.warning('请输入队列名称')
    return
  }
  submitting.value = true
  try {
    await request.post('/rdr/cohorts', cohortForm)
    message.success(isEdit.value ? '队列更新成功' : '队列创建成功')
    cohortModal.close()
    resetForm()
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await request.delete(`/rdr/cohorts/${id}`)
    message.success('队列已删除')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

function viewCohort(record: any) {
  message.info('查看队列: ' + record.name)
}

onMounted(() => fetchData())
</script>

<style scoped>
.condition-row {
  margin-bottom: 8px;
}
.text-ellipsis {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
.danger-link {
  color: #ff4d4f;
}
.danger-link:hover {
  color: #ff7875;
}
</style>
