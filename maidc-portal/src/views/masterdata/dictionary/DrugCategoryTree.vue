<template>
  <div class="dict-type-panel">
    <div class="mb-3 flex items-center gap-2">
      <el-input
        v-model="search"
        placeholder="搜索分类"
        clearable
        :suffix-icon="Search"
        @keyup.enter="filterCategories(search)"
        @clear="filterCategories('')"
      />
      <el-button :icon="Search" @click="filterCategories(search)">搜索</el-button>
    </div>
    <div v-loading="loading" class="category-tree min-h-[200px]">
      <el-tree
        ref="treeRef"
        :data="filteredTree"
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        highlight-current
        :expand-on-click-node="false"
        :default-expanded-keys="expandedKeys"
        @node-click="onNodeClick"
        @node-expand="onNodeExpand"
        @node-collapse="onNodeCollapse"
      >
        <template #default="{ data }">
          <span>{{ data.name }}</span>
          <span class="category-code">{{ data.code }}</span>
        </template>
      </el-tree>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
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
const treeRef = ref()

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

/** el-tree 点击选中（再次点击已选节点取消选中，保持原树选择的切换语义） */
function onNodeClick(data: DrugCategory) {
  if (selectedKeys.value.length && selectedKeys.value[0] === data.id) {
    selectedKeys.value = []
    treeRef.value?.setCurrentKey(null)
    emit('select', undefined)
  } else {
    selectedKeys.value = [data.id]
    emit('select', data.id)
  }
}

function onNodeExpand(data: DrugCategory) {
  if (!expandedKeys.value.includes(data.id)) expandedKeys.value.push(data.id)
}

function onNodeCollapse(data: DrugCategory) {
  expandedKeys.value = expandedKeys.value.filter(k => k !== data.id)
}

function clearSelection() {
  selectedKeys.value = []
  treeRef.value?.setCurrentKey(null)
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
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  padding: 14px;
}

.category-tree {
  flex: 1;
  overflow-y: auto;
  border-radius: 6px;
  padding: 4px;

  :deep(.el-tree) {
    --el-tree-node-content-height: 30px;
    background: transparent;

    .el-tree-node__content {
      border-radius: 6px;
    }
  }
}

.category-code {
  color: #94a3b8;
  font-size: 12px;
  margin-left: 8px;
}
</style>
