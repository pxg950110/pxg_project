import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getPatientEncounterList, getEncounterDetail } from '@/api/cdr/patientEncounter'

/**
 * Patient Encounter Store
 * Manages patient encounter timeline and detail state
 */
export const usePatientEncounterStore = defineStore('patientEncounter', () => {
  // State
  const patientInfo = ref<any>(null)
  const encounters = ref<any[]>([])
  const currentEncounter = ref<any>(null)
  const currentEncounterId = ref<number | null>(null)
  const loading = ref(false)
  const total = ref(0)
  const encounterCache = ref<Map<number, any>>(new Map())

  // Getters
  const hasEncounters = computed(() => encounters.value.length > 0)

  const currentEncounterIndex = computed(() => {
    if (!currentEncounterId.value || !encounters.value.length) return -1
    return encounters.value.findIndex(e => e.id === currentEncounterId.value)
  })

  // Actions
  /**
   * Fetch patient encounters with pagination
   * @param patientId Patient ID
   * @param page Page number (default 1)
   * @param size Page size (default 10)
   */
  async function fetchPatientEncounters(patientId: number | string, page = 1, size = 10) {
    loading.value = true
    try {
      const res = await getPatientEncounterList(patientId, { page, size })
      const data = res.data.data

      // Extract patient info and encounter list from response
      // Backend returns PatientEncounterListDTO with patient info at top level and encounters array
      if (data) {
        // Patient info fields are at the top level of the response
        patientInfo.value = {
          patientId: data.patientId,
          patientName: data.patientName,
          gender: data.gender,
          age: data.age,
          patientNo: data.patientNo,
          idCard: data.idCard,
          phone: data.phone,
          allergyHistory: data.allergyHistory,
          familyHistory: data.familyHistory
        }
        encounters.value = data.encounters || []
        total.value = data.encounters?.length || 0
      }

      return data
    } finally {
      loading.value = false
    }
  }

  /**
   * Fetch encounter detail by ID
   * Uses cache if available to avoid redundant API calls
   * @param encounterId Encounter ID
   */
  async function fetchEncounterDetail(encounterId: number | string) {
    const id = Number(encounterId)

    // Check cache first
    if (encounterCache.value.has(id)) {
      currentEncounter.value = encounterCache.value.get(id)
      currentEncounterId.value = id
      return currentEncounter.value
    }

    loading.value = true
    try {
      const res = await getEncounterDetail(encounterId)
      const data = res.data.data

      if (data) {
        // Cache the encounter detail
        encounterCache.value.set(id, data)
        currentEncounter.value = data
        currentEncounterId.value = id
      }

      return data
    } finally {
      loading.value = false
    }
  }

  /**
   * Select an encounter from timeline
   * Automatically fetches detail if not cached
   * @param encounterId Encounter ID
   */
  async function selectEncounter(encounterId: number | string) {
    const id = Number(encounterId)
    currentEncounterId.value = id

    // If encounter is already cached, use it
    if (encounterCache.value.has(id)) {
      currentEncounter.value = encounterCache.value.get(id)
      return currentEncounter.value
    }

    // Otherwise fetch from API
    return await fetchEncounterDetail(id)
  }

  /**
   * Clear encounter detail cache
   */
  function clearCache() {
    encounterCache.value.clear()
  }

  /**
   * Reset store state
   */
  function $reset() {
    patientInfo.value = null
    encounters.value = []
    currentEncounter.value = null
    currentEncounterId.value = null
    loading.value = false
    total.value = 0
    encounterCache.value.clear()
  }

  return {
    // State
    patientInfo,
    encounters,
    currentEncounter,
    currentEncounterId,
    loading,
    total,
    encounterCache,

    // Getters
    hasEncounters,
    currentEncounterIndex,

    // Actions
    fetchPatientEncounters,
    fetchEncounterDetail,
    selectEncounter,
    clearCache,
    $reset
  }
})