# 患者就诊360视图设计规范

> **版本**: v1.0
> **日期**: 2026-05-12
> **状态**: 设计评审通过

---

## 1. 概述

### 1.1 目标

构建以就诊为维度的患者360视图,整合患者基本信息、就诊记录、诊断、检验、影像、用药、手术等多维度临床数据,提供时间轴导航和快速定位功能。

### 1.2 用户角色

- **临床医生**: 日常诊疗中快速了解患者病史,辅助诊断决策
- **科研人员**: 研究队列筛选、病例分析、特征提取
- **医院管理者**: 医疗质量监控、资源调配、绩效分析

### 1.3 设计原则

- **以就诊为核心**: 时间轴展示就诊历史,每次就诊展示完整数据
- **按需加载**: 默认展示最近一次就诊,历史数据按需加载
- **快速导航**: Tab导航快速定位到数据模块
- **时间轴增强**: 每个就诊节点显示主诊断信息

---

## 2. 页面布局设计

### 2.1 整体布局

```
┌─────────────────────────────────────────────────────────┐
│ 顶部导航栏 (系统通用)                                      │
├─────────────────────────────────────────────────────────┤
│ 患者基本信息卡片 (固定高度)                                │
├─────────────────────────────────────────────────────────┤
│ 主内容区域 (flex布局)                                      │
│ ┌──────────────┬────────────────────────────────────┐   │
│ │ 左侧时间轴    │ 右侧就诊详情                        │   │
│ │ (宽度 320px) │ ┌────────────────────────────────┐ │   │
│ │              │ │ Tab导航 (固定在顶部)             │ │   │
│ │              │ ├────────────────────────────────┤ │   │
│ │              │ │ 就诊基本信息                    │ │   │
│ │              │ │ 诊断信息 (折叠面板)             │ │   │
│ │              │ │ 检验结果 (折叠面板)             │ │   │
│ │              │ │ 影像检查 (折叠面板)             │ │   │
│ │              │ │ 用药记录 (折叠面板)             │ │   │
│ │              │ │ 手术记录 (折叠面板)             │ │   │
│ │              │ └────────────────────────────────┘ │   │
│ └──────────────┴────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 2.2 路由设计

- **患者就诊列表**: `/cdr/patient/:patientId/encounters`
- **就诊详情页**: `/cdr/patient/:patientId/encounter/:encounterId`

---

## 3. 数据模型设计

### 3.1 核心实体

基于现有CDR表结构:

- **c_patient**: 患者基本信息
- **c_encounter**: 就诊记录
- **c_diagnosis**: 诊断记录
- **c_lab_test**: 检验申请
- **c_lab_panel**: 检验结果明细
- **c_imaging_exam**: 影像检查
- **c_medication**: 用药记录
- **c_operation**: 手术记录

### 3.2 DTO设计

#### PatientEncounterListDTO

```java
public class PatientEncounterListDTO {
    private Long patientId;
    private String patientName;
    private String gender;
    private Integer age;
    private String patientNo;
    private String idCard; // 脱敏
    private String phone; // 脱敏
    private String allergyHistory;
    private String familyHistory;
    private List<EncounterTimelineDTO> encounters;
}
```

#### EncounterTimelineDTO

```java
public class EncounterTimelineDTO {
    private Long encounterId;
    private String encounterNo;
    private String encounterType; // OUTPATIENT/INPATIENT/EMERGENCY
    private String deptCode;
    private String deptName;
    private LocalDateTime admitTime;
    private LocalDateTime dischargeTime;
    private String mainDiagnosis; // 主诊断
    private String status; // ACTIVE/DISCHARGED
    private Boolean isCurrent; // 是否当前就诊
}
```

#### EncounterDetailDTO

```java
public class EncounterDetailDTO {
    private EncounterBasicInfo basicInfo;
    private List<DiagnosisDTO> diagnoses;
    private List<LabTestDTO> labTests;
    private List<ImagingExamDTO> imagingExams;
    private List<MedicationDTO> medications;
    private List<OperationDTO> operations;
}
```

---

## 4. API接口设计

### 4.1 患者就诊列表

```
GET /api/cdr/patients/{patientId}/encounters
```

**请求参数**:
- `patientId`: 患者ID (必填)
- `page`: 页码 (默认1)
- `size`: 每页大小 (默认10)

**响应**:
```json
{
  "code": 200,
  "data": {
    "patientInfo": {
      "patientId": 12345,
      "patientName": "张三",
      "gender": "男",
      "age": 65,
      "patientNo": "12345",
      "allergyHistory": "青霉素"
    },
    "encounters": [
      {
        "encounterId": 1001,
        "encounterNo": "ENC20260510001",
        "encounterType": "INPATIENT",
        "deptName": "心内科",
        "admitTime": "2026-05-10T09:00:00",
        "mainDiagnosis": "高血压III级",
        "status": "ACTIVE",
        "isCurrent": true
      }
    ],
    "total": 15,
    "page": 1,
    "size": 10
  }
}
```

### 4.2 就诊详情

```
GET /api/cdr/encounters/{encounterId}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "basicInfo": {
      "encounterId": 1001,
      "encounterType": "INPATIENT",
      "deptName": "心内科",
      "doctorName": "李医生",
      "admitTime": "2026-05-10T09:00:00",
      "bedNo": "12床",
      "wardName": "心内科一病区",
      "mainDiagnosis": "高血压III级",
      "status": "ACTIVE"
    },
    "diagnoses": [...],
    "labTests": [...],
    "imagingExams": [...],
    "medications": [...],
    "operations": [...]
  }
}
```

---

## 5. 前端组件设计

### 5.1 组件结构

```
src/views/cdr/patient/
├── PatientEncounter360.vue          # 主页面
├── components/
│   ├── PatientInfoCard.vue          # 患者信息卡片
│   ├── EncounterTimeline.vue        # 就诊时间轴
│   ├── EncounterDetail.vue          # 就诊详情
│   ├── DiagnosisSection.vue         # 诊断信息模块
│   ├── LabTestSection.vue           # 检验结果模块
│   ├── ImagingSection.vue           # 影像检查模块
│   ├── MedicationSection.vue        # 用药记录模块
│   └── OperationSection.vue         # 手术记录模块
└── api/
    └── patientEncounter.js          # API接口
```

### 5.2 关键交互

#### 时间轴交互
- 点击时间轴节点,加载对应就诊详情
- 默认选中最近一次就诊(状态为ACTIVE或最近时间)
- 滚动加载: 滚动到底部时加载更早的就诊记录

#### Tab导航交互
- Tab导航固定在右侧顶部(position: sticky)
- 点击Tab: 平滑滚动到对应模块
- 滚动内容: Tab自动切换高亮状态

---

## 6. 性能优化策略

### 6.1 按需加载

- **时间轴**: 初始加载最近N次就诊(默认10次)
- **就诊详情**: 点击时间轴节点时才加载完整详情
- **滚动加载**: 时间轴滚动到底部时加载更早的就诊记录

### 6.2 数据缓存

- 已加载的就诊详情缓存到前端状态管理(Pinia)
- 切换时间轴节点时,优先从缓存读取

### 6.3 分页查询

- 检验结果、影像检查等数据量大的模块,支持分页加载
- 默认展示最近10条记录,点击"加载更多"获取历史数据

---

## 7. 技术栈

### 7.1 后端

- **框架**: Spring Boot 3.x
- **数据库**: PostgreSQL
- **ORM**: Spring Data JPA
- **API文档**: Swagger/OpenAPI

### 7.2 前端

- **框架**: Vue 3 + Composition API
- **UI组件库**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router
- **HTTP客户端**: Axios
- **图表库**: ECharts (用于检验结果趋势图)

---

## 8. 安全与合规

### 8.1 数据脱敏

- 患者姓名、身份证号、电话等敏感字段脱敏展示
- 脱敏规则:
  - 姓名: 张** (保留姓,其余用*替代)
  - 身份证: 320***********1234 (保留前3位和后4位)
  - 电话: 138****5678 (保留前3位和后4位)

### 8.2 权限控制

- 基于角色的访问控制(RBAC)
- 不同角色看到不同数据范围(后续迭代)

---

## 9. 后续迭代规划

### Phase 2: 权限控制
- 不同角色看到不同数据范围
- 医生看完整数据,科研人员看脱敏数据

### Phase 3: 高级功能
- 检验结果趋势图表
- 影像查看器集成(PACS)
- 智能提醒(过敏史、药物相互作用)

---

## 10. 参考资料

- [MAIDC设计文档](../archive/specs/2026-04-08-maidc-design.md)
- [CDR数据模型](../archive/specs/2026-04-08-maidc-design.md#43-cdr---临床数据仓库)
- [Element Plus文档](https://element-plus.org/)
- [Vue 3文档](https://vuejs.org/)
