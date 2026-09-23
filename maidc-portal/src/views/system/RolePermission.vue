<template>
  <PageContainer title="权限配置" :loading="loading">
    <template #extra>
      <el-button @click="router.back()">返回</el-button>
      <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
    </template>

    <el-card shadow="never">
      <el-tree
        ref="treeRef"
        :data="permissionTree"
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        show-checkbox
        default-expand-all
        :default-checked-keys="checkedKeys"
        @check="syncCheckedKeys"
      />
    </el-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import { getPermissionTree, assignPermissions } from '@/api/system'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const permissionTree = ref<any[]>([])
const checkedKeys = ref<number[]>([])
const treeRef = ref()

function syncCheckedKeys() {
  checkedKeys.value = (treeRef.value?.getCheckedKeys(false) || []) as number[]
}

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
    ElMessage.success('权限保存成功')
  } finally { saving.value = false }
}

onMounted(loadData)
</script>
