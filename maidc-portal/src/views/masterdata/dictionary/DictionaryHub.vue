<template>
  <div class="dict-hub-container">
    <!-- Sub-navigation tabs -->
    <div class="dict-nav-bar tech-card">
      <div class="dict-tab-group">
        <button
          v-for="tab in dictTabs"
          :key="tab.key"
          class="dict-tab-btn"
          :class="{ active: currentTab === tab.key }"
          @click="currentTab = tab.key"
        >
          <component :is="tab.icon" class="tab-icon" />
          <span>{{ tab.label }}</span>
        </button>
      </div>
    </div>

    <!-- Sub-views -->
    <div class="dict-view-body">
      <DrugList v-if="currentTab === 'drugs'" :embedded="true" />
      <DiagnosisList v-else-if="currentTab === 'diagnoses'" :embedded="true" />
      <LabItemList v-else-if="currentTab === 'labs'" :embedded="true" />
      <ExamItemList v-else-if="currentTab === 'exams'" :embedded="true" />
      <FeeItemList v-else-if="currentTab === 'fees'" :embedded="true" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, markRaw } from 'vue'
import {
  MedicineBoxOutlined,
  HeartOutlined,
  ExperimentOutlined,
  ScanOutlined,
  DollarOutlined,
} from '@ant-design/icons-vue'
import DrugList from './DrugList.vue'
import DiagnosisList from './DiagnosisList.vue'
import LabItemList from './LabItemList.vue'
import ExamItemList from './ExamItemList.vue'
import FeeItemList from './FeeItemList.vue'

const currentTab = ref<string>('drugs')

const dictTabs = [
  { key: 'drugs', label: '药品字典 (Drugs)', icon: markRaw(MedicineBoxOutlined) },
  { key: 'diagnoses', label: '诊断字典 (ICD-10)', icon: markRaw(HeartOutlined) },
  { key: 'labs', label: '检验项目 (Labs)', icon: markRaw(ExperimentOutlined) },
  { key: 'exams', label: '检查项目 (Exams)', icon: markRaw(ScanOutlined) },
  { key: 'fees', label: '收费项目 (Fee Items)', icon: markRaw(DollarOutlined) },
]
</script>

<style lang="scss" scoped>
.dict-hub-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
}

.dict-nav-bar {
  padding: 8px 12px;
  border-radius: 12px;
  display: flex;
  align-items: center;
}

.dict-tab-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.dict-tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(15, 26, 46, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.16);
  color: var(--tech-text-muted);
  font-size: 13px;
  font-weight: 500;
  padding: 6px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;

  .tab-icon {
    font-size: 14px;
  }

  &:hover {
    background: rgba(34, 211, 238, 0.08);
    border-color: rgba(34, 211, 238, 0.3);
    color: var(--tech-primary);
  }

  &.active {
    background: rgba(34, 211, 238, 0.16);
    border-color: var(--tech-primary);
    color: var(--tech-primary);
    font-weight: 600;
    box-shadow: 0 0 12px rgba(34, 211, 238, 0.2);
  }
}

.dict-view-body {
  flex: 1;
  min-height: 0;
}
</style>
