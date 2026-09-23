<template>
  <div class="welcome-section relative overflow-hidden rounded-xl bg-gradient-to-r from-sky-600 via-sky-700 to-slate-900 p-6 text-white shadow-clinical mb-4 flex flex-col md:flex-row md:items-center justify-between gap-4">
    <!-- 装饰性科技背景网格纹理 -->
    <div class="absolute inset-0 opacity-10 pointer-events-none bg-[radial-gradient(#fff_1px,transparent_1px)] [background-size:16px_16px]" />

    <div class="relative z-10 space-y-1.5 min-w-0">
      <div class="flex items-center gap-3 flex-wrap">
        <h2 class="text-xl font-bold tracking-tight m-0 text-white flex items-center gap-2">
          <span>{{ greeting }}，{{ userName }}</span>
        </h2>
        <span
          v-if="role"
          class="text-xs px-2.5 py-0.5 rounded-full bg-white/20 text-sky-100 font-medium border border-white/20 backdrop-blur-sm"
        >
          {{ role }}
        </span>
      </div>

      <p class="text-xs text-sky-100/80 m-0 flex items-center gap-2 flex-wrap font-sans">
        <span>{{ date }}</span>
        <span v-if="orgName || deptName" class="text-sky-200">
          · {{ [orgName, deptName].filter(Boolean).join(' · ') }}
        </span>
      </p>
    </div>

    <!-- 快捷患者直达检索框（权限控制） -->
    <div v-if="canSearchPatient" class="relative z-10 w-full md:w-80">
      <el-input
        v-model="keyword"
        placeholder="快速搜索患者（姓名/病案号）..."
        clearable
        size="default"
        class="search-input !rounded-lg"
        @keyup.enter="handlePatientSearch"
      >
        <template #prefix>
          <el-icon class="text-slate-400"><Search /></el-icon>
        </template>
        <template #append>
          <el-button type="primary" class="!bg-sky-500 !text-white !border-0" @click="handlePatientSearch">
            搜索
          </el-button>
        </template>
      </el-input>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { usePermissionStore } from '@/stores/permission'

const props = defineProps<{
  userName: string
  date: string
  role?: string
  orgName?: string | null
  deptName?: string | null
}>()

const router = useRouter()
const permissionStore = usePermissionStore()
const keyword = ref('')

const canSearchPatient = computed(() => permissionStore.hasPermission('cdr:read'))

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

function handlePatientSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  router.push({ path: '/data/cdr/patients', query: { keyword: kw } })
}
</script>

<style scoped>
:deep(.search-input .el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.95);
  box-shadow: none !important;
  border-radius: 8px 0 0 8px;
}
:deep(.search-input .el-input-group__append) {
  background-color: #0ea5e9;
  border: none;
  border-radius: 0 8px 8px 0;
}
</style>
