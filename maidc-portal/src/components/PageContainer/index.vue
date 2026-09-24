<template>
  <div class="page-container">
    <div v-if="breadcrumb && breadcrumb.length" class="page-breadcrumb">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item v-for="(item, idx) in breadcrumb" :key="idx" :to="item.path">
          {{ item.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div v-if="title" class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">{{ title }}</h2>
        <span v-if="subtitle" class="page-subtitle">{{ subtitle }}</span>
      </div>
      <div class="page-header-extra">
        <slot name="extra" />
      </div>
    </div>
    <div v-loading="loading ?? false" class="page-content">
      <slot />
    </div>
    <div v-if="$slots.footer" class="page-footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
interface BreadcrumbItem {
  title: string
  path?: string
}

interface Props {
  title?: string
  subtitle?: string
  breadcrumb?: BreadcrumbItem[]
  loading?: boolean
}

defineProps<Props>()
</script>

<style scoped>
.page-container {
  background: var(--el-bg-color);
  border-radius: 10px;
  padding: 24px;
  min-height: 100%;
}
.page-breadcrumb {
  margin-bottom: 16px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0;
}
.page-subtitle {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}
.page-content {
  min-height: 200px;
}
.page-footer {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  text-align: right;
}
</style>
