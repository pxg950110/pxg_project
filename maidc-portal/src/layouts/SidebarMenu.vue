<template>
  <a-menu
    mode="inline"
    :selected-keys="selectedKeys"
    :open-keys="openKeys"
    :inline-collapsed="collapsed"
    @openChange="onOpenChange"
    @click="onMenuClick"
  >
    <template v-for="route in menuRoutes" :key="route.name">
      <a-sub-menu v-if="route.children?.filter(c => !c.meta?.hidden).length" :key="route.name">
        <template #title>
          <component :is="iconMap[route.meta?.icon as string]" v-if="route.meta?.icon" />
          <span>{{ route.meta?.title }}</span>
        </template>
        <template v-for="child in route.children" :key="child.name">
          <a-menu-item v-if="!child.meta?.hidden" :key="child.name">
            <span>{{ child.meta?.title }}</span>
          </a-menu-item>
        </template>
      </a-sub-menu>
      <a-menu-item v-else-if="!route.meta?.hidden" :key="route.name">
        <component :is="iconMap[route.meta?.icon as string]" v-if="route.meta?.icon" />
        <span>{{ route.meta?.title }}</span>
      </a-menu-item>
    </template>
  </a-menu>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePermissionStore } from '@/stores/permission'
import {
  DashboardOutlined,
  ExperimentOutlined,
  DatabaseOutlined,
  EditOutlined,
  ScheduleOutlined,
  AlertOutlined,
  FileSearchOutlined,
  BellOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'

defineProps<{ collapsed: boolean }>()

const iconMap: Record<string, any> = {
  DashboardOutlined,
  ExperimentOutlined,
  DatabaseOutlined,
  EditOutlined,
  ScheduleOutlined,
  AlertOutlined,
  FileSearchOutlined,
  BellOutlined,
  SettingOutlined,
}

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()

// Unwrap root "/" route to get top-level menu groups (Dashboard, Model, Data, etc.)
const menuRoutes = computed(() => {
  const root = permissionStore.routes[0]
  if (root?.path === '/' && root.children?.length) {
    return root.children
  }
  return permissionStore.routes
})

const selectedKeys = computed(() => [String(route.name)])
const openKeys = ref<string[]>([])

// Auto-open parent menu based on current route
watch(
  () => route.name,
  (name) => {
    for (const parent of permissionStore.routes) {
      if (parent.children?.some(c => c.name === name)) {
        if (!openKeys.value.includes(parent.name as string)) {
          openKeys.value.push(parent.name as string)
        }
      }
    }
  },
  { immediate: true },
)

function onOpenChange(keys: string[]) {
  openKeys.value = keys
}

function onMenuClick({ key }: { key: string }) {
  router.push({ name: key }).catch(() => {})
}
</script>
