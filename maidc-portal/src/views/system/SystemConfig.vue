<template>
  <PageContainer title="系统配置">
    <template #extra>
      <a-button type="primary" @click="configModal.open()">
        <PlusOutlined /> 新增配置
      </a-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a @click="openEditModal(record)">编辑</a>
        </template>
      </template>
    </a-table>

    <!-- Config Modal -->
    <a-modal v-model:open="configModal.visible" :title="editingId ? '编辑配置' : '新增配置'" @ok="handleSave" :confirm-loading="submitting">
      <a-form layout="vertical">
        <a-form-item label="配置组"><a-input v-model:value="configForm.group" /></a-form-item>
        <a-form-item label="配置键" required><a-input v-model:value="configForm.key" /></a-form-item>
        <a-form-item label="配置值" required><a-textarea v-model:value="configForm.value" :rows="3" /></a-form-item>
        <a-form-item label="描述"><a-input v-model:value="configForm.description" /></a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import { getConfigs, updateConfig } from '@/api/system'
import request from '@/utils/request'

const configModal = useModal()
const submitting = ref(false)
const editingId = ref<number | null>(null)

const searchFields = [
  { name: 'group', label: '配置组', type: 'input', placeholder: '配置组名' },
]

const columns = [
  { title: '配置组', dataIndex: 'group', key: 'group', width: 120 },
  { title: '配置键', dataIndex: 'key', key: 'key', width: 180 },
  { title: '配置值', dataIndex: 'value', key: 'value', ellipsis: true },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '操作', key: 'action', width: 80 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getConfigs({ page: params.page, page_size: params.pageSize })
)

const configForm = reactive({ group: '', key: '', value: '', description: '' })

function handleSearch() { fetchData() }
function handleReset() { fetchData() }

function openEditModal(record: any) {
  editingId.value = record.id
  Object.assign(configForm, record)
  configModal.open()
}

watch(() => configModal.visible, (v) => {
  if (v && !editingId.value) {
    Object.assign(configForm, { group: '', key: '', value: '', description: '' })
  }
})

async function handleSave() {
  submitting.value = true
  try {
    if (editingId.value) {
      await updateConfig(editingId.value, configForm)
    } else {
      await request.post('/system/configs', configForm)
    }
    message.success('配置保存成功')
    configModal.close()
    editingId.value = null
    fetchData()
  } finally { submitting.value = false }
}

onMounted(() => fetchData())
</script>
