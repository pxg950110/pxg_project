<template>
  <DictionaryListView type="drug" :embedded="embedded" />
</template>

<script setup lang="ts">
import DictionaryListView from './DictionaryListView.vue'

defineOptions({ name: 'DrugList' })

withDefaults(defineProps<{ embedded?: boolean }>(), { embedded: false })
</script>
