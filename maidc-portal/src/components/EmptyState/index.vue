<template>
  <div class="empty-state">
    <div class="empty-icon">
      <el-icon :size="64"><Box /></el-icon>
    </div>
    <h3 class="empty-title">{{ title }}</h3>
    <p v-if="description" class="empty-description">{{ description }}</p>
    <el-button v-if="actionText" type="primary" @click="emit('action')">
      {{ actionText }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { Box } from '@element-plus/icons-vue'

interface Props {
  title: string
  description?: string
  actionText?: string
}

defineProps<Props>()

interface Emits {
  (e: 'action'): void
}

const emit = defineEmits<Emits>()
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  text-align: center;
}
.empty-icon {
  color: #94a3b8;
  margin-bottom: 16px;
}
.empty-title {
  font-size: 16px;
  font-weight: 500;
  color: #334155;
  margin: 0 0 8px;
}
.empty-description {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 24px;
  max-width: 400px;
}
</style>
