<template>
  <div class="h-full">
    <!-- Header -->
    <el-card shadow="never" class="!rounded-xl !border-slate-200/80 mb-3">
      <div class="flex items-center gap-4">
        <el-icon :size="24" color="#8b5cf6"><Coin /></el-icon>
        <div class="min-w-0 flex-1">
          <div class="text-base font-semibold">{{ codeSystem?.name || '编码体系列表' }}</div>
          <div class="text-xs text-slate-400">{{ codeSystem?.oid || '管理所有编码体系' }}</div>
        </div>
        <el-input
          v-model="keyword"
          placeholder="搜索编码体系"
          clearable
          class="!w-60"
          @keyup.enter="fetchList"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
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
              :class="selectedItem?.id === item.id ? 'bg-violet-50' : ''"
              @click="selectItem(item)"
            >
              <el-avatar :size="32" class="shrink-0" style="background-color: #8b5cf6">
                {{ item.name?.charAt(0) }}
              </el-avatar>
              <div class="min-w-0">
                <div class="font-medium text-sm">{{ item.name }}</div>
                <div class="text-xs text-slate-500">{{ item.oid }}</div>
                <div class="text-xs text-slate-400 mt-1">{{ item.description }}</div>
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
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="OID">{{ selectedItem.oid }}</el-descriptions-item>
              <el-descriptions-item label="名称">{{ selectedItem.name }}</el-descriptions-item>
              <el-descriptions-item label="版本">{{ selectedItem.version }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="selectedItem.status === 'ACTIVE' ? 'success' : 'info'">
                  {{ selectedItem.status }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">{{ selectedItem.description }}</el-descriptions-item>
              <el-descriptions-item label="来源">{{ selectedItem.source }}</el-descriptions-item>
              <el-descriptions-item label="URI">{{ selectedItem.uri }}</el-descriptions-item>
            </el-descriptions>

            <!-- Concepts/Codes Table -->
            <div class="mt-4">
              <div class="flex items-center justify-between mb-2">
                <span class="font-semibold">编码列表</span>
                <div class="flex items-center gap-2">
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
                max-height="300"
              >
                <el-table-column prop="code" label="代码" width="100" />
                <el-table-column prop="displayName" label="显示名" width="150" />
                <el-table-column prop="description" label="描述" show-overflow-tooltip />
                <el-table-column prop="sortOrder" label="排序" width="60" />
                <el-table-column label="状态" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.isActive ? 'success' : 'info'">
                      {{ row.isActive ? '有效' : '无效' }}
                    </el-tag>
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

          <el-empty v-else :image-size="60" description="请从左侧选择编码体系" />
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Coin, Upload, Search } from '@element-plus/icons-vue'

const props = defineProps<{
  codeSystemId?: number
}>()

const emit = defineEmits<{
  edit: [record: any]
}>()

const keyword = ref('')
const loading = ref(false)
const loadingCodes = ref(false)
const listData = ref<any[]>([])
const selectedItem = ref<any>()
const codes = ref<any[]>([])
const codeSystem = ref<any>()

const fetchList = async () => {
  loading.value = true
  try {
    // TODO: Call API
    listData.value = []
  } catch (error) {
    ElMessage.error('获取编码体系列表失败')
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
    ElMessage.error('获取编码列表失败')
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

const handleAddCode = () => ElMessage.info('新增编码')
const handleEditCode = (record: any) => ElMessage.info('编辑编码')
const handleDeleteCode = async (record: any) => ElMessage.success('删除成功')
const handleImportCodes = () => ElMessage.info('导入编码')

onMounted(() => fetchList())
</script>
