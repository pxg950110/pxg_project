<template>
  <PageContainer :title="detail?.space?.name ? `${detail.space.name}知识库` : '专病知识库'">
    <template #extra>
      <a-space>
        <a-button v-if="detail?.cohort?.cohortId && !detail?.cohort?.deleted" @click="goCohort">
          <ApartmentOutlined /> 查看关联队列
        </a-button>
        <a-button @click="router.back()">返回</a-button>
      </a-space>
    </template>

    <a-spin :spinning="loadingSpace">
      <!-- 空间头部 -->
      <div class="space-header" v-if="detail">
        <div class="space-header__meta">
          <a-tag v-for="c in detail.space?.icdCodes || []" :key="c" color="geekblue">{{ c }}</a-tag>
          <span class="muted">{{ detail.space?.description }}</span>
        </div>
        <div class="space-header__cohort" v-if="detail.cohort?.cohortId && !detail.cohort.deleted">
          <a-card size="small" class="cohort-card">
            <b>{{ detail.cohort.name }}</b>
            <a-divider type="vertical" />
            患者 {{ detail.cohort.patientCount ?? 0 }} 人
            <a-button type="link" size="small" @click="goCohort">进入队列 <ArrowRightOutlined /></a-button>
          </a-card>
        </div>
        <div class="space-header__cohort" v-else-if="detail.cohort?.deleted">
          <a-alert type="warning" show-icon message="关联的专病队列已被删除，可在编辑空间时重新关联" />
        </div>
        <div class="space-header__cohort" v-else-if="hasPermission('cdr:diseasekb:manage')">
          <a-alert type="info" show-icon message="尚未关联专病队列，可在列表页编辑空间时选择关联" />
        </div>
      </div>
    </a-spin>

    <a-tabs v-model:activeKey="activeTab">
      <!-- ==================== 页签一：知识管理 ==================== -->
      <a-tab-pane key="items" tab="知识管理">
        <div class="search-bar">
          <a-radio-group v-model:value="itemType" @change="onFilterChange">
            <a-radio-button value="">全部 ({{ detail?.itemTotal ?? 0 }})</a-radio-button>
            <a-radio-button value="GUIDELINE">指南/共识 ({{ detail?.counts?.GUIDELINE ?? 0 }})</a-radio-button>
            <a-radio-button value="LITERATURE">文献 ({{ detail?.counts?.LITERATURE ?? 0 }})</a-radio-button>
            <a-radio-button value="PATHWAY">诊疗路径 ({{ detail?.counts?.PATHWAY ?? 0 }})</a-radio-button>
            <a-radio-button value="SCALE">量表/表单 ({{ detail?.counts?.SCALE ?? 0 }})</a-radio-button>
            <a-radio-button value="ARCHIVED">归档</a-radio-button>
          </a-radio-group>
          <a-input-search
            v-model:value="itemKeyword" placeholder="搜索标题 / 摘要" style="width: 260px"
            @search="onFilterChange" allow-clear
          />
          <a-button
            v-if="hasPermission('cdr:diseasekb:manage')" type="primary" style="margin-left: auto"
            @click="openItemModal()"
          >
            <template #icon><PlusOutlined /></template> 新建条目
          </a-button>
        </div>

        <a-table
          :data-source="items" :loading="itemsLoading" row-key="id"
          :pagination="{ current: itemPage, pageSize: 20, total: itemTotal, showTotal: (t: number) => `共 ${t} 条` }"
          @change="(p: any) => { itemPage = p.current; loadItems() }"
        >
          <a-table-column title="标题" data-index="title">
            <template #default="{ record }">
              <a-typography-link @click="openItemDrawer(record.id)">{{ record.title }}</a-typography-link>
              <div class="muted" style="font-size: 12px; margin-top: 2px">
                {{ record.source }}<template v-if="record.versionNo"> · {{ record.versionNo }}</template>
                <template v-if="record.fileName"> · <PaperClipOutlined /> {{ record.fileName }}</template>
              </div>
            </template>
          </a-table-column>
          <a-table-column title="类型" data-index="itemType" :width="110">
            <template #default="{ record }">
              <a-tag :color="typeColor[record.itemType]">{{ typeLabel[record.itemType] ?? record.itemType }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="AI 摘要" data-index="aiSummary" :width="340" ellipsis>
            <template #default="{ record }">
              <span v-if="record.aiStatus === 'DONE' && record.aiSummary">{{ record.aiSummary }}</span>
              <span v-else-if="record.aiStatus === 'FAILED'" class="muted">生成失败，可在详情中重新生成</span>
              <span v-else class="muted">AI 处理排队中</span>
            </template>
          </a-table-column>
          <a-table-column title="状态" data-index="status" :width="90">
            <template #default="{ record }">
              <a-badge
                :status="record.status === 'PUBLISHED' ? 'success' : record.status === 'DRAFT' ? 'default' : 'warning'"
                :text="statusLabel[record.status] ?? record.status"
              />
            </template>
          </a-table-column>
          <a-table-column title="操作" :width="220" v-if="hasPermission('cdr:diseasekb:manage')">
            <template #default="{ record }">
              <a-button type="link" size="small" @click="openItemDrawer(record.id)">查看</a-button>
              <a-button type="link" size="small" @click="openItemModal(record)">编辑</a-button>
              <a-popconfirm
                v-if="record.status === 'DRAFT'" title="发布后将全员可见并触发 AI 处理，确认？"
                @confirm="handlePublish(record, 'PUBLISH')"
              >
                <a-button type="link" size="small">发布</a-button>
              </a-popconfirm>
              <a-button
                v-if="record.status === 'PUBLISHED'" type="link" size="small"
                @click="handlePublish(record, 'ARCHIVE')"
              >下架</a-button>
              <a-popconfirm title="确认删除该条目？" @confirm="handleDeleteItem(record)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </template>
          </a-table-column>
        </a-table>
      </a-tab-pane>

      <!-- ==================== 页签二：AI 问答 ==================== -->
      <a-tab-pane key="qa" tab="AI 问答">
        <div class="qa-layout">
          <!-- 会话列表 -->
          <div class="qa-sessions">
            <a-button block type="primary" ghost @click="newSession"><PlusOutlined /> 新会话</a-button>
            <div class="qa-session-list">
              <div
                v-for="s in sessions" :key="s.id"
                class="qa-session-item" :class="{ active: s.id === sessionId }"
                @click="switchSession(s.id)"
              >
                <MessageOutlined class="qa-session-item__icon" />
                <span class="qa-session-item__title">{{ s.title || '新会话' }}</span>
                <DeleteOutlined class="qa-session-item__del" @click.stop="removeSession(s)" />
              </div>
              <a-empty v-if="!sessions.length" :image-style="{ height: '40px' }" description="暂无会话" style="margin-top: 24px" />
            </div>
          </div>

          <!-- 对话区 -->
          <div class="qa-chat">
            <a-alert
              type="warning" show-icon banner
              message="内容由 AI 基于本知识库生成，仅供参考，不构成诊疗依据"
            />
            <div class="qa-messages" ref="messagesRef">
              <div v-for="m in messages" :key="m.id" class="qa-msg" :class="m.role === 'USER' ? 'user' : 'ai'">
                <div class="qa-msg__bubble">
                  <div class="qa-msg__text">{{ m.content }}</div>
                  <div v-if="m.citations?.length" class="qa-msg__citations">
                    <div
                      v-for="(c, i) in m.citations" :key="i" class="citation"
                      @click="openItemDrawer(c.itemId)"
                    >
                      <PaperClipOutlined /> 引用{{ i + 1 }}: {{ c.title }}
                      <span v-if="c.snippet" class="muted"> §{{ c.snippet.slice(0, 40) }}…</span>
                    </div>
                  </div>
                </div>
              </div>
              <div v-if="qaStreaming" class="qa-msg ai">
                <div class="qa-msg__bubble"><span class="cursor">▍</span>{{ streamBuffer }}</div>
              </div>
              <a-empty
                v-if="!messages.length && !qaStreaming" description="向本专病知识库提问，例如：一线用药原则是什么？"
                style="margin: 80px 0"
              />
            </div>
            <div class="qa-input">
              <a-input
                v-model:value="question" placeholder="继续提问…（Enter 发送）" @press-enter="ask"
                :disabled="qaStreaming"
              />
              <a-button type="primary" :loading="qaStreaming" @click="ask">发送</a-button>
            </div>
          </div>
        </div>
      </a-tab-pane>
    </a-tabs>

    <!-- ==================== 条目新建/编辑弹窗 ==================== -->
    <a-modal
      v-model:open="itemModalVisible"
      :title="itemForm.id ? '编辑知识条目' : '新建知识条目'" width="760px"
      :confirm-loading="itemSubmitting" @ok="handleItemSubmit"
    >
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="18">
            <a-form-item label="标题" required>
              <a-input v-model:value="itemForm.title" :maxlength="512" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="类型" required>
              <a-select v-model:value="itemForm.itemType">
                <a-select-option value="GUIDELINE">指南/共识</a-select-option>
                <a-select-option value="LITERATURE">学术文献</a-select-option>
                <a-select-option value="PATHWAY">诊疗路径</a-select-option>
                <a-select-option value="SCALE">量表/表单</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="人工摘要">
          <a-textarea v-model:value="itemForm.summary" :rows="2" placeholder="可选" />
        </a-form-item>
        <a-form-item label="正文（Markdown）">
          <a-textarea v-model:value="itemForm.content" :rows="6" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="来源"><a-input v-model:value="itemForm.source" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="作者/机构"><a-input v-model:value="itemForm.authors" /></a-form-item></a-col>
          <a-col :span="8">
            <a-form-item label="发布日期">
              <a-date-picker v-model:value="publishDateValue" style="width: 100%" value-format="YYYY-MM-DD" />
            </a-form-item>
          </a-col>
          <a-col :span="8"><a-form-item label="版本号"><a-input v-model:value="itemForm.versionNo" placeholder="如 2024版" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="标签"><a-select v-model:value="tagList" mode="tags" placeholder="回车添加" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="附件（PDF/DOCX/XLSX ≤ 50MB，创建后可在列表上传）">
          <a-input v-if="itemForm.fileName" :value="itemForm.fileName" disabled />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ==================== 条目详情抽屉（含 AI 解读） ==================== -->
    <a-drawer v-model:open="drawerVisible" width="640" :title="drawerItem?.title">
      <template v-if="drawerItem">
        <a-descriptions size="small" :column="2" bordered>
          <a-descriptions-item label="类型">
            <a-tag :color="typeColor[drawerItem.itemType]">{{ typeLabel[drawerItem.itemType] }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="状态">{{ statusLabel[drawerItem.status] }}</a-descriptions-item>
          <a-descriptions-item label="来源" :span="2">{{ drawerItem.source || '-' }}</a-descriptions-item>
          <a-descriptions-item label="发布日期">{{ drawerItem.publishDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="版本">{{ drawerItem.versionNo || '-' }}</a-descriptions-item>
        </a-descriptions>

        <a-collapse style="margin-top: 16px">
          <a-collapse-panel key="ai" header="🤖 AI 解读（自动生成）">
            <template v-if="drawerItem.aiStatus === 'DONE'">
              <p v-if="drawerItem.aiSummary">{{ drawerItem.aiSummary }}</p>
              <pre v-if="drawerItem.aiExtract" class="ai-extract">{{ JSON.stringify(drawerItem.aiExtract, null, 2) }}</pre>
              <p v-if="!drawerItem.aiSummary && !drawerItem.aiExtract" class="muted">暂无解读内容</p>
            </template>
            <p v-else-if="drawerItem.aiStatus === 'FAILED'" class="muted">AI 解读生成失败，可点击重新生成</p>
            <p v-else class="muted">AI 处理排队中…</p>
            <a-button
              v-if="hasPermission('cdr:diseasekb:manage')" size="small" style="margin-top: 8px"
              :loading="recomputing" @click="handleRecompute"
            >重新生成</a-button>
          </a-collapse-panel>
        </a-collapse>

        <div class="drawer-content" style="margin-top: 16px">{{ drawerItem.content || drawerItem.summary || '（无正文）' }}</div>

        <div style="margin-top: 16px" v-if="drawerItem.fileUrl">
          <a-button type="primary" ghost><DownloadOutlined /> {{ drawerItem.fileName || '查看附件' }}</a-button>
        </div>
      </template>
    </a-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, PaperClipOutlined, DownloadOutlined,
  MessageOutlined, DeleteOutlined, ArrowRightOutlined, ApartmentOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { usePermission } from '@/hooks/usePermission'
import {
  getDiseaseKbSpace, getDiseaseKbItems, getDiseaseKbItem, createDiseaseKbItem, updateDiseaseKbItem,
  deleteDiseaseKbItem, publishDiseaseKbItem, recomputeDiseaseKbItem,
  getQaSessions, createQaSession, getQaMessages, deleteQaSession, askDiseaseKb,
} from '@/api/diseaseKb'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const spaceId = route.params.id as string

const typeLabel: Record<string, string> = { GUIDELINE: '指南/共识', LITERATURE: '文献', PATHWAY: '诊疗路径', SCALE: '量表/表单' }
const typeColor: Record<string, string> = { GUIDELINE: 'purple', LITERATURE: 'cyan', PATHWAY: 'geekblue', SCALE: 'orange' }
const statusLabel: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', ARCHIVED: '已归档' }

// ---- 空间详情 {space, counts, itemTotal, cohort} ----
const detail = ref<any>(null)
const loadingSpace = ref(false)

// ---- 条目列表 ----
const items = ref<any[]>([])
const itemsLoading = ref(false)
const itemPage = ref(1)
const itemTotal = ref(0)
const itemType = ref('')
const itemKeyword = ref('')

// ---- 条目弹窗 / 抽屉 ----
const itemModalVisible = ref(false)
const itemSubmitting = ref(false)
const itemForm = ref<any>({ itemType: 'GUIDELINE' })
const publishDateValue = ref<string | undefined>(undefined)
const tagList = ref<string[]>([])
const drawerVisible = ref(false)
const drawerItem = ref<any>(null)
const recomputing = ref(false)

// ---- AI 问答 ----
const sessions = ref<any[]>([])
const sessionId = ref<number | null>(null)
const messages = ref<any[]>([])
const question = ref('')
const qaStreaming = ref(false)
const streamBuffer = ref('')
const messagesRef = ref<HTMLElement>()

const activeTab = ref('items')

// 标签 select(string[]) ↔ 后端 tags jsonb（{tag: true} 形式）
function tagsToObj(list: string[]) {
  const obj: Record<string, boolean> = {}
  list.forEach(t => { obj[t] = true })
  return obj
}
function objToTags(obj: any): string[] {
  return obj && typeof obj === 'object' ? Object.keys(obj) : []
}

async function loadSpace() {
  loadingSpace.value = true
  try {
    const res = await getDiseaseKbSpace(spaceId)
    detail.value = res.data?.data
  } finally {
    loadingSpace.value = false
  }
}

async function loadItems() {
  itemsLoading.value = true
  try {
    const isArchived = itemType.value === 'ARCHIVED'
    const res = await getDiseaseKbItems(spaceId, {
      page: itemPage.value,
      page_size: 20,
      item_type: isArchived ? undefined : itemType.value || undefined,
      status: isArchived ? 'ARCHIVED' : undefined,
      keyword: itemKeyword.value || undefined,
    })
    items.value = res.data?.data?.content || []
    itemTotal.value = res.data?.data?.totalElements || 0
  } finally {
    itemsLoading.value = false
  }
}

function onFilterChange() {
  itemPage.value = 1
  loadItems()
}

function openItemModal(record?: any) {
  if (record) {
    itemForm.value = { ...record }
    publishDateValue.value = record.publishDate || undefined
    tagList.value = objToTags(record.tags)
  } else {
    itemForm.value = { itemType: itemType.value && itemType.value !== 'ARCHIVED' ? itemType.value : 'GUIDELINE' }
    publishDateValue.value = undefined
    tagList.value = []
  }
  itemModalVisible.value = true
}

async function handleItemSubmit() {
  if (!itemForm.value.title?.trim()) {
    message.warning('请填写标题')
    return
  }
  const payload = {
    ...itemForm.value,
    publishDate: publishDateValue.value,
    tags: tagsToObj(tagList.value),
  }
  itemSubmitting.value = true
  try {
    if (itemForm.value.id) await updateDiseaseKbItem(itemForm.value.id, payload)
    else await createDiseaseKbItem(spaceId, payload)
    message.success('已保存')
    itemModalVisible.value = false
    loadItems()
    loadSpace()
  } finally {
    itemSubmitting.value = false
  }
}

async function handlePublish(record: any, action: 'PUBLISH' | 'ARCHIVE') {
  await publishDiseaseKbItem(record.id, action)
  message.success(action === 'PUBLISH' ? '已发布' : '已下架')
  loadItems()
  loadSpace()
}

async function handleDeleteItem(record: any) {
  await deleteDiseaseKbItem(record.id)
  message.success('已删除')
  loadItems()
  loadSpace()
}

async function openItemDrawer(id: number) {
  const res = await getDiseaseKbItem(id)
  drawerItem.value = res.data?.data
  drawerVisible.value = true
}

async function handleRecompute() {
  if (!drawerItem.value) return
  recomputing.value = true
  try {
    await recomputeDiseaseKbItem(drawerItem.value.id)
    drawerItem.value.aiStatus = 'PENDING'
    message.success('已重新触发 AI 处理')
  } finally {
    recomputing.value = false
  }
}

function goCohort() {
  router.push({ name: 'DiseaseDetail', params: { id: detail.value.cohort.cohortId } })
}

// ---- AI 问答 ----

async function loadSessions() {
  try {
    const res = await getQaSessions(spaceId)
    sessions.value = res.data?.data || []
  } catch { /* 无 ai 权限时静默 */ }
}

async function newSession() {
  const res = await createQaSession(spaceId)
  const s = res.data?.data
  sessions.value.unshift(s)
  sessionId.value = s.id
  messages.value = []
}

async function switchSession(id: number) {
  sessionId.value = id
  const res = await getQaMessages(id)
  messages.value = res.data?.data || []
  scrollBottom()
}

async function removeSession(s: any) {
  await deleteQaSession(s.id)
  sessions.value = sessions.value.filter(x => x.id !== s.id)
  if (sessionId.value === s.id) {
    sessionId.value = null
    messages.value = []
  }
}

async function ask() {
  const q = question.value.trim()
  if (!q || qaStreaming.value) return
  if (!sessionId.value) await newSession()
  messages.value.push({ id: Date.now(), role: 'USER', content: q })
  question.value = ''
  qaStreaming.value = true
  streamBuffer.value = ''
  let citations: any[] = []
  scrollBottom()
  await askDiseaseKb(sessionId.value!, q, {
    onDelta: (t) => {
      streamBuffer.value += t
      scrollBottom()
    },
    onCitations: (c) => { citations = c },
    onDone: () => {
      if (streamBuffer.value || citations.length) {
        messages.value.push({ id: Date.now() + 1, role: 'ASSISTANT', content: streamBuffer.value, citations })
      }
      qaStreaming.value = false
      streamBuffer.value = ''
      scrollBottom()
    },
    onError: (msg) => {
      message.warning(msg)
      messages.value.push({ id: Date.now() + 1, role: 'ASSISTANT', content: `（${msg}）` })
      qaStreaming.value = false
      scrollBottom()
    },
  })
}

function scrollBottom() {
  nextTick(() => messagesRef.value?.scrollTo({ top: messagesRef.value.scrollHeight, behavior: 'smooth' }))
}

onMounted(() => {
  loadSpace()
  loadItems()
  loadSessions()
})
</script>

<style scoped>
.space-header { margin-bottom: 16px; display: flex; flex-direction: column; gap: 8px; }
.space-header__meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.cohort-card { background: #f6f9ff; }
.muted { color: #999; }
.search-bar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; flex-wrap: wrap; }

/* AI 问答布局 */
.qa-layout { display: flex; gap: 16px; height: calc(100vh - 340px); min-height: 480px; }
.qa-sessions { width: 220px; flex-shrink: 0; display: flex; flex-direction: column; gap: 12px; }
.qa-session-list { flex: 1; overflow-y: auto; border: 1px solid #f0f0f0; border-radius: 8px; padding: 8px; }
.qa-session-item { display: flex; align-items: center; gap: 8px; padding: 8px 10px; border-radius: 6px; cursor: pointer; }
.qa-session-item:hover { background: #f5f5f5; }
.qa-session-item.active { background: #e6f4ff; }
.qa-session-item__title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
.qa-session-item__del { color: #bbb; visibility: hidden; }
.qa-session-item:hover .qa-session-item__del { visibility: visible; }
.qa-chat { flex: 1; display: flex; flex-direction: column; border: 1px solid #f0f0f0; border-radius: 8px; overflow: hidden; }
.qa-messages { flex: 1; overflow-y: auto; padding: 20px; background: #fafafa; }
.qa-msg { display: flex; margin-bottom: 16px; }
.qa-msg.user { justify-content: flex-end; }
.qa-msg__bubble { max-width: 78%; padding: 10px 14px; border-radius: 10px; background: #fff; border: 1px solid #eee; line-height: 1.7; }
.qa-msg.user .qa-msg__bubble { background: #2d5afa; color: #fff; border: none; }
.qa-msg__text { white-space: pre-wrap; word-break: break-word; }
.qa-msg__citations { margin-top: 10px; border-top: 1px dashed #e8e8e8; padding-top: 8px; }
.citation { font-size: 12px; color: #2d5afa; cursor: pointer; margin-top: 4px; }
.citation:hover { text-decoration: underline; }
.qa-input { display: flex; gap: 8px; padding: 12px; border-top: 1px solid #f0f0f0; background: #fff; }
.cursor { animation: blink 1s infinite; color: #2d5afa; }
@keyframes blink { 50% { opacity: 0; } }
.ai-extract { background: #f6f6f6; padding: 12px; border-radius: 6px; font-size: 12px; overflow-x: auto; }
.drawer-content { line-height: 1.8; white-space: pre-wrap; }
</style>
