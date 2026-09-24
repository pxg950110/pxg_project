<template>
  <PageContainer title="流量路由管理" subtitle="管理模型部署的流量分配与路由策略">
    <template #extra>
      <el-button type="primary" @click="openCreateModal">
        <el-icon class="mr-1"><Plus /></el-icon>新建路由
      </el-button>
    </template>

    <!-- Route Cards -->
    <div class="flex flex-col gap-4">
      <el-card
        v-for="route in displayRoutes"
        :key="route.id"
        :class="['route-card', { 'route-card-disabled': route.status === 'disabled' }]"
        class="!rounded-xl !border-slate-200/80"
        shadow="hover"
      >
        <!-- Card Header -->
        <div class="route-card-header">
          <div class="route-card-title-row">
            <span class="route-card-name">{{ route.name }}</span>
            <el-tag
              size="small"
              :type="typeTagType(route.type)"
              :style="typeTagStyle(route.type)"
            >{{ typeBadgeLabel(route.type) }}</el-tag>
            <el-tag size="small" :type="route.status === 'active' ? 'success' : 'danger'">
              {{ route.status === 'active' ? '启用' : '禁用' }}
            </el-tag>
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
          <div class="flex items-center gap-2">
            <el-button link type="primary" size="small" @click="editRoute(route)">编辑</el-button>
            <el-button link type="primary" size="small" @click="showDetail(route)">详情</el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- Route Detail Section -->
    <div v-if="selectedRoute" class="route-detail-section">
      <el-divider />
      <div class="route-detail-header">
        <h3 class="route-detail-title">路由配置详情 — {{ selectedRoute.name }}</h3>
        <el-button text size="small" @click="selectedRoute = null">
          <el-icon class="mr-1"><Close /></el-icon>关闭
        </el-button>
      </div>

      <el-descriptions :column="2" border size="small" class="route-detail-desc">
        <el-descriptions-item label="默认部署">
          {{ selectedRoute.config.defaultDeployment }}
        </el-descriptions-item>
        <el-descriptions-item v-if="selectedRoute.config.canaryPercent != null" label="金丝雀百分比">
          {{ selectedRoute.config.canaryPercent }}%
        </el-descriptions-item>
        <el-descriptions-item v-if="selectedRoute.config.weights" label="权重配比">
          {{ selectedRoute.config.weights }}
        </el-descriptions-item>
        <el-descriptions-item label="成功率阈值">
          {{ selectedRoute.config.successThreshold }}
        </el-descriptions-item>
        <el-descriptions-item label="自动提升">
          {{ selectedRoute.config.autoPromote ? '开启' : '关闭' }}
        </el-descriptions-item>
      </el-descriptions>

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
    <el-dialog
      v-model="createModal.visible"
      :title="editingId ? '编辑路由' : '新建路由'"
      width="700px"
    >
      <el-form label-width="100px">
        <el-form-item label="路由名称" required>
          <el-input v-model="routeForm.name" placeholder="请输入路由名称" />
        </el-form-item>
        <el-form-item label="路由类型">
          <el-select v-model="routeForm.type" placeholder="请选择路由类型">
            <el-option label="金丝雀发布 (CANARY)" value="CANARY" />
            <el-option label="AB测试 (AB_TEST)" value="AB_TEST" />
            <el-option label="加权路由 (WEIGHTED)" value="WEIGHTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="流量规则">
          <TrafficRuleEditor v-model="routeForm.rules" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createModal.close()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import TrafficRuleEditor from '@/components/TrafficRuleEditor/index.vue'
import { useModal } from '@/hooks/useModal'
import { getRoutes, createRoute, updateRoute } from '@/api/model'

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

const displayRoutes = ref<Route[]>([])

const routeForm = reactive({
  name: '',
  type: 'CANARY' as 'CANARY' | 'AB_TEST' | 'WEIGHTED',
  rules: [] as any[],
})

function typeBadgeColor(type: string): string {
  const map: Record<string, string> = {
    CANARY: 'primary',
    AB_TEST: 'success',
    WEIGHTED: 'purple',
  }
  return map[type] || 'info'
}

const purpleTagStyle = { color: '#8b5cf6', backgroundColor: '#f5f3ff', borderColor: '#ddd6fe' }

function typeTagType(type: string): string {
  const c = typeBadgeColor(type)
  return c === 'purple' ? 'primary' : c
}

function typeTagStyle(type: string) {
  return typeBadgeColor(type) === 'purple' ? purpleTagStyle : undefined
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
    const res = await getRoutes()
    const data = res.data?.data || []
    routes.value = data
    displayRoutes.value = data
  } catch {
    routes.value = []
    displayRoutes.value = []
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
      await updateRoute(editingId.value, routeForm)
    } else {
      await createRoute(routeForm)
    }
    ElMessage.success('路由保存成功')
    createModal.close()
    editingId.value = null
    loadRoutes()
  } catch {
    ElMessage.error('路由保存失败')
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
  color: #0f172a;
}

.route-card-model {
  font-size: 14px;
  color: #94a3b8;
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
  background: #f8fafc;
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
  color: #0f172a;
}

.traffic-bar-version {
  color: #94a3b8;
}

/* Card Footer */
.route-card-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
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
  color: #0f172a;
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
  color: #0f172a;
  margin-bottom: 12px;
}

/* JSON Preview */
.json-preview {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 16px;
  overflow-x: auto;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #0f172a;
  margin: 0;
}
</style>
