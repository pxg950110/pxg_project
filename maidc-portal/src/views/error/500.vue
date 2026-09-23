<template>
  <div class="flex min-h-screen items-center justify-center bg-slate-50">
    <el-result icon="error" title="500" sub-title="抱歉，服务器出现了错误。">
      <template #extra>
        <el-button type="primary" @click="router.push('/')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>
