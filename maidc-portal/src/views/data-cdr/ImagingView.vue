<template>
  <div class="imaging-view">
    <a-spin :spinning="loading">
      <div v-if="studies.length > 0" class="imaging-grid">
        <a-card
          v-for="study in studies"
          :key="study.id"
          :bordered="true"
          hoverable
          class="imaging-card"
          @click="openReport(study)"
        >
          <!-- Thumbnail -->
          <div class="imaging-thumbnail">
            <ImagePreview
              v-if="study.thumbnail_url"
              :src="study.thumbnail_url"
              :list="study.image_urls"
            />
            <div v-else class="thumbnail-placeholder">
              <PictureOutlined style="font-size: 32px; color: #bfbfbf" />
              <span>暂无影像</span>
            </div>
          </div>

          <!-- Study Info -->
          <div class="imaging-info">
            <div class="imaging-title">
              <a-tag :color="modalityColorMap[study.modality] || 'blue'" size="small">
                {{ study.modality }}
              </a-tag>
              <span class="study-type">{{ study.study_type }}</span>
            </div>
            <div class="imaging-meta">
              <div class="meta-row">
                <span class="meta-label">检查部位：</span>
                <span>{{ study.body_part }}</span>
              </div>
              <div class="meta-row">
                <span class="meta-label">检查日期：</span>
                <span>{{ formatDate(study.study_date) }}</span>
              </div>
              <div class="meta-row">
                <span class="meta-label">报告摘要：</span>
                <a-typography-paragraph
                  :content="study.report_summary || '暂无报告'"
                  :ellipsis="{ rows: 2, tooltip: true }"
                  class="report-summary"
                />
              </div>
            </div>
          </div>
        </a-card>
      </div>

      <a-empty v-else description="暂无影像检查记录" />
    </a-spin>

    <!-- Full Report Modal -->
    <a-modal
      v-model:open="reportVisible"
      :title="currentStudy ? `${currentStudy.study_type} - ${currentStudy.body_part}` : '检查报告'"
      width="720px"
      :footer="null"
      destroy-on-close
    >
      <template v-if="currentStudy">
        <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="检查类型">{{ currentStudy.study_type }}</a-descriptions-item>
          <a-descriptions-item label="检查部位">{{ currentStudy.body_part }}</a-descriptions-item>
          <a-descriptions-item label="检查日期">{{ formatDateTime(currentStudy.study_date) }}</a-descriptions-item>
          <a-descriptions-item label="报告医生">{{ currentStudy.report_doctor }}</a-descriptions-item>
        </a-descriptions>

        <div class="report-section">
          <h4 class="report-section-title">影像所见</h4>
          <p class="report-text">{{ currentStudy.findings || '暂无' }}</p>
        </div>

        <div class="report-section">
          <h4 class="report-section-title">诊断意见</h4>
          <p class="report-text">{{ currentStudy.impression || '暂无' }}</p>
        </div>

        <div v-if="currentStudy.image_urls && currentStudy.image_urls.length > 0" class="report-section">
          <h4 class="report-section-title">影像资料</h4>
          <ImagePreview :src="currentStudy.image_urls[0]" :list="currentStudy.image_urls" />
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { PictureOutlined } from '@ant-design/icons-vue'
import ImagePreview from '@/components/ImagePreview/index.vue'
import { getImagingStudies } from '@/api/data'
import { formatDate, formatDateTime } from '@/utils/date'

defineOptions({ name: 'ImagingView' })

interface Props {
  patientId: string
  encounterId: string
}

const props = defineProps<Props>()

const loading = ref(false)
const studies = ref<any[]>([])
const reportVisible = ref(false)
const currentStudy = ref<any>(null)

const modalityColorMap: Record<string, string> = {
  CT: 'blue',
  MRI: 'purple',
  XRay: 'green',
  Ultrasound: 'cyan',
  PET: 'orange',
}

function openReport(study: any) {
  currentStudy.value = study
  reportVisible.value = true
}

async function loadData() {
  loading.value = true
  try {
    const res = await getImagingStudies(props.patientId, props.encounterId)
    studies.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.imaging-view {
  padding-top: 8px;
}
.imaging-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}
.imaging-card {
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.imaging-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
}
.imaging-card :deep(.ant-card-body) {
  padding: 16px;
}
.imaging-thumbnail {
  width: 100%;
  height: 180px;
  background: #f5f5f5;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  overflow: hidden;
}
.imaging-thumbnail :deep(.image-preview) {
  width: 100%;
  height: 100%;
}
.imaging-thumbnail :deep(.preview-main) {
  height: 100%;
}
.imaging-thumbnail :deep(.main-image) {
  max-height: 170px;
}
.thumbnail-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #bfbfbf;
  font-size: 13px;
}
.imaging-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.imaging-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.study-type {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
}
.imaging-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.meta-row {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}
.meta-label {
  color: rgba(0, 0, 0, 0.45);
}
.report-summary {
  margin-bottom: 0 !important;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}
.report-section {
  margin-bottom: 16px;
}
.report-section-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  margin-bottom: 8px;
  padding-left: 8px;
  border-left: 3px solid #1677ff;
}
.report-text {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.65);
  line-height: 1.8;
  white-space: pre-wrap;
  background: #fafafa;
  padding: 12px 16px;
  border-radius: 6px;
}
</style>
