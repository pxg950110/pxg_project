<template>
  <PageContainer title="系统配置" subtitle="管理系统全局参数与基础配置">
    <div v-loading="loading" class="min-h-[200px]">
    <div class="config-cards">
      <div v-for="group in configGroups" :key="group.key" class="config-card">
        <div class="config-card-header">
          <component :is="group.icon" class="card-icon" />
          <span class="card-title">{{ group.title }}</span>
        </div>
        <div class="config-card-body">
          <div v-for="item in group.items" :key="item.label" class="config-row">
            <span class="config-label">{{ item.label }}</span>
            <div class="config-value-area">
              <template v-if="item.editing">
                <el-input
                  v-model="item.editValue"
                  size="small"
                  class="config-input"
                  @keyup.enter="saveItem(item)"
                />
              </template>
              <template v-else>
                <span class="config-value">{{ item.value }}</span>
              </template>
            </div>
            <div class="config-action">
              <template v-if="item.editing">
                <a class="action-link save-link" @click="saveItem(item)">保存</a>
                <a class="action-link cancel-link" @click="cancelEdit(item)">取消</a>
              </template>
              <template v-else>
                <a class="action-link" @click="startEdit(item)">编辑</a>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  Setting,
  Coin,
  CircleCheck,
  Bell
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import { getConfigs, updateConfig } from '@/api/system'

interface ConfigItem {
  id: number
  label: string
  value: string
  editValue: string
  editing: boolean
  configKey: string
  configGroup: string
}

interface ConfigGroup {
  key: string
  title: string
  icon: typeof Setting
  items: ConfigItem[]
}

const groupIconMap: Record<string, typeof Setting> = {
  basic: Setting,
  storage: Coin,
  security: CircleCheck,
  notification: Bell
}

const groupTitleMap: Record<string, string> = {
  basic: '基础配置',
  storage: '存储配置',
  security: '安全配置',
  notification: '通知配置'
}

const loading = ref(false)
const configGroups = ref<ConfigGroup[]>([])

function createItem(raw: any): ConfigItem {
  return reactive({
    id: raw.id,
    label: raw.description || raw.config_key,
    value: raw.config_value ?? '',
    editValue: raw.config_value ?? '',
    editing: false,
    configKey: raw.config_key,
    configGroup: raw.config_group
  })
}

async function loadConfigs() {
  loading.value = true
  try {
    const res = await getConfigs({ page: 1, page_size: 100 })
    const pageData = (res.data as any)?.data || (res.data as any)
    const items = pageData?.items || pageData?.records || []
    // Group items by config_group
    const groupMap = new Map<string, ConfigItem[]>()
    for (const raw of items) {
      const group = raw.config_group || 'basic'
      if (!groupMap.has(group)) {
        groupMap.set(group, [])
      }
      groupMap.get(group)!.push(createItem(raw))
    }
    configGroups.value = Array.from(groupMap.entries()).map(([key, items]) => ({
      key,
      title: groupTitleMap[key] || key,
      icon: groupIconMap[key] || Setting,
      items
    }))
  } catch {
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

function startEdit(item: ConfigItem) {
  item.editValue = item.value
  item.editing = true
}

function cancelEdit(item: ConfigItem) {
  item.editValue = item.value
  item.editing = false
}

async function saveItem(item: ConfigItem) {
  try {
    await updateConfig(item.id, {
      config_key: item.configKey,
      config_value: item.editValue,
      config_group: item.configGroup,
      description: item.label
    })
    item.value = item.editValue
    item.editing = false
    ElMessage.success('配置保存成功')
    await loadConfigs()
  } catch {
    ElMessage.error('保存配置失败')
  }
}

onMounted(loadConfigs)
</script>

<style scoped>
.config-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-card {
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  overflow: hidden;
}

.config-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
  background: #f8fafc;
}

.card-icon {
  font-size: 18px;
  color: #0ea5e9;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.config-card-body {
  padding: 0;
}

.config-row {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid #f8fafc;
}

.config-row:last-child {
  border-bottom: none;
}

.config-label {
  flex-shrink: 0;
  width: 180px;
  font-size: 14px;
  color: #64748b;
}

.config-value-area {
  flex: 1;
  min-width: 0;
}

.config-value {
  font-size: 14px;
  color: #0f172a;
}

.config-input {
  max-width: 320px;
}

.config-action {
  flex-shrink: 0;
  margin-left: 16px;
  display: flex;
  gap: 12px;
}

.action-link {
  font-size: 14px;
  color: #0ea5e9;
  cursor: pointer;
  white-space: nowrap;
}

.action-link:hover {
  color: #38bdf8;
}

.cancel-link {
  color: #94a3b8;
}

.cancel-link:hover {
  color: #64748b;
}
</style>
