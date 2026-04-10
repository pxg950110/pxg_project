<template>
  <PageContainer title="通知设置">
    <a-card v-for="channel in channels" :key="channel.key" :title="channel.label" style="margin-bottom: 16px">
      <a-table :columns="columns" :data-source="getSettingsByChannel(channel.key)" :loading="loading" row-key="id" size="small" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'enabled'">
            <a-switch :checked="record.enabled" @change="(v: boolean) => handleToggle(record, v)" />
          </template>
        </template>
      </a-table>
    </a-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { getNotificationSettings, updateNotificationSetting } from '@/api/msg'

const loading = ref(false)
const settings = ref<any[]>([])

const channels = [
  { key: 'IN_APP', label: '站内通知' },
  { key: 'EMAIL', label: '邮件通知' },
  { key: 'SMS', label: '短信通知' },
]

const columns = [
  { title: '事件类型', dataIndex: 'event_type', key: 'event_type' },
  { title: '事件说明', dataIndex: 'event_desc', key: 'event_desc' },
  { title: '启用', dataIndex: 'enabled', key: 'enabled', width: 80 },
]

function getSettingsByChannel(channel: string) {
  return settings.value.filter(s => s.channel === channel)
}

async function loadSettings() {
  loading.value = true
  try {
    const res = await getNotificationSettings()
    settings.value = res.data.data
  } finally { loading.value = false }
}

async function handleToggle(record: any, enabled: boolean) {
  await updateNotificationSetting(record.id, { ...record, enabled })
  message.success('设置已更新')
  record.enabled = enabled
}

onMounted(loadSettings)
</script>
