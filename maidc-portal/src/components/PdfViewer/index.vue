<template>
  <div class="pdf-viewer">
    <iframe
      :src="pdfUrl"
      class="pdf-iframe"
      frameborder="0"
      allowfullscreen
    />
    <div v-if="!url" class="pdf-empty">
      <FilePdfOutlined style="font-size: 48px; color: #d9d9d9" />
      <p>未指定PDF文件</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { FilePdfOutlined } from '@ant-design/icons-vue'

interface Props {
  url: string
}

const props = defineProps<Props>()

const pdfUrl = computed(() => {
  if (!props.url) return ''
  // Use the browser's built-in PDF viewer via Google Docs viewer or direct embed
  return props.url
})
</script>

<style scoped>
.pdf-viewer {
  width: 100%;
  min-height: 500px;
  position: relative;
}
.pdf-iframe {
  width: 100%;
  height: 100%;
  min-height: 600px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
}
.pdf-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: rgba(0, 0, 0, 0.25);
}
</style>
