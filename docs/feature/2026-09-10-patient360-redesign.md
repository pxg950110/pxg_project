# 功能实现记录：患者就诊 360 重设计（参考健康分析设计稿）

| 项目 | 内容 |
|------|------|
| 日期 | 2026-09-10 |
| 分支 | feature/disease-kb |
| 设计参考 | 用户提供的"智能健康分析"设计稿（原型 `/proto/health`、`/proto/p360`） |
| 原型 | `/proto/health`（患者自视角）、`/proto/p360`（360 视角 mock） |

## 实现概要

将患者就诊 360 从"就诊列表+详情"的表单式布局重设计为"患者全景"叙事：横幅=患者身份+过敏/家族史预警，统计行=就诊/诊断/异常指标/完整度，中央人体图=诊断按 ICD 章节映射身体系统热区，四周卡片=各系统诊断组，底部=就诊时间轴+病史摘要。前后端全量接真实数据。

## 后端（maidc-parent/maidc-data）

### 新增

| 接口 | 说明 |
|------|------|
| `GET /api/v1/cdr/patient-360/{patientId}/diagnosis-map` | 诊断按 ICD-10 首字母聚合到 15 个身体系统，附人体热区坐标（hotspotX/Y 百分比）、就诊次数、最近就诊、主诊断标记 |
| `GET /api/v1/cdr/patient-360/{patientId}/abnormal-indicators?limit=` | 体检异常指标（`c_checkup_item_result.abnormal_flag`），direction（偏高/偏低/异常）由 result_value 对比 reference_range 推导（支持 `150-420` / `<x` / `>x` 两种范围格式） |

### 修复（原有 SQL 与实际表结构脱节，长期静默失败返回空）

| 方法 | 修正 |
|------|------|
| getPatientBasicInfo | 实际 `c_patient` 无 patient_no/age/phone 等列（mimic 风格），改为 name/gender/birth_date/age(COALESCE(anchor_age, 按出生日推算))/dod |
| getEncounterStats | `encounter_time`→`admission_time`；`los_days` 列不存在，移除总住院日统计 |
| getTimeline | 重写：`encounter_time`→`admission_time`，`encounter_no/admit_source/dischargeDisposition`→`attending_doctor/discharge_time`，`diag_code/diag_name/diag_rank`→`icd_code/icd_name/diagnosis_type` |

## 前端（maidc-portal）

| 文件 | 说明 |
|------|------|
| `src/api/patient360.ts` | 新增：8 个 patient-360 接口封装 + TS 类型 |
| `src/views/data-cdr/patient/Patient360Overview.vue` | 新增：重设计版正式页面（真实数据、并行加载 Promise.allSettled、时间轴按就诊去重合并多诊断、异常指标方向着色、时间轴"详情"跳既有 EncounterDetail） |
| `src/router/asyncRoutes.ts` | 路由切换：`/data/cdr/patient/:patientId`（路由名 PatientEncounter360 不变，随访档案入口自动落新页）→ 新页面；旧页面迁移至 `/data/cdr/patient/:patientId/encounters`（PatientEncounterLegacy） |

## 验证

- [x] maidc-data 打包通过；前端 vue-tsc 改动文件零错误
- [x] diagnosis-map(576)：泌尿生殖（肾结石 N20.0 主诊断）/损伤与中毒/循环系统三组，热区坐标与主诊断标记正确
- [x] abnormal-indicators(1440)：尿酸 141.79μmol/L 判"偏低"（参考 150-420）、总胆固醇判"偏高"，方向推导正确
- [x] encounter-stats/timeline/basic-info 修复后均返回真实数据（陈亮 32 诊断 8 就诊）
- [x] 页面 `/data/cdr/patient/576` 经 vite 代理 HTTP 200

## 已知限制 / 后续

- 旧版 diagnosis-stats/lab-stats/imaging-stats 仍引用不存在的结构（diag_code/result_flag 等），静默返回空——新页面不依赖，留作技术债（getPatient360 聚合接口质量同样受影响）
- ICD→身体系统映射按首字母粗分（15 组），J 未细分鼻/肺；如需精确可引入 ICD 章节映射表
- direction 推导仅支持数值型结果+两种参考范围格式；文本型（阳性/弱阳性）统一为"异常"（橙色）
- 演示数据为 mimic 衍生合成数据，张娟/陈亮等姓名与性别可能不匹配（数据源噪声，非代码问题）
