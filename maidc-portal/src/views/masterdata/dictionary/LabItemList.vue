<template>
  <DictionaryListView type="lab" :embedded="embedded" />
</template>

<script setup lang="ts">
import DictionaryListView from './DictionaryListView.vue'

defineOptions({ name: 'LabItemList' })

withDefaults(defineProps<{ embedded?: boolean }>(), { embedded: false })
</script>
