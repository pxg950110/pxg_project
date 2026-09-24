<template>
  <PageContainer title="知识体系管理">
    <template #extra>
      <el-button type="primary" @click="handleCreateItem">
        <el-icon class="mr-1"><Plus /></el-icon>
        新增知识条目
      </el-button>
    </template>

    <div style="display: flex; gap: 16px; height: calc(100vh - 180px)">
      <!-- Left: Category tree -->
      <div style="width: 240px; flex-shrink: 0; display: flex; flex-direction: column">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px">
          <span style="font-weight: 600; font-size: 13px; color: #64748b">知识分类</span>
          <el-button text size="small" @click="handleCreateCategory">
            <el-icon><Plus /></el-icon>
          </el-button>
        </div>
        <div class="cat-list" style="flex: 1; overflow-y: auto">
          <div class="cat-item" :class="{ active: !selectedCategoryId }" @click="selectCategory(null)">
            全部
          </div>
          <div v-for="cat in categories" :key="cat.id" class="cat-item" :class="{ active: selectedCategoryId === cat.id }"
            @click="selectCategory(cat.id)">
            <span>{{ cat.name }}</span>
            <el-button text size="small" type="danger" @click.stop="handleDeleteCategory(cat)" style="opacity: 0; transition: opacity 0.2s">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- Right: Knowledge items -->
      <div style="flex: 1; display: flex; flex-direction: column; min-width: 0">
        <!-- Search bar -->
        <el-card shadow="never" :body-style="{ padding: '8px 16px' }" style="margin-bottom: 12px">
          <div class="flex flex-wrap items-center gap-3">
            <el-select v-model="filters.itemType" placeholder="类型" clearable style="width: 180px" @change="fetchItems(1)">
              <el-option value="GUIDELINE" label="临床指南" />
              <el-option value="LITERATURE" label="文献" />
              <el-option value="CONSENSUS" label="专家共识" />
              <el-option value="STANDARD" label="标准规范" />
              <el-option value="OTHER" label="其他" />
            </el-select>
            <el-input v-model="filters.keyword" placeholder="搜索标题或摘要"
              clearable :suffix-icon="Search" style="width: 320px"
              @keyup.enter="fetchItems(1)" @clear="fetchItems(1)" />
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </el-card>

        <!-- Item list -->
        <div v-loading="loading" style="flex: 1; overflow-y: auto; min-height: 200px">
          <div v-if="items.length === 0 && !loading" style="text-align: center; padding: 80px 0; color: #94a3b8">暂无知识条目</div>
          <div v-for="item in items" :key="item.id" class="knowledge-card" @click="openDetail(item)">
            <div style="display: flex; justify-content: space-between; align-items: flex-start">
              <div style="flex: 1; min-width: 0">
                <div style="font-weight: 600; font-size: 15px; margin-bottom: 4px">{{ item.title }}</div>
                <div style="color: #94a3b8; font-size: 13px; margin-bottom: 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">{{ item.summary || '无摘要' }}</div>
                <div class="flex flex-wrap items-center gap-2">
                  <el-tag :type="typeTagType(item.itemType)" :style="typeTagStyle(item.itemType)">{{ typeLabel(item.itemType) }}</el-tag>
                  <el-tag v-if="item.source" type="info">{{ item.source }}</el-tag>
                  <span v-if="item.publishDate" style="color: #cbd5e1; font-size: 12px">{{ item.publishDate }}</span>
                  <span v-if="item.authors" style="color: #cbd5e1; font-size: 12px">{{ item.authors }}</span>
                </div>
              </div>
              <el-tag :type="item.status === 'PUBLISHED' ? 'success' : 'warning'">
                {{ item.status === 'PUBLISHED' ? '已发布' : '草稿' }}
              </el-tag>
            </div>
          </div>
        </div>
        <el-pagination
          v-if="pagination.total > pagination.pageSize"
          class="mt-3 justify-end"
          size="small"
          background
          layout="prev, pager, next"
          :total="pagination.total"
          :current-page="pagination.current"
          :page-size="pagination.pageSize"
          @current-change="fetchItems"
        />
      </div>
    </div>

    <!-- Detail drawer -->
    <el-drawer v-model="drawerVisible" :title="currentItem?.title || '知识详情'" size="700px" :destroy-on-close="true">
      <template v-if="currentItem">
        <el-descriptions border :column="2" size="small" style="margin-bottom: 16px">
          <el-descriptions-item label="类型">{{ typeLabel(currentItem.itemType) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentItem.status === 'PUBLISHED' ? 'success' : 'warning'">{{ currentItem.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="来源">{{ currentItem.source || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发布日期">{{ currentItem.publishDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="作者" :span="2">{{ currentItem.authors || '-' }}</el-descriptions-item>
          <el-descriptions-item label="摘要" :span="2">{{ currentItem.summary || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="currentItem.content" style="background: #f8fafc; padding: 16px; border-radius: 6px; margin-bottom: 16px; white-space: pre-wrap; font-size: 14px; line-height: 1.8; max-height: 400px; overflow-y: auto">{{ currentItem.content }}</div>

        <div v-if="currentItem.fileName" style="margin-bottom: 16px">
          <el-button link type="primary"><el-icon class="mr-1"><Paperclip /></el-icon>{{ currentItem.fileName }}</el-button>
        </div>

        <el-divider>关联概念</el-divider>
        <el-table :data="conceptLinks" v-loading="conceptsLoading" row-key="id" size="small">
          <el-table-column label="概念编码" prop="conceptCode" width="140" />
          <el-table-column label="概念名称" prop="conceptName" />
          <el-table-column label="关联" width="80">
            <template #default="{ row }">
              <el-tag>{{ row.relevance }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70">
            <template #default="{ row }">
              <el-popconfirm title="确认取消关联？" @confirm="handleRemoveConcept(row.id)">
                <template #reference>
                  <el-button link type="danger" size="small">移除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-button plain class="w-full" style="margin-top: 8px; border-style: dashed" @click="conceptModalVisible = true">
          <el-icon class="mr-1"><Plus /></el-icon> 关联概念
        </el-button>
      </template>
    </el-drawer>

    <!-- Create/Edit knowledge item modal -->
    <el-dialog v-model="itemModalVisible" :title="isEdit ? '编辑知识条目' : '新增知识条目'"
      width="700px" :destroy-on-close="true">
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemFormRules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="itemForm.title" placeholder="知识条目标题" />
        </el-form-item>
        <div class="grid grid-cols-2 gap-x-4">
          <el-form-item label="类型" prop="itemType" label-width="90px">
            <el-select v-model="itemForm.itemType" placeholder="选择类型" class="w-full">
              <el-option value="GUIDELINE" label="临床指南" />
              <el-option value="LITERATURE" label="文献" />
              <el-option value="CONSENSUS" label="专家共识" />
              <el-option value="STANDARD" label="标准规范" />
              <el-option value="OTHER" label="其他" />
            </el-select>
          </el-form-item>
          <el-form-item label="分类" label-width="90px">
            <el-select v-model="itemForm.categoryId" placeholder="选择分类" clearable class="w-full">
              <el-option v-for="cat in categories" :key="cat.id" :value="cat.id" :label="cat.name" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="作者">
          <el-input v-model="itemForm.authors" placeholder="作者（多人用逗号分隔）" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="itemForm.source" placeholder="如：中华医学会、NEJM" />
        </el-form-item>
        <el-form-item label="发布日期">
          <el-date-picker v-model="itemForm.publishDate" type="date" value-format="YYYY-MM-DD" class="w-full" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="itemForm.summary" type="textarea" :rows="3" placeholder="内容摘要" />
        </el-form-item>
        <el-form-item label="正文内容">
          <el-input v-model="itemForm.content" type="textarea" :rows="6" placeholder="知识正文" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="itemForm.status">
            <el-radio value="DRAFT">草稿</el-radio>
            <el-radio value="PUBLISHED">发布</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleItemSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Category modal -->
    <el-dialog v-model="catModalVisible" title="新增分类" width="520px" :destroy-on-close="true">
      <el-form label-width="100px">
        <el-form-item label="分类名称">
          <el-input v-model="newCatName" placeholder="如：临床指南、循证文献" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="catModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCatSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Associate concept modal -->
    <el-dialog v-model="conceptModalVisible" title="关联概念" width="520px" :destroy-on-close="true">
      <el-form label-width="100px">
        <el-form-item label="概念ID">
          <el-input-number v-model="associateConceptId" placeholder="输入概念ID" class="w-full" />
        </el-form-item>
        <el-form-item label="关联类型">
          <el-select v-model="associateRelevance" class="w-full">
            <el-option value="PRIMARY" label="主要" />
            <el-option value="RELATED" label="相关" />
            <el-option value="REFERENCE" label="参考" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="conceptModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssociateConcept">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Paperclip, Search } from '@element-plus/icons-vue'
import { type FormInstance, type FormItemRule } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import {
  getKnowledgeCategories, createKnowledgeCategory, deleteKnowledgeCategory,
  getKnowledgeItems, getKnowledgeItem, createKnowledgeItem, updateKnowledgeItem,
  deleteKnowledgeItem, getKnowledgeConcepts, associateConcept, removeAssociation,
} from '@/api/masterdata'

defineOptions({ name: 'KnowledgeList' })

type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'
const PURPLE_TAG_STYLE = { color: '#8b5cf6', background: '#f5f3ff', borderColor: '#ddd6fe' }

const TYPE_MAP: Record<string, string> = { GUIDELINE: '临床指南', LITERATURE: '文献', CONSENSUS: '专家共识', STANDARD: '标准规范', OTHER: '其他' }
const TYPE_COLOR: Record<string, TagType> = { GUIDELINE: 'primary', LITERATURE: 'success', CONSENSUS: 'info', STANDARD: 'warning', OTHER: 'info' }
const typeLabel = (t: string) => TYPE_MAP[t] || t
const typeTagType = (t: string): TagType => TYPE_COLOR[t] || 'info'
const typeTagStyle = (t: string) => (t === 'CONSENSUS' ? PURPLE_TAG_STYLE : undefined)

// ── Categories ──
const categories = ref<any[]>([])
const selectedCategoryId = ref<number | null>(null)
const catModalVisible = ref(false)
const newCatName = ref('')

async function fetchCategories() {
  const res = await getKnowledgeCategories()
  categories.value = res.data.data || []
}

function selectCategory(id: number | null) {
  selectedCategoryId.value = id
  fetchItems(1)
}

function handleCreateCategory() {
  newCatName.value = ''
  catModalVisible.value = true
}

async function handleCatSubmit() {
  if (!newCatName.value.trim()) return ElMessage.warning('请输入分类名称')
  await createKnowledgeCategory({ name: newCatName.value.trim() })
  ElMessage.success('分类创建成功')
  catModalVisible.value = false
  fetchCategories()
}

async function confirmDeleteCategory(cat: any) {
  await deleteKnowledgeCategory(cat.id)
  if (selectedCategoryId.value === cat.id) selectedCategoryId.value = null
  fetchCategories()
  fetchItems(1)
}

function handleDeleteCategory(cat: any) {
  ElMessageBox.confirm(`确认删除分类「${cat.name}」？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => confirmDeleteCategory(cat)).catch(() => {})
}

// ── Knowledge Items ──
const loading = ref(false)
const items = ref<any[]>([])
const filters = reactive({ itemType: undefined as string | undefined, keyword: '' })
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

async function fetchItems(page = 1) {
  loading.value = true
  try {
    const params: any = { page, pageSize: pagination.pageSize }
    if (selectedCategoryId.value) params.categoryId = selectedCategoryId.value
    if (filters.itemType) params.itemType = filters.itemType
    if (filters.keyword) params.keyword = filters.keyword
    const res = await getKnowledgeItems(params)
    const data = res.data.data
    items.value = data?.content || data?.items || []
    pagination.total = data?.totalElements || data?.total || 0
    pagination.current = (data?.number ?? page - 1) + 1
  } finally { loading.value = false }
}

function resetFilters() {
  filters.itemType = undefined
  filters.keyword = ''
  fetchItems(1)
}

// ── Item CRUD ──
const itemModalVisible = ref(false)
const editingItemId = ref<number | null>(null)
const isEdit = computed(() => editingItemId.value !== null)
const itemFormRef = ref<FormInstance>()
const itemForm = reactive({
  title: '', categoryId: undefined as number | undefined, itemType: 'GUIDELINE',
  summary: '', content: '', source: '', authors: '',
  publishDate: undefined as any, status: 'DRAFT',
})
const itemFormRules: Record<string, FormItemRule[]> = {
  title: [{ required: true, message: '请输入标题' }],
  itemType: [{ required: true, message: '请选择类型' }],
}

function handleCreateItem() {
  editingItemId.value = null
  Object.assign(itemForm, { title: '', categoryId: selectedCategoryId.value, itemType: 'GUIDELINE', summary: '', content: '', source: '', authors: '', publishDate: undefined, status: 'DRAFT' })
  itemModalVisible.value = true
}

async function openDetail(item: any) {
  drawerVisible.value = true
  try {
    const res = await getKnowledgeItem(item.id)
    currentItem.value = res.data.data
  } catch { currentItem.value = item }
  loadConceptLinks(item.id)
}

async function handleItemSubmit() {
  await itemFormRef.value?.validate()
  const data = { ...itemForm }
  if (isEdit.value) {
    await updateKnowledgeItem(editingItemId.value!, data)
    ElMessage.success('更新成功')
  } else {
    await createKnowledgeItem(data)
    ElMessage.success('创建成功')
  }
  itemModalVisible.value = false
  fetchItems()
}

// ── Detail drawer ──
const drawerVisible = ref(false)
const currentItem = ref<any>(null)
const conceptLinks = ref<any[]>([])
const conceptsLoading = ref(false)

async function loadConceptLinks(knowledgeId: number) {
  conceptsLoading.value = true
  try {
    const res = await getKnowledgeConcepts(knowledgeId)
    conceptLinks.value = res.data.data || []
  } finally { conceptsLoading.value = false }
}

const conceptModalVisible = ref(false)
const associateConceptId = ref<number>()
const associateRelevance = ref('RELATED')

async function handleAssociateConcept() {
  if (!associateConceptId.value) return ElMessage.warning('请输入概念ID')
  await associateConcept(currentItem.value.id, { conceptId: associateConceptId.value, relevance: associateRelevance.value })
  ElMessage.success('关联成功')
  conceptModalVisible.value = false
  loadConceptLinks(currentItem.value.id)
}

async function handleRemoveConcept(linkId: number) {
  await removeAssociation(linkId)
  ElMessage.success('已移除')
  loadConceptLinks(currentItem.value.id)
}

onMounted(() => { fetchCategories(); fetchItems() })
</script>

<style scoped>
.cat-item {
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2px;
  transition: background 0.2s;
}
.cat-item:hover { background: #f8fafc; }
.cat-item:hover > button { opacity: 1 !important; }
.cat-item.active { background: #f0f9ff; color: #0ea5e9; font-weight: 600; }

.knowledge-card {
  padding: 16px;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: border-color 0.2s;
}
.knowledge-card:hover { border-color: #7dd3fc; }
</style>
