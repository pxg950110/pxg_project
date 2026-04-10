<template>
  <PageContainer title="标注工作台" :loading="loading">
    <template #extra>
      <a-space>
        <a-button @click="handleAiPreAnnotate" :loading="aiLoading">
          <RobotOutlined /> AI 预标注
        </a-button>
        <a-button type="primary" @click="handleSave" :loading="saving">保存</a-button>
      </a-space>
    </template>

    <a-row :gutter="16" v-if="task">
      <a-col :span="18">
        <a-card>
          <div class="annotation-area">
            <div v-if="task.task_type === 'IMAGE'" class="image-annotation">
              <div class="canvas-wrapper">
                <p class="placeholder-text">影像标注区域（集成 Canvas 标注工具）</p>
              </div>
            </div>
            <div v-else class="text-annotation">
              <a-card v-for="(item, index) in textItems" :key="index" size="small" style="margin-bottom: 8px">
                <p>{{ item.text }}</p>
                <a-select v-model:value="item.label" style="width: 200px" placeholder="选择标签">
                  <a-select-option v-for="label in labels" :key="label" :value="label">{{ label }}</a-select-option>
                </a-select>
              </a-card>
            </div>
          </div>
          <div class="navigation" style="margin-top: 16px; text-align: center">
            <a-space>
              <a-button :disabled="currentIndex <= 0" @click="currentIndex--">上一条</a-button>
              <span>{{ currentIndex + 1 }} / {{ totalCount }}</span>
              <a-button :disabled="currentIndex >= totalCount - 1" @click="currentIndex++">下一条</a-button>
            </a-space>
          </div>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card title="任务信息" size="small">
          <a-descriptions :column="1" size="small">
            <a-descriptions-item label="任务名">{{ task.name }}</a-descriptions-item>
            <a-descriptions-item label="类型">{{ task.task_type }}</a-descriptions-item>
            <a-descriptions-item label="进度">{{ task.progress }}</a-descriptions-item>
          </a-descriptions>
        </a-card>
        <a-card title="标签列表" size="small" style="margin-top: 8px">
          <a-tag v-for="label in labels" :key="label" style="margin-bottom: 4px">{{ label }}</a-tag>
        </a-card>
      </a-col>
    </a-row>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { RobotOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { getLabelTask, triggerAiPreAnnotate } from '@/api/label'

const route = useRoute()
const task = ref<any>(null)
const loading = ref(false)
const saving = ref(false)
const aiLoading = ref(false)
const currentIndex = ref(0)
const totalCount = ref(0)

const labels = ref<string[]>(['正常', '异常', '待确认'])
const textItems = ref<Array<{ text: string; label: string }>>([])

async function loadTask() {
  loading.value = true
  try {
    const res = await getLabelTask(Number(route.params.id))
    task.value = res.data.data
    totalCount.value = res.data.data.total_count || 10
    if (task.value.task_type === 'TEXT') {
      textItems.value = Array.from({ length: 5 }, (_, i) => ({ text: `样本文本 ${i + 1}`, label: '' }))
    }
  } finally { loading.value = false }
}

async function handleAiPreAnnotate() {
  aiLoading.value = true
  try {
    await triggerAiPreAnnotate(Number(route.params.id))
    message.success('AI 预标注已提交')
  } finally { aiLoading.value = false }
}

function handleSave() {
  message.success('标注已保存')
}

onMounted(loadTask)
</script>

<style scoped>
.annotation-area { min-height: 400px; }
.canvas-wrapper { height: 400px; display: flex; align-items: center; justify-content: center; background: #fafafa; border: 1px dashed #d9d9d9; border-radius: 4px; }
.placeholder-text { color: rgba(0,0,0,0.25); }
</style>
