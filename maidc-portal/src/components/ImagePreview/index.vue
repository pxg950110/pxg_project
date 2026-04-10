<template>
  <div class="image-preview">
    <div class="preview-main">
      <a-image
        :src="currentSrc"
        :preview="true"
        class="main-image"
      >
        <template #previewRender>
          <img
            :src="currentSrc"
            :style="previewTransform"
            class="preview-zoomed"
          />
          <div class="preview-toolbar">
            <a-button size="small" @click="rotate -= 90"><RotateLeftOutlined /> 旋转</a-button>
            <a-button size="small" @click="rotate += 90"><RotateRightOutlined /> 旋转</a-button>
            <a-button size="small" @click="zoomIn"><ZoomInOutlined /> 放大</a-button>
            <a-button size="small" @click="zoomOut"><ZoomOutOutlined /> 缩小</a-button>
            <a-button size="small" @click="resetTransform"><SyncOutlined /> 重置</a-button>
          </div>
        </template>
      </a-image>
    </div>
    <div v-if="list && list.length > 1" class="preview-thumbs">
      <div
        v-for="(item, idx) in list"
        :key="idx"
        :class="['thumb-item', { active: item === currentSrc }]"
        @click="currentSrc = item"
      >
        <img :src="item" alt="thumb" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  RotateLeftOutlined,
  RotateRightOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  SyncOutlined,
} from '@ant-design/icons-vue'

interface Props {
  src: string
  list?: string[]
}

const props = defineProps<Props>()

const currentSrc = ref(props.src)
const zoom = ref(1)
const rotate = ref(0)

const previewTransform = computed(() => ({
  transform: `scale(${zoom.value}) rotate(${rotate.value}deg)`,
  transition: 'transform 0.3s ease',
  maxWidth: '90vw',
  maxHeight: '90vh',
}))

function zoomIn() {
  zoom.value = Math.min(zoom.value + 0.25, 5)
}

function zoomOut() {
  zoom.value = Math.max(zoom.value - 0.25, 0.25)
}

function resetTransform() {
  zoom.value = 1
  rotate.value = 0
}
</script>

<style scoped>
.image-preview {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.preview-main {
  text-align: center;
}
.main-image {
  max-width: 100%;
  max-height: 400px;
  object-fit: contain;
  border-radius: 4px;
}
.preview-zoomed {
  display: block;
  margin: 0 auto;
}
.preview-toolbar {
  position: fixed;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  background: rgba(0, 0, 0, 0.6);
  padding: 8px 16px;
  border-radius: 8px;
  z-index: 10000;
}
.preview-toolbar :deep(.ant-btn) {
  background: transparent;
  color: #fff;
  border-color: rgba(255, 255, 255, 0.3);
}
.preview-thumbs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 4px 0;
}
.thumb-item {
  width: 60px;
  height: 60px;
  border: 2px solid transparent;
  border-radius: 4px;
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;
  transition: border-color 0.2s;
}
.thumb-item:hover {
  border-color: #1677ff;
}
.thumb-item.active {
  border-color: #1677ff;
}
.thumb-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
