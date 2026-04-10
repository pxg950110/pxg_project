<template>
  <div class="empty-state">
    <div class="empty-icon">
      <InboxOutlined />
    </div>
    <h3 class="empty-title">{{ title }}</h3>
    <p v-if="description" class="empty-description">{{ description }}</p>
    <a-button v-if="actionText" type="primary" @click="emit('action')">
      {{ actionText }}
    </a-button>
  </div>
</template>

<script setup lang="ts">
import { InboxOutlined } from '@ant-design/icons-vue'

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
  font-size: 64px;
  color: #bfbfbf;
  margin-bottom: 16px;
}
.empty-title {
  font-size: 16px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.65);
  margin: 0 0 8px;
}
.empty-description {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
  margin: 0 0 24px;
  max-width: 400px;
}
</style>
