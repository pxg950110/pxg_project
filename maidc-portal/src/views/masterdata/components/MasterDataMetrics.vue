<template>
  <div class="metrics-grid">
    <!-- Card 1: 概念域 -->
    <div class="metric-card" @click="emit('switch-tab', 'concept-domains')">
      <div class="metric-card-top">
        <div class="metric-icon-box concept">
          <AppstoreOutlined />
        </div>
        <div class="metric-badge">WS/T 303</div>
      </div>
      <div class="metric-info">
        <div class="metric-title">概念域 (Concept Domain)</div>
        <div class="metric-value-row">
          <span class="metric-num">{{ loading ? '-' : stats.conceptDomains }}</span>
          <span class="metric-unit">项</span>
        </div>
        <div class="metric-sub">
          <span class="sub-pill blue">可枚举 {{ stats.enumerableDomains }}</span>
          <span class="sub-pill green">不可枚举 {{ stats.nonEnumerableDomains }}</span>
        </div>
      </div>
      <div class="metric-action-row">
        <span class="action-link">查看明细 & 值含义 <ArrowRightOutlined /></span>
        <button class="mini-add-btn" @click.stop="emit('create', 'concept-domain')" title="新建概念域">
          <PlusOutlined /> 新建
        </button>
      </div>
    </div>

    <!-- Card 2: 值域 -->
    <div class="metric-card" @click="emit('switch-tab', 'value-domains')">
      <div class="metric-card-top">
        <div class="metric-icon-box value-domain">
          <DatabaseOutlined />
        </div>
        <div class="metric-badge">表示格式</div>
      </div>
      <div class="metric-info">
        <div class="metric-title">值域 (Value Domain)</div>
        <div class="metric-value-row">
          <span class="metric-num">{{ loading ? '-' : stats.valueDomains }}</span>
          <span class="metric-unit">个</span>
        </div>
        <div class="metric-sub">
          <span class="sub-text">绑定标准格式规则与允许值集合</span>
        </div>
      </div>
      <div class="metric-action-row">
        <span class="action-link">管理值域规则 <ArrowRightOutlined /></span>
        <button class="mini-add-btn" @click.stop="emit('create', 'value-domain')" title="新建值域">
          <PlusOutlined /> 新建
        </button>
      </div>
    </div>

    <!-- Card 3: 数据元概念 -->
    <div class="metric-card" @click="emit('switch-tab', 'data-element-concepts')">
      <div class="metric-card-top">
        <div class="metric-icon-box dec">
          <BranchesOutlined />
        </div>
        <div class="metric-badge">OC + Property</div>
      </div>
      <div class="metric-info">
        <div class="metric-title">数据元概念 (DEC)</div>
        <div class="metric-value-row">
          <span class="metric-num">{{ loading ? '-' : stats.decCount }}</span>
          <span class="metric-unit">条</span>
        </div>
        <div class="metric-sub">
          <span class="sub-text">对象类与特性的语义组合</span>
        </div>
      </div>
      <div class="metric-action-row">
        <span class="action-link">查看语义血缘 <ArrowRightOutlined /></span>
        <button class="mini-add-btn" @click.stop="emit('create', 'data-element-concept')" title="新建数据元概念">
          <PlusOutlined /> 新建
        </button>
      </div>
    </div>

    <!-- Card 4: 常用临床字典 -->
    <div class="metric-card" @click="emit('switch-tab', 'dictionaries')">
      <div class="metric-card-top">
        <div class="metric-icon-box dict">
          <MedicineBoxOutlined />
        </div>
        <div class="metric-badge">5大临床字典</div>
      </div>
      <div class="metric-info">
        <div class="metric-title">常用临床字典</div>
        <div class="metric-value-row">
          <span class="metric-num">{{ loading ? '-' : stats.totalDictCount }}</span>
          <span class="metric-unit">收录项</span>
        </div>
        <div class="metric-sub">
          <span class="sub-pill purple">药品 {{ stats.drugCount }}</span>
          <span class="sub-pill blue">诊断 {{ stats.diagCount }}</span>
          <span class="sub-pill green">检验检查 {{ stats.labExamCount }}</span>
        </div>
      </div>
      <div class="metric-action-row">
        <span class="action-link">进入字典维护工作台 <ArrowRightOutlined /></span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  AppstoreOutlined,
  DatabaseOutlined,
  BranchesOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  ArrowRightOutlined,
} from '@ant-design/icons-vue'
import {
  conceptDomainApi,
  valueDomainApi,
  dataElementConceptApi,
} from '@/api/dataElementStandard'
import {
  getDrugs,
  getDiagnoses,
  getLabItems,
  getExamItems,
  getFeeItems,
} from '@/api/medical-dictionary'

const emit = defineEmits<{
  (e: 'switch-tab', tabKey: string): void
  (e: 'create', type: 'concept-domain' | 'value-domain' | 'data-element-concept'): void
}>()

const loading = ref(false)
const stats = reactive({
  conceptDomains: 0,
  enumerableDomains: 0,
  nonEnumerableDomains: 0,
  valueDomains: 0,
  decCount: 0,
  drugCount: 0,
  diagCount: 0,
  labExamCount: 0,
  totalDictCount: 0,
})

const fetchStats = async () => {
  loading.value = true
  try {
    const [cdRes, vdRes, decRes] = await Promise.allSettled([
      conceptDomainApi.list({ page: 1, page_size: 100 }),
      valueDomainApi.list({ page: 1, page_size: 1 }),
      dataElementConceptApi.list({ page: 1, page_size: 1 }),
    ])

    if (cdRes.status === 'fulfilled') {
      const data = (cdRes.value as any)?.data?.data || (cdRes.value as any)?.data || {}
      const list = Array.isArray(data) ? data : (data.content || [])
      const total = data.totalElements ?? list.length
      stats.conceptDomains = total
      stats.enumerableDomains = list.filter((i: any) => i.domainType === 'ENUMERABLE').length
      stats.nonEnumerableDomains = total - stats.enumerableDomains
    }

    if (vdRes.status === 'fulfilled') {
      const data = (vdRes.value as any)?.data?.data || (vdRes.value as any)?.data || {}
      stats.valueDomains = data.totalElements ?? (Array.isArray(data) ? data.length : 0)
    }

    if (decRes.status === 'fulfilled') {
      const data = (decRes.value as any)?.data?.data || (decRes.value as any)?.data || {}
      stats.decCount = data.totalElements ?? (Array.isArray(data) ? data.length : 0)
    }

    // 临床字典统计
    const [drugRes, diagRes, labRes, examRes, feeRes] = await Promise.allSettled([
      getDrugs({ page: 1, page_size: 1 }),
      getDiagnoses({ page: 1, page_size: 1 }),
      getLabItems({ page: 1, page_size: 1 }),
      getExamItems({ page: 1, page_size: 1 }),
      getFeeItems({ page: 1, page_size: 1 }),
    ])

    const getCount = (res: PromiseSettledResult<any>) => {
      if (res.status === 'fulfilled') {
        const d = res.value?.data?.data || res.value?.data || {}
        return d.total ?? d.totalElements ?? (Array.isArray(d) ? d.length : 0)
      }
      return 0
    }

    stats.drugCount = getCount(drugRes)
    stats.diagCount = getCount(diagRes)
    const labCount = getCount(labRes)
    const examCount = getCount(examRes)
    const feeCount = getCount(feeRes)
    stats.labExamCount = labCount + examCount
    stats.totalDictCount = stats.drugCount + stats.diagCount + labCount + examCount + feeCount
  } catch (err) {
    console.warn('[MasterDataMetrics] fetch error:', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchStats()
})

defineExpose({
  refresh: fetchStats,
})
</script>

<style lang="scss" scoped>
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;

  @media (max-width: 1400px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.metric-card {
  padding: 16px 18px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  min-height: 146px;

  &:hover {
    transform: translateY(-2px);
    border-color: #91caff;
    .action-link {
      color: #1677ff;
    }
  }
}

.metric-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.metric-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;

  &.concept {
    background: #e6f4ff;
    color: #1677ff;
    border: 1px solid #91caff;
  }

  &.value-domain {
    background: rgba(56, 189, 248, 0.1);
    color: #38bdf8;
    border: 1px solid rgba(56, 189, 248, 0.3);
  }

  &.dec {
    background: rgba(129, 140, 248, 0.1);
    color: #818cf8;
    border: 1px solid rgba(129, 140, 248, 0.3);
  }

  &.dict {
    background: #f6ffed;
    color: #52c41a;
    border: 1px solid #b7eb8f;
  }
}

.metric-badge {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 999px;
  background: #f5f5f5;
  color: rgba(0, 0, 0, 0.45);
  border: 1px solid #f0f0f0;
}

.metric-title {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  font-weight: 500;
  margin-bottom: 4px;
}

.metric-value-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 8px;

  .metric-num {
    font-size: 26px;
    font-weight: 700;
    color: rgba(0, 0, 0, 0.88);
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    letter-spacing: -0.02em;
  }

  .metric-unit {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
  }
}

.metric-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  min-height: 22px;

  .sub-text {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
  }

  .sub-pill {
    font-size: 11px;
    padding: 1px 6px;
    border-radius: 4px;

    &.blue {
      background: rgba(56, 189, 248, 0.12);
      color: #38bdf8;
    }

    &.green {
      background: #f6ffed;
      color: #52c41a;
    }

    &.purple {
      background: rgba(129, 140, 248, 0.1);
      color: #818cf8;
    }
  }
}

.metric-action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;

  .action-link {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
    display: inline-flex;
    align-items: center;
    gap: 4px;
    transition: color 0.2s;
  }

  .mini-add-btn {
    background: #e6f4ff;
    border: 1px solid #91caff;
    color: #1677ff;
    font-size: 11px;
    font-weight: 500;
    padding: 2px 8px;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #1677ff;
      color: #fff;
    }
  }
}
</style>
