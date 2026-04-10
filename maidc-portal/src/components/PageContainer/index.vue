<template>
  <div class="page-container">
    <div v-if="breadcrumb && breadcrumb.length" class="page-breadcrumb">
      <a-breadcrumb>
        <a-breadcrumb-item v-for="(item, idx) in breadcrumb" :key="idx">
          <router-link v-if="item.path" :to="item.path">{{ item.title }}</router-link>
          <span v-else>{{ item.title }}</span>
        </a-breadcrumb-item>
      </a-breadcrumb>
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
    <a-spin :spinning="loading ?? false">
      <div class="page-content">
        <slot />
      </div>
    </a-spin>
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
  background: #fff;
  border-radius: 8px;
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
  color: rgba(0, 0, 0, 0.88);
  margin: 0;
}
.page-subtitle {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
}
.page-content {
  min-height: 200px;
}
.page-footer {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  text-align: right;
}
</style>
