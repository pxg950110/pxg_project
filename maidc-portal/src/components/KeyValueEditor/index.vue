<template>
  <div class="kv-editor">
    <div v-for="(item, idx) in rows" :key="idx" class="kv-row">
      <el-input
        v-model="item.key"
        placeholder="键"
        class="kv-input"
        @input="handleChange"
      />
      <span class="kv-separator">:</span>
      <el-input
        v-model="item.value"
        placeholder="值"
        class="kv-input"
        @input="handleChange"
      />
      <el-button
        type="danger"
        link
        :disabled="rows.length <= 1"
        @click="removeRow(idx)"
      >
        <el-icon><Delete /></el-icon>
      </el-button>
    </div>
    <el-button class="w-full !border-dashed mt-1" @click="addRow">
      <el-icon class="mr-1"><Plus /></el-icon>
      添加
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Delete, Plus } from '@element-plus/icons-vue'

interface KVPair {
  key: string
  value: string
}

interface Props {
  modelValue: KVPair[]
}

interface Emits {
  (e: 'update:modelValue', value: KVPair[]): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const rows = ref<KVPair[]>(props.modelValue?.length ? props.modelValue.map((kv) => ({ ...kv })) : [{ key: '', value: '' }])

watch(
  () => props.modelValue,
  (val) => {
    rows.value = val?.length ? val.map((kv) => ({ ...kv })) : [{ key: '', value: '' }]
  },
  { deep: true },
)

function addRow() {
  rows.value.push({ key: '', value: '' })
}

function removeRow(idx: number) {
  rows.value.splice(idx, 1)
  handleChange()
}

function handleChange() {
  emit('update:modelValue', rows.value.map((kv) => ({ ...kv })))
}
</script>

<style scoped>
.kv-editor {
  width: 100%;
}
.kv-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.kv-input {
  flex: 1;
}
.kv-separator {
  color: #94a3b8;
  flex-shrink: 0;
}
</style>
