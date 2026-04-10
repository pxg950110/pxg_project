<template>
  <a-menu
    mode="inline"
    :selected-keys="selectedKeys"
    :open-keys="openKeys"
    :inline-collapsed="collapsed"
    @openChange="onOpenChange"
    @click="onMenuClick"
  >
    <template v-for="route in permissionStore.routes" :key="route.name">
      <a-sub-menu v-if="route.children?.length" :key="route.name">
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
      <a-menu-item v-else :key="route.name">
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
  router.push({ name: key })
}
</script>
