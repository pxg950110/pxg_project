<template>
  <div class="p-6 space-y-6 max-w-[1600px] mx-auto">
    <!-- 空间头部导航与行动区 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div class="flex items-center gap-3">
        <el-button link class="!text-slate-600 hover:!text-sky-600 !p-0" @click="router.back()">
          <el-icon :size="20"><Back /></el-icon>
        </el-button>
        <div>
          <h2 class="text-xl font-bold text-slate-900 tracking-tight m-0 flex items-center gap-2.5">
            <span class="w-2 h-5 bg-indigo-500 rounded-full" />
            {{ detail?.space?.name ? `${detail.space.name} 专病知识库` : '专病知识库详情' }}
          </h2>
          <div class="flex items-center gap-2 mt-1 flex-wrap">
            <el-tag
              v-for="c in detail?.space?.icdCodes || []"
              :key="c"
              size="small"
              type="info"
              effect="plain"
              class="!rounded font-mono font-medium"
            >
              {{ c }}
            </el-tag>
            <span class="text-xs text-slate-500 truncate max-w-lg">
              {{ detail?.space?.description || '暂无专病空间简介' }}
            </span>
          </div>
        </div>
      </div>

      <!-- 右侧队列与快捷操作 -->
      <div class="flex items-center gap-3">
        <div
          v-if="detail?.cohort?.cohortId && !detail?.cohort?.deleted"
          class="flex items-center gap-3 px-3 py-1.5 rounded-xl bg-sky-50/80 border border-sky-200/80 text-xs text-sky-800"
        >
          <div class="flex items-center gap-1.5 font-medium">
            <el-icon class="text-sky-600"><Connection /></el-icon>
            已关联专病队列：<span class="font-bold">{{ detail.cohort.name }}</span>
          </div>
          <span class="font-mono text-sky-700">（{{ detail.cohort.patientCount ?? 0 }} 例在管患者）</span>
          <el-button type="primary" link size="small" class="!text-xs font-semibold" @click="goCohort">
            进入队列画像 &rarr;
          </el-button>
        </div>

        <el-button @click="router.back()">返回</el-button>
      </div>
    </div>

    <!-- 顶层 Tab 导航容器 -->
    <el-tabs v-model="activeTab" class="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-clinical-sm">
      <!-- ==================== 页签一：知识构建与条目管理 ==================== -->
      <el-tab-pane label="专病知识管理" name="items">
        <div class="space-y-4 pt-2">
          <!-- 筛选与操作工具栏 -->
          <div class="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-slate-100">
            <div class="flex items-center gap-3 flex-wrap flex-1">
              <el-radio-group v-model="itemType" size="small" @change="onFilterChange">
                <el-radio-button label="">全部 ({{ detail?.itemTotal ?? 0 }})</el-radio-button>
                <el-radio-button label="GUIDELINE">指南/共识 ({{ detail?.counts?.GUIDELINE ?? 0 }})</el-radio-button>
                <el-radio-button label="LITERATURE">学术文献 ({{ detail?.counts?.LITERATURE ?? 0 }})</el-radio-button>
                <el-radio-button label="PATHWAY">临床路径 ({{ detail?.counts?.PATHWAY ?? 0 }})</el-radio-button>
                <el-radio-button label="SCALE">量表/表单 ({{ detail?.counts?.SCALE ?? 0 }})</el-radio-button>
                <el-radio-button label="ARCHIVED">已归档</el-radio-button>
              </el-radio-group>

              <el-input
                v-model="itemKeyword"
                placeholder="搜索标题、摘要或来源..."
                clearable
                size="small"
                class="!w-64"
                @clear="onFilterChange"
                @keyup.enter="onFilterChange"
              >
                <template #prefix>
                  <el-icon class="text-slate-400"><Search /></el-icon>
                </template>
              </el-input>
            </div>

            <div class="flex items-center gap-2">
              <el-button
                v-if="hasPermission('cdr:diseasekb:manage')"
                type="primary"
                size="small"
                class="!rounded-lg"
                @click="openItemModal()"
              >
                <el-icon class="mr-1"><Plus /></el-icon>
                新建知识条目
              </el-button>
            </div>
          </div>

          <!-- 知识条目表格 -->
          <el-table
            v-loading="itemsLoading"
            :data="items"
            row-key="id"
            size="small"
            class="w-full"
          >
            <el-table-column label="文献 / 知识标题" min-width="260">
              <template #default="{ row }">
                <div class="py-1">
                  <div
                    class="font-semibold text-slate-800 hover:text-sky-600 cursor-pointer transition-colors"
                    @click="openItemDrawer(row.id)"
                  >
                    {{ row.title }}
                  </div>
                  <div class="flex items-center gap-2 text-xs text-slate-400 mt-1 flex-wrap font-mono">
                    <span v-if="row.source">{{ row.source }}</span>
                    <span v-if="row.versionNo">· {{ row.versionNo }}</span>
                    <span v-if="row.fileName" class="flex items-center gap-0.5 text-sky-600">
                      <el-icon><Paperclip /></el-icon> {{ row.fileName }}
                    </span>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="知识分类" width="120">
              <template #default="{ row }">
                <el-tag size="small" :type="typeTagType(row.itemType)" effect="plain" class="!rounded">
                  {{ typeLabel[row.itemType] || row.itemType }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="AI 核心摘要与实体提取" min-width="320" show-overflow-tooltip>
              <template #default="{ row }">
                <div v-if="row.aiStatus === 'DONE' && row.aiSummary" class="text-xs text-slate-600 truncate">
                  <span class="inline-block w-1.5 h-1.5 rounded-full bg-emerald-500 mr-1.5" />
                  {{ row.aiSummary }}
                </div>
                <div v-else-if="row.aiStatus === 'FAILED'" class="text-xs text-rose-500 flex items-center gap-1">
                  <el-icon><CircleClose /></el-icon> AI 提取失败，可进入重新提取
                </div>
                <div v-else class="text-xs text-slate-400 flex items-center gap-1">
                  <el-icon class="animate-spin text-sky-500"><Loading /></el-icon> AI 解析处理排队中
                </div>
              </template>
            </el-table-column>

            <el-table-column label="发布状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="statusTagType(row.status)" effect="light" class="!rounded">
                  {{ statusLabel[row.status] || row.status }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <div class="flex items-center gap-1">
                  <el-button link type="primary" size="small" class="!text-xs" @click="openItemDrawer(row.id)">
                    查看
                  </el-button>
                  <el-button
                    v-if="hasPermission('cdr:diseasekb:manage')"
                    link
                    type="primary"
                    size="small"
                    class="!text-xs"
                    @click="openItemModal(row)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-if="row.status === 'DRAFT' && hasPermission('cdr:diseasekb:manage')"
                    link
                    type="success"
                    size="small"
                    class="!text-xs"
                    @click="handlePublish(row, 'PUBLISH')"
                  >
                    发布
                  </el-button>
                  <el-button
                    v-if="row.status === 'PUBLISHED' && hasPermission('cdr:diseasekb:manage')"
                    link
                    type="warning"
                    size="small"
                    class="!text-xs"
                    @click="handlePublish(row, 'ARCHIVE')"
                  >
                    下架
                  </el-button>
                  <el-button
                    v-if="hasPermission('cdr:diseasekb:manage')"
                    link
                    type="danger"
                    size="small"
                    class="!text-xs"
                    @click="handleDeleteItem(row)"
                  >
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页器 -->
          <div v-if="itemTotal > 20" class="flex justify-end pt-3">
            <el-pagination
              v-model:current-page="itemPage"
              :page-size="20"
              :total="itemTotal"
              layout="total, prev, pager, next"
              background
              size="small"
              @current-change="loadItems"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- ==================== 页签二：AI 专病知识问答与循证检索 ==================== -->
      <el-tab-pane label="AI 临床循证问答" name="qa">
        <div class="flex flex-col lg:flex-row gap-4 h-[650px] pt-2">
          <!-- 左侧会话列表栏 -->
          <div class="w-full lg:w-64 flex flex-col border border-slate-200/80 rounded-xl p-3 bg-slate-50/60">
            <el-button
              type="primary"
              plain
              class="w-full !rounded-lg mb-3"
              @click="newSession"
            >
              <el-icon class="mr-1.5"><Plus /></el-icon>
              新建问答会话
            </el-button>

            <div class="flex-1 overflow-y-auto space-y-1.5 pr-1">
              <div
                v-for="s in sessions"
                :key="s.id"
                class="group flex items-center justify-between p-2.5 rounded-lg text-xs cursor-pointer transition-colors"
                :class="s.id === sessionId ? 'bg-white shadow-clinical-sm text-sky-600 font-semibold border border-sky-200/60' : 'text-slate-600 hover:bg-white/80'"
                @click="switchSession(s.id)"
              >
                <div class="flex items-center gap-2 truncate flex-1 min-w-0">
                  <el-icon class="text-slate-400 flex-shrink-0"><ChatDotRound /></el-icon>
                  <span class="truncate">{{ s.title || '新对话' }}</span>
                </div>
                <el-icon
                  class="opacity-0 group-hover:opacity-100 text-slate-400 hover:text-rose-500 transition-opacity ml-1"
                  @click.stop="removeSession(s)"
                >
                  <Delete />
                </el-icon>
              </div>

              <div v-if="!sessions.length" class="py-12 text-center text-xs text-slate-400">
                暂无历史会话
              </div>
            </div>
          </div>

          <!-- 右侧对话问答区 -->
          <div class="flex-1 flex flex-col border border-slate-200/80 rounded-xl bg-white overflow-hidden shadow-clinical-sm">
            <div class="px-4 py-2 bg-amber-50/80 border-b border-amber-200/60 text-xs text-amber-800 flex items-center gap-2">
              <el-icon class="text-amber-600"><WarningFilled /></el-icon>
              本问答基于该专病库中已发布指南、学术文献生成，可追踪原文证据链，仅供医学科研参考。
            </div>

            <!-- 消息流容器 -->
            <div ref="messagesRef" class="flex-1 p-5 overflow-y-auto space-y-4">
              <div v-if="!messages.length && !qaStreaming" class="py-24 text-center">
                <el-icon :size="48" class="text-sky-300 mb-2"><MagicStick /></el-icon>
                <div class="text-sm font-semibold text-slate-700">基于专病知识库的智能临床问答</div>
                <p class="text-xs text-slate-400 mt-1">
                  输入临床诊疗问题，如：“慢性鼻窦炎伴鼻息肉的首选规范治疗路径是什么？”
                </p>
              </div>

              <div
                v-for="m in messages"
                :key="m.id"
                class="flex gap-3"
                :class="m.role === 'USER' ? 'justify-end' : 'justify-start'"
              >
                <div
                  v-if="m.role !== 'USER'"
                  class="w-8 h-8 rounded-full bg-gradient-to-tr from-sky-500 to-indigo-600 text-white flex items-center justify-center flex-shrink-0 text-xs shadow-clinical-sm"
                >
                  <el-icon><Cpu /></el-icon>
                </div>

                <div
                  class="max-w-[78%] rounded-2xl p-4 text-xs leading-relaxed"
                  :class="m.role === 'USER' ? 'bg-sky-600 text-white rounded-br-none' : 'bg-slate-50 border border-slate-200/80 text-slate-800 rounded-bl-none shadow-clinical-sm'"
                >
                  <div class="whitespace-pre-wrap">{{ m.content }}</div>

                  <!-- 引用文献出处证据链 -->
                  <div v-if="m.citations?.length" class="mt-3 pt-2.5 border-t border-slate-200/60 space-y-1.5">
                    <div class="text-[11px] font-semibold text-slate-500 flex items-center gap-1">
                      <el-icon><Paperclip /></el-icon> 循证引用来源：
                    </div>
                    <div
                      v-for="(c, i) in m.citations"
                      :key="i"
                      class="flex items-center gap-1.5 p-1.5 rounded-lg bg-white border border-slate-200 text-sky-700 hover:bg-sky-50 cursor-pointer transition-colors"
                      @click="openItemDrawer(c.itemId)"
                    >
                      <el-tag size="small" type="primary" effect="light" class="!rounded !text-[10px]">
                        引用 [{{ i + 1 }}]
                      </el-tag>
                      <span class="font-semibold truncate flex-1">{{ c.title }}</span>
                      <span v-if="c.snippet" class="text-slate-400 truncate max-w-xs">§ {{ c.snippet }}</span>
                    </div>
                  </div>
                </div>

                <div
                  v-if="m.role === 'USER'"
                  class="w-8 h-8 rounded-full bg-slate-200 text-slate-600 flex items-center justify-center flex-shrink-0 text-xs"
                >
                  <el-icon><User /></el-icon>
                </div>
              </div>

              <!-- 流式回复打字机 -->
              <div v-if="qaStreaming" class="flex gap-3 justify-start">
                <div class="w-8 h-8 rounded-full bg-gradient-to-tr from-sky-500 to-indigo-600 text-white flex items-center justify-center flex-shrink-0 text-xs shadow-clinical-sm">
                  <el-icon><Cpu /></el-icon>
                </div>
                <div class="max-w-[78%] rounded-2xl rounded-bl-none p-4 text-xs leading-relaxed bg-slate-50 border border-slate-200/80 text-slate-800">
                  <span class="inline-block animate-pulse font-bold text-sky-600">▋</span>
                  {{ streamBuffer }}
                </div>
              </div>
            </div>

            <!-- 输入底栏 -->
            <div class="p-3 border-t border-slate-200 flex items-center gap-2 bg-slate-50/40">
              <el-input
                v-model="question"
                placeholder="请输入关于本专病的临床诊疗、用药或随访准则问题... (Enter 发送)"
                :disabled="qaStreaming"
                clearable
                @keyup.enter="ask"
              />
              <el-button
                type="primary"
                :loading="qaStreaming"
                class="!rounded-lg px-5"
                @click="ask"
              >
                发送提问
              </el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ==================== 知识条目新建 / 编辑弹窗 ==================== -->
    <el-dialog
      v-model="itemModalVisible"
      :title="itemForm.id ? '编辑专病知识条目' : '新建专病知识条目'"
      width="780px"
      destroy-on-close
      class="!rounded-2xl"
    >
      <el-form label-position="top" class="space-y-3">
        <div class="grid grid-cols-4 gap-4">
          <el-form-item label="条目标题" required class="col-span-3">
            <el-input v-model="itemForm.title" placeholder="如：《中国慢性鼻窦炎诊断和治疗指南 (2024)》" maxlength="256" />
          </el-form-item>

          <el-form-item label="知识类型" required class="col-span-1">
            <el-select v-model="itemForm.itemType" class="w-full">
              <el-option label="指南/共识" value="GUIDELINE" />
              <el-option label="学术文献" value="LITERATURE" />
              <el-option label="临床路径" value="PATHWAY" />
              <el-option label="量表/表单" value="SCALE" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="人工核心摘要">
          <el-input v-model="itemForm.summary" type="textarea" :rows="2" placeholder="简述该指南或文献的主要适用病种、推荐要点等（选填）" />
        </el-form-item>

        <el-form-item label="知识正文 (Markdown 格式)">
          <el-input v-model="itemForm.content" type="textarea" :rows="7" placeholder="录入正文或关键诊疗原则标准..." />
        </el-form-item>

        <div class="grid grid-cols-2 gap-4">
          <el-form-item label="来源出处">
            <el-input v-model="itemForm.source" placeholder="如：中华耳鼻咽喉头颈外科杂志" />
          </el-form-item>
          <el-form-item label="作者 / 主编机构">
            <el-input v-model="itemForm.authors" placeholder="如：中华医学会耳鼻咽喉头颈外科学分会" />
          </el-form-item>
        </div>

        <div class="grid grid-cols-3 gap-4">
          <el-form-item label="发布日期">
            <el-date-picker v-model="publishDateValue" type="date" value-format="YYYY-MM-DD" placeholder="选择发布日期" class="!w-full" />
          </el-form-item>
          <el-form-item label="版本标识">
            <el-input v-model="itemForm.versionNo" placeholder="如 2024修订版" />
          </el-form-item>
          <el-form-item label="知识标签">
            <el-select
              v-model="tagList"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="输入标签按回车"
              class="w-full"
            />
          </el-form-item>
        </div>

        <el-form-item v-if="itemForm.fileName" label="已挂载文件附件">
          <div class="p-2.5 rounded-lg bg-slate-50 border border-slate-200 text-xs text-slate-700 flex items-center gap-2">
            <el-icon class="text-sky-600"><Document /></el-icon>
            {{ itemForm.fileName }}
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="flex items-center justify-end gap-2">
          <el-button @click="itemModalVisible = false">取消</el-button>
          <el-button type="primary" :loading="itemSubmitting" @click="handleItemSubmit">保存条目</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ==================== 条目详情抽屉（含 AI 知识图谱与解读） ==================== -->
    <el-drawer
      v-model="drawerVisible"
      size="680px"
      :title="drawerItem?.title || '知识条目详情'"
      destroy-on-close
    >
      <template v-if="drawerItem">
        <div class="space-y-5 pb-10">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="知识类型">
              <el-tag size="small" :type="typeTagType(drawerItem.itemType)" effect="plain">
                {{ typeLabel[drawerItem.itemType] }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag size="small" :type="statusTagType(drawerItem.status)" effect="light">
                {{ statusLabel[drawerItem.status] }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="来源出处" :span="2">
              {{ drawerItem.source || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="发布时间">
              {{ drawerItem.publishDate || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="版本编号">
              {{ drawerItem.versionNo || '-' }}
            </el-descriptions-item>
          </el-descriptions>

          <!-- AI 解读与结构化抽取折叠面板 -->
          <el-collapse class="!border-slate-200 rounded-xl overflow-hidden shadow-clinical-sm">
            <el-collapse-item name="ai">
              <template #title>
                <div class="flex items-center gap-2 font-semibold text-xs text-sky-800 px-2">
                  <el-icon class="text-sky-600"><Cpu /></el-icon>
                  🤖 AI 知识结构化解析结果
                </div>
              </template>
              <div class="p-3 text-xs leading-relaxed space-y-2">
                <div v-if="drawerItem.aiStatus === 'DONE'">
                  <p v-if="drawerItem.aiSummary" class="text-slate-700 bg-sky-50/60 p-2.5 rounded-lg border border-sky-100">
                    {{ drawerItem.aiSummary }}
                  </p>
                  <pre
                    v-if="drawerItem.aiExtract"
                    class="p-2.5 bg-slate-900 text-emerald-400 rounded-lg font-mono text-[11px] overflow-x-auto"
                  >{{ JSON.stringify(drawerItem.aiExtract, null, 2) }}</pre>
                </div>
                <div v-else-if="drawerItem.aiStatus === 'FAILED'" class="text-rose-500">
                  AI 提取生成失败，请点击下方重新生成。
                </div>
                <div v-else class="text-slate-400">
                  AI 解析处理正在排队中...
                </div>

                <div v-if="hasPermission('cdr:diseasekb:manage')" class="pt-2">
                  <el-button size="small" type="primary" plain :loading="recomputing" @click="handleRecompute">
                    重新生成 AI 解读
                  </el-button>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>

          <!-- 正文呈现 -->
          <div class="space-y-2">
            <div class="text-xs font-semibold text-slate-800">正文内容</div>
            <div class="p-4 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-700 leading-relaxed whitespace-pre-wrap max-h-96 overflow-y-auto">
              {{ drawerItem.content || drawerItem.summary || '（暂无详细正文内容）' }}
            </div>
          </div>

          <!-- 附件下载 -->
          <div v-if="drawerItem.fileUrl" class="pt-2">
            <el-button type="primary" plain size="small" class="!rounded-lg">
              <el-icon class="mr-1"><Download /></el-icon>
              下载附件：{{ drawerItem.fileName || '知识附件' }}
            </el-button>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Back,
  Connection,
  Search,
  Plus,
  Paperclip,
  CircleClose,
  Loading,
  ChatDotRound,
  Delete,
  WarningFilled,
  MagicStick,
  Cpu,
  User,
  Document,
  Download,
} from '@element-plus/icons-vue'
import { usePermission } from '@/hooks/usePermission'
import {
  getDiseaseKbSpace,
  getDiseaseKbItems,
  getDiseaseKbItem,
  createDiseaseKbItem,
  updateDiseaseKbItem,
  deleteDiseaseKbItem,
  publishDiseaseKbItem,
  recomputeDiseaseKbItem,
  getQaSessions,
  createQaSession,
  getQaMessages,
  deleteQaSession,
  askDiseaseKb,
} from '@/api/diseaseKb'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const spaceId = route.params.id as string

const typeLabel: Record<string, string> = {
  GUIDELINE: '指南/共识',
  LITERATURE: '学术文献',
  PATHWAY: '临床路径',
  SCALE: '量表/表单',
}

function typeTagType(t: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  switch (t) {
    case 'GUIDELINE': return 'warning'
    case 'LITERATURE': return 'success'
    case 'PATHWAY': return ''
    case 'SCALE': return 'info'
    default: return 'info'
  }
}

const statusLabel: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ARCHIVED: '已归档',
}

function statusTagType(s: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  switch (s) {
    case 'PUBLISHED': return 'success'
    case 'DRAFT': return 'info'
    case 'ARCHIVED': return 'warning'
    default: return 'info'
  }
}

// ---- 空间详情 ----
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

function tagsToObj(list: string[]) {
  const obj: Record<string, boolean> = {}
  list.forEach((t) => { obj[t] = true })
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
    ElMessage.warning('请填写条目标题')
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
    ElMessage.success('知识条目已保存')
    itemModalVisible.value = false
    loadItems()
    loadSpace()
  } catch (e: any) {
    ElMessage.error('保存失败: ' + (e.message || ''))
  } finally {
    itemSubmitting.value = false
  }
}

async function handlePublish(record: any, action: 'PUBLISH' | 'ARCHIVE') {
  try {
    if (action === 'PUBLISH') {
      await ElMessageBox.confirm('发布后将全员可见并触发 AI 深度结构化解析，确认？', '发布确认', {
        confirmButtonText: '确定发布',
        cancelButtonText: '取消',
        type: 'info',
      })
    }
    await publishDiseaseKbItem(record.id, action)
    ElMessage.success(action === 'PUBLISH' ? '条目已发布' : '条目已下架')
    loadItems()
    loadSpace()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('操作失败: ' + (e.message || ''))
  }
}

async function handleDeleteItem(record: any) {
  try {
    await ElMessageBox.confirm('确定删除该知识条目？删除后无法恢复。', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'error',
    })
    await deleteDiseaseKbItem(record.id)
    ElMessage.success('条目已删除')
    loadItems()
    loadSpace()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('删除失败: ' + (e.message || ''))
  }
}

async function openItemDrawer(itemId: number) {
  try {
    const res = await getDiseaseKbItem(itemId)
    drawerItem.value = res.data?.data
    drawerVisible.value = true
  } catch (e: any) {
    ElMessage.error('加载条目详情失败: ' + (e.message || ''))
  }
}

async function handleRecompute() {
  if (!drawerItem.value?.id) return
  recomputing.value = true
  try {
    await recomputeDiseaseKbItem(drawerItem.value.id)
    ElMessage.success('AI 解析已重新触发，请稍后刷新')
    await openItemDrawer(drawerItem.value.id)
  } catch (e: any) {
    ElMessage.error('重新生成失败: ' + (e.message || ''))
  } finally {
    recomputing.value = false
  }
}

// ---- AI 问答交互 ----
async function loadSessions() {
  try {
    const res = await getQaSessions(spaceId)
    sessions.value = res.data?.data || []
    if (sessions.value.length && !sessionId.value) {
      switchSession(sessions.value[0].id)
    }
  } catch {}
}

async function newSession() {
  try {
    const res = await createQaSession(spaceId)
    const created = res.data?.data
    if (created?.id) {
      sessions.value.unshift(created)
      switchSession(created.id)
    }
  } catch (e: any) {
    ElMessage.error('创建会话失败: ' + (e.message || ''))
  }
}

async function switchSession(id: number) {
  sessionId.value = id
  messages.value = []
  try {
    const res = await getQaMessages(id)
    messages.value = res.data?.data || []
    scrollToBottom()
  } catch {}
}

async function removeSession(s: any) {
  try {
    await deleteQaSession(s.id)
    sessions.value = sessions.value.filter((item) => item.id !== s.id)
    if (sessionId.value === s.id) {
      sessionId.value = sessions.value[0]?.id || null
      if (sessionId.value) switchSession(sessionId.value)
      else messages.value = []
    }
  } catch {}
}

async function ask() {
  if (!question.value.trim() || qaStreaming.value) return
  if (!sessionId.value) {
    await newSession()
  }
  const q = question.value
  question.value = ''
  messages.value.push({ id: Date.now(), role: 'USER', content: q })
  scrollToBottom()

  qaStreaming.value = true
  streamBuffer.value = ''

  let citations: any[] = []
  try {
    await askDiseaseKb(sessionId.value!, q, {
      onDelta: (chunk: string) => {
        streamBuffer.value += chunk
        scrollToBottom()
      },
      onCitations: (c: any[]) => {
        citations = c
      },
      onDone: () => {
        messages.value.push({
          id: Date.now() + 1,
          role: 'ASSISTANT',
          content: streamBuffer.value,
          citations: citations,
        })
        streamBuffer.value = ''
        qaStreaming.value = false
        scrollToBottom()
      },
      onError: (msg: string) => {
        ElMessage.error('问答请求失败: ' + (msg || '网络异常'))
        qaStreaming.value = false
      },
    })
  } catch {
    qaStreaming.value = false
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function goCohort() {
  if (detail.value?.cohort?.cohortId) {
    router.push({ name: 'DiseaseDetail', params: { id: detail.value.cohort.cohortId } })
  }
}

onMounted(() => {
  loadSpace()
  loadItems()
  loadSessions()
})
</script>
