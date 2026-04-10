<template>
  <div class="desensitize-preview">
    <div class="preview-pane">
      <div class="pane-header">
        <FileTextOutlined />
        <span>原始数据</span>
      </div>
      <div class="pane-content original">
        <pre>{{ original }}</pre>
      </div>
    </div>
    <div class="preview-divider">
      <SwapOutlined />
    </div>
    <div class="preview-pane">
      <div class="pane-header safe">
        <SafetyOutlined />
        <span>脱敏数据</span>
      </div>
      <div class="pane-content desensitized">
        <pre>{{ desensitized }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { FileTextOutlined, SafetyOutlined, SwapOutlined } from '@ant-design/icons-vue'

interface Props {
  original: string
  desensitized: string
}

defineProps<Props>()
</script>

<style scoped>
.desensitize-preview {
  display: flex;
  gap: 0;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  overflow: hidden;
}
.preview-pane {
  flex: 1;
  min-width: 0;
}
.pane-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.65);
}
.pane-header.safe {
  background: #f6ffed;
  color: #52c41a;
}
.pane-content {
  padding: 12px 16px;
  max-height: 400px;
  overflow: auto;
}
.pane-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
}
.original {
  background: #fff;
}
.desensitized {
  background: #f6ffed;
}
.preview-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  background: #fafafa;
  border-left: 1px solid #f0f0f0;
  border-right: 1px solid #f0f0f0;
  color: rgba(0, 0, 0, 0.25);
}
</style>
