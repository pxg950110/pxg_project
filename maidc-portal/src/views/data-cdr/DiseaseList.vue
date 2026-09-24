<template>
  <div class="p-6 space-y-6 max-w-[1600px] mx-auto">
    <!-- 顶部标题与行动区 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-bold text-slate-900 tracking-tight m-0 flex items-center gap-2.5">
          <span class="w-2 h-5 bg-sky-500 rounded-full" />
          专病库管理
        </h2>
        <p class="text-xs text-slate-500 mt-1 m-0">
          遵循「先构建专病实体，再配置纳排过滤规则」临床体系，纳排多模态患者队列并驱动精准随访与科研。
        </p>
      </div>

      <div class="flex items-center gap-3">
        <el-button
          type="primary"
          class="!rounded-xl shadow-clinical-sm hover:shadow-clinical"
          @click="openCreateDiseaseModal"
        >
          <el-icon class="mr-1.5"><Plus /></el-icon>
          新建专病库
        </el-button>
      </div>
    </div>

    <!-- 顶部专病态势概览 KPI 指标卡片 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-clinical-sm flex items-center justify-between">
        <div>
          <div class="text-xs font-medium text-slate-500">已立项专病库</div>
          <div class="flex items-baseline gap-1 mt-1">
            <span class="text-2xl font-bold font-mono text-slate-900">{{ stats.totalCohorts }}</span>
            <span class="text-xs text-slate-400">个病种</span>
          </div>
        </div>
        <div class="w-11 h-11 rounded-xl bg-sky-50 text-sky-600 flex items-center justify-center">
          <el-icon :size="22"><MedicineBoxOutlined /></el-icon>
        </div>
      </div>

      <div class="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-clinical-sm flex items-center justify-between">
        <div>
          <div class="text-xs font-medium text-slate-500">在管入组患者规模</div>
          <div class="flex items-baseline gap-1 mt-1">
            <span class="text-2xl font-bold font-mono text-emerald-600">{{ formatNumber(stats.totalPatients) }}</span>
            <span class="text-xs text-slate-400">例</span>
          </div>
        </div>
        <div class="w-11 h-11 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
          <el-icon :size="22"><UserFilled /></el-icon>
        </div>
      </div>

      <div class="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-clinical-sm flex items-center justify-between">
        <div>
          <div class="text-xs font-medium text-slate-500">已配置纳排过滤</div>
          <div class="flex items-baseline gap-1 mt-1">
            <span class="text-2xl font-bold font-mono text-indigo-600">{{ stats.configuredCohorts }}</span>
            <span class="text-xs text-slate-400">个已生效</span>
          </div>
        </div>
        <div class="w-11 h-11 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
          <el-icon :size="22"><Filter /></el-icon>
        </div>
      </div>

      <div class="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-clinical-sm flex items-center justify-between">
        <div>
          <div class="text-xs font-medium text-slate-500">待配置规则专病</div>
          <div class="flex items-baseline gap-1 mt-1">
            <span class="text-2xl font-bold font-mono text-amber-600">{{ stats.pendingCohorts }}</span>
            <span class="text-xs text-slate-400">个需完善</span>
          </div>
        </div>
        <div class="w-11 h-11 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
          <el-icon :size="22"><WarningFilled /></el-icon>
        </div>
      </div>
    </div>

    <!-- 筛选过滤与工具栏 -->
    <div class="bg-white rounded-2xl border border-slate-200/80 p-4 shadow-clinical-sm flex flex-wrap items-center justify-between gap-4">
      <div class="flex items-center gap-3 flex-wrap flex-1">
        <el-input
          v-model="keyword"
          placeholder="搜索专病库名称或描述..."
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
          placeholder="启用状态"
          clearable
          class="!w-36"
          @change="loadData"
        >
          <el-option label="全部状态" value="" />
          <el-option label="已启用" value="ACTIVE" />
          <el-option label="未启用" value="INACTIVE" />
        </el-select>

        <el-select
          v-model="ruleFilter"
          placeholder="规则配置状态"
          clearable
          class="!w-40"
          @change="applyRuleFilter"
        >
          <el-option label="全部配置状态" value="ALL" />
          <el-option label="已配置规则" value="CONFIGURED" />
          <el-option label="待配置规则" value="PENDING" />
        </el-select>

        <el-button @click="loadData">
          <el-icon class="mr-1"><Refresh /></el-icon>
          查询
        </el-button>
      </div>

      <div class="flex items-center gap-2">
        <span class="text-xs text-slate-400">共计 {{ total }} 个专病库</span>
      </div>
    </div>

    <!-- 专病卡片网格列表 -->
    <div v-loading="loading" class="min-h-[360px]">
      <div v-if="filteredList.length === 0 && !loading" class="py-16 text-center bg-white rounded-2xl border border-slate-200/80">
        <el-icon :size="48" class="text-slate-300 mb-3"><FolderOpened /></el-icon>
        <p class="text-sm font-medium text-slate-600 mb-1">暂无符合条件的专病库</p>
        <p class="text-xs text-slate-400 mb-4">您可以先构建一个专病档案，然后为其配置多模态纳入规则与随访方案。</p>
        <el-button type="primary" size="small" @click="openCreateDiseaseModal">
          立即新建专病档案
        </el-button>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
        <DiseaseCard
          v-for="item in filteredList"
          :key="item.id"
          :data="item"
          @edit="openEditDiseaseModal(item)"
          @detail="goDetail(item)"
          @sync="handleSync(item)"
          @delete="handleDelete(item)"
          @configure-rules="openConfigureRulesDrawer(item)"
          @knowledge="goKnowledge(item)"
          @followup="goFollowup(item)"
        />
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

    <!-- ================= 阶段 1：构建专病档案（新建/编辑专病实体）弹窗 ================= -->
    <el-dialog
      v-model="diseaseModalVisible"
      :title="editingDiseaseId ? '编辑专病档案' : '第一步：构建专病档案'"
      width="640px"
      destroy-on-close
      class="!rounded-2xl"
    >
      <div class="mb-4 p-3 rounded-xl bg-sky-50/70 border border-sky-100 text-xs text-sky-800 leading-relaxed">
        <div class="font-semibold mb-0.5 flex items-center gap-1.5">
          <el-icon class="text-sky-600"><InfoFilled /></el-icon>
          临床规范指引：
        </div>
        请先确立专病的基本信息、主要科室领域及建设目标。专病档案构建完成后，系统将引导您为其针对性配置多模态纳排过滤规则。
      </div>

      <el-form
        ref="diseaseFormRef"
        :model="diseaseForm"
        :rules="diseaseFormRules"
        label-position="top"
        class="space-y-3"
      >
        <el-form-item label="专病库名称" prop="name" required>
          <el-input
            v-model="diseaseForm.name"
            placeholder="例如：慢性心力衰竭专病库、原发性肺癌专病队列"
            maxlength="80"
            show-word-limit
          />
        </el-form-item>

        <div class="grid grid-cols-2 gap-4">
          <el-form-item label="所属临床专科 / 科室领域" prop="department">
            <el-select v-model="diseaseForm.department" placeholder="请选择临床专科" class="w-full">
              <el-option label="心血管内科" value="心血管内科" />
              <el-option label="呼吸与危重症医学科" value="呼吸与危重症医学科" />
              <el-option label="肿瘤科" value="肿瘤科" />
              <el-option label="内分泌代谢科" value="内分泌代谢科" />
              <el-option label="神经内科" value="神经内科" />
              <el-option label="消化内科" value="消化内科" />
              <el-option label="肾脏内科" value="肾脏内科" />
              <el-option label="耳鼻咽喉头颈外科" value="耳鼻咽喉头颈外科" />
              <el-option label="胸外科" value="胸外科" />
              <el-option label="综合/多学科联合" value="综合/多学科联合" />
            </el-select>
          </el-form-item>

          <el-form-item label="核心 ICD-10 编码（可选）">
            <el-input v-model="diseaseForm.icdCode" placeholder="如 I50.9 / C34.9" />
          </el-form-item>
        </div>

        <el-form-item label="专病简介与队列建设目标">
          <el-input
            v-model="diseaseForm.description"
            type="textarea"
            :rows="3"
            placeholder="简要描述该专病库的建库目的、重点研究方向、收治标准等（选填）"
          />
        </el-form-item>

        <div class="grid grid-cols-2 gap-4">
          <el-form-item label="专病启用状态">
            <el-radio-group v-model="diseaseForm.status">
              <el-radio-button value="ACTIVE">已启用</el-radio-button>
              <el-radio-button value="INACTIVE">未启用</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="自动增量患者同步">
            <el-switch
              v-model="diseaseForm.autoSync"
              active-text="开启每日定时同步"
              inactive-text="仅手动触发"
            />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="flex items-center justify-between">
          <span class="text-xs text-slate-400">构建完成后可立即为其配置过滤规则</span>
          <div class="flex items-center gap-2">
            <el-button @click="diseaseModalVisible = false">取消</el-button>
            <el-button type="primary" :loading="submittingDisease" @click="handleSaveDisease">
              {{ editingDiseaseId ? '保存变更' : '完成专病构建' }}
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- ================= 阶段 2：专病入组过滤配置中心（抽屉式配置器） ================= -->
    <el-drawer
      v-model="rulesDrawerVisible"
      :title="`专病纳排过滤规则配置 - ${activeDiseaseItem?.name || ''}`"
      size="760px"
      destroy-on-close
    >
      <template #header>
        <div class="flex items-center justify-between w-full pr-4">
          <div class="flex items-center gap-2">
            <span class="w-1.5 h-4 bg-sky-500 rounded-full" />
            <span class="text-base font-bold text-slate-900">纳排过滤规则配置</span>
            <el-tag size="small" type="primary" effect="plain" class="!rounded">
              {{ activeDiseaseItem?.name }}
            </el-tag>
          </div>
        </div>
      </template>

      <div class="space-y-6 pb-20">
        <!-- 引导提示卡 -->
        <div class="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 text-xs text-slate-600 leading-relaxed flex items-start gap-2.5">
          <el-icon :size="18" class="text-sky-500 flex-shrink-0 mt-0.5"><Guide /></el-icon>
          <div>
            <div class="font-semibold text-slate-800 mb-0.5">多模态患者队列准入规则设定</div>
            支持从临床指南模板快速载入、AI 智能助手一键推荐，或自定义编排诊断、检验、用药、影像等多模态纳入逻辑。
          </div>
        </div>

        <!-- 快速引入方式选择栏 -->
        <div class="p-4 rounded-xl border border-slate-200/80 bg-white shadow-clinical-sm space-y-3">
          <div class="text-xs font-semibold text-slate-800 flex items-center justify-between">
            <span class="flex items-center gap-1.5">
              <el-icon class="text-sky-500"><MagicStick /></el-icon>
              快速规则生成与引入
            </span>
            <span class="text-[11px] text-slate-400">推荐使用 AI 或指南模板初始化</span>
          </div>

          <div class="flex flex-wrap items-center gap-3">
            <!-- AI 一键推荐规则 -->
            <el-button
              type="primary"
              plain
              size="small"
              :loading="aiSuggesting"
              class="!rounded-lg"
              @click="handleAiSuggestRules"
            >
              <el-icon class="mr-1"><Cpu /></el-icon>
              AI 智能推荐纳排条件
            </el-button>

            <!-- 模板检索自动补全 -->
            <el-autocomplete
              v-model="templateSearchKey"
              :fetch-suggestions="queryDiseaseTemplates"
              placeholder="搜索参考模板 (如心衰、糖尿病等)"
              clearable
              size="small"
              class="!w-64"
              @select="handleSelectTemplate"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-autocomplete>
          </div>

          <!-- AI 推荐反馈提示条 -->
          <div v-if="aiResult" class="mt-2 p-2.5 rounded-lg bg-sky-50/70 border border-sky-100 flex items-center justify-between text-xs">
            <div class="flex items-center gap-2">
              <el-tag size="small" :type="aiResult.confidence > 70 ? 'success' : 'warning'" effect="dark" class="!rounded font-mono">
                置信度 {{ aiResult.confidence }}%
              </el-tag>
              <span class="text-slate-700">AI 推荐了 {{ aiResult.groups?.length || 0 }} 组多模态过滤准则</span>
            </div>
            <el-button type="primary" link size="small" class="!text-xs font-semibold" @click="acceptAiRules">
              立即采纳生效
            </el-button>
          </div>
        </div>

        <!-- 规则编排工作区 -->
        <div class="space-y-2">
          <div class="flex items-center justify-between">
            <span class="text-xs font-semibold text-slate-800 flex items-center gap-1.5">
              <el-icon class="text-sky-500"><Operation /></el-icon>
              多模态过滤条件编排
            </span>
            <span class="text-xs text-slate-400">组内支持 AND/OR，组间支持逻辑级联</span>
          </div>

          <ConditionBuilder v-model="activeDrawerRules" />
        </div>

        <!-- 实时匹配与测算结果区 -->
        <div class="p-4 rounded-xl border border-slate-200 bg-slate-50/70 flex items-center justify-between">
          <div>
            <div class="text-xs text-slate-500 font-medium">当前规则预估匹配患者</div>
            <div class="flex items-baseline gap-1 mt-0.5">
              <span class="font-mono text-xl font-bold text-slate-900">
                {{ previewCount !== null ? formatNumber(previewCount) : '--' }}
              </span>
              <span class="text-xs text-slate-400">人符合准入条件</span>
            </div>
          </div>

          <el-button
            size="small"
            type="primary"
            plain
            :loading="previewing"
            @click="handlePreviewMatch"
          >
            <el-icon class="mr-1"><DataAnalysis /></el-icon>
            测试患者匹配
          </el-button>
        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-between p-2">
          <el-button @click="rulesDrawerVisible = false">取消</el-button>
          <div class="flex items-center gap-2">
            <el-button
              type="primary"
              :loading="submittingRules"
              @click="handleSaveRules"
            >
              保存过滤配置并更新队列
            </el-button>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  Plus,
  Search,
  Refresh,
  FolderOpened,
  InfoFilled,
  WarningFilled,
  UserFilled,
  Filter,
  Guide,
  MagicStick,
  Cpu,
  Operation,
  DataAnalysis,
} from '@element-plus/icons-vue'
import DiseaseCard from '@/components/DiseaseCard/index.vue'
import ConditionBuilder from '@/components/ConditionBuilder/index.vue'
import {
  getDiseaseCohorts,
  createDiseaseCohort,
  updateDiseaseCohort,
  deleteDiseaseCohort,
  syncDiseaseCohort,
  previewDiseaseCohort,
  searchDiseaseTemplates,
  aiSuggestDiseaseRules,
} from '@/api/data'

// 假装一个 MedicineBox 图标组件用于替代 AntD
const MedicineBoxOutlined = FolderOpened

const router = useRouter()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 12
const keyword = ref('')
const statusFilter = ref('')
const ruleFilter = ref('ALL')

// 顶部概览统计
const stats = computed(() => {
  const all = list.value || []
  const totalCohorts = total.value || all.length
  let totalPatients = 0
  let configuredCohorts = 0
  let pendingCohorts = 0

  all.forEach((item) => {
    totalPatients += item.patientCount || 0
    let hasRules = false
    try {
      const r = typeof item.inclusionRules === 'string' ? JSON.parse(item.inclusionRules) : item.inclusionRules
      if (r?.groups?.length > 0 && r.groups.some((g: any) => g.conditions?.length > 0)) {
        hasRules = true
      }
    } catch {
      hasRules = false
    }
    if (hasRules) configuredCohorts++
    else pendingCohorts++
  })

  return { totalCohorts, totalPatients, configuredCohorts, pendingCohorts }
})

// 过滤列表（结合规则配置状态）
const filteredList = computed(() => {
  let result = list.value
  if (ruleFilter.value === 'CONFIGURED') {
    result = result.filter((item) => {
      try {
        const r = typeof item.inclusionRules === 'string' ? JSON.parse(item.inclusionRules) : item.inclusionRules
        return r?.groups?.length > 0 && r.groups.some((g: any) => g.conditions?.length > 0)
      } catch {
        return false
      }
    })
  } else if (ruleFilter.value === 'PENDING') {
    result = result.filter((item) => {
      try {
        const r = typeof item.inclusionRules === 'string' ? JSON.parse(item.inclusionRules) : item.inclusionRules
        return !r?.groups?.length || !r.groups.some((g: any) => g.conditions?.length > 0)
      } catch {
        return true
      }
    })
  }
  return result
})

function formatNumber(num?: number): string {
  if (!num) return '0'
  return num.toLocaleString()
}

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
    ElMessage.error('加载专病列表失败: ' + (e.message || '网络异常'))
  } finally {
    loading.value = false
  }
}

function applyRuleFilter() {
  // filteredList computed handles this automatically
}

// ================= 阶段 1：构建专病基础档案 =================
const diseaseModalVisible = ref(false)
const submittingDisease = ref(false)
const editingDiseaseId = ref<number | null>(null)
const diseaseFormRef = ref<FormInstance>()

const diseaseForm = reactive({
  name: '',
  department: '心血管内科',
  icdCode: '',
  description: '',
  status: 'ACTIVE',
  autoSync: true,
})

const diseaseFormRules: FormRules = {
  name: [{ required: true, message: '请输入专病库名称', trigger: 'blur' }],
}

function openCreateDiseaseModal() {
  editingDiseaseId.value = null
  diseaseForm.name = ''
  diseaseForm.department = '心血管内科'
  diseaseForm.icdCode = ''
  diseaseForm.description = ''
  diseaseForm.status = 'ACTIVE'
  diseaseForm.autoSync = true
  diseaseModalVisible.value = true
}

function openEditDiseaseModal(item: any) {
  editingDiseaseId.value = item.id
  diseaseForm.name = item.name || ''
  diseaseForm.department = item.department || '心血管内科'
  diseaseForm.icdCode = item.icdCode || ''
  diseaseForm.description = item.description || ''
  diseaseForm.status = item.status || 'ACTIVE'
  diseaseForm.autoSync = item.autoSync !== false
  diseaseModalVisible.value = true
}

async function handleSaveDisease() {
  if (!diseaseFormRef.value) return
  await diseaseFormRef.value.validate(async (valid) => {
    if (!valid) return
    submittingDisease.value = true
    try {
      const payload: Record<string, any> = {
        name: diseaseForm.name,
        description: diseaseForm.description,
        status: diseaseForm.status,
        department: diseaseForm.department,
        icdCode: diseaseForm.icdCode,
        autoSync: diseaseForm.autoSync,
      }

      if (editingDiseaseId.value) {
        await updateDiseaseCohort(editingDiseaseId.value, payload)
        ElMessage.success('专病档案更新成功')
        diseaseModalVisible.value = false
        loadData()
      } else {
        // 新建专病
        const res = await createDiseaseCohort(payload)
        ElMessage.success('专病构建成功')
        diseaseModalVisible.value = false
        await loadData()

        // 核心流程引导：专病建好后，提示用户是否立即配置过滤规则
        const createdItem = res.data?.data || list.value.find((c) => c.name === diseaseForm.name)
        if (createdItem) {
          ElMessageBox.confirm(
            `专病「${diseaseForm.name}」已成功立项！是否立即为其配置多模态入组过滤规则？`,
            '下一步操作指引',
            {
              confirmButtonText: '立即配置过滤规则',
              cancelButtonText: '暂不配置，稍后再说',
              type: 'info',
            },
          )
            .then(() => {
              openConfigureRulesDrawer(createdItem)
            })
            .catch(() => {})
        }
      }
    } catch (e: any) {
      ElMessage.error('保存专病档案失败: ' + (e.message || ''))
    } finally {
      submittingDisease.value = false
    }
  })
}

// ================= 阶段 2：专病入组过滤配置中心 =================
const rulesDrawerVisible = ref(false)
const submittingRules = ref(false)
const activeDiseaseItem = ref<any>(null)
const activeDrawerRules = ref<any>(null)
const previewing = ref(false)
const previewCount = ref<number | null>(null)
const templateSearchKey = ref('')
const aiSuggesting = ref(false)
const aiResult = ref<{ groups: any[]; confidence: number; source: string } | null>(null)

function openConfigureRulesDrawer(item: any) {
  activeDiseaseItem.value = item
  previewCount.value = item.patientCount ?? null
  aiResult.value = null
  templateSearchKey.value = ''

  let rules = item.inclusionRules
  if (typeof rules === 'string') {
    try {
      rules = JSON.parse(rules)
    } catch {
      rules = null
    }
  }

  // 若无规则，提供基础默认空结构
  activeDrawerRules.value = rules || {
    groupLogic: 'AND',
    groups: [
      {
        domain: 'DIAGNOSIS',
        logic: 'OR',
        conditions: [{ field: 'diagnosis_code', operator: 'LIKE', value: item.icdCode || '' }],
      },
    ],
  }

  rulesDrawerVisible.value = true
}

// AI 智能推荐纳排条件
async function handleAiSuggestRules() {
  if (!activeDiseaseItem.value?.name) return
  aiSuggesting.value = true
  try {
    const res = await aiSuggestDiseaseRules(activeDiseaseItem.value.name)
    aiResult.value = res.data?.data || null
    if (aiResult.value?.groups?.length) {
      ElMessage.success(`AI 已推荐 ${aiResult.value.groups.length} 组纳排准则`)
    } else {
      ElMessage.info('未找到与该疾病匹配的推荐规则，建议手动设定')
    }
  } catch (e: any) {
    ElMessage.error('AI 推荐失败: ' + (e.message || '接口调用异常'))
  } finally {
    aiSuggesting.value = false
  }
}

function acceptAiRules() {
  if (aiResult.value?.groups?.length) {
    activeDrawerRules.value = {
      groupLogic: 'AND',
      groups: aiResult.value.groups,
    }
    ElMessage.success('已采纳 AI 纳排准则')
  }
}

// 模板检索提示
async function queryDiseaseTemplates(queryString: string, cb: (results: any[]) => void) {
  if (!queryString) {
    cb([])
    return
  }
  try {
    const res = await searchDiseaseTemplates(queryString)
    const items = (res.data?.data || []).map((t: any) => ({
      value: t.diseaseName,
      template: t,
    }))
    cb(items)
  } catch {
    cb([])
  }
}

function handleSelectTemplate(item: any) {
  const tpl = item.template
  if (tpl?.inclusionTemplate) {
    try {
      const parsed = typeof tpl.inclusionTemplate === 'string'
        ? JSON.parse(tpl.inclusionTemplate)
        : tpl.inclusionTemplate
      activeDrawerRules.value = parsed
      ElMessage.success(`已应用「${item.value}」标准指南模板`)
    } catch {
      ElMessage.warning('模板解析失败')
    }
  }
}

// 测试匹配患者数量
async function handlePreviewMatch() {
  if (!activeDiseaseItem.value?.id) return
  previewing.value = true
  try {
    const res = await previewDiseaseCohort(activeDiseaseItem.value.id)
    previewCount.value = res.data?.data?.patientCount ?? 0
    ElMessage.success(`测算完成：符合入组准则患者约 ${previewCount.value} 人`)
  } catch (e: any) {
    ElMessage.error('测试匹配失败: ' + (e.message || ''))
    previewCount.value = null
  } finally {
    previewing.value = false
  }
}

// 保存纳排过滤配置
async function handleSaveRules() {
  if (!activeDiseaseItem.value?.id) return
  submittingRules.value = true
  try {
    const payload = {
      name: activeDiseaseItem.value.name,
      description: activeDiseaseItem.value.description,
      status: activeDiseaseItem.value.status || 'ACTIVE',
      inclusionRules: JSON.stringify(activeDrawerRules.value),
    }

    await updateDiseaseCohort(activeDiseaseItem.value.id, payload)
    ElMessage.success('纳排过滤规则配置保存成功')
    rulesDrawerVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error('保存过滤配置失败: ' + (e.message || ''))
  } finally {
    submittingRules.value = false
  }
}

// ================= 其他业务动作 =================
function goDetail(item: any) {
  router.push({ name: 'DiseaseDetail', params: { id: item.id } })
}

function goKnowledge(item: any) {
  router.push({ path: '/data/cdr/disease-kb', query: { cohortId: item.id } })
}

function goFollowup(item: any) {
  router.push({ path: '/followup/workbench', query: { cohortId: item.id } })
}

async function handleSync(item: any) {
  try {
    await ElMessageBox.confirm(
      `确认依据当前多模态纳排规则同步「${item.name}」患者队列？`,
      '患者队列同步确认',
      {
        confirmButtonText: '确定同步',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await syncDiseaseCohort(item.id)
    ElMessage.success('队列同步已触发')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error('触发同步失败: ' + (e.message || ''))
    }
  }
}

async function handleDelete(item: any) {
  try {
    await ElMessageBox.confirm(
      `确定删除专病库「${item.name}」？删除后其入组队列与随访方案将受到影响。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error',
      },
    )
    await deleteDiseaseCohort(item.id)
    ElMessage.success('专病库已删除')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败: ' + (e.message || ''))
    }
  }
}

onMounted(loadData)
</script>
