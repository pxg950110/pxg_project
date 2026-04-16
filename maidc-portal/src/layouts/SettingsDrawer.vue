<template>
  <a-drawer
    :open="visible"
    title="页面设置"
    placement="right"
    :width="300"
    @close="emit('update:visible', false)"
  >
    <!-- Theme Color -->
    <div class="setting-section">
      <div class="setting-label">主题色</div>
      <div class="color-grid">
        <a-tooltip v-for="c in presetColors" :key="c.value" :title="c.label">
          <div
            :class="['color-item', { active: '#' + uiStore.primaryColor === c.value }]"
            :style="{ background: c.value }"
            @click="uiStore.setPrimaryColor(c.value.replace('#', ''))"
          >
            <CheckOutlined v-if="'#' + uiStore.primaryColor === c.value" class="check-icon" />
          </div>
        </a-tooltip>
      </div>
    </div>

    <a-divider />

    <!-- Tab Bar Toggle -->
    <div class="setting-section">
      <div class="setting-row">
        <div>
          <div class="setting-label">页签栏</div>
          <div class="setting-desc">在顶部显示已打开页面的标签</div>
        </div>
        <a-switch :checked="uiStore.tabBarEnabled" @change="uiStore.toggleTabBar()" />
      </div>
    </div>

    <a-divider />

    <!-- Future settings can be added below -->
    <div class="setting-section">
      <div class="setting-row">
        <div>
          <div class="setting-label">更多设置</div>
          <div class="setting-desc">持续更新中…</div>
        </div>
      </div>
    </div>
  </a-drawer>
</template>

<script setup lang="ts">
import { CheckOutlined } from '@ant-design/icons-vue'
import { useUiStore } from '@/stores/ui'

defineProps<{ visible: boolean }>()
const emit = defineEmits<{ 'update:visible': [value: boolean] }>()

const uiStore = useUiStore()

const presetColors = [
  { label: '极客蓝', value: '#1677ff' },
  { label: '极光绿', value: '#52c41a' },
  { label: '酱紫', value: '#722ed1' },
  { label: '薄暮红', value: '#f5222d' },
  { label: '日暮橙', value: '#fa8c16' },
]
</script>

<style lang="scss" scoped>
.setting {
  &-section {
    margin-bottom: 4px;
  }

  &-label {
    font-size: 14px;
    font-weight: 500;
    color: rgba(0, 0, 0, 0.85);
    margin-bottom: 8px;
  }

  &-desc {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
    margin-top: 2px;
  }

  &-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}

.color-grid {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.color-item {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 2px solid transparent;

  &:hover {
    transform: scale(1.1);
  }

  &.active {
    border-color: rgba(0, 0, 0, 0.25);
    box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.06);
  }

  .check-icon {
    color: #fff;
    font-size: 14px;
  }
}
</style>
