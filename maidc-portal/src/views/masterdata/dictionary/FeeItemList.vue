<template>
  <DictionaryListView type="fee" :embedded="embedded" />
</template>

<script setup lang="ts">
import DictionaryListView from './DictionaryListView.vue'

defineOptions({ name: 'FeeItemList' })

withDefaults(defineProps<{ embedded?: boolean }>(), { embedded: false })
</script>
