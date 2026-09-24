# 02 — 后端 CRUD + 匹配引擎

**Goal:** 实现专病库全部后端 API，包括 CRUD、匹配引擎、患者管理、统计、导出。

**Files:**
- Create: `maidc-data/src/main/java/com/maidc/data/entity/DiseaseCohortEntity.java`
- Create: `maidc-data/src/main/java/com/maidc/data/entity/DiseaseCohortPatientEntity.java`
- Create: `maidc-data/src/main/java/com/maidc/data/entity/DiseaseTemplateEntity.java`
- Create: `maidc-data/src/main/java/com/maidc/data/repository/DiseaseCohortRepository.java`
- Create: `maidc-data/src/main/java/com/maidc/data/repository/DiseaseCohortPatientRepository.java`
- Create: `maidc-data/src/main/java/com/maidc/data/repository/DiseaseTemplateRepository.java`
- Create: `maidc-data/src/main/java/com/maidc/data/service/DiseaseCohortService.java`
- Create: `maidc-data/src/main/java/com/maidc/data/controller/DiseaseCohortController.java`
- Create: `maidc-data/src/test/java/com/maidc/data/service/DiseaseCohortServiceTest.java`

---

## Task 2.1: Entity 层

- [ ] **Step 1: 创建 DiseaseCohortEntity**

遵循 BaseEntity + @Where + @SQLDelete 模式。字段：name, description, inclusionRules (Type JSONB 用 `@Column(columnDefinition = "jsonb")` + String 类型), patientCount, autoSync, status, lastSyncAt。Schema = "cdr", table = "c_disease_cohort"。

- [ ] **Step 2: 创建 DiseaseCohortPatientEntity**

字段：cohortId, patientId, matchSource, matchedAt。Schema = "cdr", table = "c_disease_cohort_patient"。无软删除。

- [ ] **Step 3: 创建 DiseaseTemplateEntity**

Schema = "system", table = "s_disease_template"。字段：diseaseName, icdCodes (String 数组用 `@Column(columnDefinition = "text[]")`), inclusionTemplate (jsonb), description, isBuiltin。

## Task 2.2: Repository 层

- [ ] **Step 4: 创建 3 个 Repository 接口**

```java
// DiseaseCohortRepository
@Repository
public interface DiseaseCohortRepository extends JpaRepository<DiseaseCohortEntity, Long>, JpaSpecificationExecutor<DiseaseCohortEntity> {}

// DiseaseCohortPatientRepository
@Repository
public interface DiseaseCohortPatientRepository extends JpaRepository<DiseaseCohortPatientEntity, Long> {
    List<DiseaseCohortPatientEntity> findByCohortId(Long cohortId);
    boolean existsByCohortIdAndPatientId(Long cohortId, Long patientId);
    void deleteByCohortIdAndPatientIdAndMatchSource(Long cohortId, Long patientId, String matchSource);
    long countByCohortId(Long cohortId);
}

// DiseaseTemplateRepository
@Repository
public interface DiseaseTemplateRepository extends JpaRepository<DiseaseTemplateEntity, Long> {
    List<DiseaseTemplateEntity> findByDiseaseNameContainingAndIsDeletedFalse(String keyword);
}
```

## Task 2.3: Service — CRUD + 匹配引擎

- [ ] **Step 5: 创建 DiseaseCohortService**

实现以下方法：
- `listCohorts(keyword, status, page, size)` — Specification 动态查询
- `getCohort(id)` — 按ID查询
- `createCohort(entity)` — 保存并触发首次匹配
- `updateCohort(id, entity)` — 更新，若 rules 变化则重新匹配
- `deleteCohort(id)` — 软删除
- `matchPatients(cohortId)` — 核心：解析 inclusion_rules JSON，按 group 生成 SQL 查询 patient_id，合并结果写入关联表。同步时不删除 match_source=MANUAL 的记录。
- `matchPreview(cohortId)` — 只返回预计匹配数不写入
- `getPatients(cohortId, page, size)` — 分页查询关联患者
- `addPatient(cohortId, patientId)` — 手动添加
- `removePatient(cohortId, patientId)` — 仅移除 MANUAL 来源
- `getStatistics(cohortId)` — 聚合性别/年龄分布
- `exportCsv(cohortId)` — CSV 流式导出

匹配引擎核心逻辑（使用 DataSource + JDBC，参考 SmartSearchService 模式）：

```java
private Set<Long> executeGroupQuery(Connection conn, InclusionGroup group) {
    String table = DOMAIN_TABLE_MAP.get(group.getDomain());
    StringBuilder sql = new StringBuilder("SELECT DISTINCT patient_id FROM " + table + " WHERE is_deleted = false AND (");
    for (int i = 0; i < group.getConditions().size(); i++) {
        if (i > 0) sql.append(" ").append(group.getLogic()).append(" ");
        Condition c = group.getConditions().get(i);
        sql.append(c.getField()).append(" ").append(toSqlOp(c.getOperator())).append(" ?");
    }
    sql.append(")");
    // execute and return patient_id set
}
```

合并逻辑：AND = 交集，OR = 并集。

## Task 2.4: Controller

- [ ] **Step 6: 创建 DiseaseCohortController**

```java
@RestController
@RequestMapping("/api/v1/cdr/disease-cohorts")
@RequiredArgsConstructor
public class DiseaseCohortController {
    private final DiseaseCohortService service;

    @GetMapping
    public R<Page<DiseaseCohortEntity>> list(...) {}

    @GetMapping("/{id}")
    public R<DiseaseCohortEntity> get(@PathVariable Long id) {}

    @PostMapping
    public R<DiseaseCohortEntity> create(@RequestBody DiseaseCohortEntity entity) {}

    @PutMapping("/{id}")
    public R<DiseaseCohortEntity> update(@PathVariable Long id, @RequestBody DiseaseCohortEntity entity) {}

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {}

    @PostMapping("/{id}/sync")
    public R<Void> sync(@PathVariable Long id) {}

    @GetMapping("/{id}/match-preview")
    public R<Map<String, Object>> matchPreview(@PathVariable Long id) {}

    @GetMapping("/{id}/patients")
    public R<Page<Map<String, Object>>> patients(@PathVariable Long id, ...) {}

    @PostMapping("/{id}/patients/{patientId}")
    public R<Void> addPatient(@PathVariable Long id, @PathVariable Long patientId) {}

    @DeleteMapping("/{id}/patients/{patientId}")
    public R<Void> removePatient(@PathVariable Long id, @PathVariable Long patientId) {}

    @GetMapping("/{id}/statistics")
    public R<Map<String, Object>> statistics(@PathVariable Long id) {}

    @GetMapping("/{id}/export")
    public void export(@PathVariable Long id, HttpServletResponse response) {}
}
```

## Task 2.5: 测试

- [ ] **Step 7: 编写 DiseaseCohortServiceTest**

Mock Repository + DataSource，测试：
- createCohort 正常保存
- matchPatients AND 逻辑（两组取交集）
- matchPatients OR 逻辑（两组取并集）
- addPatient / removePatient
- 统计聚合

- [ ] **Step 8: 运行测试**

```bash
cd maidc-parent && mvn test -pl maidc-data -Dtest=DiseaseCohortServiceTest -V
```

- [ ] **Step 9: Commit**

```bash
git add maidc-data/src/main/java/com/maidc/data/entity/Disease*.java
git add maidc-data/src/main/java/com/maidc/data/repository/Disease*.java
git add maidc-data/src/main/java/com/maidc/data/service/DiseaseCohortService.java
git add maidc-data/src/main/java/com/maidc/data/controller/DiseaseCohortController.java
git add maidc-data/src/test/java/com/maidc/data/service/DiseaseCohortServiceTest.java
git commit -m "feat(disease): add backend CRUD, matching engine, and patient management"
```
