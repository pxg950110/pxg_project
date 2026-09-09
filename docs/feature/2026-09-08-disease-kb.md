# 功能实现记录：专病知识库后端（P0）

## 基本信息

| 项目 | 内容 |
|------|------|
| 日期 | 2026-09-08 |
| 需求号 | 20260908（手动输入） |
| 分支 | feature/disease-kb（源：feature/permission-system） |
| 设计文档 | 过程文件/2026-09-08/（评估/PRD/原型/TFS分析 四份） |

## 实现范围与裁剪

- **已实现（P0）**：知识空间 CRUD + 唯一名校验 + 队列联动（含悬空引用容错）、四类知识条目 CRUD + 状态机（DRAFT→PUBLISHED→ARCHIVED）、附件上传（MinIO diseasekb 桶，≤50MB，PDF/DOCX/XLSX）、zhparser 中文检索（FTS + ILIKE 回退）、AI 问答会话/消息数据层、20 个 REST 接口全量 @RequirePermission（cdr:diseasekb:read/manage/ai）
- **本切片裁剪**：AI 编排服务（DiseaseKnowledgeAiService）与 ai-worker 三个端点未接入——内容变更标记 ai_status=PENDING 待补算，/qa/ask 按降级语义抛 KB_AI_UNAVAILABLE(5034)；SSE 流式契约已在设计文档约定

## 修改文件清单

### 新增
| 文件 | 说明 |
|------|------|
| docker/init-db/19-disease-kb.sql | 5 表 DDL + FTS/HNSW 索引 + 权限码种子 |
| entity/DiseaseKbSpaceEntity.java | 知识空间（icdCodes text[]、cohortId） |
| entity/DiseaseKbItemEntity.java | 知识条目（ai_summary/ai_extract/ai_status） |
| entity/DiseaseKbQaSessionEntity.java | 问答会话 |
| entity/DiseaseKbQaMessageEntity.java | 问答消息（citations jsonb） |
| repository/DiseaseKb*Repository.java ×4 | JPA 仓库 |
| dto/DiseaseKbSearchVO.java | 检索结果（ts_headline 高亮片段） |
| service/DiseaseKnowledgeService.java | 业务逻辑 |
| controller/DiseaseKnowledgeController.java | 20 接口 + 权限注解 |
| test/DiseaseKnowledgeServiceTest.java | 20 个单测 |

### 修改
| 文件 | 说明 |
|------|------|
| common-core/.../ErrorCode.java | 新增 5031-5034（空间不存在/条目不存在/重名/AI不可用） |

## 验证结果

- [x] DiseaseKnowledgeServiceTest：20/20 通过
- [x] maidc-data 全量回归：75/75 通过（含既有 PatientEncounterServiceTest 等）
- [x] mvn -pl maidc-data -am compile：BUILD SUCCESS

## 待处理事项

- [ ] AI 切片：ai-worker /llm/summary /rag/chat /embedding + DiseaseKnowledgeAiService（SSE 透传契约已定）
- [ ] 代码提交（git-merge）与 PR

---

# 前端切片实现记录（同分支 feature/disease-kb）

## 修改文件清单

### 新增
| 文件 | 说明 |
|------|------|
| maidc-portal/src/api/diseaseKb.ts | API 封装：空间/条目/检索/问答 + fetch SSE 客户端（5034 降级时解析错误信息提示） |
| maidc-portal/src/views/data-cdr/DiseaseKnowledgeList.vue | 知识空间列表页：卡片栅格、新建/编辑弹窗（ICD tags、队列关联、色板）、权限门控 |
| maidc-portal/src/views/data-cdr/DiseaseKnowledgeDetail.vue | 详情页：知识管理（类型页签/表格/AI 摘要列/抽屉 AI 解读）+ AI 问答（会话列表/流式气泡/引用跳转/免责声明） |

### 修改
| 文件 | 说明 |
|------|------|
| maidc-portal/src/router/asyncRoutes.ts | 新增 cdr/disease-kb、cdr/disease-kb/:id 两条路由 |
| maidc-portal/src/views/data-cdr/DiseaseDetail.vue | 头部新增"专病知识库"入口：已关联直接跳转，未关联弹窗引导创建（getSpaceByCohort） |

## 对原型的落地适配

1. 分页响应按项目 PageResult 实际形状取 `content`/`totalElements`（原型误写 list/total）。
2. ICD 绑定改 tags 自由输入（项目无 ICD 检索端点；后端 icd_codes 为 text[]）。
3. 列表页去掉语义检索开关（后端 P0 仅关键词 FTS）；AI 摘要列 PENDING 显示"排队中"文案而非无限骨架（AI 切片未接入，避免永久骨架）。
4. 权限码对齐后端种子：cdr:diseasekb:read / manage / ai。
5. 标签 jsonb ↔ string[] 转换（tagsToObj/objToTags）。

## 验证结果

- [x] vue-tsc --noEmit：本次 5 个涉及文件 0 错误（存量无关文件错误不计入）
- [ ] 联调验证（需后端服务 + 19 号 SQL 执行 + 角色绑定 cdr:diseasekb:* 权限）
