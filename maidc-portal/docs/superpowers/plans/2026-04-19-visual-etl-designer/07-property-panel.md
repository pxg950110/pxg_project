# Task 07: EtlPropertyPanel - Node Configuration Forms

**Files:**
- Create: `src/views/data-etl/components/EtlPropertyPanel.vue`

- [ ] **Step 1: Create EtlPropertyPanel.vue**

The property panel shows configuration forms for the selected node. It dynamically renders different form fields based on the node type.

Create `src/views/data-etl/components/EtlPropertyPanel.vue`:

```vue
<template>
  <div class="etl-props">
    <template v-if="selectedNode">
      <div class="etl-props__header">
        <div class="etl-props__title">
          <span
            class="etl-props__dot"
            :style="{ background: CATEGORY_COLORS[selectedNode.data.category] }"
          />
          {{ selectedNode.data.label }}
        </div>
        <a-tag :color="statusColor" size="small">{{ statusLabel }}</a-tag>
      </div>

      <!-- Node Name -->
      <div class="etl-props__section">
        <div class="etl-props__label">节点名称</div>
        <a-input
          :value="selectedNode.data.label"
          @change="(e: any) => updateLabel(e.target.value)"
          size="small"
        />
      </div>

      <!-- Dynamic Config Forms -->
      <div class="etl-props__section">
        <!-- TABLE_INPUT / TABLE_OUTPUT: schema + table -->
        <template v-if="isTableNode">
          <div class="etl-props__label">Schema</div>
          <a-select
            :value="config.schema"
            @change="(v: string) => updateConfig({ ...config, schema: v, table: '' })"
            placeholder="选择Schema"
            style="width: 100%"
            size="small"
            :loading="schemaLoading"
          >
            <a-select-option v-for="s in schemaOptions" :key="s" :value="s">{{ s }}</a-select-option>
          </a-select>

          <div class="etl-props__label" style="margin-top: 12px">表名</div>
          <a-select
            :value="config.table"
            @change="(v: string) => updateConfig({ ...config, table: v })"
            placeholder="选择表"
            style="width: 100%"
            size="small"
            show-search
            :filter-option="filterOption"
            :disabled="!config.schema"
            :loading="tableLoading"
          >
            <a-select-option v-for="t in tableOptions" :key="t.tableName || t" :value="t.tableName || t">
              {{ t.tableName || t }}
            </a-select-option>
          </a-select>
        </template>

        <!-- TABLE_INPUT: where clause -->
        <template v-if="selectedNode.data.nodeType === 'TABLE_INPUT'">
          <div class="etl-props__label" style="margin-top: 12px">WHERE条件</div>
          <a-input
            :value="config.where"
            @change="(e: any) => updateConfig({ ...config, where: e.target.value })"
            placeholder="如: create_time > '2024-01-01'"
            size="small"
          />
        </template>

        <!-- TABLE_OUTPUT: write mode -->
        <template v-if="selectedNode.data.nodeType === 'TABLE_OUTPUT'">
          <div class="etl-props__label" style="margin-top: 12px">写入模式</div>
          <a-select
            :value="config.writeMode || 'insert'"
            @change="(v: string) => updateConfig({ ...config, writeMode: v })"
            style="width: 100%"
            size="small"
          >
            <a-select-option value="insert">Insert</a-select-option>
            <a-select-option value="upsert">Upsert</a-select-option>
            <a-select-option value="truncate">Truncate + Insert</a-select-option>
          </a-select>
        </template>

        <!-- CSV_INPUT / CSV_OUTPUT: file path -->
        <template v-if="isCsvNode">
          <div class="etl-props__label">文件路径</div>
          <a-input
            :value="config.filePath"
            @change="(e: any) => updateConfig({ ...config, filePath: e.target.value })"
            placeholder="如: /data/input.csv"
            size="small"
          />

          <div class="etl-props__label" style="margin-top: 12px">分隔符</div>
          <a-select
            :value="config.delimiter || ','"
            @change="(v: string) => updateConfig({ ...config, delimiter: v })"
            style="width: 100%"
            size="small"
          >
            <a-select-option value=",">逗号 (,)</a-select-option>
            <a-select-option value="\t">制表符 (Tab)</a-select-option>
            <a-select-option value="|">管道符 (|)</a-select-option>
          </a-select>

          <div class="etl-props__label" style="margin-top: 12px">编码</div>
          <a-select
            :value="config.encoding || 'UTF-8'"
            @change="(v: string) => updateConfig({ ...config, encoding: v })"
            style="width: 100%"
            size="small"
          >
            <a-select-option value="UTF-8">UTF-8</a-select-option>
            <a-select-option value="GBK">GBK</a-select-option>
            <a-select-option value="ISO-8859-1">ISO-8859-1</a-select-option>
          </a-select>
        </template>

        <!-- FILTER: condition -->
        <template v-if="selectedNode.data.nodeType === 'FILTER'">
          <div class="etl-props__label">过滤条件</div>
          <a-textarea
            :value="config.condition"
            @change="(e: any) => updateConfig({ ...config, condition: e.target.value })"
            placeholder="如: age > 18 AND status = 'active'"
            :rows="3"
            size="small"
          />
        </template>

        <!-- JOIN: join type + on condition -->
        <template v-if="selectedNode.data.nodeType === 'JOIN'">
          <div class="etl-props__label">JOIN类型</div>
          <a-select
            :value="config.joinType || 'INNER'"
            @change="(v: string) => updateConfig({ ...config, joinType: v })"
            style="width: 100%"
            size="small"
          >
            <a-select-option value="INNER">INNER JOIN</a-select-option>
            <a-select-option value="LEFT">LEFT JOIN</a-select-option>
            <a-select-option value="RIGHT">RIGHT JOIN</a-select-option>
            <a-select-option value="FULL">FULL JOIN</a-select-option>
          </a-select>

          <div class="etl-props__label" style="margin-top: 12px">ON条件</div>
          <a-input
            :value="config.onCondition"
            @change="(e: any) => updateConfig({ ...config, onCondition: e.target.value })"
            placeholder="如: left.id = right.id"
            size="small"
          />
        </template>

        <!-- AGGREGATE: group by + aggregation -->
        <template v-if="selectedNode.data.nodeType === 'AGGREGATE'">
          <div class="etl-props__label">分组字段 (逗号分隔)</div>
          <a-input
            :value="(config.groupBy || []).join(', ')"
            @change="(e: any) => updateConfig({ ...config, groupBy: e.target.value.split(',').map((s: string) => s.trim()).filter(Boolean) })"
            placeholder="如: department, status"
            size="small"
          />
        </template>

        <!-- VALUE_MAP placeholder -->
        <template v-if="selectedNode.data.nodeType === 'VALUE_MAP'">
          <div class="etl-props__hint">值映射在连线上的字段映射中配置</div>
        </template>

        <!-- EXPRESSION placeholder -->
        <template v-if="selectedNode.data.nodeType === 'EXPRESSION'">
          <div class="etl-props__hint">表达式在连线上的字段映射中配置</div>
        </template>

        <!-- DATE_FMT placeholder -->
        <template v-if="selectedNode.data.nodeType === 'DATE_FMT'">
          <div class="etl-props__hint">日期格式在连线上的字段映射中配置</div>
        </template>
      </div>
    </template>

    <!-- Empty state -->
    <div v-else class="etl-props__empty">
      <a-empty description="点击画布节点查看配置" :image-style="{ height: '40px' }" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { GraphNode } from '@vue-flow/core'
import {
  type EtlNodeData,
  CATEGORY_COLORS,
} from '../types/etl-designer'
import { getEtlSchemas, getEtlTables } from '@/api/etl'

const props = defineProps<{
  selectedNode: GraphNode<EtlNodeData> | null
}>()

const emit = defineEmits<{
  'update:config': [config: Record<string, any>]
}>()

const config = computed(() => props.selectedNode?.data?.config || {})

const statusColor = computed(() => {
  const s = props.selectedNode?.data?.status
  if (s === 'ready') return 'green'
  if (s === 'error') return 'red'
  return 'default'
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

// Schema/table options
const schemaOptions = ref<string[]>([])
const schemaLoading = ref(false)
const tableOptions = ref<any[]>([])
const tableLoading = ref(false)

watch(() => props.selectedNode?.data?.config?.schema, async (schema) => {
  if (!schema) {
    tableOptions.value = []
    return
  }
  tableLoading.value = true
  try {
    const res = await getEtlTables(schema)
    tableOptions.value = res.data?.data || []
  } catch {
    tableOptions.value = []
  } finally {
    tableLoading.value = false
  }
})

watch(() => props.selectedNode?.id, async () => {
  // Load schemas when node changes
  schemaLoading.value = true
  try {
    const res = await getEtlSchemas()
    schemaOptions.value = res.data?.data || []
  } catch {
    schemaOptions.value = []
  } finally {
    schemaLoading.value = false
  }
}, { immediate: true })

function updateConfig(newConfig: Record<string, any>) {
  emit('update:config', newConfig)
}

function updateLabel(label: string) {
  if (!props.selectedNode) return
  emit('update:config', { ...config.value, _label: label })
}

function filterOption(input: string, option: any) {
  const text = option.children?.[0]?.children || option.value || ''
  return String(text).toLowerCase().includes(input.toLowerCase())
}
</script>

<style scoped>
.etl-props {
  padding: 16px;
}

.etl-props__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.etl-props__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
}

.etl-props__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.etl-props__section {
  margin-bottom: 16px;
}

.etl-props__label {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.65);
  margin-bottom: 4px;
  font-weight: 500;
}

.etl-props__hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  padding: 8px;
  background: #fafafa;
  border-radius: 4px;
  text-align: center;
}

.etl-props__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add src/views/data-etl/components/EtlPropertyPanel.vue
git commit -m "feat(etl): add EtlPropertyPanel with dynamic config forms per node type"
```
