<template>
  <DictionaryListView type="diagnosis" :embedded="embedded" />
</template>

<script setup lang="ts">
import DictionaryListView from './DictionaryListView.vue'

defineOptions({ name: 'DiagnosisList' })

withDefaults(defineProps<{ embedded?: boolean }>(), { embedded: false })
</script>
