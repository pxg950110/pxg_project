<template>
  <PageContainer title="告警详情">
    <template #extra>
      <el-button @click="router.back()">返回</el-button>
    </template>

    <div class="mt-4">
      <el-row :gutter="16">
        <!-- Left Panel: Alert Info -->
        <el-col :span="14">
          <div class="h-full rounded-xl border border-slate-200/80 bg-white p-6 shadow-clinical-sm">
            <div class="mb-5 flex items-center gap-3">
              <span class="text-lg font-semibold text-slate-900">推理延迟过高</span>
              <el-tag type="danger">严重</el-tag>
            </div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="规则名称">推理延迟监控</el-descriptions-item>
              <el-descriptions-item label="触发时间">2026-04-12 10:30:00</el-descriptions-item>
              <el-descriptions-item label="当前值">850ms</el-descriptions-item>
              <el-descriptions-item label="阈值">&gt;500ms</el-descriptions-item>
              <el-descriptions-item label="关联模型/部署">肺结节检测-v2</el-descriptions-item>
              <el-descriptions-item label="通知方式">邮件 + 钉钉</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>

        <!-- Right Panel: Processing Timeline -->
        <el-col :span="10">
          <div class="h-full rounded-xl border border-slate-200/80 bg-white p-6 shadow-clinical-sm">
            <div class="mb-5 text-base font-semibold text-slate-900">处理时间线</div>
            <el-timeline>
              <el-timeline-item type="success">
                <div class="text-sm font-medium text-slate-900">告警触发</div>
                <div class="mt-1 text-[13px] text-slate-500">推理延迟超过阈值 (850ms &gt; 500ms)</div>
                <div class="mt-1 text-xs text-slate-400">2026-04-12 10:30:00</div>
              </el-timeline-item>
              <el-timeline-item type="primary">
                <div class="text-sm font-medium text-slate-900">通知已发送</div>
                <div class="mt-1 text-[13px] text-slate-500">已通知: 李医生(邮件), 张主任(钉钉)</div>
                <div class="mt-1 text-xs text-slate-400">2026-04-12 10:31:15</div>
              </el-timeline-item>
              <el-timeline-item type="warning">
                <div class="text-sm font-medium text-slate-900">等待处理</div>
                <div class="mt-1 text-[13px] text-slate-500">等待相关人员确认处理</div>
                <div class="mt-1 text-xs text-slate-400">2026-04-12 10:35:00</div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-col>
      </el-row>

      <!-- Action Buttons -->
      <div class="mt-5 flex gap-3">
        <el-button type="primary" @click="handleConfirm">确认处理</el-button>
        <el-button @click="handleFalseAlarm">标记误报</el-button>
        <el-button @click="router.back()">返回</el-button>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'

const router = useRouter()
const route = useRoute()

// Mock: use route params id for future API integration
const alertId = route.params.id

function handleConfirm() {
  ElMessage.success('告警已确认处理')
}

function handleFalseAlarm() {
  ElMessage.info('已标记为误报')
}
</script>
