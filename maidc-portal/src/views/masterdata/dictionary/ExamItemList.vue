<template>
  <DictionaryListView type="exam" :embedded="embedded" />
</template>

<script setup lang="ts">
import DictionaryListView from './DictionaryListView.vue'

defineOptions({ name: 'ExamItemList' })

withDefaults(defineProps<{ embedded?: boolean }>(), { embedded: false })
</script>
