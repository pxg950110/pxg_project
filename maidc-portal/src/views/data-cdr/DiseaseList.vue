<template>
  <PageContainer title="专病管理">
    <template #extra>
      <a-button type="primary" @click="openCreateModal">
        <template #icon><PlusOutlined /></template> 新建专病库
      </a-button>
    </template>

    <div class="search-bar">
      <a-input-search
        v-model:value="keyword"
        placeholder="搜索专病库名称"
        style="width: 280px"
        @search="loadData"
        allow-clear
      />
      <a-select v-model:value="statusFilter" style="width: 120px" placeholder="状态" allow-clear @change="loadData">
        <a-select-option value="ACTIVE">已启用</a-select-option>
        <a-select-option value="INACTIVE">未启用</a-select-option>
      </a-select>
    </div>

    <a-spin :spinning="loading">
      <div class="card-grid">
        <DiseaseCard
          v-for="item in list"
          :key="item.id"
          :data="item"
          @edit="openEditModal(item)"
          @detail="goDetail(item)"
          @sync="handleSync(item)"
        />
        <a-empty v-if="!loading && list.length === 0" description="暂无专病库" style="grid-column: 1/-1; padding: 60px 0" />
      </div>
    </a-spin>

    <div class="pagination-wrap" v-if="total > pageSize">
      <a-pagination v-model:current="page" :total="total" :page-size="pageSize" @change="loadData" show-quick-jumper />
    </div>

    <!-- Create / Edit Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑专病库' : '新建专病库'"
      width="720px"
      @ok="handleSubmit"
      :confirm-loading="submitting"
    >
      <a-form layout="vertical">
        <a-form-item label="专病名称" required>
          <a-auto-complete
            v-model:value="form.name"
            :options="templateOptions"
            placeholder="输入疾病名称搜索模板"
            @search="handleTemplateSearch"
            @select="handleTemplateSelect"
          />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" :rows="2" placeholder="可选" />
        </a-form-item>
        <a-form-item label="纳入规则" required>
          <ConditionBuilder v-model="form.inclusionRules" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="form.status" style="width: 140px">
            <a-select-option value="ACTIVE">已启用</a-select-option>
            <a-select-option value="INACTIVE">未启用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
      <template #footer>
        <a-button @click="handlePreviewMatch" :loading="previewing" v-if="editingId">
          测试匹配 ({{ previewCount !== null ? previewCount + '人' : '' }})
        </a-button>
        <a-button @click="modalVisible = false">取消</a-button>
        <a-button type="primary" @click="handleSubmit" :loading="submitting">确定</a-button>
      </template>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import DiseaseCard from '@/components/DiseaseCard/index.vue'
import ConditionBuilder from '@/components/ConditionBuilder/index.vue'
import {
  getDiseaseCohorts, createDiseaseCohort, updateDiseaseCohort,
  deleteDiseaseCohort, syncDiseaseCohort, previewDiseaseCohort,
  searchDiseaseTemplates,
} from '@/api/data'

const router = useRouter()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 12
const keyword = ref('')
const statusFilter = ref<string | undefined>(undefined)

const modalVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const previewing = ref(false)
const previewCount = ref<number | null>(null)
const templateOptions = ref<{ value: string; label: string; template: any }[]>([])

const form = ref<{
  name: string
  description: string
  status: string
  inclusionRules: any
}>({
  name: '',
  description: '',
  status: 'ACTIVE',
  inclusionRules: null,
})

async function loadData() {
  loading.value = true
  try {
    const res = await getDiseaseCohorts({
      page: page.value,
      page_size: pageSize,
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
    })
    list.value = res.data?.data?.content || []
    total.value = res.data?.data?.totalElements || 0
  } catch (e: any) {
    message.error('加载失败: ' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

function openCreateModal() {
  editingId.value = null
  previewCount.value = null
  form.value = { name: '', description: '', status: 'ACTIVE', inclusionRules: null }
  modalVisible.value = true
}

function openEditModal(item: any) {
  editingId.value = item.id
  previewCount.value = null
  let rules = item.inclusionRules
  if (typeof rules === 'string') {
    try { rules = JSON.parse(rules) } catch { rules = null }
  }
  form.value = {
    name: item.name,
    description: item.description || '',
    status: item.status || 'ACTIVE',
    inclusionRules: rules,
  }
  modalVisible.value = true
}

function goDetail(item: any) {
  router.push({ name: 'DiseaseDetail', params: { id: item.id } })
}

async function handleSync(item: any) {
  Modal.confirm({
    title: `确认同步「${item.name}」？`,
    content: '将根据纳入规则重新匹配患者',
    onOk: async () => {
      try {
        await syncDiseaseCohort(item.id)
        message.success('同步已触发')
        loadData()
      } catch (e: any) {
        message.error('同步失败: ' + (e.message || ''))
      }
    },
  })
}

async function handleSubmit() {
  if (!form.value.name) { message.warning('请输入专病库名称'); return }
  if (!form.value.inclusionRules) { message.warning('请设置纳入规则'); return }
  submitting.value = true
  try {
    const payload = {
      name: form.value.name,
      description: form.value.description,
      status: form.value.status,
      inclusionRules: JSON.stringify(form.value.inclusionRules),
    }
    if (editingId.value) {
      await updateDiseaseCohort(editingId.value, payload)
      message.success('更新成功')
    } else {
      await createDiseaseCohort(payload)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch (e: any) {
    message.error('操作失败: ' + (e.message || ''))
  } finally {
    submitting.value = false
  }
}

async function handlePreviewMatch() {
  if (!editingId.value) return
  previewing.value = true
  try {
    const res = await previewDiseaseCohort(editingId.value)
    previewCount.value = res.data?.data?.patientCount ?? 0
  } catch {
    previewCount.value = null
  } finally {
    previewing.value = false
  }
}

let searchTimer: any = null
async function handleTemplateSearch(val: string) {
  if (searchTimer) clearTimeout(searchTimer)
  if (!val) { templateOptions.value = []; return }
  searchTimer = setTimeout(async () => {
    try {
      const res = await searchDiseaseTemplates(val)
      const items = res.data?.data || []
      templateOptions.value = items.map((t: any) => ({
        value: t.diseaseName,
        label: t.diseaseName,
        template: t,
      }))
    } catch { /* ignore */ }
  }, 300)
}

function handleTemplateSelect(val: string, option: any) {
  const tpl = option.template
  if (tpl?.inclusionTemplate) {
    try {
      form.value.inclusionRules = typeof tpl.inclusionTemplate === 'string'
        ? JSON.parse(tpl.inclusionTemplate)
        : tpl.inclusionTemplate
    } catch { /* ignore parse error */ }
  }
}

onMounted(loadData)
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
