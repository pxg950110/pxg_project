<template>
  <div class="kv-editor">
    <div v-for="(item, idx) in rows" :key="idx" class="kv-row">
      <a-input
        v-model:value="item.key"
        placeholder="键"
        class="kv-input"
        @change="handleChange"
      />
      <span class="kv-separator">:</span>
      <a-input
        v-model:value="item.value"
        placeholder="值"
        class="kv-input"
        @change="handleChange"
      />
      <a-button
        type="text"
        danger
        :disabled="rows.length <= 1"
        @click="removeRow(idx)"
      >
        <DeleteOutlined />
      </a-button>
    </div>
    <a-button type="dashed" block @click="addRow">
      <PlusOutlined />
      添加
    </a-button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'

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
  color: rgba(0, 0, 0, 0.45);
  flex-shrink: 0;
}
</style>
