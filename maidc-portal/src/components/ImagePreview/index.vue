<template>
  <div class="image-preview">
    <div class="preview-main">
      <el-image
        :src="currentSrc"
        :preview-src-list="[currentSrc]"
        :preview-teleported="true"
        fit="contain"
        class="main-image"
      />
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
import { ref } from 'vue'

interface Props {
  src: string
  list?: string[]
}

const props = defineProps<Props>()

const currentSrc = ref(props.src)
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
  border-radius: 4px;
  cursor: zoom-in;
}
.preview-main :deep(.el-image__inner) {
  max-height: 400px;
  object-fit: contain;
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
  border-color: var(--el-color-primary);
}
.thumb-item.active {
  border-color: var(--el-color-primary);
}
.thumb-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
