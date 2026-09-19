<template>
  <div class="masterdata-hub-page">
    <!-- Top Header -->
    <div class="hub-header">
      <div class="hub-title-block">
        <div class="title-row">
          <h1 class="hub-title">主数据与术语管理中心</h1>
          <span class="standard-pill">WS/T 303-2023 标规中枢</span>
        </div>
        <p class="hub-subtitle">
          贯通「概念域(CD) ⇄ 值域(VD) ⇄ 数据元概念(DEC) ⇄ 临床常用字典」的标准化治理全景工作台
        </p>
      </div>

      <div class="hub-actions">
        <a-dropdown :trigger="['click']">
          <a-button type="primary" class="hub-create-btn">
            <PlusOutlined style="margin-right: 6px" /> 快速新建标准
          </a-button>
          <template #overlay>
            <a-menu @click="handleQuickCreate">
              <a-menu-item key="concept-domain">
                <AppstoreOutlined style="color: #1677ff; margin-right: 8px" /> 新建概念域 (Concept Domain)
              </a-menu-item>
              <a-menu-item key="value-domain">
                <DatabaseOutlined style="color: #38bdf8; margin-right: 8px" /> 新建值域 (Value Domain)
              </a-menu-item>
              <a-menu-item key="data-element-concept">
                <BranchesOutlined style="color: #818cf8; margin-right: 8px" /> 新建数据元概念 (DEC)
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </div>

    <!-- Metrics Cards Overview -->
    <MasterDataMetrics
      ref="metricsRef"
      @switch-tab="handleTabChange"
      @create="handleMetricCreate"
    />

    <!-- Main Workspace Tabs -->
    <div class="main-tabs-bar">
      <div class="tab-buttons">
        <button
          v-for="t in mainTabs"
          :key="t.key"
          class="hub-tab-btn"
          :class="{ active: activeTab === t.key }"
          @click="handleTabChange(t.key)"
        >
          <component :is="t.icon" class="tab-icon" />
          <span>{{ t.label }}</span>
          <span v-if="t.badge" class="tab-badge">{{ t.badge }}</span>
        </button>
      </div>
    </div>

    <!-- Tab Content Area -->
    <div class="hub-tab-content">
      <!-- 1. 概念域与值含义 -->
      <ConceptDomainList
        v-if="activeTab === 'concept-domains'"
        ref="conceptDomainListRef"
        :embedded="true"
        @navigate-value-domain="handleNavigateToValueDomain"
        @navigate-dec="handleNavigateToDec"
      />

      <!-- 2. 值域与允许值 -->
      <ValueDomainList
        v-else-if="activeTab === 'value-domains'"
        ref="valueDomainListRef"
        :embedded="true"
        :filter-concept-domain-id="filterConceptDomainId"
        @navigate-concept-domain="handleNavigateToConceptDomain"
      />

      <!-- 3. 数据元概念 -->
      <DataElementConceptList
        v-else-if="activeTab === 'data-element-concepts'"
        ref="decListRef"
        :embedded="true"
        :filter-concept-domain-id="filterConceptDomainId"
        @navigate-concept-domain="handleNavigateToConceptDomain"
      />

      <!-- 4. 常用临床字典 -->
      <DictionaryHub
        v-else-if="activeTab === 'dictionaries'"
      />

      <!-- 5. 编码体系与值集全景树 (Legacy Tree View) -->
      <div v-else-if="activeTab === 'catalog-tree'" class="legacy-tree-view-wrapper">
        <div class="legacy-tree-layout">
          <!-- Left: Tree -->
          <div class="tree-sidebar">
            <div class="tree-header">
              <span style="font-weight: 600">标准体系分类目录</span>
            </div>
            <div style="margin-bottom: 12px">
              <a-input-search
                v-model:value="searchKeyword"
                placeholder="搜索标准代码或名称..."
                allow-clear
              />
            </div>
            <a-tree
              v-model:selectedKeys="treeSelectedKeys"
              v-model:expandedKeys="treeExpandedKeys"
              :tree-data="treeData"
              :field-names="{ title: 'name', key: 'id', children: 'children' }"
              @select="handleTreeSelect"
            >
              <template #title="{ name, type, count }">
                <span>
                  <component :is="getTypeIcon(type)" style="margin-right: 6px; color: #1677ff" />
                  {{ name }}
                  <span v-if="count" class="tree-count">{{ count }}</span>
                </span>
              </template>
            </a-tree>
          </div>

          <!-- Right: Dynamic preview -->
          <div class="tree-preview-panel">
            <ConceptDomainView
              v-if="treeCurrentView === 'concept-domain'"
              :concept-domain-id="treeSelectedId"
              @edit="handleEditConceptDomain"
            />
            <CodeSystemView
              v-else-if="treeCurrentView === 'code-system'"
              :code-system-id="treeSelectedId"
              @edit="handleEditCodeSystem"
            />
            <ValueSetView
              v-else-if="treeCurrentView === 'value-set'"
              :value-set-id="treeSelectedId"
              @edit="handleEditValueSet"
            />
            <div v-else class="empty-state-box">
              <DatabaseOutlined style="font-size: 40px; color: #64748b; margin-bottom: 12px" />
              <div style="color: #94a3b8; font-size: 14px">请从左侧标准体系目录选择具体节点查看详情</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create/Edit Modals -->
    <ConceptDomainFormModal ref="conceptDomainFormRef" @success="handleModalSuccess" />
    <ValueDomainFormModal ref="valueDomainFormRef" @success="handleModalSuccess" />
    <DataElementConceptFormModal ref="decFormRef" @success="handleModalSuccess" />
    <CodeSystemFormModal ref="codeSystemFormRef" @success="handleModalSuccess" />
    <ValueSetFormModal ref="valueSetFormRef" @success="handleModalSuccess" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, markRaw, onMounted } from 'vue'
import {
  PlusOutlined,
  AppstoreOutlined,
  DatabaseOutlined,
  BranchesOutlined,
  MedicineBoxOutlined,
  ApartmentOutlined,
  FolderOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons-vue'
import MasterDataMetrics from './components/MasterDataMetrics.vue'
import ConceptDomainList from './ConceptDomainList.vue'
import ValueDomainList from './ValueDomainList.vue'
import DataElementConceptList from './DataElementConceptList.vue'
import DictionaryHub from './dictionary/DictionaryHub.vue'

// Detail views & Modals
import ConceptDomainView from './components/ConceptDomainView.vue'
import CodeSystemView from './components/CodeSystemView.vue'
import ValueSetView from './components/ValueSetView.vue'
import ConceptDomainFormModal from './components/ConceptDomainFormModal.vue'
import ValueDomainFormModal from './components/ValueDomainFormModal.vue'
import DataElementConceptFormModal from './components/DataElementConceptFormModal.vue'
import CodeSystemFormModal from './components/CodeSystemFormModal.vue'
import ValueSetFormModal from './components/ValueSetFormModal.vue'

import { conceptDomainApi, type ConceptDomain } from '@/api/dataElementStandard'
import { getCodeSystems } from '@/api/masterdata'

// Active tab management
const activeTab = ref<string>('concept-domains')
const filterConceptDomainId = ref<number | undefined>()

const metricsRef = ref()
const conceptDomainListRef = ref()
const valueDomainListRef = ref()
const decListRef = ref()

const conceptDomainFormRef = ref()
const valueDomainFormRef = ref()
const decFormRef = ref()
const codeSystemFormRef = ref()
const valueSetFormRef = ref()

const mainTabs = [
  { key: 'concept-domains', label: '概念域与值含义 (CD & VM)', icon: markRaw(AppstoreOutlined), badge: 'WS/T 303' },
  { key: 'value-domains', label: '值域与允许值 (VD & PV)', icon: markRaw(DatabaseOutlined) },
  { key: 'data-element-concepts', label: '数据元概念 (DEC)', icon: markRaw(BranchesOutlined) },
  { key: 'dictionaries', label: '常用临床字典 (Dictionaries)', icon: markRaw(MedicineBoxOutlined), badge: '5大字典' },
  { key: 'catalog-tree', label: '编码体系与目录树全景', icon: markRaw(ApartmentOutlined) },
]

// Tab switching
const handleTabChange = (key: string) => {
  activeTab.value = key
  if (key !== 'value-domains' && key !== 'data-element-concepts') {
    filterConceptDomainId.value = undefined
  }
}

// Cross-tab drilldown navigation
const handleNavigateToValueDomain = (conceptDomainId: number) => {
  filterConceptDomainId.value = conceptDomainId
  activeTab.value = 'value-domains'
}

const handleNavigateToDec = (conceptDomainId: number) => {
  filterConceptDomainId.value = conceptDomainId
  activeTab.value = 'data-element-concepts'
}

const handleNavigateToConceptDomain = (conceptDomainId: number) => {
  activeTab.value = 'concept-domains'
}

// Quick Create
const handleQuickCreate = ({ key }: { key: string }) => {
  if (key === 'concept-domain') {
    conceptDomainFormRef.value?.open()
  } else if (key === 'value-domain') {
    valueDomainFormRef.value?.open()
  } else if (key === 'data-element-concept') {
    decFormRef.value?.open()
  }
}

const handleMetricCreate = (type: string) => {
  if (type === 'concept-domain') {
    conceptDomainFormRef.value?.open()
  } else if (type === 'value-domain') {
    valueDomainFormRef.value?.open()
  } else if (type === 'data-element-concept') {
    decFormRef.value?.open()
  }
}

const handleModalSuccess = () => {
  metricsRef.value?.refresh()
  if (activeTab.value === 'concept-domains') conceptDomainListRef.value?.refresh()
  if (activeTab.value === 'value-domains') valueDomainListRef.value?.refresh()
  if (activeTab.value === 'data-element-concepts') decListRef.value?.refresh()
  refreshTree()
}

// Tree view state
const searchKeyword = ref('')
const treeSelectedKeys = ref<string[]>([])
const treeExpandedKeys = ref<string[]>(['concept-domains', 'code-systems', 'value-sets'])
const treeCurrentView = ref<string>('')
const treeSelectedId = ref<number>()

const treeData = ref<any[]>([
  {
    id: 'concept-domains',
    name: '概念域 (Concept Domains)',
    type: 'folder',
    children: [],
  },
  {
    id: 'code-systems',
    name: '编码体系 (Code Systems)',
    type: 'folder',
    children: [],
  },
  {
    id: 'value-sets',
    name: '值集 (Value Sets)',
    type: 'folder',
    children: [],
  },
])

const getTypeIcon = (type: string) => {
  const icons: Record<string, any> = {
    folder: FolderOutlined,
    'concept-domain': AppstoreOutlined,
    'code-system': DatabaseOutlined,
    'value-set': UnorderedListOutlined,
  }
  return icons[type] || FolderOutlined
}

const handleTreeSelect = (keys: string[], { node }: any) => {
  if (node.type !== 'folder') {
    treeSelectedKeys.value = keys
    treeCurrentView.value = node.type
    treeSelectedId.value = node.id
  }
}

const handleEditConceptDomain = (record: any) => {
  conceptDomainFormRef.value?.open(record)
}

const handleEditCodeSystem = (record: any) => {
  codeSystemFormRef.value?.open(record)
}

const handleEditValueSet = (record: any) => {
  valueSetFormRef.value?.open(record)
}

const refreshTree = async () => {
  try {
    const [cdRes, csRes] = await Promise.all([
      conceptDomainApi.list({ page: 1, page_size: 200 } as any),
      getCodeSystems(),
    ])
    const domains = ((cdRes as any)?.data?.data?.content ?? (cdRes as any)?.data?.content ?? []) as ConceptDomain[]
    const systems = (csRes as any)?.data?.data ?? []
    treeData.value = treeData.value.map((folder) => {
      if (folder.id === 'concept-domains') {
        return {
          ...folder,
          children: domains.map((d: any) => ({
            id: d.id,
            name: `${d.code} · ${d.name}`,
            type: 'concept-domain',
          })),
        }
      }
      if (folder.id === 'code-systems') {
        return {
          ...folder,
          children: systems.map((s: any) => ({
            id: s.id,
            name: [s.code, s.name].filter(Boolean).join(' · '),
            type: 'code-system',
          })),
        }
      }
      return folder
    })
  } catch {
    // Retain initial tree structure
  }
}

onMounted(() => {
  refreshTree()
})
</script>

<style lang="scss" scoped>
.masterdata-hub-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
}

.hub-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 2px 8px 2px;
}

.hub-title-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hub-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
  letter-spacing: -0.01em;
}

.standard-pill {
  font-size: 11px;
  font-weight: 600;
  background: #e6f4ff;
  color: #1677ff;
  border: 1px solid #91caff;
  padding: 2px 10px;
  border-radius: 999px;
}

.hub-subtitle {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  margin: 0;
}

.hub-create-btn {
  padding: 8px 16px !important;
  font-size: 13px;
}

/* Tabs Bar */
.main-tabs-bar {
  padding: 6px 8px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #f0f0f0;
}

.tab-buttons {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow-x: auto;
}

.hub-tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: transparent;
  border: 1px solid transparent;
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;

  .tab-icon {
    font-size: 15px;
  }

  &:hover {
    color: #1677ff;
    background: #f0f7ff;
  }

  &.active {
    background: #e6f4ff;
    border-color: #91caff;
    color: #1677ff;
    font-weight: 600;
  }
}

.tab-badge {
  font-size: 10px;
  background: #f5f5f5;
  color: rgba(0, 0, 0, 0.45);
  padding: 1px 6px;
  border-radius: 999px;
  border: 1px solid #f0f0f0;
}

.hub-tab-content {
  flex: 1;
  min-height: 0;
}

/* Legacy Tree Layout */
.legacy-tree-view-wrapper {
  height: calc(100vh - 240px);
}

.legacy-tree-layout {
  display: flex;
  gap: 16px;
  height: 100%;
}

.tree-sidebar {
  width: 320px;
  flex-shrink: 0;
  padding: 16px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.tree-header {
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 12px;
}

.tree-preview-panel {
  flex: 1;
  min-width: 0;
  padding: 16px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.tree-count {
  font-size: 11px;
  background: #f5f5f5;
  color: rgba(0, 0, 0, 0.45);
  padding: 1px 6px;
  border-radius: 999px;
  margin-left: 6px;
}

.empty-state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 60px 0;
}
</style>
