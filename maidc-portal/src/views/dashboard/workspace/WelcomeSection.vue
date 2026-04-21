<template>
  <div class="welcome-section">
    <div class="welcome-info">
      <h2 class="welcome-greeting">{{ greeting }}，{{ userName }}</h2>
      <p class="welcome-date">{{ date }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  userName: string
  date: string
}>()

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})
</script>

<style scoped lang="scss">
.welcome-section {
  padding: 24px;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  border-radius: 8px;
  color: #fff;
  margin-bottom: 16px;
}

.welcome-greeting {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 4px;
}

.welcome-date {
  font-size: 14px;
  opacity: 0.85;
  margin: 0;
}
</style>
