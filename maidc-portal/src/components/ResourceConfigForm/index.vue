<template>
  <div class="resource-config-form">
    <a-form layout="vertical" size="small">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="CPU（核）">
            <a-slider v-model:value="config.cpu" :min="0.5" :max="64" :step="0.5" :marks="cpuMarks" @change="emitValue" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="内存（GB）">
            <a-slider v-model:value="config.memory" :min="1" :max="256" :step="1" :marks="memoryMarks" @change="emitValue" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="GPU（卡数）">
            <a-slider v-model:value="config.gpu" :min="0" :max="8" :step="1" :marks="gpuMarks" @change="emitValue" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="副本数">
            <a-input-number
              v-model:value="config.replicas"
              :min="1"
              :max="20"
              style="width: 100%"
              @change="emitValue"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <div class="resource-summary">
        <span class="summary-item">CPU: {{ config.cpu }} 核</span>
        <span class="summary-item">内存: {{ config.memory }} GB</span>
        <span class="summary-item">GPU: {{ config.gpu }} 卡</span>
        <span class="summary-item">副本: {{ config.replicas }}</span>
      </div>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

interface ResourceConfig {
  cpu: number
  memory: number
  gpu: number
  replicas: number
}

interface Props {
  modelValue: ResourceConfig
}

interface Emits {
  (e: 'update:modelValue', value: ResourceConfig): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const config = reactive<ResourceConfig>({
  cpu: props.modelValue?.cpu ?? 2,
  memory: props.modelValue?.memory ?? 4,
  gpu: props.modelValue?.gpu ?? 0,
  replicas: props.modelValue?.replicas ?? 1,
})

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      config.cpu = val.cpu
      config.memory = val.memory
      config.gpu = val.gpu
      config.replicas = val.replicas
    }
  },
  { deep: true },
)

const cpuMarks = { 0.5: '0.5', 4: '4', 16: '16', 32: '32', 64: '64' }
const memoryMarks = { 1: '1', 8: '8', 32: '32', 64: '64', 128: '128', 256: '256' }
const gpuMarks = { 0: '0', 1: '1', 2: '2', 4: '4', 8: '8' }

function emitValue() {
  emit('update:modelValue', { ...config })
}
</script>

<style scoped>
.resource-config-form {
  max-width: 600px;
}
.resource-summary {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 4px;
  margin-top: 4px;
}
.summary-item {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  font-weight: 500;
}
</style>
