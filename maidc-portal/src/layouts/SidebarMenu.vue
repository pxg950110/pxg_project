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
          <template v-if="!child.meta?.hidden">
            <!-- Level 3: sub-menu inside sub-menu -->
            <a-sub-menu v-if="child.children?.filter(c => !c.meta?.hidden).length" :key="child.name">
              <template #title>
                <span>{{ child.meta?.title }}</span>
              </template>
              <template v-for="leaf in child.children" :key="leaf.name">
                <a-menu-item v-if="!leaf.meta?.hidden" :key="leaf.name">
                  <span>{{ leaf.meta?.title }}</span>
                </a-menu-item>
              </template>
            </a-sub-menu>
            <!-- Level 2 leaf -->
            <a-menu-item v-else :key="child.name">
              <span>{{ child.meta?.title }}</span>
            </a-menu-item>
          </template>
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
  SwapOutlined,
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
  SwapOutlined,
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

const menuRoutes = computed(() => {
  const root = permissionStore.routes[0]
  if (root?.path === '/' && root.children?.length) {
    return root.children
  }
  return permissionStore.routes
})

const selectedKeys = computed(() => [String(route.name)])
const openKeys = ref<string[]>([])

watch(
  () => route.name,
  (name) => {
    for (const parent of menuRoutes.value) {
      for (const child of parent.children || []) {
        // Level 3 match
        if (child.children?.some((c: any) => c.name === name)) {
          const pKey = parent.name as string
          const cKey = child.name as string
          const keys = [...openKeys.value]
          if (!keys.includes(pKey)) keys.push(pKey)
          if (!keys.includes(cKey)) keys.push(cKey)
          openKeys.value = keys
          return
        }
        // Level 2 match
        if (child.name === name) {
          const pKey = parent.name as string
          if (!openKeys.value.includes(pKey)) {
            openKeys.value = [...openKeys.value, pKey]
          }
          return
        }
      }
    }
  },
  { immediate: true },
)

function onOpenChange(keys: string[]) {
  // Build parent map: key -> parent key (or null for top-level)
  const parentOf = new Map<string, string | null>()
  for (const parent of menuRoutes.value) {
    parentOf.set(parent.name as string, null)
    for (const child of parent.children || []) {
      parentOf.set(child.name as string, parent.name as string)
    }
  }

  const latest = keys.find(k => !openKeys.value.includes(k))
  if (!latest) {
    // Something was closed
    const closed = openKeys.value.find(k => !keys.includes(k))
    if (!closed) { openKeys.value = keys; return }
    // When closing a level-1 item, also close its children
    const childKeys = keys.filter(k => parentOf.get(k) === closed)
    openKeys.value = keys.filter(k => !childKeys.includes(k) && k !== closed)
  } else {
    // Something was opened: close siblings at the same level, keep ancestors
    const parentKey = parentOf.get(latest)
    const next = openKeys.value.filter(k => parentOf.get(k) !== parentKey)
    next.push(latest)
    openKeys.value = next
  }
}

function onMenuClick({ key }: { key: string }) {
  router.push({ name: key }).catch(() => {})
}
</script>
