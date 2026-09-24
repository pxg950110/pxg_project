<template>
  <PageContainer title="队列管理">
    <template #extra>
      <el-button type="primary" @click="cohortModal.open()">
        <el-icon class="mr-1"><Plus /></el-icon> 新建队列
      </el-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <el-table
      :data="tableData"
      v-loading="loading"
      row-key="id"
      size="default"
    >
      <el-table-column label="队列名称" prop="name" width="180" />
      <el-table-column label="标准摘要" prop="criteria_summary" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tooltip :content="row.criteria_summary || '-'" placement="top">
            <span class="text-ellipsis">{{ row.criteria_summary || '-' }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="患者数" prop="patient_count" width="90" />
      <el-table-column label="纳入规则" width="200">
        <template #default="{ row }">
          <el-tag
            v-for="(rule, idx) in (row.inclusion_rules || []).slice(0, 3)"
            :key="idx"
            type="success"
            class="mr-1 mb-0.5"
          >
            {{ rule.field }} {{ rule.operator }} {{ rule.value }}
          </el-tag>
          <span v-if="(row.inclusion_rules || []).length > 3"> +{{ row.inclusion_rules.length - 3 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="排除规则" width="160">
        <template #default="{ row }">
          <el-tag
            v-for="(rule, idx) in (row.exclusion_rules || []).slice(0, 2)"
            :key="idx"
            type="danger"
            class="mr-1 mb-0.5"
          >
            {{ rule.field }} {{ rule.operator }} {{ rule.value }}
          </el-tag>
          <span v-if="(row.exclusion_rules || []).length > 2"> +{{ row.exclusion_rules.length - 2 }}</span>
          <span v-if="!(row.exclusion_rules || []).length">-</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <div class="flex items-center gap-2">
            <el-button link type="primary" @click="viewCohort(row)">查看</el-button>
            <el-popconfirm title="确认删除该队列？" @confirm="handleDelete(row.id)">
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

    <!-- Create/Edit Cohort Dialog -->
    <el-dialog
      v-model="cohortModal.visible"
      :title="isEdit ? '编辑队列' : '新建队列'"
      width="700px"
    >
      <el-form label-position="top">
        <el-form-item label="队列名称" required>
          <el-input v-model="cohortForm.name" placeholder="请输入队列名称" />
        </el-form-item>

        <el-divider content-position="left">纳入条件</el-divider>
        <div v-for="(condition, idx) in cohortForm.inclusion_rules" :key="'inc-' + idx" class="condition-row">
          <div class="grid grid-cols-[7fr_5fr_8fr_4fr] items-center gap-2">
            <el-input v-model="condition.field" placeholder="字段名" />
            <el-select v-model="condition.operator" placeholder="运算符">
              <el-option value="=" label="=" />
              <el-option value="!=" label="!=" />
              <el-option value=">" label=">" />
              <el-option value="<" label="<" />
              <el-option value=">=" label=">=" />
              <el-option value="<=" label="<=" />
              <el-option value="IN" label="IN" />
              <el-option value="LIKE" label="LIKE" />
            </el-select>
            <el-input v-model="condition.value" placeholder="值" />
            <el-button type="danger" text @click="removeCondition('inclusion', idx)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
        <el-button class="w-full" plain style="border-style: dashed" @click="addCondition('inclusion')">
          <el-icon class="mr-1"><Plus /></el-icon> 添加纳入条件
        </el-button>

        <el-divider content-position="left">排除条件</el-divider>
        <div v-for="(condition, idx) in cohortForm.exclusion_rules" :key="'exc-' + idx" class="condition-row">
          <div class="grid grid-cols-[7fr_5fr_8fr_4fr] items-center gap-2">
            <el-input v-model="condition.field" placeholder="字段名" />
            <el-select v-model="condition.operator" placeholder="运算符">
              <el-option value="=" label="=" />
              <el-option value="!=" label="!=" />
              <el-option value=">" label=">" />
              <el-option value="<" label="<" />
              <el-option value=">=" label=">=" />
              <el-option value="<=" label="<=" />
              <el-option value="IN" label="IN" />
              <el-option value="LIKE" label="LIKE" />
            </el-select>
            <el-input v-model="condition.value" placeholder="值" />
            <el-button type="danger" text @click="removeCondition('exclusion', idx)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
        <el-button class="w-full" plain style="border-style: dashed" @click="addCondition('exclusion')">
          <el-icon class="mr-1"><Plus /></el-icon> 添加排除条件
        </el-button>
      </el-form>
      <template #footer>
        <el-button @click="cohortModal.close()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
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

const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => request.get('/rdr/cohorts', { params: { page: params.page, page_size: params.pageSize } })
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
    ElMessage.warning('请输入队列名称')
    return
  }
  submitting.value = true
  try {
    await request.post('/rdr/cohorts', cohortForm)
    ElMessage.success(isEdit.value ? '队列更新成功' : '队列创建成功')
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
    ElMessage.success('队列已删除')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

function viewCohort(record: any) {
  ElMessage.info('查看队列: ' + record.name)
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
</style>
