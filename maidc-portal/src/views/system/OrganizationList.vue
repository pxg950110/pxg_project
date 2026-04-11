<template>
  <PageContainer>
    <template #default>
      <!-- Page Header -->
      <div class="org-page-header">
        <div class="org-page-header-left">
          <h2 class="org-page-title">组织管理</h2>
          <span class="org-page-subtitle">管理医院、科室与部门组织架构</span>
        </div>
        <div class="org-page-header-right">
          <a-button type="primary">
            <PlusOutlined /> 新增组织
          </a-button>
        </div>
      </div>

      <!-- Two-Panel Layout -->
      <div class="org-layout">
        <!-- Left Panel: Organization Tree -->
        <div class="org-tree-panel">
          <div class="org-tree-card">
            <a-input
              v-model:value="searchText"
              placeholder="搜索组织..."
              class="org-search-input"
              allow-clear
            >
              <template #prefix>
                <SearchOutlined class="org-search-icon" />
              </template>
            </a-input>

            <div class="org-tree-list">
              <template v-for="root in filteredTreeData" :key="root.id">
                <!-- Root Level -->
                <div
                  class="org-tree-item org-tree-root"
                  :class="{ 'org-tree-item-expanded': expandedKeys.includes(root.id) }"
                  @click="toggleExpand(root.id)"
                >
                  <span class="org-tree-chevron">
                    <span v-if="root.children && root.children.length">
                      <span v-if="expandedKeys.includes(root.id)">&#9662;</span>
                      <span v-else>&#9656;</span>
                    </span>
                  </span>
                  <ApartmentOutlined class="org-tree-icon" />
                  <span class="org-tree-label org-tree-label-root">{{ root.name }}</span>
                </div>

                <!-- Children of Root -->
                <template v-if="expandedKeys.includes(root.id) && root.children">
                  <template v-for="child in root.children" :key="child.id">
                    <!-- Child Item -->
                    <div
                      class="org-tree-item org-tree-child"
                      :class="{
                        'org-tree-item-active': selectedOrgId === child.id,
                        'org-tree-item-expanded': expandedKeys.includes(child.id),
                      }"
                      @click.stop="selectOrg(child.id); toggleExpand(child.id)"
                    >
                      <span class="org-tree-chevron">
                        <span v-if="child.children && child.children.length">
                          <span v-if="expandedKeys.includes(child.id)">&#9662;</span>
                          <span v-else>&#9656;</span>
                        </span>
                      </span>
                      <ApartmentOutlined class="org-tree-icon" />
                      <span class="org-tree-label">{{ child.name }}</span>
                    </div>

                    <!-- Grandchildren -->
                    <template v-if="expandedKeys.includes(child.id) && child.children">
                      <div
                        v-for="grand in child.children"
                        :key="grand.id"
                        class="org-tree-item org-tree-grandchild"
                        :class="{ 'org-tree-item-active': selectedOrgId === grand.id }"
                        @click.stop="selectOrg(grand.id)"
                      >
                        <span class="org-tree-chevron"></span>
                        <ApartmentOutlined class="org-tree-icon" />
                        <span class="org-tree-label">{{ grand.name }}</span>
                      </div>
                    </template>
                  </template>
                </template>
              </template>
            </div>
          </div>
        </div>

        <!-- Right Panel: Organization Detail -->
        <div class="org-detail-panel">
          <div v-if="selectedOrg" class="org-detail-card">
            <!-- Detail Header -->
            <div class="org-detail-header">
              <div class="org-detail-header-info">
                <h3 class="org-detail-title">{{ selectedOrg.name }}</h3>
                <span class="org-detail-desc">{{ selectedOrg.description }}</span>
              </div>
              <a-button class="org-detail-edit-btn">
                <EditOutlined /> 编辑
              </a-button>
            </div>

            <!-- 基本信息 Section -->
            <div class="org-detail-section">
              <div class="org-section-title">基本信息</div>
              <div class="org-info-grid">
                <div class="org-info-row">
                  <span class="org-info-label">组织编码</span>
                  <span class="org-info-value">{{ selectedOrg.code }}</span>
                </div>
                <div class="org-info-row">
                  <span class="org-info-label">组织类型</span>
                  <span class="org-info-value">{{ selectedOrg.type }}</span>
                </div>
                <div class="org-info-row">
                  <span class="org-info-label">负责人</span>
                  <span class="org-info-value">{{ selectedOrg.leader }}</span>
                </div>
                <div class="org-info-row">
                  <span class="org-info-label">联系方式</span>
                  <span class="org-info-value">{{ selectedOrg.phone }}</span>
                </div>
              </div>
            </div>

            <!-- 下级组织 Section -->
            <div v-if="selectedOrg.children && selectedOrg.children.length" class="org-detail-section">
              <div class="org-section-title">
                下级组织（{{ selectedOrg.children.length }}个）
              </div>
              <div class="org-sub-list">
                <div
                  v-for="sub in selectedOrg.children"
                  :key="sub.id"
                  class="org-sub-item"
                >
                  <span class="org-sub-name">{{ sub.name }}</span>
                  <span class="org-sub-code">{{ sub.code }}</span>
                  <a-button class="org-sub-edit-btn" size="small">
                    <EditOutlined />
                  </a-button>
                </div>
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div v-else class="org-detail-empty">
            <span class="org-detail-empty-text">请在左侧选择一个组织查看详情</span>
          </div>
        </div>
      </div>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { PlusOutlined, SearchOutlined, ApartmentOutlined, EditOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'

interface OrgNode {
  id: number
  name: string
  code: string
  type: string
  description: string
  leader: string
  phone: string
  children?: OrgNode[]
}

const searchText = ref('')
const selectedOrgId = ref(3) // Default: 放射科
const expandedKeys = ref<number[]>([1, 3]) // Default: expand root + 放射科

const treeData = reactive<OrgNode[]>([
  {
    id: 1,
    name: 'XX医院',
    code: 'ORG-HOSPITAL-001',
    type: '医院',
    description: 'XX医院总院',
    leader: '李院长',
    phone: '010-82340000',
    children: [
      {
        id: 2,
        name: '内科',
        code: 'DEPT-NK-001',
        type: '科室',
        description: '内科综合科室',
        leader: '王主任',
        phone: '010-82341000',
      },
      {
        id: 3,
        name: '放射科',
        code: 'DEPT-FS-001',
        type: '科室',
        description: '放射诊断科室，含CT室、MRI室',
        leader: '张主任',
        phone: '010-82345678',
        children: [
          {
            id: 4,
            name: 'CT室',
            code: 'DEPT-FS-CT-001',
            type: '部门',
            description: 'CT影像检查部门',
            leader: '刘主管',
            phone: '010-82345601',
          },
          {
            id: 5,
            name: 'MRI室',
            code: 'DEPT-FS-MRI-001',
            type: '部门',
            description: '磁共振影像检查部门',
            leader: '赵主管',
            phone: '010-82345602',
          },
        ],
      },
      {
        id: 6,
        name: '外科',
        code: 'DEPT-WK-001',
        type: '科室',
        description: '外科综合科室',
        leader: '孙主任',
        phone: '010-82342000',
      },
      {
        id: 7,
        name: '检验科',
        code: 'DEPT-JY-001',
        type: '科室',
        description: '临床检验科室',
        leader: '陈主任',
        phone: '010-82343000',
      },
      {
        id: 8,
        name: '病理科',
        code: 'DEPT-BL-001',
        type: '科室',
        description: '病理诊断科室',
        leader: '周主任',
        phone: '010-82344000',
      },
    ],
  },
])

// Filter tree by search text
const filteredTreeData = computed(() => {
  if (!searchText.value) return treeData
  const keyword = searchText.value.toLowerCase()
  return treeData
    .map((root) => {
      const rootMatch = root.name.toLowerCase().includes(keyword)
      const filteredChildren = (root.children || []).filter(
        (child) =>
          child.name.toLowerCase().includes(keyword) ||
          (child.children || []).some((grand) => grand.name.toLowerCase().includes(keyword))
      )
      if (rootMatch || filteredChildren.length > 0) {
        return { ...root, children: rootMatch ? root.children : filteredChildren }
      }
      return null
    })
    .filter(Boolean) as OrgNode[]
})

// Find selected org node by id (recursive)
function findOrg(nodes: OrgNode[], id: number): OrgNode | null {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children) {
      const found = findOrg(node.children, id)
      if (found) return found
    }
  }
  return null
}

const selectedOrg = computed(() => findOrg(treeData, selectedOrgId.value))

function selectOrg(id: number) {
  selectedOrgId.value = id
}

function toggleExpand(id: number) {
  const idx = expandedKeys.value.indexOf(id)
  if (idx >= 0) {
    expandedKeys.value.splice(idx, 1)
  } else {
    expandedKeys.value.push(id)
  }
}
</script>

<style scoped>
.org-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.org-page-header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.org-page-title {
  font-size: 22px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  margin: 0;
}

.org-page-subtitle {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
}

.org-page-header-right {
  display: flex;
  align-items: center;
}

/* Two-panel layout */
.org-layout {
  display: flex;
  gap: 20px;
  min-height: 500px;
}

/* Left panel: tree */
.org-tree-panel {
  width: 280px;
  min-width: 280px;
  flex-shrink: 0;
}

.org-tree-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.org-search-input {
  margin-bottom: 12px;
}

.org-search-icon {
  color: rgba(0, 0, 0, 0.25);
}

.org-tree-list {
  display: flex;
  flex-direction: column;
}

/* Tree items */
.org-tree-item {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
  user-select: none;
}

.org-tree-item:hover {
  background: rgba(0, 0, 0, 0.04);
}

.org-tree-item-active {
  background: rgba(22, 119, 255, 0.06);
}

.org-tree-item-active:hover {
  background: rgba(22, 119, 255, 0.1);
}

.org-tree-item-expanded {
  background: rgba(22, 119, 255, 0.04);
}

.org-tree-root {
  font-weight: 600;
}

.org-tree-child {
  padding-left: 28px;
}

.org-tree-grandchild {
  padding-left: 48px;
}

.org-tree-chevron {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  font-size: 10px;
  color: rgba(0, 0, 0, 0.45);
  flex-shrink: 0;
  margin-right: 2px;
}

.org-tree-icon {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
  margin-right: 6px;
  flex-shrink: 0;
}

.org-tree-label {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.88);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.org-tree-label-root {
  font-weight: 600;
}

/* Right panel: detail */
.org-detail-panel {
  flex: 1;
  min-width: 0;
}

.org-detail-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 24px;
  background: #fff;
}

.org-detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.org-detail-header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.org-detail-title {
  font-size: 18px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  margin: 0;
}

.org-detail-desc {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}

.org-detail-edit-btn {
  flex-shrink: 0;
}

/* Sections */
.org-detail-section {
  margin-bottom: 20px;
}

.org-detail-section:last-child {
  margin-bottom: 0;
}

.org-section-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  margin-bottom: 16px;
}

/* Info grid */
.org-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 32px;
}

.org-info-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.org-info-label {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  white-space: nowrap;
}

.org-info-value {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.88);
}

/* Sub-organization list */
.org-sub-list {
  display: flex;
  flex-direction: column;
}

.org-sub-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.org-sub-item:last-child {
  border-bottom: none;
}

.org-sub-name {
  flex: 1;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.88);
}

.org-sub-code {
  width: 130px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  margin-right: 12px;
}

.org-sub-edit-btn {
  flex-shrink: 0;
}

/* Empty state */
.org-detail-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fff;
}

.org-detail-empty-text {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.25);
}
</style>
