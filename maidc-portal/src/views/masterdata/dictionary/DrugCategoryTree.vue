<template>
  <div class="dict-type-panel">
    <a-input-search v-model:value="search" placeholder="搜索分类" allow-clear
      style="margin-bottom: 12px" @search="filterCategories" />
    <div class="category-tree">
      <a-spin :spinning="loading">
        <a-tree
          v-model:selectedKeys="selectedKeys"
          :tree-data="filteredTree"
          :expanded-keys="expandedKeys"
          :field-names="{ title: 'name', key: 'id', children: 'children' }"
          @select="onSelect"
          @expand="onExpand"
          show-line
        >
          <template #title="{ name, code }">
            <span>{{ name }}</span>
            <span class="category-code">{{ code }}</span>
          </template>
        </a-tree>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getDrugCategories, type DrugCategory } from '@/api/medical-dictionary'

const emit = defineEmits<{
  (e: 'select', categoryId: number | undefined): void
  (e: 'loaded', tree: DrugCategory[]): void
}>()

const loading = ref(false)
const search = ref('')
const tree = ref<DrugCategory[]>([])
const filteredTree = ref<DrugCategory[]>([])
const selectedKeys = ref<number[]>([])
const expandedKeys = ref<number[]>([])

async function fetchTree() {
  loading.value = true
  try {
    const res = await getDrugCategories()
    tree.value = res.data.data || []
    filteredTree.value = tree.value
    emit('loaded', tree.value)
  } finally {
    loading.value = false
  }
}

function filterCategories(keyword: string) {
  if (!keyword) {
    filteredTree.value = tree.value
  } else {
    filteredTree.value = filterNodes(tree.value, keyword.toLowerCase())
  }
}

function filterNodes(nodes: DrugCategory[], keyword: string): DrugCategory[] {
  return nodes.reduce((acc: DrugCategory[], node) => {
    if (node.name.toLowerCase().includes(keyword) || node.code.toLowerCase().includes(keyword)) {
      acc.push({ ...node, children: node.children ? filterNodes(node.children, keyword) : [] })
    } else if (node.children?.length) {
      const matchedChildren = filterNodes(node.children, keyword)
      if (matchedChildren.length) {
        acc.push({ ...node, children: matchedChildren })
      }
    }
    return acc
  }, [])
}

function onSelect(keys: (number | string)[]) {
  emit('select', keys.length ? (keys[0] as number) : undefined)
}

function onExpand(keys: (number | string)[]) {
  expandedKeys.value = keys as number[]
}

function clearSelection() {
  selectedKeys.value = []
}

defineExpose({ clearSelection })

onMounted(fetchTree)
</script>

<style lang="scss" scoped>
.dict-type-panel {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--tech-panel-bg);
  border: 1px solid var(--tech-panel-border);
  border-radius: 12px;
  padding: 14px;

  :deep(.ant-input-affix-wrapper),
  :deep(.ant-input) {
    background: var(--tech-input-bg) !important;
    border-color: var(--tech-input-border) !important;
    color: var(--tech-text-body) !important;
  }

  :deep(.ant-input::placeholder) {
    color: var(--tech-text-dim);
  }

  :deep(.ant-input-search-button .anticon) {
    color: var(--tech-text-muted);
  }

  :deep(.ant-tree) {
    background: transparent;
    color: var(--tech-text-body);

    .ant-tree-node-content-wrapper:hover {
      background: rgba(34, 211, 238, 0.08);
      color: var(--tech-primary);
    }

    .ant-tree-node-content-wrapper.ant-tree-node-selected {
      background: rgba(34, 211, 238, 0.16) !important;
      color: var(--tech-primary);
    }

    .ant-tree-switcher {
      color: var(--tech-text-muted);
    }

    .ant-tree-indent-unit::before {
      border-color: rgba(148, 163, 184, 0.2);
    }
  }
}

.category-tree {
  flex: 1;
  overflow-y: auto;
  border-radius: 6px;
  padding: 4px;
}

.category-code {
  color: var(--tech-text-dim);
  font-size: 12px;
  margin-left: 8px;
}
</style>
