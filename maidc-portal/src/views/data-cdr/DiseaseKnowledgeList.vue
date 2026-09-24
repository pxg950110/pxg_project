<template>
  <div class="p-6 space-y-6 max-w-[1600px] mx-auto">
    <!-- 顶部标题与行动区 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-bold text-slate-900 tracking-tight m-0 flex items-center gap-2.5">
          <span class="w-2 h-5 bg-indigo-500 rounded-full" />
          专病知识库空间
        </h2>
        <p class="text-xs text-slate-500 mt-1 m-0">
          汇聚专病临床指南、专家共识、学术文献与量表标准，通过 AI 深度解析构建结构化循证知识库并驱动智能问答。
        </p>
      </div>

      <div class="flex items-center gap-3">
        <el-button
          v-if="hasPermission('cdr:diseasekb:manage')"
          type="primary"
          class="!rounded-xl shadow-clinical-sm hover:shadow-clinical"
          @click="openCreateModal"
        >
          <el-icon class="mr-1.5"><Plus /></el-icon>
          新建知识空间
        </el-button>
      </div>
    </div>

    <!-- 筛选过滤与工具栏 -->
    <div class="bg-white rounded-2xl border border-slate-200/80 p-4 shadow-clinical-sm flex flex-wrap items-center justify-between gap-4">
      <div class="flex items-center gap-3 flex-wrap flex-1">
        <el-input
          v-model="keyword"
          placeholder="搜索专病空间名称、ICD编码或简介..."
          clearable
          class="!w-72"
          @clear="loadData"
          @keyup.enter="loadData"
        >
          <template #prefix>
            <el-icon class="text-slate-400"><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="statusFilter"
          placeholder="空间状态"
          clearable
          class="!w-36"
          @change="loadData"
        >
          <el-option label="全部状态" value="" />
          <el-option label="已启用" value="ACTIVE" />
          <el-option label="已停用" value="INACTIVE" />
        </el-select>

        <el-button @click="loadData">
          <el-icon class="mr-1"><Refresh /></el-icon>
          查询
        </el-button>
      </div>

      <div class="flex items-center gap-2">
        <span class="text-xs text-slate-400">共计 {{ total }} 个专病知识空间</span>
      </div>
    </div>

    <!-- 知识空间卡片网格 -->
    <div v-loading="loading" class="min-h-[360px]">
      <div v-if="!loading && list.length === 0" class="py-16 text-center bg-white rounded-2xl border border-slate-200/80">
        <el-icon :size="48" class="text-slate-300 mb-3"><Reading /></el-icon>
        <p class="text-sm font-medium text-slate-600 mb-1">暂无专病知识空间</p>
        <p class="text-xs text-slate-400 mb-4">您可以新建一个知识空间并关联专病队列，上传指南与文献开启 AI 循证构建。</p>
        <el-button
          v-if="hasPermission('cdr:diseasekb:manage')"
          type="primary"
          size="small"
          @click="openCreateModal"
        >
          立即新建知识空间
        </el-button>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
        <div
          v-for="space in list"
          :key="space.id"
          class="group relative flex flex-col justify-between rounded-2xl border border-slate-200/80 bg-white p-5 shadow-clinical-sm transition-all duration-200 hover:-translate-y-1 hover:shadow-clinical hover:border-indigo-300/80 cursor-pointer overflow-hidden"
          @click="goDetail(space)"
        >
          <!-- 顶部个性化主题强调线 -->
          <div
            class="absolute top-0 left-0 right-0 h-1 transition-opacity"
            :style="{ background: space.iconColor || '#6366f1' }"
          />

          <!-- 头部标题与状态 -->
          <div>
            <div class="flex items-start justify-between gap-3 mb-3">
              <div class="flex items-center gap-3 min-w-0">
                <div
                  class="flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-xl text-white font-bold text-sm shadow-clinical-sm"
                  :style="{ background: space.iconColor || '#6366f1' }"
                >
                  <el-icon :size="20"><Reading /></el-icon>
                </div>
                <div class="min-w-0">
                  <h4 class="text-base font-semibold text-slate-900 truncate leading-snug group-hover:text-indigo-600 transition-colors m-0">
                    {{ space.name }}
                  </h4>
                  <p class="text-xs text-slate-400 truncate mt-0.5 m-0 font-mono">
                    更新时间：{{ formatDate(space.updatedAt) }}
                  </p>
                </div>
              </div>

              <el-tag
                size="small"
                :type="space.status === 'ACTIVE' ? 'success' : 'info'"
                effect="light"
                class="!rounded font-medium"
              >
                {{ space.status === 'ACTIVE' ? '已启用' : '已停用' }}
              </el-tag>
            </div>

            <!-- 描述 -->
            <p class="text-xs text-slate-500 line-clamp-2 h-8 leading-relaxed mb-3">
              {{ space.description || '暂无详细描述，支持收纳临床指南、专家共识与多轮 AI 循证问答。' }}
            </p>

            <!-- 队列关联标签 -->
            <div class="mb-3">
              <div
                v-if="space.cohortId"
                class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-sky-50 border border-sky-200/80 text-xs text-sky-700 font-medium"
              >
                <el-icon><Connection /></el-icon>
                已联动临床队列 #{{ space.cohortId }}
              </div>
              <div
                v-else
                class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-50 border border-slate-200 text-xs text-slate-400"
              >
                <el-icon><InfoFilled /></el-icon>
                未关联特定队列（通用专病库）
              </div>
            </div>

            <!-- ICD 标签组 -->
            <div v-if="space.icdCodes?.length" class="flex flex-wrap gap-1.5 mb-2">
              <el-tag
                v-for="c in space.icdCodes.slice(0, 4)"
                :key="c"
                size="small"
                type="info"
                effect="plain"
                class="!rounded font-mono font-medium !text-[11px]"
              >
                {{ c }}
              </el-tag>
              <span v-if="space.icdCodes.length > 4" class="text-[11px] text-slate-400 self-center">
                +{{ space.icdCodes.length - 4 }}
              </span>
            </div>
          </div>

          <!-- 底部操作栏 -->
          <div class="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs" @click.stop>
            <span class="text-slate-400">ID: #{{ space.id }}</span>
            <div class="flex items-center gap-2">
              <el-button link type="primary" size="small" class="!text-xs font-semibold" @click="goDetail(space)">
                进入空间 &rarr;
              </el-button>
              <el-button
                v-if="hasPermission('cdr:diseasekb:manage')"
                link
                type="default"
                size="small"
                class="!text-xs text-slate-500 hover:text-indigo-600"
                @click="openEditModal(space)"
              >
                编辑
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页控制 -->
      <div v-if="total > pageSize" class="flex justify-end mt-6">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next, jumper"
          background
          @current-change="loadData"
        />
      </div>
    </div>

    <!-- 新建 / 编辑知识空间弹窗 -->
    <el-dialog
      v-model="modalVisible"
      :title="editingId ? '编辑专病知识空间' : '新建专病知识空间'"
      width="600px"
      destroy-on-close
      class="!rounded-2xl"
    >
      <el-form label-position="top" class="space-y-3">
        <el-form-item label="专病空间名称" required>
          <el-input v-model="form.name" placeholder="如：2型糖尿病专病知识库、慢性心衰诊疗指南库" maxlength="128" />
        </el-form-item>

        <el-form-item label="ICD-10 疾病编码绑定">
          <el-select
            v-model="form.icdCodes"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入 ICD-10 编码后按回车，如 E11 / I50"
            class="w-full"
          />
        </el-form-item>

        <el-form-item label="关联专病队列 (选填)">
          <el-select
            v-model="form.cohortId"
            placeholder="可选；绑定后可直接穿梭查看队列入组画像"
            clearable
            filterable
            class="w-full"
          >
            <el-option
              v-for="c in cohortOptions"
              :key="c.value"
              :label="c.label"
              :value="c.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="专病空间描述与建库目标">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="简要说明该专病知识空间的收录范畴、临床适用范围等（选填）"
          />
        </el-form-item>

        <el-form-item label="主题辨识色">
          <div class="flex items-center gap-3">
            <div
              v-for="c in palette"
              :key="c"
              class="w-7 h-7 rounded-full cursor-pointer transition-all duration-150 flex items-center justify-center border-2"
              :class="form.iconColor === c ? 'scale-110 border-slate-900 shadow-md' : 'border-transparent hover:scale-105'"
              :style="{ background: c }"
              @click="form.iconColor = c"
            >
              <el-icon v-if="form.iconColor === c" class="text-white text-xs"><Check /></el-icon>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="flex items-center justify-end gap-2">
          <el-button @click="modalVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ editingId ? '保存修改' : '立即创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Plus,
  Search,
  Refresh,
  Reading,
  Connection,
  InfoFilled,
  Check,
} from '@element-plus/icons-vue'
import { getDiseaseCohorts } from '@/api/data'
import {
  getDiseaseKbSpaces,
  createDiseaseKbSpace,
  updateDiseaseKbSpace,
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
const statusFilter = ref<string>('')

const modalVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)

const palette = ['#6366f1', '#0ea5e9', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899', '#06b6d4']
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
  } catch (e: any) {
    ElMessage.error('获取知识空间列表失败: ' + (e.message || ''))
  } finally {
    loading.value = false
  }
}

async function loadCohorts() {
  try {
    const res = await getDiseaseCohorts({ page: 1, page_size: 100, status: 'ACTIVE' })
    cohortOptions.value = (res.data?.data?.content || []).map((c: any) => ({ value: c.id, label: c.name }))
  } catch {
    /* 队列加载失败不阻塞主流程 */
  }
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
    icdCodes: space.icdCodes ? [...space.icdCodes] : [],
    cohortId: space.cohortId ?? undefined,
    iconColor: space.iconColor ?? palette[0],
  }
  modalVisible.value = true
}

async function handleSubmit() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请填写专病空间名称')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) await updateDiseaseKbSpace(editingId.value, form.value)
    else await createDiseaseKbSpace(form.value)
    ElMessage.success(editingId.value ? '专病空间已更新' : '专病空间创建成功')
    modalVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error('保存失败: ' + (e.message || ''))
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
