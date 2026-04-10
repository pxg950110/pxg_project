<template>
  <PageContainer title="流量路由">
    <template #extra>
      <a-button type="primary" @click="createModal.open()">
        <PlusOutlined /> 新建路由
      </a-button>
    </template>

    <a-table :columns="columns" :data-source="routes" :loading="loading" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'rules'">
          <a-tag v-for="(rule, i) in record.rules" :key="i" color="blue">
            {{ rule.version_no }}: {{ rule.weight }}%
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="editRoute(record)">编辑</a>
            <a-popconfirm title="确定删除？" @confirm="deleteRoute(record.id)">
              <a class="danger-link">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- Create/Edit Route Modal -->
    <a-modal v-model:open="createModal.visible" :title="editingId ? '编辑路由' : '新建路由'" @ok="handleSave" :confirm-loading="submitting" width="700px">
      <a-form layout="vertical">
        <a-form-item label="路由名称" required>
          <a-input v-model:value="routeForm.name" />
        </a-form-item>
        <a-form-item label="流量规则">
          <TrafficRuleEditor v-model="routeForm.rules" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import TrafficRuleEditor from '@/components/TrafficRuleEditor/index.vue'
import { useModal } from '@/hooks/useModal'
import request from '@/utils/request'

const createModal = useModal()
const submitting = ref(false)
const loading = ref(false)
const routes = ref<any[]>([])
const editingId = ref<number | null>(null)

const columns = [
  { title: '路由名称', dataIndex: 'name', key: 'name' },
  { title: '模型', dataIndex: 'model_name', key: 'model_name' },
  { title: '流量规则', key: 'rules' },
  { title: '创建时间', dataIndex: 'created_at', key: 'created_at' },
  { title: '操作', key: 'action', width: 150 },
]

const routeForm = reactive({ name: '', rules: [] as any[] })

async function loadRoutes() {
  loading.value = true
  try {
    const res = await request.get('/deployments/routes')
    routes.value = res.data.data || []
  } finally { loading.value = false }
}

function editRoute(record: any) {
  editingId.value = record.id
  routeForm.name = record.name
  routeForm.rules = record.rules || []
  createModal.open()
}

async function handleSave() {
  submitting.value = true
  try {
    if (editingId.value) {
      await request.put(`/deployments/routes/${editingId.value}`, routeForm)
    } else {
      await request.post('/deployments/routes', routeForm)
    }
    message.success('路由保存成功')
    createModal.close()
    editingId.value = null
    loadRoutes()
  } finally { submitting.value = false }
}

async function deleteRoute(id: number) {
  await request.delete(`/deployments/routes/${id}`)
  message.success('路由已删除')
  loadRoutes()
}

onMounted(loadRoutes)
</script>

<style scoped>
.danger-link { color: #ff4d4f; }
</style>
