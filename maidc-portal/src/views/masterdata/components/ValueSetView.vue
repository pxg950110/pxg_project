<template>
  <div class="h-full">
    <!-- Header -->
    <el-card shadow="never" class="!rounded-xl !border-slate-200/80 mb-3">
      <div class="flex items-center gap-4">
        <el-icon :size="24" color="#06b6d4"><List /></el-icon>
        <div class="min-w-0 flex-1">
          <div class="text-base font-semibold">{{ valueSet?.name || '值集列表' }}</div>
          <div class="text-xs text-slate-400">{{ valueSet?.oid || '管理所有值集' }}</div>
        </div>
        <el-input
          v-model="keyword"
          placeholder="搜索值集"
          clearable
          class="!w-60"
          @keyup.enter="fetchList"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select
          v-model="status"
          placeholder="状态"
          clearable
          class="!w-32"
          @change="fetchList"
          @clear="status = undefined"
        >
          <el-option value="DRAFT" label="草稿" />
          <el-option value="ACTIVE" label="有效" />
          <el-option value="RETIRED" label="已废止" />
        </el-select>
        <el-button @click="handleCreate">
          <el-icon class="mr-1"><Plus /></el-icon>
          新增
        </el-button>
      </div>
    </el-card>

    <!-- Content -->
    <div class="flex gap-3" style="height: calc(100vh - 280px)">
      <!-- Left: List -->
      <div class="w-[350px] shrink-0">
        <el-card
          shadow="never"
          class="!rounded-xl !border-slate-200/80 h-full"
          :body-style="{ height: '100%', padding: '12px', boxSizing: 'border-box' }"
        >
          <div v-loading="loading" class="h-full overflow-y-auto">
            <div
              v-for="item in listData"
              :key="item.id"
              class="flex items-start gap-3 p-3 rounded-lg cursor-pointer hover:bg-slate-100"
              :class="selectedItem?.id === item.id ? 'bg-cyan-50' : ''"
              @click="selectItem(item)"
            >
              <el-avatar :size="32" class="shrink-0" style="background-color: #06b6d4">
                {{ item.name?.charAt(0) }}
              </el-avatar>
              <div class="min-w-0">
                <div class="flex items-center">
                  <span class="font-medium text-sm">{{ item.name }}</span>
                  <el-tag size="small" :type="getStatusColor(item.status)" class="!ml-2">
                    {{ getStatusLabel(item.status) }}
                  </el-tag>
                </div>
                <div class="text-xs text-slate-500">{{ item.oid }}</div>
                <div class="text-xs text-slate-400 mt-1">
                  {{ item.conceptDomainName || '-' }} · {{ item.codeCount || 0 }} 个代码
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- Right: Detail -->
      <div class="flex-1 min-w-0">
        <el-card
          shadow="never"
          class="!rounded-xl !border-slate-200/80 h-full"
          :body-style="{ height: '100%', padding: '12px', boxSizing: 'border-box', overflowY: 'auto' }"
        >
          <template v-if="selectedItem">
            <!-- Detail Header -->
            <div class="flex items-center justify-between mb-4">
              <span class="text-[15px] font-semibold">{{ selectedItem.name }}</span>
              <div class="flex items-center gap-2">
                <el-button link type="primary" size="small" @click="handleEdit(selectedItem)">编辑</el-button>
                <el-popconfirm title="确认删除？" @confirm="handleDelete(selectedItem)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>

            <!-- Basic Info -->
            <el-descriptions :column="3" border size="small">
              <el-descriptions-item label="OID">{{ selectedItem.oid }}</el-descriptions-item>
              <el-descriptions-item label="版本">{{ selectedItem.version }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="getStatusColor(selectedItem.status)">{{ getStatusLabel(selectedItem.status) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="所属概念域">{{ selectedItem.conceptDomainName }}</el-descriptions-item>
              <el-descriptions-item label="所属编码体系">{{ selectedItem.codeSystemName }}</el-descriptions-item>
              <el-descriptions-item label="代码数量">{{ selectedItem.codeCount }}</el-descriptions-item>
              <el-descriptions-item label="描述" :span="3">{{ selectedItem.description }}</el-descriptions-item>
            </el-descriptions>

            <!-- Codes Table -->
            <div class="mt-4">
              <div class="flex items-center justify-between mb-2">
                <span class="font-semibold">代码列表</span>
                <div class="flex items-center gap-2">
                  <el-button size="small" @click="handleExport">
                    <el-icon class="mr-1"><Download /></el-icon>
                    导出
                  </el-button>
                  <el-button size="small" @click="handleImportCodes">
                    <el-icon class="mr-1"><Upload /></el-icon>
                    导入
                  </el-button>
                  <el-button type="primary" size="small" @click="handleAddCode">
                    <el-icon class="mr-1"><Plus /></el-icon>
                    新增
                  </el-button>
                </div>
              </div>

              <el-table
                v-loading="loadingCodes"
                :data="codes"
                row-key="id"
                size="small"
                max-height="280"
              >
                <el-table-column prop="code" label="代码" width="100" />
                <el-table-column prop="displayName" label="显示名" width="150" />
                <el-table-column prop="description" label="描述" width="200" show-overflow-tooltip />
                <el-table-column prop="sortOrder" label="排序" width="60" />
                <el-table-column label="状态" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.isActive ? 'success' : 'info'">
                      {{ row.isActive ? '有效' : '无效' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="有效期" width="180">
                  <template #default="{ row }">
                    {{ row.effectiveDate || '-' }} ~ {{ row.expiryDate || '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100">
                  <template #default="{ row }">
                    <div class="flex items-center gap-2">
                      <el-button link type="primary" size="small" @click="handleEditCode(row)">编辑</el-button>
                      <el-popconfirm title="确认删除？" @confirm="handleDeleteCode(row)">
                        <template #reference>
                          <el-button link type="danger" size="small">删除</el-button>
                        </template>
                      </el-popconfirm>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>

          <el-empty v-else :image-size="60" description="请从左侧选择值集" />
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, List, Upload, Download, Search } from '@element-plus/icons-vue'

const props = defineProps<{
  valueSetId?: number
}>()

const emit = defineEmits<{
  edit: [record: any]
}>()

const keyword = ref('')
const status = ref<string>()
const loading = ref(false)
const loadingCodes = ref(false)
const listData = ref<any[]>([])
const selectedItem = ref<any>()
const codes = ref<any[]>([])
const valueSet = ref<any>()

const getStatusColor = (status: string) => ({
  DRAFT: 'info',
  ACTIVE: 'success',
  RETIRED: 'danger',
}[status] || 'info')

const getStatusLabel = (status: string) => ({
  DRAFT: '草稿',
  ACTIVE: '有效',
  RETIRED: '已废止',
}[status] || status)

const fetchList = async () => {
  loading.value = true
  try {
    // TODO: Call API
    listData.value = []
  } catch (error) {
    ElMessage.error('获取值集列表失败')
  } finally {
    loading.value = false
  }
}

const selectItem = async (item: any) => {
  selectedItem.value = item
  await loadCodes()
}

const loadCodes = async () => {
  if (!selectedItem.value) return
  loadingCodes.value = true
  try {
    // TODO: Call API
    codes.value = []
  } catch (error) {
    ElMessage.error('获取代码列表失败')
  } finally {
    loadingCodes.value = false
  }
}

const handleCreate = () => emit('edit', {})
const handleEdit = (record: any) => emit('edit', record)

const handleDelete = async (record: any) => {
  try {
    ElMessage.success('删除成功')
    fetchList()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const handleAddCode = () => ElMessage.info('新增代码')
const handleEditCode = (record: any) => ElMessage.info('编辑代码')
const handleDeleteCode = async (record: any) => ElMessage.success('删除成功')
const handleImportCodes = () => ElMessage.info('导入代码')
const handleExport = () => ElMessage.info('导出值集')

onMounted(() => fetchList())
</script>
