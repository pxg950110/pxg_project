<template>
  <PageContainer>
    <!-- Header Row -->
    <div class="detail-header">
      <div class="detail-header-left">
        <span class="detail-title">组织详情</span>
        <router-link to="/system/organizations" class="back-link">← 返回列表</router-link>
      </div>
    </div>

    <!-- Info Card -->
    <div class="info-card">
      <div class="info-card-title-row">
        <span class="info-card-name">北京协和医院</span>
        <a-tag color="success">已连接</a-tag>
      </div>
      <div class="info-row">编码: BJXH-001 &nbsp;|&nbsp; 类型: 三级甲等 &nbsp;|&nbsp; 地区: 北京市东城区</div>
      <div class="info-row">HIS: 卫宁健康 &nbsp;|&nbsp; PACS: 锐珂医疗 &nbsp;|&nbsp; 床位: 2000</div>
      <div class="info-row">地址: 北京市东城区帅府园1号 &nbsp;|&nbsp; 电话: 010-69156699</div>
    </div>

    <!-- Tab Bar -->
    <a-tabs v-model:activeKey="activeTab" class="org-tabs">
      <a-tab-pane key="departments" tab="关联科室" />
      <a-tab-pane key="datasource" tab="数据源配置" />
      <a-tab-pane key="users" tab="系统用户" />
    </a-tabs>

    <!-- Tab 1: 关联科室 -->
    <div v-if="activeTab === 'departments'" class="tab-content">
      <div class="tab-content-header">
        <span class="tab-content-title">科室列表 (共 28 个)</span>
        <a-button type="primary">
          <PlusOutlined /> 添加科室
        </a-button>
      </div>
      <a-table
        :columns="deptColumns"
        :data-source="deptData"
        :pagination="false"
        row-key="code"
        size="middle"
        class="dept-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'datasource'">
            <span class="blue-text">{{ record.datasource }}</span>
          </template>
          <template v-if="column.dataIndex === 'action'">
            <a class="blue-text">编辑</a>
          </template>
        </template>
      </a-table>
    </div>

    <!-- Tab 2: 数据源配置 -->
    <div v-if="activeTab === 'datasource'" class="tab-content">
      <a-empty description="暂无数据源配置" />
    </div>

    <!-- Tab 3: 系统用户 -->
    <div v-if="activeTab === 'users'" class="tab-content">
      <a-empty description="暂无系统用户" />
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'

const activeTab = ref('departments')

const deptColumns = [
  { title: '科室名称', dataIndex: 'name' },
  { title: '科室编码', dataIndex: 'code' },
  { title: '负责人', dataIndex: 'leader' },
  { title: '医生数', dataIndex: 'doctorCount' },
  { title: '数据源', dataIndex: 'datasource' },
  { title: '操作', dataIndex: 'action', width: 60 },
]

const deptData = ref([
  { name: '放射科', code: 'DEPT-FS-001', leader: '张主任', doctorCount: 45, datasource: 'HIS/PACS' },
  { name: '心内科', code: 'DEPT-XN-001', leader: '李主任', doctorCount: 32, datasource: 'HIS/LIS' },
  { name: '呼吸内科', code: 'DEPT-HX-001', leader: '王主任', doctorCount: 28, datasource: 'HIS' },
  { name: '病理科', code: 'DEPT-BL-001', leader: '赵主任', doctorCount: 15, datasource: 'HIS/PACS' },
])
</script>

<style scoped>
/* Header */
.detail-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.detail-header-left {
  display: flex;
  align-items: baseline;
  gap: 16px;
}

.detail-title {
  font-size: 20px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.back-link {
  font-size: 14px;
  color: #1677ff;
  text-decoration: none;
  cursor: pointer;
}

.back-link:hover {
  color: #4096ff;
}

/* Info Card */
.info-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.info-card-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.info-card-name {
  font-size: 18px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.info-row {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  line-height: 1;
}

.info-row + .info-row {
  margin-top: 12px;
}

/* Tabs */
.org-tabs {
  margin-bottom: 0;
}

.tab-content {
  padding-top: 16px;
}

.tab-content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.tab-content-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

/* Table */
.dept-table {
  margin-top: 0;
}

.blue-text {
  color: #1677ff;
  cursor: pointer;
}

.blue-text:hover {
  color: #4096ff;
}
</style>
