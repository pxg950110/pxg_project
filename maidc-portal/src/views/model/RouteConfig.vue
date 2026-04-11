<template>
  <PageContainer title="流量路由管理" subtitle="管理模型部署的流量分配与路由策略">
    <template #extra>
      <a-button type="primary" @click="openCreateModal">
        <PlusOutlined /> 新建路由
      </a-button>
    </template>

    <!-- Route Cards -->
    <a-row :gutter="[16, 16]">
      <a-col :span="24" v-for="route in displayRoutes" :key="route.id">
        <a-card :class="['route-card', { 'route-card-disabled': route.status === 'disabled' }]" hoverable>
          <!-- Card Header -->
          <div class="route-card-header">
            <div class="route-card-title-row">
              <span class="route-card-name">{{ route.name }}</span>
              <a-tag :color="typeBadgeColor(route.type)">{{ typeBadgeLabel(route.type) }}</a-tag>
              <a-tag :color="route.status === 'active' ? 'success' : 'error'">
                {{ route.status === 'active' ? '启用' : '禁用' }}
              </a-tag>
            </div>
            <div class="route-card-model">{{ route.model }}</div>
          </div>

          <!-- Traffic Distribution Bar -->
          <div class="route-card-traffic">
            <div class="traffic-bar">
              <div
                v-for="(rule, i) in route.rules"
                :key="i"
                class="traffic-bar-segment"
                :style="{ width: rule.weight + '%', backgroundColor: rule.color }"
              />
            </div>
            <div class="traffic-bar-labels">
              <div v-for="(rule, i) in route.rules" :key="i" class="traffic-bar-label">
                <span class="traffic-bar-dot" :style="{ backgroundColor: rule.color }" />
                <span class="traffic-bar-percent">{{ rule.weight }}%</span>
                <span class="traffic-bar-version">{{ rule.version }}</span>
              </div>
            </div>
          </div>

          <!-- Card Footer -->
          <div class="route-card-footer">
            <a-space>
              <a-button type="link" size="small" @click="editRoute(route)">编辑</a-button>
              <a-button type="link" size="small" @click="showDetail(route)">详情</a-button>
            </a-space>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- Route Detail Section -->
    <div v-if="selectedRoute" class="route-detail-section">
      <a-divider />
      <div class="route-detail-header">
        <h3 class="route-detail-title">路由配置详情 — {{ selectedRoute.name }}</h3>
        <a-button type="text" size="small" @click="selectedRoute = null">
          <CloseOutlined /> 关闭
        </a-button>
      </div>

      <a-descriptions :column="2" bordered size="small" class="route-detail-desc">
        <a-descriptions-item label="默认部署">
          {{ selectedRoute.config.defaultDeployment }}
        </a-descriptions-item>
        <a-descriptions-item v-if="selectedRoute.config.canaryPercent != null" label="金丝雀百分比">
          {{ selectedRoute.config.canaryPercent }}%
        </a-descriptions-item>
        <a-descriptions-item v-if="selectedRoute.config.weights" label="权重配比">
          {{ selectedRoute.config.weights }}
        </a-descriptions-item>
        <a-descriptions-item label="成功率阈值">
          {{ selectedRoute.config.successThreshold }}
        </a-descriptions-item>
        <a-descriptions-item label="自动提升">
          {{ selectedRoute.config.autoPromote ? '开启' : '关闭' }}
        </a-descriptions-item>
      </a-descriptions>

      <!-- Traffic Visualization -->
      <div class="route-detail-traffic">
        <h4>流量分配</h4>
        <div class="traffic-bar traffic-bar-large">
          <div
            v-for="(rule, i) in selectedRoute.rules"
            :key="i"
            class="traffic-bar-segment"
            :style="{ width: rule.weight + '%', backgroundColor: rule.color }"
          />
        </div>
        <div class="traffic-bar-labels">
          <div v-for="(rule, i) in selectedRoute.rules" :key="i" class="traffic-bar-label">
            <span class="traffic-bar-dot" :style="{ backgroundColor: rule.color }" />
            <span class="traffic-bar-percent">{{ rule.weight }}%</span>
            <span class="traffic-bar-version">{{ rule.version }}</span>
          </div>
        </div>
      </div>

      <!-- JSON Preview -->
      <div class="route-detail-json">
        <h4>配置 JSON</h4>
        <pre class="json-preview"><code>{{ selectedRoute.json }}</code></pre>
      </div>
    </div>

    <!-- Create/Edit Route Modal -->
    <a-modal
      v-model:open="createModal.visible"
      :title="editingId ? '编辑路由' : '新建路由'"
      @ok="handleSave"
      :confirm-loading="submitting"
      width="700px"
    >
      <a-form layout="vertical">
        <a-form-item label="路由名称" required>
          <a-input v-model:value="routeForm.name" placeholder="请输入路由名称" />
        </a-form-item>
        <a-form-item label="路由类型">
          <a-select v-model:value="routeForm.type" placeholder="请选择路由类型">
            <a-select-option value="CANARY">金丝雀发布 (CANARY)</a-select-option>
            <a-select-option value="AB_TEST">AB测试 (AB_TEST)</a-select-option>
            <a-select-option value="WEIGHTED">加权路由 (WEIGHTED)</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="流量规则">
          <TrafficRuleEditor v-model="routeForm.rules" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { PlusOutlined, CloseOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import TrafficRuleEditor from '@/components/TrafficRuleEditor/index.vue'
import { useModal } from '@/hooks/useModal'
import request from '@/utils/request'

interface TrafficRule {
  version: string
  weight: number
  color: string
}

interface RouteConfig {
  defaultDeployment: string
  canaryPercent?: number
  successThreshold: string
  autoPromote: boolean
  weights?: string
}

interface Route {
  id: number
  name: string
  type: 'CANARY' | 'AB_TEST' | 'WEIGHTED'
  model: string
  status: 'active' | 'disabled'
  rules: TrafficRule[]
  config: RouteConfig
  json: string
}

const createModal = useModal()
const submitting = ref(false)
const loading = ref(false)
const routes = ref<Route[]>([])
const selectedRoute = ref<Route | null>(null)
const editingId = ref<number | null>(null)

// Mock data used as fallback when API is unavailable
const mockRoutes: Route[] = [
  {
    id: 1,
    name: '肺结节检测-金丝雀发布',
    type: 'CANARY',
    model: '肺结节检测模型',
    status: 'active',
    rules: [
      { version: '生产v2.1.0', weight: 90, color: '#1677ff' },
      { version: '灰度v2.3.1', weight: 10, color: '#52c41a' },
    ],
    config: {
      defaultDeployment: '生产v2.1.0',
      canaryPercent: 10,
      successThreshold: '95%',
      autoPromote: false,
    },
    json: '{\n  "route_type": "canary",\n  "default_deployment": "prod-v2.1.0",\n  "canary_deployment": "gray-v2.3.1",\n  "canary_percent": 10,\n  "success_threshold": 0.95,\n  "auto_promote": false\n}',
  },
  {
    id: 2,
    name: '心电图分析-AB测试',
    type: 'AB_TEST',
    model: '心电图分析模型',
    status: 'active',
    rules: [
      { version: '版本A v1.0', weight: 50, color: '#52c41a' },
      { version: '版本B v1.1', weight: 50, color: '#faad14' },
    ],
    config: {
      defaultDeployment: '版本A v1.0',
      successThreshold: '90%',
      autoPromote: true,
    },
    json: '{\n  "route_type": "a_b_test",\n  "version_a": "v1.0",\n  "version_b": "v1.1",\n  "traffic_split": 0.5,\n  "auto_promote": true\n}',
  },
  {
    id: 3,
    name: '糖尿病预测-加权路由',
    type: 'WEIGHTED',
    model: '糖尿病预测模型',
    status: 'disabled',
    rules: [
      { version: '模型A v2.0', weight: 60, color: '#722ed1' },
      { version: '模型B v1.5', weight: 40, color: '#b37feb' },
    ],
    config: {
      defaultDeployment: '模型A v2.0',
      weights: '60:40',
      successThreshold: '85%',
      autoPromote: false,
    },
    json: '{\n  "route_type": "weighted",\n  "deployments": [\n    {"version": "v2.0", "weight": 60},\n    {"version": "v1.5", "weight": 40}\n  ]\n}',
  },
]

// Display routes: prefer API data, fall back to mock
const displayRoutes = ref<Route[]>(mockRoutes)

const routeForm = reactive({
  name: '',
  type: 'CANARY' as 'CANARY' | 'AB_TEST' | 'WEIGHTED',
  rules: [] as any[],
})

function typeBadgeColor(type: string): string {
  const map: Record<string, string> = {
    CANARY: 'blue',
    AB_TEST: 'green',
    WEIGHTED: 'purple',
  }
  return map[type] || 'default'
}

function typeBadgeLabel(type: string): string {
  const map: Record<string, string> = {
    CANARY: 'CANARY',
    AB_TEST: 'AB_TEST',
    WEIGHTED: 'WEIGHTED',
  }
  return map[type] || type
}

async function loadRoutes() {
  loading.value = true
  try {
    const res = await request.get('/deployments/routes')
    const data = res.data?.data || []
    if (data.length > 0) {
      routes.value = data
      displayRoutes.value = data
    } else {
      displayRoutes.value = mockRoutes
    }
  } catch {
    displayRoutes.value = mockRoutes
  } finally {
    loading.value = false
  }
}

function openCreateModal() {
  editingId.value = null
  routeForm.name = ''
  routeForm.type = 'CANARY'
  routeForm.rules = []
  createModal.open()
}

function editRoute(record: Route) {
  editingId.value = record.id
  routeForm.name = record.name
  routeForm.type = record.type
  routeForm.rules = record.rules.map((r) => ({ ...r }))
  createModal.open()
}

function showDetail(route: Route) {
  selectedRoute.value = route
}

async function handleSave() {
  submitting.value = true
  try {
    if (editingId.value) {
      await request.put(`/deployments/routes/${editingId.value}`, routeForm)
    } else {
      await request.post('/deployments/routes', routeForm)
    }
    message.success('路由保存成功')
    createModal.close()
    editingId.value = null
    loadRoutes()
  } catch {
    message.error('路由保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadRoutes)
</script>

<style scoped>
.route-card {
  border-radius: 8px;
}

.route-card-disabled {
  opacity: 0.7;
}

.route-card-header {
  margin-bottom: 16px;
}

.route-card-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.route-card-name {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.route-card-model {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 2px;
}

/* Traffic Bar */
.route-card-traffic {
  margin-bottom: 16px;
}

.traffic-bar {
  display: flex;
  height: 20px;
  border-radius: 4px;
  overflow: hidden;
  background: #f5f5f5;
}

.traffic-bar-large {
  height: 28px;
  border-radius: 6px;
}

.traffic-bar-segment {
  min-width: 2px;
  transition: width 0.3s ease;
}

.traffic-bar-labels {
  display: flex;
  gap: 24px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.traffic-bar-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.traffic-bar-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.traffic-bar-percent {
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.traffic-bar-version {
  color: rgba(0, 0, 0, 0.45);
}

/* Card Footer */
.route-card-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

/* Detail Section */
.route-detail-section {
  margin-top: 8px;
}

.route-detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.route-detail-title {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  margin: 0;
}

.route-detail-desc {
  margin-bottom: 24px;
}

.route-detail-traffic {
  margin-bottom: 24px;
}

.route-detail-traffic h4,
.route-detail-json h4 {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  margin-bottom: 12px;
}

/* JSON Preview */
.json-preview {
  background: #f6f8fa;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 16px;
  overflow-x: auto;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #24292e;
  margin: 0;
}
</style>
