<template>
  <header class="h-[60px] bg-white border-b border-slate-200/80 px-4 md:px-6 flex items-center justify-between z-20 sticky top-0 shadow-xs select-none">
    <!-- Left: Collapse toggle + Breadcrumb navigation -->
    <div class="flex items-center gap-4 min-w-0">
      <button
        type="button"
        class="w-8 h-8 rounded-lg flex items-center justify-center text-slate-600 hover:text-sky-600 hover:bg-slate-100 transition-colors focus:outline-none cursor-pointer"
        :title="uiStore.sidebarCollapsed ? '展开菜单' : '折叠菜单'"
        @click="uiStore.toggleSidebar()"
      >
        <el-icon class="text-lg">
          <Expand v-if="uiStore.sidebarCollapsed" />
          <Fold v-else />
        </el-icon>
      </button>

      <!-- Breadcrumb Navigation -->
      <el-breadcrumb separator="/" class="hidden sm:flex items-center text-xs md:text-sm">
        <el-breadcrumb-item
          v-for="(item, index) in breadcrumbList"
          :key="index"
          :to="item.path ? { path: item.path } : undefined"
          class="transition-colors hover:text-sky-600"
        >
          <span
            :class="[
              index === breadcrumbList.length - 1
                ? 'font-semibold text-slate-800'
                : 'text-slate-500 font-normal hover:text-sky-600'
            ]"
          >
            {{ item.title }}
          </span>
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- Center: Quick Global Clinical Search Input -->
    <div class="hidden lg:flex items-center justify-center flex-1 max-w-md mx-6">
      <el-input
        ref="searchInputRef"
        v-model="searchKeyword"
        placeholder="快速检索患者、临床指标、知识库..."
        class="clinical-search-input w-full"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon class="text-slate-400"><Search /></el-icon>
        </template>
        <template #suffix>
          <div class="flex items-center">
            <kbd class="hidden xl:inline-flex items-center px-1.5 py-0.5 text-[10px] font-mono bg-slate-100 text-slate-500 rounded border border-slate-200">
              Ctrl K
            </kbd>
          </div>
        </template>
      </el-input>
    </div>

    <!-- Right: Department/Role tag, Notifications, User Dropdown -->
    <div class="flex items-center gap-3 md:gap-4 flex-shrink-0">
      <!-- Department / Role Badge -->
      <el-tag
        effect="light"
        class="hidden md:inline-flex items-center gap-1.5 px-2.5 py-1 text-xs border border-sky-200 bg-sky-50 text-sky-700 font-medium rounded-md"
      >
        <span class="w-1.5 h-1.5 rounded-full bg-sky-500"></span>
        <span>{{ departmentRoleText }}</span>
      </el-tag>

      <!-- Unread Notifications Popover -->
      <el-popover
        v-model:visible="msgPopoverVisible"
        trigger="click"
        placement="bottom-end"
        :width="360"
        popper-class="p-0 shadow-lg rounded-xl border border-slate-200 overflow-hidden"
      >
        <template #reference>
          <button
            type="button"
            class="relative w-8 h-8 rounded-lg flex items-center justify-center text-slate-600 hover:text-sky-600 hover:bg-slate-100 transition-colors focus:outline-none cursor-pointer"
            title="通知中心"
          >
            <el-badge :value="unreadCount" :max="99" :hidden="unreadCount === 0">
              <el-icon class="text-lg"><Bell /></el-icon>
            </el-badge>
          </button>
        </template>

        <!-- Message Panel Content -->
        <div class="msg-panel flex flex-col bg-white">
          <div class="flex items-center justify-between px-4 py-3 border-b border-slate-100 bg-slate-50/50">
            <span class="font-semibold text-sm text-slate-800">消息通知</span>
            <button
              type="button"
              class="text-xs text-sky-600 hover:text-sky-700 transition-colors cursor-pointer"
              @click="handleMarkAllRead"
            >
              全部已读
            </button>
          </div>

          <div class="flex gap-2 px-4 py-2 border-b border-slate-100">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              type="button"
              :class="[
                'text-xs px-2.5 py-1 rounded-md transition-colors cursor-pointer',
                activeTab === tab.key
                  ? 'bg-sky-500 text-white font-medium'
                  : 'text-slate-600 hover:bg-slate-100'
              ]"
              @click="activeTab = tab.key"
            >
              {{ tab.label }}
            </button>
          </div>

          <div class="max-h-[300px] overflow-y-auto divide-y divide-slate-100">
            <div v-if="loading" class="flex items-center justify-center py-8">
              <el-icon class="is-loading text-xl text-sky-500"><Loading /></el-icon>
            </div>
            <template v-else>
              <div
                v-for="msg in filteredMessages"
                :key="msg.id"
                :class="[
                  'p-3 cursor-pointer transition-colors flex gap-2.5 items-start',
                  msg.isRead ? 'hover:bg-slate-50' : 'bg-sky-50/40 hover:bg-sky-50/70'
                ]"
                @click="handleMsgClick(msg)"
              >
                <span
                  class="w-2 h-2 rounded-full mt-1.5 flex-shrink-0"
                  :style="{ backgroundColor: msg.typeColor }"
                ></span>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center justify-between gap-2">
                    <span class="text-xs font-medium text-slate-800 truncate">{{ msg.title }}</span>
                    <span class="text-[11px] text-slate-400 whitespace-nowrap">{{ msg.time }}</span>
                  </div>
                  <div class="text-[11px] text-slate-500 mt-1 line-clamp-1">{{ msg.content || msg.typeLabel }}</div>
                </div>
              </div>
              <div v-if="filteredMessages.length === 0" class="text-center py-8 text-slate-400 text-xs">
                暂无消息
              </div>
            </template>
          </div>

          <div class="text-center py-2.5 border-t border-slate-100 bg-slate-50/50">
            <button
              type="button"
              class="text-xs text-sky-600 hover:text-sky-700 font-medium transition-colors cursor-pointer"
              @click="goMessagePage"
            >
              查看全部消息 &rarr;
            </button>
          </div>
        </div>
      </el-popover>

      <!-- Settings Drawer Trigger -->
      <button
        type="button"
        class="w-8 h-8 rounded-lg flex items-center justify-center text-slate-600 hover:text-sky-600 hover:bg-slate-100 transition-colors focus:outline-none cursor-pointer"
        title="个性化设置"
        @click="settingsVisible = true"
      >
        <el-icon class="text-lg"><Setting /></el-icon>
      </button>

      <!-- User Avatar & Dropdown -->
      <el-dropdown trigger="click" @command="handleUserCommand">
        <div class="flex items-center gap-2 cursor-pointer p-1 rounded-lg hover:bg-slate-100 transition-colors">
          <el-avatar
            :size="30"
            class="bg-gradient-to-tr from-sky-500 to-cyan-500 text-white font-bold text-xs shadow-xs"
          >
            {{ avatarText }}
          </el-avatar>
          <div class="hidden sm:flex flex-col items-start leading-tight">
            <span class="text-xs font-semibold text-slate-800">{{ displayName }}</span>
            <span class="text-[10px] text-slate-400">{{ primaryRoleName }}</span>
          </div>
          <el-icon class="text-xs text-slate-400 ml-0.5"><ArrowDown /></el-icon>
        </div>

        <template #dropdown>
          <el-dropdown-menu class="w-44">
            <div class="px-4 py-2 border-b border-slate-100">
              <p class="text-xs font-semibold text-slate-800">{{ displayName }}</p>
              <p class="text-[11px] text-slate-400 truncate">{{ authStore.userInfo?.username }}</p>
            </div>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>
              <span>个人中心</span>
            </el-dropdown-item>
            <el-dropdown-item command="config">
              <el-icon><Operation /></el-icon>
              <span>系统设置</span>
            </el-dropdown-item>
            <el-dropdown-item divided command="logout" class="text-red-500 hover:text-red-600">
              <el-icon><SwitchButton /></el-icon>
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- Existing Settings Drawer preserved -->
    <SettingsDrawer v-model:visible="settingsVisible" />
  </header>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  Fold,
  Expand,
  Search,
  Bell,
  Setting,
  User,
  Operation,
  SwitchButton,
  ArrowDown,
  Loading,
} from '@element-plus/icons-vue'
import { getMessages, markAsRead, markAllAsRead } from '@/api/msg'
import SettingsDrawer from './SettingsDrawer.vue'

interface MessageItem {
  id: number
  type: string
  typeLabel: string
  typeColor: string
  title: string
  content: string
  time: string
  isRead: boolean
}

const TYPE_MAP: Record<string, { label: string; color: string }> = {
  SYSTEM: { label: '系统', color: '#0EA5E9' },
  ALERT: { label: '告警', color: '#EF4444' },
  APPROVAL: { label: '审批', color: '#10B981' },
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const uiStore = useUiStore()

const searchKeyword = ref('')
const searchInputRef = ref<any>(null)
const settingsVisible = ref(false)
const msgPopoverVisible = ref(false)
const messages = ref<MessageItem[]>([])
const loading = ref(false)
const activeTab = ref('all')

// Department and role display badge
const departmentRoleText = computed(() => {
  const roles = authStore.userInfo?.roles || []
  if (roles.includes('admin')) {
    return '医疗数据中心 / 超级管理'
  }
  if (roles.includes('clinician')) {
    return '呼吸内科 / 临床科研'
  }
  if (roles.includes('researcher')) {
    return '医学转化中心 / 科研PI'
  }
  return '临床医研协同中心'
})

const displayName = computed(() => {
  return authStore.userInfo?.realName || authStore.userInfo?.username || '临床医生'
})

const primaryRoleName = computed(() => {
  const roles = authStore.userInfo?.roles || []
  if (roles.includes('admin')) return '超级管理员'
  if (roles.includes('clinician')) return '临床主任医师'
  if (roles.includes('researcher')) return '科研研究员'
  return '临床业务专家'
})

const avatarText = computed(() => {
  const name = displayName.value
  return name.charAt(0).toUpperCase()
})

// Breadcrumbs computed from route matched
const breadcrumbList = computed(() => {
  const matched = route.matched.filter(r => r.meta && r.meta.title && !r.meta.hiddenBreadcrumb)
  const list: Array<{ title: string; path?: string }> = []

  const first = matched[0]
  if (first && first.path !== '/dashboard/workspace' && first.name !== 'DashboardWorkspace') {
    list.push({ title: '工作台', path: '/dashboard/workspace' })
  }

  matched.forEach((item, index) => {
    // If it's not the last one and has a valid redirect or path
    const isLast = index === matched.length - 1
    list.push({
      title: item.meta.title as string,
      path: isLast ? undefined : (item.redirect as string || item.path),
    })
  })

  return list
})

// Quick global search handler
function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({
      path: '/data/cdr/search',
      query: { q: searchKeyword.value.trim() },
    })
  } else {
    router.push('/data/cdr/search')
  }
}

// Global Ctrl+K / Cmd+K keyboard shortcut
function handleGlobalKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    searchInputRef.value?.focus()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleGlobalKeydown)
  fetchMessages()
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalKeydown)
})

// Messages & Notifications
const unreadCount = computed(() => messages.value.filter(m => !m.isRead).length)

const tabs = computed(() => [
  { key: 'all', label: '全部' },
  { key: 'unread', label: `未读 (${unreadCount.value})` },
])

const filteredMessages = computed(() => {
  const list = messages.value.map(m => ({
    ...m,
    typeLabel: m.typeLabel || TYPE_MAP[m.type]?.label || m.type,
    typeColor: m.typeColor || TYPE_MAP[m.type]?.color || '#0EA5E9',
  }))
  if (activeTab.value === 'unread') return list.filter(m => !m.isRead)
  return list
})

async function fetchMessages() {
  loading.value = true
  try {
    const res = await getMessages({ page: 1, page_size: 20 })
    messages.value = res.data.data?.items || []
  } catch {
    messages.value = []
  } finally {
    loading.value = false
  }
}

async function handleMsgClick(msg: MessageItem) {
  if (!msg.isRead) {
    try {
      await markAsRead(msg.id)
      const target = messages.value.find(m => m.id === msg.id)
      if (target) target.isRead = true
    } catch { /* ignore */ }
  }
}

async function handleMarkAllRead() {
  try {
    await markAllAsRead()
    messages.value.forEach(m => { m.isRead = true })
    ElMessage.success('已全部标记为已读')
  } catch { /* ignore */ }
}

function goMessagePage() {
  msgPopoverVisible.value = false
  router.push('/message/list')
}

watch(msgPopoverVisible, (visible) => {
  if (visible) fetchMessages()
})

// User action dropdown handler
function handleUserCommand(command: string) {
  if (command === 'profile') {
    router.push('/system/users')
  } else if (command === 'config') {
    router.push('/system/config')
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录医疗AI数据中心吗？', '确认退出', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(async () => {
      await authStore.logoutAction()
      ElMessage.success('已安全退出')
      router.push('/login')
    }).catch(() => {})
  }
}
</script>

<style lang="scss" scoped>
.clinical-search-input {
  :deep(.el-input__wrapper) {
    background-color: #F8FAFC;
    border-radius: 9999px;
    box-shadow: 0 0 0 1px #E2E8F0 inset;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    padding-left: 12px;
    padding-right: 12px;

    &.is-focus {
      background-color: #FFFFFF;
      box-shadow: 0 0 0 2px #0EA5E9 inset !important;
    }
  }
}
</style>
