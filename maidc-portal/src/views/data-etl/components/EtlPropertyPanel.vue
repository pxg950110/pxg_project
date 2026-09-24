<template>
  <div class="etl-props">
    <template v-if="selectedNode">
      <div class="etl-props__header">
        <div class="etl-props__title">
          <span class="etl-props__dot" :style="{ background: CATEGORY_COLORS[selectedNode.data.category] }" />
          {{ selectedNode.data.label }}
        </div>
        <el-tag :type="statusType" size="small">{{ statusLabel }}</el-tag>
      </div>

      <div class="etl-props__section">
        <div class="etl-props__label">节点名称</div>
        <el-input
          :model-value="selectedNode.data.label"
          @update:model-value="(v: string) => updateLabel(v)"
          size="small"
        />
      </div>

      <div class="etl-props__section">
        <!-- TABLE_INPUT / TABLE_OUTPUT -->
        <template v-if="isTableNode">
          <div class="etl-props__label">Schema</div>
          <el-select
            :model-value="config.schema"
            @update:model-value="(v: any) => updateConfig({ ...config, schema: v, table: '' })"
            placeholder="选择Schema"
            style="width: 100%"
            size="small"
            :loading="schemaLoading"
          >
            <el-option v-for="s in schemaOptions" :key="s" :value="s" :label="s" />
          </el-select>

          <div class="etl-props__label" style="margin-top: 12px">表名</div>
          <el-select
            :model-value="config.table"
            @update:model-value="(v: any) => updateConfig({ ...config, table: v })"
            placeholder="选择表"
            style="width: 100%"
            size="small"
            filterable
            :disabled="!config.schema"
            :loading="tableLoading"
          >
            <el-option
              v-for="t in tableOptions"
              :key="t.tableName || t"
              :value="t.tableName || t"
              :label="t.tableName || t"
            />
          </el-select>
        </template>

        <!-- TABLE_INPUT where -->
        <template v-if="selectedNode.data.nodeType === 'TABLE_INPUT'">
          <div class="etl-props__label" style="margin-top: 12px">WHERE条件</div>
          <el-input
            :model-value="config.where"
            @update:model-value="(v: string) => updateConfig({ ...config, where: v })"
            placeholder="如: create_time > '2024-01-01'"
            size="small"
          />
        </template>

        <!-- TABLE_OUTPUT write mode -->
        <template v-if="selectedNode.data.nodeType === 'TABLE_OUTPUT'">
          <div class="etl-props__label" style="margin-top: 12px">写入模式</div>
          <el-select
            :model-value="config.writeMode || 'insert'"
            @update:model-value="(v: any) => updateConfig({ ...config, writeMode: v })"
            style="width: 100%"
            size="small"
          >
            <el-option value="insert" label="Insert" />
            <el-option value="upsert" label="Upsert" />
            <el-option value="truncate" label="Truncate + Insert" />
          </el-select>
        </template>

        <!-- CSV nodes -->
        <template v-if="isCsvNode">
          <div class="etl-props__label">文件路径</div>
          <el-input
            :model-value="config.filePath"
            @update:model-value="(v: string) => updateConfig({ ...config, filePath: v })"
            placeholder="如: /data/input.csv"
            size="small"
          />
          <div class="etl-props__label" style="margin-top: 12px">分隔符</div>
          <el-select
            :model-value="config.delimiter || ','"
            @update:model-value="(v: any) => updateConfig({ ...config, delimiter: v })"
            style="width: 100%"
            size="small"
          >
            <el-option value="," label="逗号 (,)" />
            <el-option value="\t" label="制表符 (Tab)" />
            <el-option value="|" label="管道符 (|)" />
          </el-select>
          <div class="etl-props__label" style="margin-top: 12px">编码</div>
          <el-select
            :model-value="config.encoding || 'UTF-8'"
            @update:model-value="(v: any) => updateConfig({ ...config, encoding: v })"
            style="width: 100%"
            size="small"
          >
            <el-option value="UTF-8" label="UTF-8" />
            <el-option value="GBK" label="GBK" />
          </el-select>
        </template>

        <!-- FILTER -->
        <template v-if="selectedNode.data.nodeType === 'FILTER'">
          <div class="etl-props__label">过滤条件</div>
          <el-input
            :model-value="config.condition"
            @update:model-value="(v: string) => updateConfig({ ...config, condition: v })"
            type="textarea"
            placeholder="如: age > 18 AND status = 'active'"
            :rows="3"
            size="small"
          />
        </template>

        <!-- JOIN -->
        <template v-if="selectedNode.data.nodeType === 'JOIN'">
          <div class="etl-props__label">JOIN类型</div>
          <el-select
            :model-value="config.joinType || 'INNER'"
            @update:model-value="(v: any) => updateConfig({ ...config, joinType: v })"
            style="width: 100%"
            size="small"
          >
            <el-option value="INNER" label="INNER JOIN" />
            <el-option value="LEFT" label="LEFT JOIN" />
            <el-option value="RIGHT" label="RIGHT JOIN" />
            <el-option value="FULL" label="FULL JOIN" />
          </el-select>
          <div class="etl-props__label" style="margin-top: 12px">ON条件</div>
          <el-input
            :model-value="config.onCondition"
            @update:model-value="(v: string) => updateConfig({ ...config, onCondition: v })"
            placeholder="如: left.id = right.id"
            size="small"
          />
        </template>

        <!-- AGGREGATE -->
        <template v-if="selectedNode.data.nodeType === 'AGGREGATE'">
          <div class="etl-props__label">分组字段 (逗号分隔)</div>
          <el-input
            :model-value="(config.groupBy || []).join(', ')"
            @update:model-value="(v: string) => updateConfig({ ...config, groupBy: v.split(',').map((s: string) => s.trim()).filter(Boolean) })"
            placeholder="如: department, status"
            size="small"
          />
        </template>

        <!-- Transform hint -->
        <template v-if="isTransformNode">
          <div class="etl-props__hint">转换规则在连线上的字段映射中配置</div>
        </template>
      </div>
    </template>

    <div v-else class="etl-props__empty">
      <el-empty description="点击画布节点查看配置" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { GraphNode } from '@vue-flow/core'
import { type EtlNodeData, CATEGORY_COLORS } from '../types/etl-designer'
import { getEtlSchemas, getEtlTables } from '@/api/etl'

const props = defineProps<{
  selectedNode: GraphNode<EtlNodeData> | null
}>()

const emit = defineEmits<{
  'update:config': [config: Record<string, any>]
  'update:label': [label: string]
}>()

const config = computed(() => props.selectedNode?.data?.config || {})
const statusType = computed<'success' | 'danger' | 'info'>(() => {
  const s = props.selectedNode?.data?.status
  if (s === 'ready') return 'success'
  if (s === 'error') return 'danger'
  return 'info'
})
const statusLabel = computed(() => {
  const s = props.selectedNode?.data?.status
  if (s === 'ready') return '已配置'
  if (s === 'error') return '配置缺失'
  return '草稿'
})

const isTableNode = computed(() =>
  ['TABLE_INPUT', 'TABLE_OUTPUT'].includes(props.selectedNode?.data?.nodeType || ''),
)
const isCsvNode = computed(() =>
  ['CSV_INPUT', 'CSV_OUTPUT'].includes(props.selectedNode?.data?.nodeType || ''),
)
const isTransformNode = computed(() =>
  ['VALUE_MAP', 'EXPRESSION', 'DATE_FMT', 'CONSTANT', 'LOOKUP'].includes(props.selectedNode?.data?.nodeType || ''),
)

const schemaOptions = ref<string[]>([])
const schemaLoading = ref(false)
const tableOptions = ref<any[]>([])
const tableLoading = ref(false)

watch(() => props.selectedNode?.data?.config?.schema, async (schema) => {
  if (!schema) { tableOptions.value = []; return }
  tableLoading.value = true
  try {
    const res = await getEtlTables(schema)
    tableOptions.value = res.data?.data || []
  } catch { tableOptions.value = [] }
  finally { tableLoading.value = false }
})

watch(() => props.selectedNode?.id, async () => {
  schemaLoading.value = true
  try {
    const res = await getEtlSchemas()
    schemaOptions.value = res.data?.data || []
  } catch { schemaOptions.value = [] }
  finally { schemaLoading.value = false }
}, { immediate: true })

function updateConfig(newConfig: Record<string, any>) { emit('update:config', newConfig) }
function updateLabel(label: string) { emit('update:label', label) }
</script>

<style scoped>
.etl-props { padding: 16px; }
.etl-props__header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #f1f5f9;
}
.etl-props__title { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; }
.etl-props__dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.etl-props__section { margin-bottom: 16px; }
.etl-props__label { font-size: 12px; color: #64748b; margin-bottom: 4px; font-weight: 500; }
.etl-props__hint {
  font-size: 12px; color: #94a3b8; padding: 8px;
  background: #f8fafc; border-radius: 4px; text-align: center;
}
.etl-props__empty { display: flex; align-items: center; justify-content: center; height: 200px; }
</style>
