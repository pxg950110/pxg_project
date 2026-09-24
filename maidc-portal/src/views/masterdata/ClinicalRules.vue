<template>
  <PageContainer title="临床规则">
    <el-tabs v-model="activeTab">
      <!-- Tab 1: Reference Ranges -->
      <el-tab-pane label="参考范围" name="refRange">
        <!-- Filter bar -->
        <el-card shadow="never" style="margin-bottom: 16px">
          <div class="flex flex-wrap items-center gap-4">
            <el-input-number v-model="refFilters.conceptId" placeholder="概念ID" class="w-52" @change="fetchRefRanges" />
            <el-select v-model="refFilters.gender" placeholder="性别" clearable class="w-40" @change="fetchRefRanges">
              <el-option value="MALE" label="男" />
              <el-option value="FEMALE" label="女" />
              <el-option value="BOTH" label="通用" />
            </el-select>
            <div class="ml-auto">
              <el-button type="primary" @click="handleCreateRefRange">
                <el-icon class="mr-1"><Plus /></el-icon>
                新增参考范围
              </el-button>
            </div>
          </div>
        </el-card>

        <el-table :data="pagedRefRanges" v-loading="refLoading" row-key="id">
          <el-table-column label="概念ID" prop="conceptId" width="90" />
          <el-table-column label="性别" width="80">
            <template #default="{ row }">
              <el-tag
                :type="row.gender === 'MALE' ? 'primary' : row.gender === 'FEMALE' ? 'info' : 'success'"
                :style="row.gender === 'FEMALE' ? PINK_TAG_STYLE : undefined"
              >
                {{ row.gender === 'MALE' ? '男' : row.gender === 'FEMALE' ? '女' : '通用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="年龄范围" width="140">
            <template #default="{ row }">
              {{ row.ageMin ?? 0 }} - {{ row.ageMax ?? 999 }} {{ row.ageUnit || '岁' }}
            </template>
          </el-table-column>
          <el-table-column label="参考范围" width="180">
            <template #default="{ row }">
              {{ row.rangeLow ?? '-N/A' }} ~ {{ row.rangeHigh ?? 'N/A' }} {{ row.unit || '' }}
            </template>
          </el-table-column>
          <el-table-column label="危急值" width="120">
            <template #default="{ row }">
              <span v-if="row.criticalLow != null || row.criticalHigh != null">
                {{ row.criticalLow ?? '-' }} / {{ row.criticalHigh ?? '-' }}
              </span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="单位" prop="unit" width="80" />
        </el-table>
        <el-pagination
          class="mt-4 justify-end"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="refRanges.length"
          :current-page="refPagination.current"
          :page-size="refPagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="handleRefPageChange"
          @size-change="handleRefSizeChange"
        />

        <!-- Evaluate section -->
        <el-card shadow="never" style="margin-top: 16px">
          <template #header>匹配测试</template>
          <div class="flex flex-wrap items-center gap-4">
            <el-input-number v-model="evalParams.conceptId" placeholder="概念ID" class="w-44" />
            <el-select v-model="evalParams.gender" placeholder="性别" class="w-32">
              <el-option value="MALE" label="男" />
              <el-option value="FEMALE" label="女" />
            </el-select>
            <el-input-number v-model="evalParams.age" placeholder="年龄" class="w-32" />
            <el-input-number v-model="evalParams.value" placeholder="检测值" class="w-32" />
            <el-button type="primary" :loading="evalLoading" @click="handleEvaluate">匹配测试</el-button>
          </div>
          <div v-if="evalResult" style="margin-top: 12px; padding: 12px; background: #ecfdf5; border-radius: 4px">
            <p><strong>匹配结果：</strong>
              <el-tag :type="evalResult.status === 'NORMAL' ? 'success' : evalResult.status === 'ABNORMAL' ? 'danger' : 'warning'">
                {{ evalResult.status }}
              </el-tag>
            </p>
            <p v-if="evalResult.referenceRange">参考范围: {{ evalResult.referenceRange.rangeLow }} ~ {{ evalResult.referenceRange.rangeHigh }} {{ evalResult.referenceRange.unit }}</p>
            <p v-if="evalResult.interpretation">{{ evalResult.interpretation }}</p>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- Tab 2: Drug Interactions -->
      <el-tab-pane label="药物相互作用" name="drugInteraction">
        <!-- Filter bar -->
        <el-card shadow="never" style="margin-bottom: 16px">
          <div class="flex flex-wrap items-center gap-4">
            <el-input-number v-model="drugFilters.drug1" placeholder="药物1 ID" class="w-52" />
            <el-input-number v-model="drugFilters.drug2" placeholder="药物2 ID" class="w-52" />
            <el-button type="primary" @click="fetchDrugInteractions">查询</el-button>
            <div class="ml-auto">
              <el-button type="primary" @click="handleCreateDrugInteraction">
                <el-icon class="mr-1"><Plus /></el-icon>
                新增相互作用
              </el-button>
            </div>
          </div>
        </el-card>

        <el-table :data="drugInteractions" v-loading="drugLoading" row-key="id">
          <el-table-column label="药物1" width="150">
            <template #default="{ row }">
              {{ row.drug1Name || row.drug1Id }}
            </template>
          </el-table-column>
          <el-table-column label="药物2" width="150">
            <template #default="{ row }">
              {{ row.drug2Name || row.drug2Id }}
            </template>
          </el-table-column>
          <el-table-column label="严重程度" width="100">
            <template #default="{ row }">
              <el-tag :type="severityTagType(row.severity)" :style="severityTagStyle(row.severity)">{{ row.severity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="描述" prop="description" show-overflow-tooltip />
          <el-table-column label="临床建议" prop="clinicalAction" width="200" show-overflow-tooltip />
        </el-table>

        <!-- Prescription check -->
        <el-card shadow="never" style="margin-top: 16px">
          <template #header>处方审核</template>
          <div class="flex items-start gap-4">
            <el-input v-model="prescriptionDrugs" type="textarea" :rows="3"
              placeholder="输入药物ID，多个用逗号分隔（如: 1,2,3）" class="flex-1" />
            <el-button type="primary" :loading="checkLoading" style="margin-top: 4px" @click="handleCheckDrugList">审核</el-button>
          </div>
          <el-table v-if="checkResults.length > 0" :data="checkResults" row-key="id" size="small" style="margin-top: 12px">
            <el-table-column label="药物1" prop="drug1Name" width="120" />
            <el-table-column label="药物2" prop="drug2Name" width="120" />
            <el-table-column label="严重程度" width="100">
              <template #default="{ row }">
                <el-tag :type="severityTagType(row.severity)" :style="severityTagStyle(row.severity)">{{ row.severity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="描述" prop="description" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- Create Reference Range Modal -->
    <el-dialog v-model="refModalVisible" title="新增参考范围" width="640px" :destroy-on-close="true">
      <el-form ref="refFormRef" :model="refFormState" :rules="refFormRules" label-width="100px">
        <el-form-item label="概念ID" prop="conceptId">
          <el-input-number v-model="refFormState.conceptId" class="w-full" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="refFormState.gender" class="w-full">
            <el-option value="MALE" label="男" />
            <el-option value="FEMALE" label="女" />
            <el-option value="BOTH" label="通用" />
          </el-select>
        </el-form-item>
        <div class="grid grid-cols-2 gap-x-4">
          <el-form-item label="最小年龄" prop="ageMin" label-width="90px">
            <el-input-number v-model="refFormState.ageMin" class="w-full" />
          </el-form-item>
          <el-form-item label="最大年龄" prop="ageMax" label-width="90px">
            <el-input-number v-model="refFormState.ageMax" class="w-full" />
          </el-form-item>
        </div>
        <div class="grid grid-cols-2 gap-x-4">
          <el-form-item label="参考下限" prop="rangeLow" label-width="90px">
            <el-input-number v-model="refFormState.rangeLow" class="w-full" />
          </el-form-item>
          <el-form-item label="参考上限" prop="rangeHigh" label-width="90px">
            <el-input-number v-model="refFormState.rangeHigh" class="w-full" />
          </el-form-item>
        </div>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="refFormState.unit" placeholder="如 mg/dL, mmol/L" />
        </el-form-item>
        <div class="grid grid-cols-2 gap-x-4">
          <el-form-item label="危急低下限" prop="criticalLow" label-width="90px">
            <el-input-number v-model="refFormState.criticalLow" class="w-full" />
          </el-form-item>
          <el-form-item label="危急高上限" prop="criticalHigh" label-width="90px">
            <el-input-number v-model="refFormState.criticalHigh" class="w-full" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="refModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRefSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Create Drug Interaction Modal -->
    <el-dialog v-model="drugModalVisible" title="新增药物相互作用" width="600px" :destroy-on-close="true">
      <el-form ref="drugFormRef" :model="drugFormState" :rules="drugFormRules" label-width="100px">
        <el-form-item label="药物1 ID" prop="drug1Id">
          <el-input-number v-model="drugFormState.drug1Id" class="w-full" />
        </el-form-item>
        <el-form-item label="药物2 ID" prop="drug2Id">
          <el-input-number v-model="drugFormState.drug2Id" class="w-full" />
        </el-form-item>
        <el-form-item label="严重程度" prop="severity">
          <el-select v-model="drugFormState.severity" class="w-full">
            <el-option value="MINOR" label="轻微 (MINOR)" />
            <el-option value="MODERATE" label="中等 (MODERATE)" />
            <el-option value="SEVERE" label="严重 (SEVERE)" />
            <el-option value="CONTRAINDICATED" label="禁忌 (CONTRAINDICATED)" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="drugFormState.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="临床建议" prop="clinicalAction">
          <el-input v-model="drugFormState.clinicalAction" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drugModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDrugSubmit">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { type FormInstance, type FormItemRule } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import {
  getReferenceRanges, evaluateReferenceRange, createReferenceRange,
  getDrugInteractions, checkDrugInteraction, checkDrugList, createDrugInteraction,
} from '@/api/masterdata'

defineOptions({ name: 'ClinicalRules' })

type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'
const PURPLE_TAG_STYLE = { color: '#8b5cf6', background: '#f5f3ff', borderColor: '#ddd6fe' }
const PINK_TAG_STYLE = { color: '#ec4899', background: '#fdf2f8', borderColor: '#fbcfe8' }

const activeTab = ref('refRange')

// ====== Reference Ranges ======
const refFilters = reactive({ conceptId: undefined as number | undefined, gender: undefined as string | undefined })
const refLoading = ref(false)
const refRanges = ref<any[]>([])
const refPagination = reactive({ current: 1, pageSize: 20, total: 0 })

const pagedRefRanges = computed(() => {
  const start = (refPagination.current - 1) * refPagination.pageSize
  return refRanges.value.slice(start, start + refPagination.pageSize)
})

async function fetchRefRanges() {
  refLoading.value = true
  try {
    const params: any = {}
    if (refFilters.conceptId) params.conceptId = refFilters.conceptId
    if (refFilters.gender) params.gender = refFilters.gender
    const res = await getReferenceRanges(params)
    refRanges.value = res.data.data || []
  } finally {
    refLoading.value = false
  }
}

function handleRefPageChange(page: number) {
  refPagination.current = page
  fetchRefRanges()
}

function handleRefSizeChange(size: number) {
  refPagination.pageSize = size
  refPagination.current = 1
  fetchRefRanges()
}

// Evaluate
const evalParams = reactive({ conceptId: undefined as number | undefined, gender: undefined as string | undefined, age: undefined as number | undefined, value: undefined as number | undefined })
const evalLoading = ref(false)
const evalResult = ref<any>(null)

async function handleEvaluate() {
  if (!evalParams.conceptId || !evalParams.gender || evalParams.age == null || evalParams.value == null) {
    ElMessage.warning('请填写所有测试参数')
    return
  }
  evalLoading.value = true
  try {
    const res = await evaluateReferenceRange(evalParams)
    evalResult.value = res.data.data
  } catch {
    evalResult.value = null
  } finally {
    evalLoading.value = false
  }
}

// Create reference range modal
const refModalVisible = ref(false)
const refFormRef = ref<FormInstance>()
const refFormState = reactive({
  conceptId: undefined as number | undefined,
  gender: 'BOTH',
  ageMin: undefined as number | undefined,
  ageMax: undefined as number | undefined,
  rangeLow: undefined as number | undefined,
  rangeHigh: undefined as number | undefined,
  unit: '',
  criticalLow: undefined as number | undefined,
  criticalHigh: undefined as number | undefined,
})
const refFormRules: Record<string, FormItemRule[]> = {
  conceptId: [{ required: true, message: '请输入概念ID' }],
  gender: [{ required: true, message: '请选择性别' }],
  rangeLow: [{ required: true, message: '请输入参考下限' }],
  rangeHigh: [{ required: true, message: '请输入参考上限' }],
}

function handleCreateRefRange() {
  Object.assign(refFormState, { conceptId: undefined, gender: 'BOTH', ageMin: undefined, ageMax: undefined, rangeLow: undefined, rangeHigh: undefined, unit: '', criticalLow: undefined, criticalHigh: undefined })
  refModalVisible.value = true
}

async function handleRefSubmit() {
  await refFormRef.value?.validate()
  await createReferenceRange(refFormState)
  ElMessage.success('创建成功')
  refModalVisible.value = false
  fetchRefRanges()
}

// ====== Drug Interactions ======
const drugFilters = reactive({ drug1: undefined as number | undefined, drug2: undefined as number | undefined })
const drugLoading = ref(false)
const drugInteractions = ref<any[]>([])

function severityTagType(severity: string): TagType {
  const types: Record<string, TagType> = { MINOR: 'primary', MODERATE: 'warning', SEVERE: 'danger', CONTRAINDICATED: 'info' }
  return types[severity] || 'info'
}

function severityTagStyle(severity: string) {
  return severity === 'CONTRAINDICATED' ? PURPLE_TAG_STYLE : undefined
}

async function fetchDrugInteractions() {
  if (!drugFilters.drug1 || !drugFilters.drug2) {
    ElMessage.warning('请输入两个药物ID')
    return
  }
  drugLoading.value = true
  try {
    const res = await checkDrugInteraction(drugFilters.drug1, drugFilters.drug2)
    drugInteractions.value = res.data.data || []
  } finally {
    drugLoading.value = false
  }
}

// Prescription check
const prescriptionDrugs = ref('')
const checkLoading = ref(false)
const checkResults = ref<any[]>([])

async function handleCheckDrugList() {
  const ids = prescriptionDrugs.value.split(',').map(s => Number(s.trim())).filter(n => !isNaN(n))
  if (ids.length < 2) {
    ElMessage.warning('请至少输入2个药物ID')
    return
  }
  checkLoading.value = true
  try {
    const res = await checkDrugList(ids)
    checkResults.value = res.data.data || []
  } finally {
    checkLoading.value = false
  }
}

// Create drug interaction modal
const drugModalVisible = ref(false)
const drugFormRef = ref<FormInstance>()
const drugFormState = reactive({
  drug1Id: undefined as number | undefined,
  drug2Id: undefined as number | undefined,
  severity: 'MODERATE',
  description: '',
  clinicalAction: '',
})
const drugFormRules: Record<string, FormItemRule[]> = {
  drug1Id: [{ required: true, message: '请输入药物1 ID' }],
  drug2Id: [{ required: true, message: '请输入药物2 ID' }],
  severity: [{ required: true, message: '请选择严重程度' }],
}

function handleCreateDrugInteraction() {
  Object.assign(drugFormState, { drug1Id: undefined, drug2Id: undefined, severity: 'MODERATE', description: '', clinicalAction: '' })
  drugModalVisible.value = true
}

async function handleDrugSubmit() {
  await drugFormRef.value?.validate()
  await createDrugInteraction(drugFormState)
  ElMessage.success('创建成功')
  drugModalVisible.value = false
}
</script>
