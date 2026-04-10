<template>
  <a-breadcrumb class="breadcrumb-nav">
    <a-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
      <router-link v-if="item.to" :to="item.to">{{ item.title }}</router-link>
      <span v-else>{{ item.title }}</span>
    </a-breadcrumb-item>
  </a-breadcrumb>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const breadcrumbs = computed(() => {
  const matched = route.matched.filter(r => r.meta?.title)
  return matched.map(r => ({
    path: r.path,
    title: r.meta.title as string,
    to: r.redirect ? undefined : { path: r.path },
  }))
})
</script>

<style scoped>
.breadcrumb-nav {
  font-size: 13px;
}
</style>
