<template>
  <PageContainer title="数据源管理">
    <template #extra>
      <a-button type="primary" @click="handleCreate">
        <template #icon><PlusOutlined /></template>
        新建数据源
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
        <template v-if="column.key === 'type'">
          <a-tag :color="sourceTypeColorMap[record.type] || 'default'">
            {{ sourceTypeMap[record.type] || record.type }}
          </a-tag>
        </template>
        <template v-if="column.key === 'connectionStatus'">
          <StatusBadge :status="record.connection_status" type="connection" />
        </template>
        <template v-if="column.key === 'syncMode'">
          {{ syncModeMap[record.sync_mode] || record.sync_mode }}
        </template>
        <template v-if="column.key === 'lastSyncTime'">
          {{ record.last_sync_time ? formatDateTime(record.last_sync_time) : '-' }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-button type="link" size="small" @click="handleTestConnection(record)">测试连接</a-button>
            <a-button type="link" size="small" @click="handleSync(record)">同步</a-button>
            <a-popconfirm title="确定删除此数据源？" @confirm="handleDelete(record)">
              <a-button type="link" danger size="small">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 新建/编辑数据源弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑数据源' : '新建数据源'"
      :confirm-loading="submitLoading"
      :width="680"
      @ok="handleSubmit"
      @cancel="handleModalCancel"
      destroy-on-close
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item label="数据源名称" name="name">
          <a-input v-model:value="formState.name" placeholder="请输入数据源名称" />
        </a-form-item>

        <a-form-item label="数据源类型" name="type">
          <a-select v-model:value="formState.type" placeholder="请选择类型" :disabled="isEdit">
            <a-select-option v-for="item in sourceTypeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-divider orientation="left">连接配置</a-divider>

        <a-form-item label="主机地址" name="host">
          <a-input v-model:value="formState.host" placeholder="如: 192.168.1.100" />
        </a-form-item>

        <a-form-item label="端口" name="port">
          <a-input-number v-model:value="formState.port" :min="1" :max="65535" placeholder="端口号" style="width: 100%" />
        </a-form-item>

        <a-form-item label="数据库" name="database">
          <a-input v-model:value="formState.database" placeholder="数据库名称" />
        </a-form-item>

        <a-form-item label="用户名" name="username">
          <a-input v-model:value="formState.username" placeholder="数据库用户名" />
        </a-form-item>

        <a-form-item label="密码" name="password">
          <a-input-password v-model:value="formState.password" :placeholder="isEdit ? '留空则不修改' : '数据库密码'" />
        </a-form-item>

        <a-divider orientation="left">同步配置</a-divider>

        <a-form-item label="同步模式" name="sync_mode">
          <a-radio-group v-model:value="formState.sync_mode">
            <a-radio value="realtime">实时同步</a-radio>
            <a-radio value="batch">批量同步</a-radio>
            <a-radio value="manual">手动同步</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item v-if="formState.sync_mode === 'batch'" label="Cron 表达式" name="cron_expression">
          <a-input v-model:value="formState.cron_expression" placeholder="如: 0 0 2 * * ? (每天凌晨2点)" />
          <div class="form-help">格式: 秒 分 时 日 月 周</div>
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import { useTable } from '@/hooks/useTable'
import {
  getDataSources,
  createDataSource,
  updateDataSource,
  deleteDataSource,
  testDataSourceConnection,
  syncDataSource,
} from '@/api/data'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'DataSourceList' })

const router = useRouter()

// ===== 常量 =====
const sourceTypeMap: Record<string, string> = {
  HIS: 'HIS',
  LIS: 'LIS',
  PACS: 'PACS',
  EMR: 'EMR',
  EXTERNAL: '外部系统',
}

const sourceTypeColorMap: Record<string, string> = {
  HIS: 'blue',
  LIS: 'green',
  PACS: 'purple',
  EMR: 'orange',
  EXTERNAL: 'cyan',
}

const sourceTypeOptions = [
  { label: 'HIS', value: 'HIS' },
  { label: 'LIS', value: 'LIS' },
  { label: 'PACS', value: 'PACS' },
  { label: 'EMR', value: 'EMR' },
  { label: '外部系统', value: 'EXTERNAL' },
]

const syncModeMap: Record<string, string> = {
  realtime: '实时同步',
  batch: '批量同步',
  manual: '手动同步',
}

// ===== 搜索 =====
const searchFields = [
  { name: 'keyword', label: '关键词', type: 'input' as const, placeholder: '数据源名称' },
  { name: 'type', label: '类型', type: 'select' as const, options: sourceTypeOptions },
  { name: 'status', label: '连接状态', type: 'select' as const, options: [
    { label: '已连接', value: 'CONNECTED' },
    { label: '已断开', value: 'DISCONNECTED' },
    { label: '连接异常', value: 'ERROR' },
  ] },
]

let currentSearchParams: Record<string, any> = {}

function handleSearch(values: Record<string, any>) {
  currentSearchParams = values
  fetchData({ page: 1 })
}

function handleReset() {
  currentSearchParams = {}
  fetchData({ page: 1 })
}

// ===== 表格 =====
const columns = [
  { title: '数据源名称', dataIndex: 'name', key: 'name', width: 180, ellipsis: true },
  { title: '类型', key: 'type', width: 100 },
  { title: '连接状态', key: 'connectionStatus', width: 110 },
  { title: '同步模式', key: 'syncMode', width: 100 },
  { title: '最后同步时间', key: 'lastSyncTime', width: 170 },
  { title: '操作', key: 'action', width: 260, fixed: 'right' as const },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getDataSources({
    page: params.page,
    page_size: params.pageSize,
    ...currentSearchParams,
  }),
)

// ===== 弹窗 =====
const modalVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const isEdit = computed(() => editingId.value !== null)

const formState = reactive({
  name: '',
  type: undefined as string | undefined,
  host: '',
  port: 3306 as number,
  database: '',
  username: '',
  password: '',
  sync_mode: 'manual' as string,
  cron_expression: '',
})

const formRules: Record<string, Rule[]> = {
  name: [{ required: true, message: '请输入数据源名称' }],
  type: [{ required: true, message: '请选择数据源类型' }],
  host: [{ required: true, message: '请输入主机地址' }],
  port: [{ required: true, message: '请输入端口' }],
  database: [{ required: true, message: '请输入数据库名称' }],
  username: [{ required: true, message: '请输入用户名' }],
  sync_mode: [{ required: true, message: '请选择同步模式' }],
}

function handleCreate() {
  editingId.value = null
  Object.assign(formState, {
    name: '', type: undefined, host: '', port: 3306,
    database: '', username: '', password: '',
    sync_mode: 'manual', cron_expression: '',
  })
  modalVisible.value = true
}

function handleEdit(record: any) {
  editingId.value = record.id
  Object.assign(formState, {
    name: record.name,
    type: record.type,
    host: record.config?.host || '',
    port: record.config?.port || 3306,
    database: record.config?.database || '',
    username: record.config?.username || '',
    password: '',
    sync_mode: record.sync_mode || 'manual',
    cron_expression: record.cron_expression || '',
  })
  modalVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validateFields()
  submitLoading.value = true
  try {
    const data = {
      name: formState.name,
      type: formState.type,
      config: {
        host: formState.host,
        port: formState.port,
        database: formState.database,
        username: formState.username,
        ...(formState.password ? { password: formState.password } : {}),
      },
      sync_mode: formState.sync_mode,
      ...(formState.sync_mode === 'batch' ? { cron_expression: formState.cron_expression } : {}),
    }
    if (isEdit.value) {
      await updateDataSource(editingId.value!, data)
      message.success('更新成功')
    } else {
      await createDataSource(data)
      message.success('创建成功')
    }
    handleModalCancel()
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

function handleModalCancel() {
  formRef.value?.resetFields()
  modalVisible.value = false
  editingId.value = null
}

// ===== 操作 =====
async function handleTestConnection(record: any) {
  const hide = message.loading('正在测试连接...', 0)
  try {
    const res = await testDataSourceConnection(record.id)
    hide()
    if (res.data.data.success) {
      message.success('连接成功')
    } else {
      message.error(`连接失败: ${res.data.data.message}`)
    }
  } catch {
    hide()
  }
}

async function handleSync(record: any) {
  try {
    await syncDataSource(record.id)
    message.success('同步任务已启动')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

async function handleDelete(record: any) {
  await deleteDataSource(record.id)
  message.success('删除成功')
  fetchData()
}

onMounted(() => fetchData())
</script>

<style scoped>
.form-help {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
