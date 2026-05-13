<template>
  <div v-loading="loading" class="patient-encounter-360">
    <!-- Patient Info Card - Fixed at top -->
    <PatientInfoCard
      v-if="patientInfo"
      :patient-info="patientInfo"
    />

    <!-- Main Content Area - Flex Layout -->
    <div v-if="hasEncounters" class="main-content">
      <!-- Left: Encounter Timeline -->
      <EncounterTimeline
        :encounters="encounters"
        :current-encounter-id="currentEncounterId"
        @select="handleSelectEncounter"
      />

      <!-- Right: Encounter Detail -->
      <EncounterDetail :encounter-detail="currentEncounter" />
    </div>

    <!-- Empty State -->
    <div v-else-if="!loading" class="empty-state">
      <div class="empty-icon">📋</div>
      <div class="empty-text">暂无就诊记录</div>
      <div class="empty-hint">该患者暂无就诊记录数据</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { usePatientEncounterStore } from '@/stores/cdr/patientEncounter'
import PatientInfoCard from './components/PatientInfoCard.vue'
import EncounterTimeline from './components/EncounterTimeline.vue'
import EncounterDetail from './components/EncounterDetail.vue'

// Use Pinia store
const store = usePatientEncounterStore()

// Get route params
const route = useRoute()

// Computed properties from store
const loading = computed(() => store.loading)
const patientInfo = computed(() => store.patientInfo)
const encounters = computed(() => store.encounters)
const currentEncounter = computed(() => store.currentEncounter)
const currentEncounterId = computed(() => store.currentEncounterId)
const hasEncounters = computed(() => store.hasEncounters)

// Get patientId from route params
const patientId = computed(() => {
  const id = route.params.patientId
  return id ? Number(id) : null
})

// Handle encounter selection from timeline
const handleSelectEncounter = async (encounterId: number) => {
  await store.selectEncounter(encounterId)
}

// Fetch patient encounters on mount
onMounted(async () => {
  if (patientId.value) {
    await store.fetchPatientEncounters(patientId.value)

    // Auto-select first encounter if available
    if (store.encounters.length > 0) {
      const firstEncounter = store.encounters[0]
      await store.selectEncounter(firstEncounter.encounterId)
    }
  }
})

// Watch for patientId changes (in case route changes)
watch(patientId, async (newId) => {
  if (newId) {
    store.$reset()
    await store.fetchPatientEncounters(newId)

    // Auto-select first encounter if available
    if (store.encounters.length > 0) {
      const firstEncounter = store.encounters[0]
      await store.selectEncounter(firstEncounter.encounterId)
    }
  }
})
</script>

<style scoped>
.patient-encounter-360 {
  height: calc(100vh - 60px); /* Full page height minus header */
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  overflow: hidden;
}

.main-content {
  flex: 1;
  display: flex;
  gap: 20px;
  padding: 0 20px 20px 20px;
  overflow: hidden;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.empty-icon {
  font-size: 64px;
  opacity: 0.5;
}

.empty-text {
  font-size: 18px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.65);
}

.empty-hint {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
