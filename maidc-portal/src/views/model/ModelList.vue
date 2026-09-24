<template>
  <PageContainer title="模型列表" subtitle="管理所有已注册的AI模型">
    <!-- Action Bar -->
    <div class="action-bar">
      <div class="action-bar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索模型名称或编码..."
          style="width: 280px"
          clearable
          :suffix-icon="Search"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="sortBy"
          style="width: 140px"
          @change="handleSortChange"
        >
          <el-option label="更新时间" value="updated_at" />
          <el-option label="创建时间" value="created_at" />
          <el-option label="模型名称" value="model_name" />
        </el-select>
      </div>
      <div class="action-bar-right">
        <el-button type="primary" @click="registerModal.open()">
          <el-icon class="mr-1"><Plus /></el-icon>注册模型
        </el-button>
      </div>
    </div>

    <!-- Category Filter Tabs -->
    <div class="category-tabs">
      <el-radio-group v-model="activeCategory" @change="handleCategoryChange">
        <el-radio-button value="全部">全部</el-radio-button>
        <el-radio-button value="影像">影像</el-radio-button>
        <el-radio-button value="NLP">NLP</el-radio-button>
        <el-radio-button value="结构化">结构化</el-radio-button>
        <el-radio-button value="多模态">多模态</el-radio-button>
        <el-radio-button value="基因组">基因组</el-radio-button>
      </el-radio-group>
    </div>

    <!-- Card Grid -->
    <div class="model-card-grid grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4" v-loading="loading">
      <el-card
        v-for="model in tableData"
        :key="model.id"
        class="model-card !rounded-xl !border-slate-200/80"
        shadow="hover"
      >
        <!-- Row 1: Name + Category Tag -->
        <div class="card-header">
          <span class="model-name">{{ model.model_name }}</span>
          <el-tag
            size="small"
            :type="categoryTagType(model.category)"
            :style="categoryTagStyle(model.category)"
          >{{ model.category }}</el-tag>
        </div>

        <!-- Row 2: Description -->
        <div class="model-desc">{{ model.description || '暂无描述' }}</div>

        <!-- Row 3: Framework + Version + Status -->
        <div class="card-meta-row">
          <el-tag size="small" class="framework-tag">{{ model.framework }}</el-tag>
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
            查看详情 <el-icon class="ml-0.5"><ArrowRight /></el-icon>
          </a>
        </div>
      </el-card>
    </div>

    <!-- Pagination -->
    <div class="pagination-bar">
      <span class="pagination-total">共 {{ pagination.total }} 个模型</span>
      <el-pagination
        background
        layout="sizes, prev, pager, next, jumper"
        :total="pagination.total"
        :current-page="pagination.current"
        :page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- Register Modal -->
    <el-dialog
      v-model="registerModal.visible"
      title="注册模型"
      width="640px"
    >
      <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="100px">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
          <el-form-item label="模型名称" prop="modelName">
            <el-input v-model="registerForm.modelName" placeholder="请输入模型名称" />
          </el-form-item>
          <el-form-item label="模型编码" prop="modelCode">
            <el-input v-model="registerForm.modelCode" placeholder="自动生成或手动输入" />
          </el-form-item>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
          <el-form-item label="模型类型" prop="modelType">
            <el-select v-model="registerForm.modelType" placeholder="请选择">
              <el-option label="影像" value="IMAGING" />
              <el-option label="NLP" value="NLP" />
              <el-option label="结构化" value="STRUCTURED" />
              <el-option label="多模态" value="MULTIMODAL" />
              <el-option label="基因组" value="GENOMIC" />
            </el-select>
          </el-form-item>
          <el-form-item label="任务类型" prop="taskType">
            <el-select v-model="registerForm.taskType" placeholder="请选择">
              <el-option label="分类" value="CLASSIFICATION" />
              <el-option label="分割" value="SEGMENTATION" />
              <el-option label="目标检测" value="OBJECT_DETECTION" />
              <el-option label="回归" value="REGRESSION" />
              <el-option label="命名实体识别" value="NER" />
              <el-option label="文本分类" value="TEXT_CLASSIFICATION" />
            </el-select>
          </el-form-item>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
          <el-form-item label="框架" prop="framework">
            <el-select v-model="registerForm.framework" placeholder="请选择">
              <el-option label="PyTorch" value="PYTORCH" />
              <el-option label="TensorFlow" value="TENSORFLOW" />
              <el-option label="SKLearn" value="SKLEARN" />
              <el-option label="XGBoost" value="XGBOOST" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="描述" prop="description">
          <el-input v-model="registerForm.description" type="textarea" :rows="3" placeholder="请输入模型描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerModal.close()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleRegister">确定</el-button>
      </template>
    </el-dialog>

    <!-- Edit Modal -->
    <el-dialog
      v-model="editModal.visible"
      title="编辑模型"
      width="640px"
    >
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="editForm.tags" placeholder="用逗号分隔标签" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editModal.close()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleEdit">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, ArrowRight, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import { useModal } from '@/hooks/useModal'
import { useTable } from '@/hooks/useTable'
import { getModels, createModel, updateModel } from '@/api/model'

const router = useRouter()
const registerModal = useModal()
const editModal = useModal<any>()
const submitting = ref(false)

// --- Model type to Chinese label mapping ---
const modelTypeLabel: Record<string, string> = {
  'IMAGING': '影像',
  'NLP': 'NLP',
  'STRUCTURED': '结构化',
  'MULTIMODAL': '多模态',
  'GENOMIC': '基因组',
}

// --- Category color map ---
const categoryColorMap: Record<string, string> = {
  '影像': 'primary',
  'NLP': 'success',
  '结构化': 'warning',
  '多模态': 'purple',
  '基因组': 'cyan',
}

const customTagStyles: Record<string, Record<string, string>> = {
  purple: { color: '#8b5cf6', backgroundColor: '#f5f3ff', borderColor: '#ddd6fe' },
  cyan: { color: '#0e7490', backgroundColor: '#ecfeff', borderColor: '#a5f3fc' },
}

function categoryTagType(category: string) {
  const t = categoryColorMap[category]
  return customTagStyles[t] ? 'primary' : (t || 'info')
}

function categoryTagStyle(category: string) {
  return customTagStyles[categoryColorMap[category]]
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
const { tableData: rawTableData, loading, pagination, fetchData } = useTable<any>(
  (params) => getModels({
    page: params.page,
    page_size: params.pageSize,
    model_type: categoryToType[activeCategory.value],
    keyword: searchKeyword.value.trim() || undefined,
  })
)

// Map camelCase API response to template-friendly format
const tableData = computed(() =>
  rawTableData.value.map((m: any) => ({
    ...m,
    model_name: m.modelName,
    category: modelTypeLabel[m.modelType] || m.modelType,
    description: m.description || '',
    version: m.latestVersion || '-',
    framework: m.framework,
    status: m.status,
    qps: m.qps ?? null,
  }))
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

function handlePageChange(page: number) {
  pagination.current = page
  fetchData({ page })
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.current = 1
  fetchData({ page: 1, pageSize: size })
}

onMounted(() => fetchData())

// --- Register form ---
const registerForm = reactive({
  modelName: '',
  modelCode: '',
  modelType: undefined as string | undefined,
  taskType: undefined as string | undefined,
  framework: undefined as string | undefined,
  description: '',
})
const registerRules = {
  modelName: [{ required: true, message: '请输入模型名称' }],
  modelCode: [{ required: true, message: '请输入模型编码' }],
  modelType: [{ required: true, message: '请选择模型类型' }],
  taskType: [{ required: true, message: '请选择任务类型' }],
  framework: [{ required: true, message: '请选择框架' }],
}
const registerFormRef = ref()

async function handleRegister() {
  try {
    await registerFormRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await createModel(registerForm)
    ElMessage.success('模型注册成功')
    registerModal.close()
    fetchData()
  } finally {
    submitting.value = false
  }
}

// --- Edit form ---
const editForm = reactive({
  description: '',
  tags: '' as string,
})
let editingId = 0

function onEditOpen() {
  if (editModal.currentRecord.value) {
    const r = editModal.currentRecord.value
    editingId = r.id
    editForm.description = r.description || ''
    editForm.tags = r.tags || ''
  }
}

watch(() => editModal.visible, (v) => { if (v) onEditOpen() })

async function handleEdit() {
  submitting.value = true
  try {
    await updateModel(editingId, editForm)
    ElMessage.success('模型更新成功')
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

.model-card :deep(.el-card__body) {
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
  color: #0f172a;
  line-height: 1.5;
}

.model-desc {
  font-size: 13px;
  color: #94a3b8;
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
  color: #64748b;
}

.qps-row {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 8px;
}

.qps-label {
  color: #94a3b8;
  margin-right: 4px;
}

.qps-value {
  font-weight: 500;
}

.card-footer {
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
  text-align: right;
}

.detail-link {
  color: #0ea5e9;
  cursor: pointer;
  font-size: 14px;
  transition: color 0.2s;
  display: inline-flex;
  align-items: center;
}

.detail-link:hover {
  color: #38bdf8;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.pagination-total {
  font-size: 14px;
  color: #94a3b8;
}
</style>
