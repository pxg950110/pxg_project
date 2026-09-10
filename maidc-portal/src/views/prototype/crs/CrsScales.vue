<template>
  <PageContainer title="量表管理 · 页面原型" :breadcrumb="[{ title: '原型' }, { title: '量表管理' }]">
    <template #extra>
      <a-button type="primary" @click="goDesign()">+ 新建量表（设计器）</a-button>
    </template>

    <a-card>
      <a-alert type="info" show-icon style="margin-bottom: 16px"
        message="量表由 definition JSONB 驱动（条目/选项/满分/MCID/解读区间）；通过表单设计器可视化维护，定义变更产生新版本，历史评估按快照展示，停用后不可被新方案引用" />
      <a-table :columns="columns" :data-source="scales" row-key="id" size="small" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'scaleCode'">
            <a-tag color="blue">{{ record.scaleCode }}</a-tag>
          </template>
          <template v-if="column.key === 'version'">
            <a-tag>v{{ record.version }}</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'ACTIVE' ? 'green' : 'default'">{{ record.status === 'ACTIVE' ? '启用' : '停用' }}</a-tag>
          </template>
          <template v-if="column.key === 'mcid'">
            <span v-if="record.definition.mcid != null">{{ record.definition.mcid }}</span>
            <span v-else style="color: #ccc">-</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space size="small">
              <a @click="preview(record)">预览</a>
              <a @click="goDesign(record.scaleCode)">设计</a>
              <a-popconfirm title="停用后不可被新方案引用，确认？" ok-text="停用">
                <a style="color: #cf1322">停用</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 预览抽屉 -->
    <a-drawer v-model:open="previewOpen" width="640" :title="`量表预览 · ${previewing?.name || ''}`">
      <template v-if="previewing">
        <a-descriptions :column="3" bordered size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="编码">{{ previewing.scaleCode }}</a-descriptions-item>
          <a-descriptions-item label="版本">v{{ previewing.version }}</a-descriptions-item>
          <a-descriptions-item label="满分">{{ previewing.definition.maxScore }}</a-descriptions-item>
          <a-descriptions-item label="MCID">
            {{ previewing.definition.mcid != null ? previewing.definition.mcid : '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="条目数">{{ previewing.definition.items.length }}</a-descriptions-item>
          <a-descriptions-item label="引擎">{{ previewing.definition.type }}</a-descriptions-item>
        </a-descriptions>

        <div class="section-title">条目定义</div>
        <a-table :data-source="previewing.definition.items" :columns="itemColumns" size="small" :pagination="false" row-key="no">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'side'">
              <a-tag v-if="record.side === 'LEFT'">左</a-tag>
              <a-tag v-else-if="record.side === 'RIGHT'">右</a-tag>
              <span v-else style="color: #ccc">-</span>
            </template>
            <template v-if="column.key === 'options'">
              <span class="opt-range">{{ optionRange(record.options) }}</span>
            </template>
          </template>
        </a-table>

        <div class="section-title" style="margin-top: 16px">解读区间</div>
        <a-table :data-source="previewing.definition.interpretation" :columns="interpColumns" size="small" :pagination="false" row-key="label" />
      </template>
    </a-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/PageContainer/index.vue'
import { scales } from './mock'

const router = useRouter()

const columns = [
  { title: '编码', key: 'scaleCode', width: 150 },
  { title: '名称', dataIndex: 'name' },
  { title: '最新版本', key: 'version', width: 90 },
  { title: '状态', key: 'status', width: 80 },
  { title: '满分', key: 'maxScore', dataIndex: ['definition', 'maxScore'], width: 70 },
  { title: 'MCID', key: 'mcid', width: 70 },
  { title: '被方案引用', key: 'refCount', width: 100, customRender: () => '1 个方案' },
  { title: '操作', key: 'action', width: 190 },
]

const itemColumns = [
  { title: '条目号', dataIndex: 'no', width: 70 },
  { title: '侧', key: 'side', width: 60 },
  { title: '内容', dataIndex: 'text' },
  { title: '选项值域', key: 'options', width: 220 },
]
const interpColumns = [
  { title: '区间', key: 'range', customRender: ({ record }: any) => `${record.min} ~ ${record.max} 分` },
  { title: '解读', key: 'label', customRender: ({ record }: any) => record.label },
]

const previewOpen = ref(false)
const previewing = ref<any>(null)

function preview(record: any) {
  previewing.value = record
  previewOpen.value = true
}
function goDesign(scaleCode?: string) {
  router.push({ name: 'ProtoCrsScaleDesigner', query: scaleCode ? { scaleCode } : {} })
}
function optionRange(options: any[]) {
  if (!options?.length) return '-'
  const vals = options.map(o => o.value)
  return `${vals[0]}（${options[0].label}）~ ${vals[vals.length - 1]}（${options[options.length - 1].label}），共 ${options.length} 级`
}
</script>

<style scoped>
.section-title { font-weight: 600; margin-bottom: 8px; }
.opt-range { color: #666; font-size: 12px; }
.edit-item-row { display: flex; gap: 8px; margin-bottom: 6px; }
</style>
