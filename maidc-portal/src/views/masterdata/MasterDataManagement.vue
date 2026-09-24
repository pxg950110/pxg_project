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
        <el-dropdown trigger="click" @command="handleQuickCreateCommand">
          <el-button type="primary" class="hub-create-btn">
            <el-icon style="margin-right: 6px"><Plus /></el-icon> 快速新建标准
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="concept-domain">
                <el-icon style="color: #0ea5e9; margin-right: 8px"><Menu /></el-icon> 新建概念域 (Concept Domain)
              </el-dropdown-item>
              <el-dropdown-item command="value-domain">
                <el-icon style="color: #38bdf8; margin-right: 8px"><Coin /></el-icon> 新建值域 (Value Domain)
              </el-dropdown-item>
              <el-dropdown-item command="data-element-concept">
                <el-icon style="color: #818cf8; margin-right: 8px"><Connection /></el-icon> 新建数据元概念 (DEC)
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
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
              <el-input
                v-model="searchKeyword"
                placeholder="搜索标准代码或名称..."
                clearable
                :suffix-icon="Search"
              />
            </div>
            <el-tree
              :data="treeData"
              node-key="id"
              :props="{ label: 'name', children: 'children' }"
              :default-expanded-keys="treeExpandedKeys"
              :expand-on-click-node="false"
              highlight-current
              @node-click="handleTreeSelect"
            >
              <template #default="{ data }">
                <span style="display: inline-flex; align-items: center">
                  <el-icon style="margin-right: 6px; color: #0ea5e9; font-size: 14px"><component :is="getTypeIcon(data.type)" /></el-icon>
                  {{ data.name }}
                  <span v-if="data.count" class="tree-count">{{ data.count }}</span>
                </span>
              </template>
            </el-tree>
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
              <el-icon style="font-size: 40px; color: #64748b; margin-bottom: 12px"><Coin /></el-icon>
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
import { ref, markRaw, onMounted } from 'vue'
import {
  Plus,
  Menu,
  Coin,
  Connection,
  FirstAidKit,
  Share,
  Folder,
  List,
  Search,
} from '@element-plus/icons-vue'
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
  { key: 'concept-domains', label: '概念域与值含义 (CD & VM)', icon: markRaw(Menu), badge: 'WS/T 303' },
  { key: 'value-domains', label: '值域与允许值 (VD & PV)', icon: markRaw(Coin) },
  { key: 'data-element-concepts', label: '数据元概念 (DEC)', icon: markRaw(Connection) },
  { key: 'dictionaries', label: '常用临床字典 (Dictionaries)', icon: markRaw(FirstAidKit), badge: '5大字典' },
  { key: 'catalog-tree', label: '编码体系与目录树全景', icon: markRaw(Share) },
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

const handleQuickCreateCommand = (cmd: string | number | object) => {
  handleQuickCreate({ key: String(cmd) })
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
    folder: Folder,
    'concept-domain': Menu,
    'code-system': Coin,
    'value-set': List,
  }
  return icons[type] || Folder
}

const handleTreeSelect = (data: any) => {
  if (data.type !== 'folder') {
    treeCurrentView.value = data.type
    treeSelectedId.value = data.id
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
  background: #f0f9ff;
  color: #0ea5e9;
  border: 1px solid #7dd3fc;
  padding: 2px 10px;
  border-radius: 999px;
}

.hub-subtitle {
  font-size: 13px;
  color: #94a3b8;
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
  border: 1px solid #f1f5f9;
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
  color: #64748b;
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
    color: #0ea5e9;
    background: #f0f9ff;
  }

  &.active {
    background: #f0f9ff;
    border-color: #7dd3fc;
    color: #0ea5e9;
    font-weight: 600;
  }
}

.tab-badge {
  font-size: 10px;
  background: #f8fafc;
  color: #94a3b8;
  padding: 1px 6px;
  border-radius: 999px;
  border: 1px solid #f1f5f9;
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
  border: 1px solid #f1f5f9;
  border-radius: 8px;
}

.tree-header {
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 12px;
}

.tree-preview-panel {
  flex: 1;
  min-width: 0;
  padding: 16px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
}

.tree-count {
  font-size: 11px;
  background: #f8fafc;
  color: #94a3b8;
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
