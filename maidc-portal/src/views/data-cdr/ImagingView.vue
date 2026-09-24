<template>
  <div class="imaging-view">
    <div v-loading="loading" class="min-h-[200px]">
      <div v-if="studies.length > 0" class="imaging-grid">
        <el-card
          v-for="study in studies"
          :key="study.id"
          shadow="hover"
          class="imaging-card !rounded-lg cursor-pointer"
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
              <el-icon :size="32" color="#cbd5e1"><Picture /></el-icon>
              <span>暂无影像</span>
            </div>
          </div>

          <!-- Study Info -->
          <div class="imaging-info">
            <div class="imaging-title">
              <el-tag
                :type="modalityTypeMap[study.modality] || 'info'"
                size="small"
                :style="modalityStyleMap[study.modality]"
              >
                {{ study.modality }}
              </el-tag>
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
                <div class="report-summary line-clamp-2" :title="study.report_summary || '暂无报告'">
                  {{ study.report_summary || '暂无报告' }}
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <el-empty v-else description="暂无影像检查记录" :image-size="60" />
    </div>

    <!-- Full Report Dialog -->
    <el-dialog
      v-model="reportVisible"
      :title="currentStudy ? `${currentStudy.study_type} - ${currentStudy.body_part}` : '检查报告'"
      width="720px"
      :destroy-on-close="true"
    >
      <template v-if="currentStudy">
        <el-descriptions :column="2" border size="small" class="mb-4">
          <el-descriptions-item label="检查类型">{{ currentStudy.study_type }}</el-descriptions-item>
          <el-descriptions-item label="检查部位">{{ currentStudy.body_part }}</el-descriptions-item>
          <el-descriptions-item label="检查日期">{{ formatDateTime(currentStudy.study_date) }}</el-descriptions-item>
          <el-descriptions-item label="报告医生">{{ currentStudy.report_doctor }}</el-descriptions-item>
        </el-descriptions>

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
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Picture } from '@element-plus/icons-vue'
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

const modalityTypeMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
  CT: 'primary',
  XRay: 'success',
  PET: 'warning',
}
const modalityStyleMap: Record<string, { color: string; background: string; borderColor: string }> = {
  MRI: { color: '#8b5cf6', background: '#f5f3ff', borderColor: '#ddd6fe' },
  Ultrasound: { color: '#06b6d4', background: '#ecfeff', borderColor: '#a5f3fc' },
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
.imaging-card :deep(.el-card__body) {
  padding: 16px;
}
.imaging-thumbnail {
  width: 100%;
  height: 180px;
  background: #f8fafc;
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
  color: #cbd5e1;
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
  color: #0f172a;
}
.imaging-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.meta-row {
  font-size: 13px;
  color: #64748b;
}
.meta-label {
  color: #94a3b8;
}
.report-summary {
  font-size: 13px;
  color: #94a3b8;
}
.report-section {
  margin-bottom: 16px;
}
.report-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 8px;
  padding-left: 8px;
  border-left: 3px solid #0ea5e9;
}
.report-text {
  font-size: 14px;
  color: #64748b;
  line-height: 1.8;
  white-space: pre-wrap;
  background: #f8fafc;
  padding: 12px 16px;
  border-radius: 6px;
}
</style>
