<template>
  <PageContainer title="专病知识库">
    <template #extra>
      <a-button v-if="hasPermission('cdr:diseasekb:manage')" type="primary" @click="openCreateModal">
        <template #icon><PlusOutlined /></template> 新建知识空间
      </a-button>
    </template>

    <div class="search-bar">
      <a-input-search
        v-model:value="keyword"
        placeholder="搜索专病名称 / 关键词"
        style="width: 320px"
        @search="loadData"
        allow-clear
      />
      <a-select v-model:value="statusFilter" style="width: 120px" placeholder="状态" allow-clear @change="loadData">
        <a-select-option value="ACTIVE">已启用</a-select-option>
        <a-select-option value="INACTIVE">已停用</a-select-option>
      </a-select>
    </div>

    <a-spin :spinning="loading">
      <div class="card-grid">
        <div v-for="space in list" :key="space.id" class="kb-card" @click="goDetail(space)">
          <div class="kb-card__head" :style="{ background: space.iconColor || '#2d5afa' }">
            <span class="kb-card__name">{{ space.name }}</span>
            <a-tag v-if="space.status !== 'ACTIVE'" color="default">已停用</a-tag>
          </div>
          <div class="kb-card__body">
            <div class="kb-card__line cohort" v-if="space.cohortId">
              <a-tag color="blue">队列已关联</a-tag>
            </div>
            <div class="kb-card__line cohort off" v-else>
              <a-tag color="default">未关联队列</a-tag>
            </div>
            <div class="kb-card__stats" v-if="space.icdCodes?.length">
              <a-tag v-for="c in space.icdCodes" :key="c" color="geekblue">{{ c }}</a-tag>
            </div>
            <div class="kb-card__footer">
              <span class="muted">更新 {{ formatDate(space.updatedAt) }}</span>
              <span @click.stop>
                <a-button type="link" size="small" @click="goDetail(space)">进入</a-button>
                <a-button
                  v-if="hasPermission('cdr:diseasekb:manage')" type="link" size="small"
                  @click="openEditModal(space)"
                >编辑</a-button>
              </span>
            </div>
          </div>
        </div>
        <a-empty
          v-if="!loading && list.length === 0" description="暂无知识空间，点击右上角新建"
          style="grid-column: 1/-1; padding: 60px 0"
        />
      </div>
    </a-spin>

    <div class="pagination-wrap" v-if="total > pageSize">
      <a-pagination v-model:current="page" :total="total" :page-size="pageSize" @change="loadData" show-quick-jumper />
    </div>

    <!-- 新建 / 编辑空间 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑知识空间' : '新建知识空间'"
      width="640px"
      :confirm-loading="submitting"
      @ok="handleSubmit"
    >
      <a-form layout="vertical">
        <a-form-item label="专病名称" required>
          <a-input v-model:value="form.name" placeholder="如：2型糖尿病" :maxlength="128" />
        </a-form-item>
        <a-form-item label="ICD-10 编码绑定">
          <a-select
            v-model:value="form.icdCodes"
            mode="tags"
            placeholder="输入 ICD-10 编码后回车，如 E11"
            :token-separators="[',', ' ']"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="关联专病队列">
          <a-select
            v-model:value="form.cohortId"
            placeholder="可选；留空则不关联"
            :options="cohortOptions"
            allow-clear
            show-search
            option-filter-prop="label"
          />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" :rows="2" placeholder="可选" />
        </a-form-item>
        <a-form-item label="图标颜色">
          <div class="color-row">
            <span
              v-for="c in palette" :key="c" class="color-dot" :class="{ active: form.iconColor === c }"
              :style="{ background: c }" @click="form.iconColor = c"
            />
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { getDiseaseCohorts } from '@/api/data'
import {
  getDiseaseKbSpaces, createDiseaseKbSpace, updateDiseaseKbSpace,
} from '@/api/diseaseKb'
import { usePermission } from '@/hooks/usePermission'

const router = useRouter()
const { hasPermission } = usePermission()

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

const palette = ['#2d5afa', '#00ab44', '#ff8c00', '#722ed1', '#13c2c2', '#eb2f96']
const cohortOptions = ref<{ value: number; label: string }[]>([])

const form = ref<{
  name: string
  description: string
  icdCodes: string[]
  cohortId: number | undefined
  iconColor: string
}>({
  name: '',
  description: '',
  icdCodes: [],
  cohortId: undefined,
  iconColor: palette[0],
})

async function loadData() {
  loading.value = true
  try {
    const res = await getDiseaseKbSpaces({
      page: page.value,
      page_size: pageSize,
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
    })
    list.value = res.data?.data?.content || []
    total.value = res.data?.data?.totalElements || 0
  } finally {
    loading.value = false
  }
}

async function loadCohorts() {
  try {
    const res = await getDiseaseCohorts({ page: 1, page_size: 100, status: 'ACTIVE' })
    cohortOptions.value = (res.data?.data?.content || []).map((c: any) => ({ value: c.id, label: c.name }))
  } catch { /* 队列下拉加载失败不阻塞页面 */ }
}

function openCreateModal() {
  editingId.value = null
  form.value = { name: '', description: '', icdCodes: [], cohortId: undefined, iconColor: palette[0] }
  modalVisible.value = true
}

function openEditModal(space: any) {
  editingId.value = space.id
  form.value = {
    name: space.name,
    description: space.description ?? '',
    icdCodes: space.icdCodes ?? [],
    cohortId: space.cohortId ?? undefined,
    iconColor: space.iconColor ?? palette[0],
  }
  modalVisible.value = true
}

async function handleSubmit() {
  if (!form.value.name.trim()) {
    message.warning('请填写专病名称')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) await updateDiseaseKbSpace(editingId.value, form.value)
    else await createDiseaseKbSpace(form.value)
    message.success(editingId.value ? '已更新' : '创建成功')
    modalVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function goDetail(space: any) {
  router.push({ name: 'DiseaseKnowledgeDetail', params: { id: space.id } })
}

function formatDate(v?: string) {
  return v ? String(v).slice(0, 10) : '-'
}

onMounted(() => {
  loadData()
  loadCohorts()
})
</script>

<style scoped>
.search-bar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.kb-card { border: 1px solid #f0f0f0; border-radius: 8px; overflow: hidden; cursor: pointer; transition: box-shadow .2s; background: #fff; }
.kb-card:hover { box-shadow: 0 4px 16px rgba(0, 0, 0, .08); }
.kb-card__head { padding: 14px 16px; color: #fff; display: flex; justify-content: space-between; align-items: center; }
.kb-card__name { font-size: 16px; font-weight: 600; }
.kb-card__body { padding: 12px 16px; }
.kb-card__line { margin-bottom: 8px; font-size: 13px; }
.kb-card__line.off { color: #999; }
.kb-card__stats { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 8px; }
.kb-card__footer { display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #f5f5f5; padding-top: 8px; }
.muted { color: #999; font-size: 12px; }
.color-row { display: flex; gap: 10px; }
.color-dot { width: 24px; height: 24px; border-radius: 50%; cursor: pointer; border: 2px solid transparent; }
.color-dot.active { border-color: #333; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
