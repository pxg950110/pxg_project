<template>
  <PageContainer title="权限配置" :loading="loading">
    <template #extra>
      <a-button @click="router.back()">返回</a-button>
      <a-button type="primary" @click="handleSave" :loading="saving">保存</a-button>
    </template>

    <a-card>
      <a-tree
        v-model:checkedKeys="checkedKeys"
        :tree-data="permissionTree"
        :field-names="{ title: 'name', key: 'id', children: 'children' }"
        checkable
        default-expand-all
      />
    </a-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { getPermissionTree, assignPermissions } from '@/api/system'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const permissionTree = ref<any[]>([])
const checkedKeys = ref<number[]>([])

async function loadData() {
  loading.value = true
  try {
    const [treeRes, roleRes] = await Promise.all([
      getPermissionTree(),
      request.get(`/roles/${route.params.id}`),
    ])
    permissionTree.value = treeRes.data.data
    checkedKeys.value = roleRes.data.data.permission_ids || []
  } finally { loading.value = false }
}

async function handleSave() {
  saving.value = true
  try {
    await assignPermissions(Number(route.params.id), checkedKeys.value)
    message.success('权限保存成功')
  } finally { saving.value = false }
}

onMounted(loadData)
</script>
