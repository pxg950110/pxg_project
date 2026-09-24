<template>
  <div class="p-6 space-y-6 max-w-[1600px] mx-auto">
    <!-- 顶部标题与行动区 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-bold text-slate-900 tracking-tight m-0 flex items-center gap-2.5">
          <span class="w-2 h-5 bg-sky-500 rounded-full" />
          精准随访工作台
        </h2>
        <p class="text-xs text-slate-500 mt-1 m-0">
          管理专病队列患者的随访计划、到期预警与量表评估，闭环追踪伴随治疗与临床预后。
        </p>
      </div>

      <div class="flex items-center gap-3">
        <el-radio-group v-model="mineOnly" size="small" @change="load">
          <el-radio-button :value="true">我负责的</el-radio-button>
          <el-radio-button :value="false">全部任务</el-radio-button>
        </el-radio-group>

        <el-input
          v-model="keyword"
          placeholder="搜索患者姓名..."
          clearable
          size="small"
          class="!w-60"
        >
          <template #prefix>
            <el-icon class="text-slate-400"><Search /></el-icon>
          </template>
        </el-input>

        <el-button size="small" @click="load">
          <el-icon class="mr-1"><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 任务到期态势卡片 -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
      <div
        class="bg-white p-4 rounded-2xl border transition-all duration-200 cursor-pointer flex items-center justify-between"
        :class="group === 'today' ? 'border-sky-500 shadow-clinical ring-2 ring-sky-100' : 'border-slate-200/80 shadow-clinical-sm hover:border-slate-300'"
        @click="group = 'today'"
      >
        <div>
          <div class="text-xs font-medium text-slate-500 flex items-center gap-1.5">
            <span class="w-2 h-2 rounded-full bg-sky-500" />
            今日到期任务
          </div>
          <div class="flex items-baseline gap-1 mt-1.5">
            <span class="text-2xl font-bold font-mono text-sky-600">{{ groups.today.length }}</span>
            <span class="text-xs text-slate-400">例需执行</span>
          </div>
        </div>
        <div class="w-11 h-11 rounded-xl bg-sky-50 text-sky-600 flex items-center justify-center">
          <el-icon :size="22"><Calendar /></el-icon>
        </div>
      </div>

      <div
        class="bg-white p-4 rounded-2xl border transition-all duration-200 cursor-pointer flex items-center justify-between"
        :class="group === 'overdue' ? 'border-rose-500 shadow-clinical ring-2 ring-rose-100' : 'border-slate-200/80 shadow-clinical-sm hover:border-slate-300'"
        @click="group = 'overdue'"
      >
        <div>
          <div class="text-xs font-medium text-slate-500 flex items-center gap-1.5">
            <span class="w-2 h-2 rounded-full bg-rose-500 animate-pulse" />
            已超期预警
          </div>
          <div class="flex items-baseline gap-1 mt-1.5">
            <span class="text-2xl font-bold font-mono text-rose-600">{{ groups.overdue.length }}</span>
            <span class="text-xs text-slate-400">例已超期</span>
          </div>
        </div>
        <div class="w-11 h-11 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center">
          <el-icon :size="22"><WarningFilled /></el-icon>
        </div>
      </div>

      <div
        class="bg-white p-4 rounded-2xl border transition-all duration-200 cursor-pointer flex items-center justify-between"
        :class="group === 'upcoming' ? 'border-indigo-500 shadow-clinical ring-2 ring-indigo-100' : 'border-slate-200/80 shadow-clinical-sm hover:border-slate-300'"
        @click="group = 'upcoming'"
      >
        <div>
          <div class="text-xs font-medium text-slate-500 flex items-center gap-1.5">
            <span class="w-2 h-2 rounded-full bg-indigo-500" />
            未来 7 天待随访
          </div>
          <div class="flex items-baseline gap-1 mt-1.5">
            <span class="text-2xl font-bold font-mono text-indigo-600">{{ groups.upcoming.length }}</span>
            <span class="text-xs text-slate-400">例即将到期</span>
          </div>
        </div>
        <div class="w-11 h-11 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
          <el-icon :size="22"><Clock /></el-icon>
        </div>
      </div>
    </div>

    <!-- 随访任务表格工作台 -->
    <div class="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-clinical-sm space-y-4">
      <div class="flex items-center justify-between pb-3 border-b border-slate-100">
        <div class="flex items-center gap-2">
          <span class="text-sm font-semibold text-slate-800">
            {{ group === 'today' ? '今日到期患者' : group === 'overdue' ? '已超期患者清单' : '近期即将随访患者' }}
          </span>
          <el-tag size="small" type="info" effect="plain" class="!rounded font-mono">
            {{ currentRows.length }} 例
          </el-tag>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="currentRows"
        row-key="id"
        size="small"
        class="w-full"
      >
        <el-table-column label="患者姓名" min-width="160">
          <template #default="{ row }">
            <div class="py-1">
              <span class="font-bold text-slate-800">{{ row.patientName }}</span>
              <span v-if="row.gender" class="text-xs text-slate-400 ml-1.5">
                ({{ row.gender }} / {{ row.age }}岁)
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="随访阶段" prop="stageName" width="140">
          <template #default="{ row }">
            <el-tag size="small" type="primary" effect="plain" class="!rounded">
              {{ row.stageName }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="应评量表" min-width="260">
          <template #default="{ row }">
            <div class="flex flex-wrap gap-1.5 py-1">
              <el-tag
                v-for="c in row.requiredScales"
                :key="c"
                size="small"
                type="danger"
                effect="light"
                class="!rounded font-mono"
              >
                必评: {{ shortScale(c) }}
              </el-tag>
              <el-tag
                v-for="c in row.optionalScales || []"
                :key="'o' + c"
                size="small"
                type="info"
                effect="plain"
                class="!rounded font-mono"
              >
                选评: {{ shortScale(c) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="随访到期日" width="220">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <span :class="row.status === 'OVERDUE' ? 'text-rose-600 font-semibold font-mono' : 'text-slate-600 font-mono'">
                {{ row.dueDate }}
              </span>
              <el-tag
                v-if="row.status === 'OVERDUE'"
                size="small"
                type="danger"
                effect="dark"
                class="!rounded font-mono"
              >
                超期 {{ row.overdueDays }} 天
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              class="!rounded-lg"
              @click="openTask(row)"
            >
              开始随访
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!currentRows.length && !loading" class="py-12 text-center text-xs text-slate-400">
        当前分类下暂无待处理随访任务
      </div>
    </div>

    <!-- ==================== 执行随访工作抽屉 ==================== -->
    <el-drawer
      v-model="drawerOpen"
      :title="`执行随访 · ${currentRow?.patientName || ''}`"
      size="760px"
      destroy-on-close
    >
      <template v-if="currentRow">
        <div class="space-y-5 pb-20">
          <!-- 患者基础信息栏 -->
          <div class="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 flex items-center justify-between text-xs">
            <div class="flex items-center gap-3">
              <span class="font-bold text-sm text-slate-900">{{ currentRow.patientName }}</span>
              <span v-if="currentRow.gender" class="text-slate-500">
                ({{ currentRow.gender }} / {{ currentRow.age }}岁)
              </span>
              <span class="text-slate-400">· 随访阶段：{{ currentRow.stageName }}</span>
            </div>
            <el-tag
              size="small"
              :type="currentRow.status === 'OVERDUE' ? 'danger' : 'primary'"
              effect="dark"
              class="!rounded font-mono"
            >
              {{ currentRow.status === 'OVERDUE' ? `已超期 ${currentRow.overdueDays} 天` : '正常进行中' }}
            </el-tag>
          </div>

          <!-- 任务提示 -->
          <div class="p-3 rounded-xl bg-sky-50/70 border border-sky-100 text-xs text-sky-800 flex items-center gap-2">
            <el-icon class="text-sky-600"><InfoFilled /></el-icon>
            本次任务：{{ currentRow.stageName }} · 必评量表【{{ currentRow.requiredScales.map(shortScale).join(' + ') }}】 · 到期时间 {{ currentRow.dueDate }}
          </div>

          <!-- 量表填写区域 -->
          <div class="space-y-4">
            <div class="text-xs font-semibold text-slate-800 flex items-center gap-1.5">
              <el-icon class="text-sky-500"><DocumentChecked /></el-icon>
              临床评估量表填报
            </div>
            <ScaleFillPanel
              v-for="code in allScaleCodes"
              :key="code"
              :scale-code="code"
              :required="currentRow.requiredScales.includes(code)"
              :patient-id="currentRow.patientId"
              @change="onScaleChange"
            />
          </div>

          <!-- 伴随治疗记录 -->
          <div class="p-4 rounded-xl border border-slate-200/80 bg-white shadow-clinical-sm space-y-3">
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold text-slate-800 flex items-center gap-1.5">
                <el-icon class="text-indigo-500"><FolderChecked /></el-icon>
                本次随访伴随治疗记录（选填）
              </span>
              <el-button size="small" type="primary" plain class="!rounded-lg" @click="treatmentOpen = true">
                <el-icon class="mr-1"><Plus /></el-icon>
                添加治疗
              </el-button>
            </div>

            <div v-if="!addedTreatments.length" class="py-6 text-center text-xs text-slate-400 bg-slate-50/60 rounded-lg">
              本次随访尚未录入新增治疗、用药或手术记录
            </div>
            <div v-else class="space-y-2">
              <div
                v-for="(t, i) in addedTreatments"
                :key="i"
                class="flex items-center justify-between p-2.5 rounded-lg bg-slate-50 border border-slate-200/80 text-xs"
              >
                <div class="flex items-center gap-2">
                  <el-tag
                    size="small"
                    :type="t.category === 'SURGERY' ? 'danger' : t.category === 'MEDICATION' ? 'warning' : 'info'"
                    effect="light"
                    class="!rounded"
                  >
                    {{ categoryLabel(t.category) }}
                  </el-tag>
                  <span class="font-semibold text-slate-800">{{ t.name }}</span>
                  <span class="text-slate-400 font-mono">· 发生日期: {{ t.occurredDate }}</span>
                </div>
                <el-button link type="danger" size="small" class="!text-xs" @click="addedTreatments.splice(i, 1)">
                  移除
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </template>

      <template #footer>
        <div class="flex items-center justify-between p-2">
          <el-button
            v-if="hasPermission('disease:followup:manage')"
            type="danger"
            plain
            size="small"
            @click="skipOpen = true"
          >
            跳过随访（医生授权）
          </el-button>
          <div class="flex items-center gap-2 ml-auto">
            <el-button @click="drawerOpen = false">取消</el-button>
            <el-button
              type="primary"
              :disabled="!allScalesComplete"
              :loading="submitting"
              @click="handleSubmit"
            >
              提交随访 (量表 {{ completeCount }}/{{ currentRow?.requiredScales.length }})
            </el-button>
          </div>
        </div>
      </template>
    </el-drawer>

    <!-- 跳过原因确认弹窗 -->
    <el-dialog v-model="skipOpen" title="跳过随访任务（医生授权）" width="480px" destroy-on-close class="!rounded-2xl">
      <el-form label-position="top">
        <el-form-item label="跳过原因（必填）" required>
          <el-input
            v-model="skipReason"
            type="textarea"
            :rows="3"
            maxlength="512"
            show-word-limit
            placeholder="例如：患者住院期间由病房专科医生完成评估，或依临床医嘱暂停"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex items-center justify-end gap-2">
          <el-button @click="skipOpen = false">取消</el-button>
          <el-button type="danger" @click="handleSkip">确认跳过</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 添加伴随治疗弹窗 -->
    <el-dialog v-model="treatmentOpen" title="添加伴随治疗记录" width="480px" destroy-on-close class="!rounded-2xl">
      <el-form label-position="top" class="space-y-3">
        <el-form-item label="治疗类别" required>
          <el-radio-group v-model="treatForm.category" size="small">
            <el-radio-button value="MEDICATION">药物治疗</el-radio-button>
            <el-radio-button value="SURGERY">手术操作</el-radio-button>
            <el-radio-button value="OTHER">其他干预</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="项目名称" required>
          <el-input v-model="treatForm.name" placeholder="如：鼻用糠酸莫米松喷雾 / ESS 鼻内镜手术" />
        </el-form-item>

        <el-form-item label="发生日期" required>
          <el-date-picker
            v-model="treatForm.date"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择治疗发生日期"
            class="!w-full"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex items-center justify-end gap-2">
          <el-button @click="treatmentOpen = false">取消</el-button>
          <el-button type="primary" @click="addTreatment">确认添加</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import {
  Search,
  Refresh,
  Calendar,
  WarningFilled,
  Clock,
  InfoFilled,
  DocumentChecked,
  FolderChecked,
  Plus,
} from '@element-plus/icons-vue'
import ScaleFillPanel from '@/views/data-cdr/followup/components/ScaleFillPanel.vue'
import { getMyWorkbench, completeTask, skipTask, getScales } from '@/api/followup'
import { usePermissionStore } from '@/stores/permission'
import { useAuthStore } from '@/stores/auth'

const permissionStore = usePermissionStore()
const authStore = useAuthStore()
const hasPermission = (code: string) => permissionStore.hasPermission(code)

const mineOnly = ref(true)
const keyword = ref('')
const group = ref<'today' | 'overdue' | 'upcoming'>('today')
const loading = ref(false)

interface TaskRow {
  id: number
  followupId: number
  patientId?: number
  patientName?: string
  gender?: string
  age?: number
  stageCode: string
  stageName: string
  dueDate: string
  status: 'PENDING' | 'OVERDUE'
  requiredScales: string[]
  optionalScales?: string[]
  overdueDays: number
}

const groups = ref<Record<'today' | 'overdue' | 'upcoming', TaskRow[]>>({ today: [], overdue: [], upcoming: [] })
const scaleNames = ref<Record<string, string>>({})
const shortScale = (code: string) => scaleNames.value[code]?.split(' ')[0] || code
const categoryLabel = (c: string) => ({ MEDICATION: '药物', SURGERY: '手术', OTHER: '其他' } as any)[c]

async function load() {
  loading.value = true
  try {
    const res = await getMyWorkbench({ userId: authStore.userInfo?.id, all: !mineOnly.value })
    const data = res.data?.data
    groups.value = {
      today: data?.today || [],
      overdue: data?.overdue || [],
      upcoming: data?.upcoming || [],
    }
    if (group.value === 'overdue' && !groups.value.overdue.length) group.value = 'today'
  } catch {
    ElMessage.error('随访工作台加载失败')
  } finally {
    loading.value = false
  }
}

async function loadScaleCatalog() {
  try {
    const res = await getScales()
    const scales = res.data?.data || []
    scales.forEach((s: any) => {
      scaleNames.value[s.scaleCode] = s.name
    })
  } catch {}
}

const currentRows = computed(() => {
  const rows = groups.value[group.value] || []
  return keyword.value ? rows.filter((r) => (r.patientName || '').includes(keyword.value)) : rows
})

/* 抽屉与表单 */
const drawerOpen = ref(false)
const currentRow = ref<TaskRow | null>(null)
const scaleResults = ref<Record<string, { complete: boolean }>>({})
const scaleAnswers = ref<Record<string, Record<string, any>>>({})
const submitting = ref(false)
const addedTreatments = ref<{ category: string; name: string; occurredDate: string }[]>([])
const treatmentOpen = ref(false)
const skipOpen = ref(false)
const skipReason = ref('')

const treatForm = ref<{ category: string; name: string; date: string }>({
  category: 'MEDICATION',
  name: '',
  date: dayjs().format('YYYY-MM-DD'),
})

const allScaleCodes = computed(() =>
  currentRow.value ? [...currentRow.value.requiredScales, ...(currentRow.value.optionalScales || [])] : [],
)

function openTask(row: TaskRow) {
  currentRow.value = row
  scaleResults.value = {}
  scaleAnswers.value = {}
  addedTreatments.value = []
  drawerOpen.value = true
}

function onScaleChange(p: { scaleCode: string; answers: Record<string, any>; totalScore: number; complete: boolean }) {
  scaleResults.value[p.scaleCode] = { complete: p.complete }
  scaleAnswers.value[p.scaleCode] = p.answers
}

const completeCount = computed(() =>
  currentRow.value
    ? currentRow.value.requiredScales.filter((c) => scaleResults.value[c]?.complete).length
    : 0,
)

const allScalesComplete = computed(() =>
  !!currentRow.value && currentRow.value.requiredScales.every((c) => scaleResults.value[c]?.complete),
)

function addTreatment() {
  if (!treatForm.value.name.trim()) {
    ElMessage.warning('请输入治疗项目名称')
    return
  }
  addedTreatments.value.push({
    category: treatForm.value.category,
    name: treatForm.value.name,
    occurredDate: treatForm.value.date || dayjs().format('YYYY-MM-DD'),
  })
  treatForm.value.name = ''
  treatmentOpen.value = false
  ElMessage.success('伴随治疗记录已添加')
}

async function handleSubmit() {
  if (!currentRow.value) return
  submitting.value = true
  try {
    await completeTask(
      currentRow.value.id,
      {
        assessments: Object.keys(scaleAnswers.value).map((code) => ({
          scaleCode: code,
          answers: scaleAnswers.value[code],
        })),
        treatments: addedTreatments.value.map((t) => ({ ...t })),
      },
      authStore.userInfo?.id,
    )
    ElMessage.success('随访任务已提交并归档')
    drawerOpen.value = false
    load()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '随访提交失败')
  } finally {
    submitting.value = false
  }
}

async function handleSkip() {
  if (!currentRow.value) return
  if (!skipReason.value.trim()) {
    ElMessage.warning('请填写跳过原因')
    return
  }
  try {
    await skipTask(currentRow.value.id, skipReason.value.trim(), authStore.userInfo?.id)
    ElMessage.success('任务已由医生授权跳过')
    skipOpen.value = false
    drawerOpen.value = false
    load()
  } catch (e: any) {
    ElMessage.error('操作失败: ' + (e.message || ''))
  }
}

onMounted(() => {
  load()
  loadScaleCatalog()
})
</script>
