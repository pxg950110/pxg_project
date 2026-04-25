<template>
  <PageContainer title="编码体系">
    <template #extra>
      <a-button type="primary" @click="handleCreate">
        <template #icon><PlusOutlined /></template>
        新增编码体系
      </a-button>
    </template>

    <a-spin :spinning="loading">
      <a-empty v-if="!loading && codeSystems.length === 0" description="暂无编码体系数据" />
      <a-row :gutter="[16, 16]" v-else>
        <a-col :span="6" v-for="cs in codeSystems" :key="cs.id">
          <a-card hoverable @click="handleViewConcepts(cs)" style="height: 100%">
            <template #title>
              <span>{{ cs.name }}</span>
            </template>
            <template #extra>
              <a-tag :color="cs.status === 'ACTIVE' ? 'green' : cs.status === 'DRAFT' ? 'orange' : 'default'">
                {{ cs.status === 'ACTIVE' ? '启用' : cs.status === 'DRAFT' ? '草稿' : cs.status }}
              </a-tag>
            </template>
            <a-descriptions :column="1" size="small">
              <a-descriptions-item label="编码">{{ cs.code }}</a-descriptions-item>
              <a-descriptions-item label="版本">{{ cs.version || '-' }}</a-descriptions-item>
              <a-descriptions-item label="概念数">
                <a-statistic :value="statsMap[cs.id]?.conceptCount ?? '-'" :value-style="{ fontSize: '18px' }" />
              </a-descriptions-item>
              <a-descriptions-item label="层级支持">
                <ApartmentOutlined v-if="cs.hierarchySupport" style="color: #1890ff" />
                <MinusOutlined v-else style="color: #999" />
                {{ cs.hierarchySupport ? '支持' : '不支持' }}
              </a-descriptions-item>
            </a-descriptions>
            <template #actions>
              <a-button type="link" size="small" @click.stop="handleEdit(cs)">编辑</a-button>
              <a-button type="link" size="small" @click.stop="handleViewConcepts(cs)">浏览概念</a-button>
            </template>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>

    <a-modal v-model:open="modalVisible" :title="isEdit ? '编辑编码体系' : '新增编码体系'"
      :width="600" @ok="handleSubmit" @cancel="handleModalCancel" destroy-on-close>
      <a-form ref="formRef" :model="formState" :rules="formRules"
        :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="编码" name="code">
          <a-input v-model:value="formState.code" placeholder="如 ICD-10, LOINC, SNOMED" :disabled="isEdit" />
        </a-form-item>
        <a-form-item label="名称" name="name">
          <a-input v-model:value="formState.name" placeholder="编码体系名称" />
        </a-form-item>
        <a-form-item label="版本" name="version">
          <a-input v-model:value="formState.version" placeholder="如 2024 版" />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formState.description" :rows="3" placeholder="编码体系描述" />
        </a-form-item>
        <a-form-item label="层级支持" name="hierarchySupport">
          <a-switch v-model:checked="formState.hierarchySupport" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined, ApartmentOutlined, MinusOutlined } from '@ant-design/icons-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import PageContainer from '@/components/PageContainer/index.vue'
import { getCodeSystems, getCodeSystemStats, createCodeSystem, updateCodeSystem } from '@/api/masterdata'

defineOptions({ name: 'CodeSystems' })
const router = useRouter()

const loading = ref(false)
const codeSystems = ref<any[]>([])
const statsMap = ref<Record<number, any>>({})

async function fetchData() {
  loading.value = true
  try {
    const res = await getCodeSystems()
    codeSystems.value = res.data.data || []
    // Load stats for each code system
    for (const cs of codeSystems.value) {
      try {
        const statsRes = await getCodeSystemStats(cs.id)
        statsMap.value[cs.id] = statsRes.data.data
      } catch {
        statsMap.value[cs.id] = { conceptCount: 0 }
      }
    }
  } finally {
    loading.value = false
  }
}

// Modal
const modalVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const isEdit = computed(() => editingId.value !== null)

const formState = reactive({
  code: '',
  name: '',
  version: '',
  description: '',
  hierarchySupport: false,
})

const formRules: Record<string, Rule[]> = {
  code: [{ required: true, message: '请输入编码' }],
  name: [{ required: true, message: '请输入名称' }],
}

function handleCreate() {
  editingId.value = null
  Object.assign(formState, { code: '', name: '', version: '', description: '', hierarchySupport: false })
  modalVisible.value = true
}

function handleEdit(record: any) {
  editingId.value = record.id
  Object.assign(formState, {
    code: record.code,
    name: record.name,
    version: record.version || '',
    description: record.description || '',
    hierarchySupport: record.hierarchySupport || false,
  })
  modalVisible.value = true
}

function handleModalCancel() {
  formRef.value?.resetFields()
  modalVisible.value = false
  editingId.value = null
}

async function handleSubmit() {
  await formRef.value?.validateFields()
  const data = { ...formState }
  if (isEdit.value) {
    await updateCodeSystem(editingId.value!, data)
    message.success('更新成功')
  } else {
    await createCodeSystem(data)
    message.success('创建成功')
  }
  handleModalCancel()
  fetchData()
}

function handleViewConcepts(cs: any) {
  router.push({ path: '/masterdata/concepts', query: { codeSystemId: String(cs.id) } })
}

onMounted(() => fetchData())
</script>
