<template>
  <PageContainer title="模型列表" subtitle="管理所有已注册的AI模型">
    <!-- Action Bar -->
    <div class="action-bar">
      <div class="action-bar-left">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索模型名称或编码..."
          style="width: 280px"
          allow-clear
          @search="handleSearch"
        />
        <a-select
          v-model:value="sortBy"
          style="width: 140px"
          @change="handleSortChange"
        >
          <a-select-option value="updated_at">更新时间</a-select-option>
          <a-select-option value="created_at">创建时间</a-select-option>
          <a-select-option value="model_name">模型名称</a-select-option>
        </a-select>
      </div>
      <div class="action-bar-right">
        <a-button type="primary" @click="registerModal.open()">
          <PlusOutlined /> 注册模型
        </a-button>
      </div>
    </div>

    <!-- Category Filter Tabs -->
    <div class="category-tabs">
      <a-radio-group v-model:value="activeCategory" button-style="solid" @change="handleCategoryChange">
        <a-radio-button value="全部">全部</a-radio-button>
        <a-radio-button value="影像">影像</a-radio-button>
        <a-radio-button value="NLP">NLP</a-radio-button>
        <a-radio-button value="结构化">结构化</a-radio-button>
        <a-radio-button value="多模态">多模态</a-radio-button>
        <a-radio-button value="基因组">基因组</a-radio-button>
      </a-radio-group>
    </div>

    <!-- Card Grid -->
    <a-spin :spinning="loading">
    <a-row :gutter="[16, 16]" class="model-card-grid">
      <a-col v-for="model in tableData" :key="model.id" :span="8">
        <a-card class="model-card" hoverable>
          <!-- Row 1: Name + Category Tag -->
          <div class="card-header">
            <span class="model-name">{{ model.model_name }}</span>
            <a-tag :color="categoryColorMap[model.category]">{{ model.category }}</a-tag>
          </div>

          <!-- Row 2: Description -->
          <div class="model-desc">{{ model.description }}</div>

          <!-- Row 3: Framework + Version + Status -->
          <div class="card-meta-row">
            <a-tag size="small" class="framework-tag">{{ model.framework }}</a-tag>
            <span class="version-text">{{ model.version }}</span>
            <StatusBadge :status="model.status" type="model" />
          </div>

          <!-- Row 4: QPS (only for PUBLISHED) -->
          <div v-if="model.status === 'PUBLISHED' && model.qps !== null" class="qps-row">
            <span class="qps-label">QPS:</span>
            <span class="qps-value">{{ model.qps }}</span>
          </div>

          <!-- Row 5: View Details Link -->
          <div class="card-footer">
            <a class="detail-link" @click="router.push(`/model/${model.id}`)">
              查看详情 <RightOutlined />
            </a>
          </div>
        </a-card>
      </a-col>
    </a-row>
    </a-spin>

    <!-- Pagination -->
    <div class="pagination-bar">
      <span class="pagination-total">共 {{ pagination.total }} 个模型</span>
      <a-pagination
        v-model:current="pagination.current"
        :total="pagination.total"
        :page-size="pagination.pageSize"
        show-quick-jumper
        show-size-changer
        size="small"
        @change="(page: number, pageSize: number) => fetchData({ page, pageSize })"
      />
    </div>

    <!-- Register Modal -->
    <a-modal
      v-model:open="registerModal.visible"
      title="注册模型"
      @ok="handleRegister"
      :confirm-loading="submitting"
      width="640px"
    >
      <a-form :model="registerForm" :rules="registerRules" ref="registerFormRef" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="模型名称" name="model_name">
              <a-input v-model:value="registerForm.model_name" placeholder="请输入模型名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="模型编码" name="model_code">
              <a-input v-model:value="registerForm.model_code" placeholder="自动生成或手动输入" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="模型类型" name="model_type">
              <a-select v-model:value="registerForm.model_type" placeholder="请选择">
                <a-select-option value="IMAGING">影像</a-select-option>
                <a-select-option value="NLP">NLP</a-select-option>
                <a-select-option value="STRUCTURED">结构化</a-select-option>
                <a-select-option value="MULTIMODAL">多模态</a-select-option>
                <a-select-option value="GENOMIC">基因组</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="框架" name="framework">
              <a-select v-model:value="registerForm.framework" placeholder="请选择">
                <a-select-option value="PyTorch">PyTorch</a-select-option>
                <a-select-option value="TensorFlow">TensorFlow</a-select-option>
                <a-select-option value="SKLearn">SKLearn</a-select-option>
                <a-select-option value="XGBoost">XGBoost</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="registerForm.description" :rows="3" placeholder="请输入模型描述" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Edit Modal -->
    <a-modal
      v-model:open="editModal.visible"
      title="编辑模型"
      @ok="handleEdit"
      :confirm-loading="submitting"
      width="640px"
    >
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="模型名称" name="model_name">
          <a-input v-model:value="editForm.model_name" />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="editForm.description" :rows="3" />
        </a-form-item>
        <a-form-item label="标签">
          <a-select v-model:value="editForm.tags" mode="tags" placeholder="输入标签后回车" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { PlusOutlined, RightOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import { useModal } from '@/hooks/useModal'
import { useTable } from '@/hooks/useTable'
import { getModels, createModel, updateModel } from '@/api/model'

const router = useRouter()
const registerModal = useModal()
const editModal = useModal<any>()
const submitting = ref(false)

// --- Category color map ---
const categoryColorMap: Record<string, string> = {
  '影像': 'blue',
  'NLP': 'green',
  '结构化': 'orange',
  '多模态': 'purple',
  '基因组': 'cyan',
}

// --- Filter / search state ---
const searchKeyword = ref('')
const activeCategory = ref('全部')
const sortBy = ref('updated_at')

// Category to model_type mapping for API filter
const categoryToType: Record<string, string | undefined> = {
  '全部': undefined,
  '影像': 'IMAGING',
  'NLP': 'NLP',
  '结构化': 'STRUCTURED',
  '多模态': 'MULTIMODAL',
  '基因组': 'GENOMIC',
}

// --- Table hook with API ---
const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => getModels({
    page: params.page,
    page_size: params.pageSize,
    model_type: categoryToType[activeCategory.value],
    keyword: searchKeyword.value.trim() || undefined,
  })
)

function handleSearch() {
  fetchData({ page: 1 })
}

function handleCategoryChange() {
  fetchData({ page: 1 })
}

function handleSortChange() {
  fetchData({ page: 1 })
}

onMounted(() => fetchData())

// --- Register form ---
const registerForm = reactive({
  model_name: '',
  model_code: '',
  model_type: undefined as string | undefined,
  framework: undefined as string | undefined,
  description: '',
})
const registerRules = {
  model_name: [{ required: true, message: '请输入模型名称' }],
  model_type: [{ required: true, message: '请选择模型类型' }],
  framework: [{ required: true, message: '请选择框架' }],
}
const registerFormRef = ref()

async function handleRegister() {
  submitting.value = true
  try {
    await createModel(registerForm)
    message.success('模型注册成功')
    registerModal.close()
    fetchData()
  } finally {
    submitting.value = false
  }
}

// --- Edit form ---
const editForm = reactive({
  model_name: '',
  description: '',
  tags: [] as string[],
})
let editingId = 0

function onEditOpen() {
  if (editModal.currentRecord.value) {
    const r = editModal.currentRecord.value
    editingId = r.id
    editForm.model_name = r.model_name
    editForm.description = r.description
    editForm.tags = r.tags || []
  }
}

watch(() => editModal.visible, (v) => { if (v) onEditOpen() })

async function handleEdit() {
  submitting.value = true
  try {
    await updateModel(editingId, editForm)
    message.success('模型更新成功')
    editModal.close()
    fetchData()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.action-bar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-bar-right {
  display: flex;
  align-items: center;
}

.category-tabs {
  margin-bottom: 20px;
}

.model-card-grid {
  min-height: 200px;
}

.model-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.model-card :deep(.ant-card-body) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.model-name {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  line-height: 1.5;
}

.model-desc {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  line-height: 1.6;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.framework-tag {
  border-style: dashed;
}

.version-text {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}

.qps-row {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  margin-bottom: 8px;
}

.qps-label {
  color: rgba(0, 0, 0, 0.45);
  margin-right: 4px;
}

.qps-value {
  font-weight: 500;
}

.card-footer {
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  text-align: right;
}

.detail-link {
  color: #1677ff;
  cursor: pointer;
  font-size: 14px;
  transition: color 0.2s;
}

.detail-link:hover {
  color: #4096ff;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.pagination-total {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
