<template>
  <PageContainer title="领域管理">
    <template #extra>
      <el-button type="primary" @click="handleCreate">
        <el-icon class="mr-1"><Plus /></el-icon>
        新增领域
      </el-button>
    </template>

    <el-table :data="domains" v-loading="loading" row-key="id">
      <el-table-column label="领域代码" prop="code" width="140" />
      <el-table-column label="中文名称" prop="name" show-overflow-tooltip />
      <el-table-column label="英文名称" prop="nameEn" width="150" show-overflow-tooltip />
      <el-table-column label="描述" prop="description" show-overflow-tooltip />
      <el-table-column label="图标" prop="icon" width="120" />
      <el-table-column label="排序" prop="sortOrder" width="80" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'warning'">
            {{ row.status === 'ACTIVE' ? '启用' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <div class="flex items-center gap-2">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除此领域？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="modalVisible" :title="isEdit ? '编辑领域' : '新增领域'" width="560px" :destroy-on-close="true">
      <el-form ref="formRef" :model="formState" :rules="formRules" label-width="100px">
        <el-form-item label="领域代码" prop="code">
          <el-input v-model="formState.code" placeholder="如 Diagnosis, Procedure" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="中文名称" prop="name">
          <el-input v-model="formState.name" placeholder="如 诊断、手术操作" />
        </el-form-item>
        <el-form-item label="英文名称" prop="nameEn">
          <el-input v-model="formState.nameEn" placeholder="English name" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formState.description" type="textarea" :rows="2" placeholder="领域描述" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="formState.icon" placeholder="Ant Design 图标名，如 HeartOutlined" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="formState.sortOrder" :min="0" :max="999" class="w-full" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="formState.status" placeholder="选择状态" class="w-full">
            <el-option value="ACTIVE" label="启用" />
            <el-option value="DRAFT" label="草稿" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { type FormInstance, type FormItemRule } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import { getDomains, createDomain, updateDomain, deleteDomain } from '@/api/masterdata'

defineOptions({ name: 'DomainManager' })

const loading = ref(false)
const domains = ref<any[]>([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getDomains()
    domains.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

const modalVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const isEdit = computed(() => editingId.value !== null)

const formState = reactive({
  code: '',
  name: '',
  nameEn: '',
  description: '',
  icon: '',
  sortOrder: 0,
  status: 'ACTIVE',
})

const formRules: Record<string, FormItemRule[]> = {
  code: [{ required: true, message: '请输入领域代码' }],
  name: [{ required: true, message: '请输入中文名称' }],
}

function handleCreate() {
  editingId.value = null
  Object.assign(formState, { code: '', name: '', nameEn: '', description: '', icon: '', sortOrder: 0, status: 'ACTIVE' })
  modalVisible.value = true
}

function handleEdit(record: any) {
  editingId.value = record.id
  Object.assign(formState, {
    code: record.code,
    name: record.name,
    nameEn: record.nameEn || '',
    description: record.description || '',
    icon: record.icon || '',
    sortOrder: record.sortOrder ?? 0,
    status: record.status || 'ACTIVE',
  })
  modalVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (isEdit.value) {
    await updateDomain(editingId.value!, formState)
    ElMessage.success('更新成功')
  } else {
    await createDomain(formState)
    ElMessage.success('创建成功')
  }
  modalVisible.value = false
  fetchData()
}

async function handleDelete(id: number) {
  await deleteDomain(id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(() => fetchData())
</script>
