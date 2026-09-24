<template>
  <PageContainer title="机构管理">
    <template #extra>
      <el-button type="primary" @click="handleCreate">
        <el-icon class="mr-1"><Plus /></el-icon>
        新增机构
      </el-button>
    </template>

    <el-table :data="institutions" v-loading="loading" row-key="id">
      <el-table-column label="机构编码" prop="instCode" width="140" />
      <el-table-column label="机构名称" prop="name" show-overflow-tooltip />
      <el-table-column label="简称" prop="shortName" width="150" show-overflow-tooltip />
      <el-table-column label="类型" prop="instType" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <span class="inline-flex items-center gap-1.5 text-sm">
            <span
              class="inline-block h-1.5 w-1.5 rounded-full"
              :style="{ background: row.status === 'ACTIVE' ? '#10b981' : '#cbd5e1' }"
            />
            {{ row.status === 'ACTIVE' ? '启用' : row.status }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="联系人" prop="contactPerson" width="100" />
      <el-table-column label="联系电话" prop="contactPhone" width="140" />
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="modalVisible" :title="isEdit ? '编辑机构' : '新增机构'" width="560px" :destroy-on-close="true">
      <el-form ref="formRef" :model="formState" :rules="formRules" label-width="100px">
        <el-form-item label="机构编码" prop="instCode">
          <el-input v-model="formState.instCode" placeholder="如 HOSP_A" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="机构名称" prop="name">
          <el-input v-model="formState.name" placeholder="机构全称" />
        </el-form-item>
        <el-form-item label="简称" prop="shortName">
          <el-input v-model="formState.shortName" placeholder="机构简称" />
        </el-form-item>
        <el-form-item label="机构类型" prop="instType">
          <el-select v-model="formState.instType" placeholder="选择类型" clearable class="w-full">
            <el-option value="HOSPITAL" label="综合医院" />
            <el-option value="SPECIALIST" label="专科医院" />
            <el-option value="COMMUNITY" label="社区卫生中心" />
            <el-option value="CLINIC" label="诊所" />
            <el-option value="OTHER" label="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="formState.contactPerson" placeholder="联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="formState.contactPhone" placeholder="联系电话" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="formState.address" type="textarea" :rows="2" placeholder="机构地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { type FormInstance, type FormItemRule } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import { getInstitutions, createInstitution, updateInstitution } from '@/api/masterdata'

defineOptions({ name: 'InstitutionList' })

const loading = ref(false)
const institutions = ref<any[]>([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getInstitutions()
    institutions.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

const modalVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const isEdit = computed(() => editingId.value !== null)

const formState = reactive({
  instCode: '',
  name: '',
  shortName: '',
  instType: undefined as string | undefined,
  contactPerson: '',
  contactPhone: '',
  address: '',
})

const formRules: Record<string, FormItemRule[]> = {
  instCode: [{ required: true, message: '请输入机构编码' }],
  name: [{ required: true, message: '请输入机构名称' }],
}

function handleCreate() {
  editingId.value = null
  Object.assign(formState, { instCode: '', name: '', shortName: '', instType: undefined, contactPerson: '', contactPhone: '', address: '' })
  modalVisible.value = true
}

function handleEdit(record: any) {
  editingId.value = record.id
  Object.assign(formState, {
    instCode: record.instCode,
    name: record.name,
    shortName: record.shortName || '',
    instType: record.instType,
    contactPerson: record.contactPerson || '',
    contactPhone: record.contactPhone || '',
    address: record.address || '',
  })
  modalVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (isEdit.value) {
    await updateInstitution(editingId.value!, formState)
    ElMessage.success('更新成功')
  } else {
    await createInstitution(formState)
    ElMessage.success('创建成功')
  }
  modalVisible.value = false
  fetchData()
}

onMounted(() => fetchData())
</script>
