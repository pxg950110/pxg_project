<template>
  <aside
    class="sidebar-container select-none flex flex-col h-screen z-30 transition-all duration-300 ease-in-out border-r border-slate-800 bg-[#0F172A]"
    :style="{ width: uiStore.sidebarCollapsed ? '64px' : '240px' }"
  >
    <!-- Logo & Brand Header -->
    <div
      class="h-[60px] flex items-center px-4 gap-3 border-b border-slate-800/80 cursor-pointer overflow-hidden flex-shrink-0"
      @click="router.push('/dashboard/workspace')"
    >
      <div class="w-8 h-8 rounded-lg bg-gradient-to-tr from-sky-600 to-cyan-400 flex items-center justify-center flex-shrink-0 shadow-md shadow-sky-500/20">
        <img src="@/assets/logo.svg" alt="MAIDC Logo" class="w-5 h-5 brightness-0 invert" />
      </div>
      <div v-show="!uiStore.sidebarCollapsed" class="flex flex-col overflow-hidden transition-opacity duration-200">
        <span class="text-[15px] font-bold tracking-wide text-white leading-tight whitespace-nowrap">
          MAIDC
        </span>
        <span class="text-[11px] font-normal text-slate-400 whitespace-nowrap leading-tight">
          医疗AI数据中心
        </span>
      </div>
    </div>

    <!-- Navigation Menu Scrollable Area -->
    <el-scrollbar class="flex-1 overflow-x-hidden sidebar-scrollbar">
      <el-menu
        :default-active="activeMenu"
        :collapse="uiStore.sidebarCollapsed"
        :collapse-transition="false"
        class="border-none w-full bg-transparent modern-clinical-menu"
        background-color="#0F172A"
        text-color="#94A3B8"
        active-text-color="#FFFFFF"
        :unique-opened="false"
      >
        <template v-for="route in menuRoutes" :key="route.name || route.path">
          <!-- Multi-level Submenu -->
          <el-sub-menu
            v-if="hasVisibleChildren(route)"
            :index="String(route.name || route.path)"
            popper-class="clinical-menu-popper"
          >
            <template #title>
              <el-icon v-if="getIcon(route.meta?.icon)" class="menu-icon text-[17px]">
                <component :is="getIcon(route.meta?.icon)" />
              </el-icon>
              <span class="menu-title font-medium text-[13px]">{{ route.meta?.title }}</span>
            </template>

            <template v-for="child in getVisibleChildren(route)" :key="child.name || child.path">
              <!-- 3rd Level Submenu -->
              <el-sub-menu
                v-if="hasVisibleChildren(child)"
                :index="String(child.name || child.path)"
                popper-class="clinical-menu-popper"
              >
                <template #title>
                  <span class="menu-title text-[13px]">{{ child.meta?.title }}</span>
                </template>
                <el-menu-item
                  v-for="leaf in getVisibleChildren(child)"
                  :key="leaf.name || leaf.path"
                  :index="String(leaf.name || leaf.path)"
                  @click="handleNav(leaf)"
                  class="menu-leaf-item"
                >
                  <span class="menu-title text-[13px]">{{ leaf.meta?.title }}</span>
                </el-menu-item>
              </el-sub-menu>

              <!-- 2nd Level Leaf -->
              <el-menu-item
                v-else
                :index="String(child.name || child.path)"
                @click="handleNav(child)"
                class="menu-leaf-item"
              >
                <span class="menu-title text-[13px]">{{ child.meta?.title }}</span>
              </el-menu-item>
            </template>
          </el-sub-menu>

          <!-- Top-level Single Item -->
          <el-menu-item
            v-else
            :index="String(route.name || route.path)"
            @click="handleNav(route)"
            class="menu-leaf-item"
          >
            <el-icon v-if="getIcon(route.meta?.icon)" class="menu-icon text-[17px]">
              <component :is="getIcon(route.meta?.icon)" />
            </el-icon>
            <template #title>
              <span class="menu-title font-medium text-[13px]">{{ route.meta?.title }}</span>
            </template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-scrollbar>

    <!-- Bottom System Status / Version Info -->
    <div
      v-if="!uiStore.sidebarCollapsed"
      class="px-4 py-3 border-t border-slate-800/80 bg-slate-900/50 flex items-center justify-between text-xs text-slate-400"
    >
      <div class="flex items-center gap-2">
        <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
        <span class="text-slate-300 font-mono text-[11px]">系统运行正常</span>
      </div>
      <span class="text-[10px] text-slate-400 font-mono">v1.2.0</span>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUiStore } from '@/stores/ui'
import { usePermissionStore } from '@/stores/permission'
import {
  Odometer,
  FirstAidKit,
  Coin,
  Operation,
  DataAnalysis,
  Edit,
  Document,
  Warning,
  Files,
  Bell,
  Setting,
  Menu as MenuIcon,
} from '@element-plus/icons-vue'

const uiStore = useUiStore()
const permissionStore = usePermissionStore()
const route = useRoute()
const router = useRouter()

// Comprehensive icon mapping from both AntD names and general icons to Element Plus icons
const iconMap: Record<string, any> = {
  // AntD naming used in asyncRoutes.ts
  DashboardOutlined: Odometer,
  MedicineBoxOutlined: FirstAidKit,
  DatabaseOutlined: Coin,
  ExperimentOutlined: Operation,
  SwapOutlined: DataAnalysis,
  EditOutlined: Edit,
  ScheduleOutlined: Document,
  AlertOutlined: Warning,
  FileSearchOutlined: Files,
  BellOutlined: Bell,
  SettingOutlined: Setting,

  // Standard Element Plus aliases
  Odometer,
  FirstAidKit,
  Coin,
  Operation,
  DataAnalysis,
  Edit,
  Document,
  Warning,
  Files,
  Bell,
  Setting,
}

function getIcon(iconName: unknown): any {
  if (typeof iconName === 'string' && iconMap[iconName]) {
    return iconMap[iconName]
  }
  return MenuIcon
}

const menuRoutes = computed(() => {
  const root = permissionStore.routes[0]
  let list = permissionStore.routes
  if (root?.path === '/' && root.children?.length) {
    list = root.children
  }
  return list
    .filter((r: any) => !r.meta?.hidden)
    .sort((a: any, b: any) => (a.meta?.sort ?? 99) - (b.meta?.sort ?? 99))
})

const activeMenu = computed(() => {
  const { meta, name } = route
  if (meta?.activeMenu) {
    return String(meta.activeMenu)
  }
  return String(name || route.path)
})

function hasVisibleChildren(item: any): boolean {
  if (!item.children || item.children.length === 0) return false
  return item.children.some((c: any) => !c.meta?.hidden)
}

function getVisibleChildren(item: any): any[] {
  if (!item.children) return []
  return item.children.filter((c: any) => !c.meta?.hidden)
}

function handleNav(item: any) {
  if (item.name) {
    router.push({ name: item.name }).catch(() => {})
  } else if (item.path) {
    router.push(item.path).catch(() => {})
  }
}
</script>

<style lang="scss" scoped>
.sidebar-container {
  box-shadow: 4px 0 16px -2px rgba(15, 23, 42, 0.4);
}

.modern-clinical-menu {
  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 44px;
    line-height: 44px;
    margin: 4px 8px;
    border-radius: 8px;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    color: #94A3B8;

    &:hover {
      background-color: #1E293B !important;
      color: #F8FAFC !important;

      .el-icon {
        color: #38BDF8 !important;
      }
    }
  }

  :deep(.el-menu-item.is-active) {
    background: linear-gradient(135deg, #0284C7 0%, #0EA5E9 100%) !important;
    color: #FFFFFF !important;
    font-weight: 600;
    box-shadow: 0 4px 12px rgba(14, 165, 233, 0.35);

    .el-icon {
      color: #FFFFFF !important;
    }
  }

  :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
    color: #38BDF8 !important;
    font-weight: 600;

    .el-icon {
      color: #38BDF8 !important;
    }
  }

  :deep(.el-menu--inline) {
    background-color: #090E17 !important;
    padding: 4px 0;
  }
}
</style>

<style lang="scss">
/* Clinical dropdown popper for collapsed menu */
.clinical-menu-popper {
  .el-menu--popup {
    background-color: #0F172A !important;
    border: 1px solid #1E293B !important;
    border-radius: 8px !important;
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.5) !important;
    padding: 6px !important;

    .el-menu-item {
      height: 38px;
      line-height: 38px;
      border-radius: 6px;
      margin: 2px 0;
      color: #94A3B8;

      &:hover {
        background-color: #1E293B !important;
        color: #FFFFFF !important;
      }

      &.is-active {
        background-color: #0284C7 !important;
        color: #FFFFFF !important;
      }
    }
  }
}
</style>
