# ODS Embulk 导入实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 Embulk CLI + ProcessBuilder 的 ODS 数据导入系统，将 135GB MIMIC-III/IV CSV 导入 PostgreSQL ods schema，含行数校验。

**Architecture:** Spring Boot maidc-data 模块中新增 ETL 导入服务。OdsImportService 动态生成 Embulk YAML 配置，线程池调度 ProcessBuilder 调 Embulk CLI，导入后校验行数一致性。元数据记录在 ods.ods_import_log 和 ods.ods_import_check。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Embulk v0.11.5 (CLI), PostgreSQL 15, ProcessBuilder, JDBC

---

### Task 1: 环境准备 — 提交 ODS DDL + 安装 Embulk

**Files:**
- Commit (existing untracked): `docker/init-db/07-ods-schema.sql`, `08-ods-mimic3.sql`, `09-ods-mimic4.sql`, `10-cdr-patch.sql`
- Create: `scripts/install-embulk.sh`

- [ ] **Step 1: 提交 ODS DDL 文件到 git**

```bash
cd E:/pxg_project
git add docker/init-db/07-ods-schema.sql docker/init-db/08-ods-mimic3.sql docker/init-db/09-ods-mimic4.sql docker/init-db/10-cdr-patch.sql
git commit -m "feat: add ODS schema + MIMIC-III/IV table DDL for data import"
```

- [ ] **Step 2: 创建 Embulk 安装脚本**

Create `scripts/install-embulk.sh`:

```bash
#!/bin/bash
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
EMBULK_JAR="$SCRIPT_DIR/embulk.jar"

if [ -f "$EMBULK_JAR" ]; then
    echo "Embulk already installed at $EMBULK_JAR"
    java -jar "$EMBULK_JAR" --version
    exit 0
fi

echo "Downloading Embulk v0.11.5..."
curl -L https://github.com/embulk/embulk/releases/download/v0.11.5/embulk-0.11.5.jar -o "$EMBULK_JAR"

echo "Installing PostgreSQL output plugin..."
java -jar "$EMBULK_JAR" gem install embulk-output-postgresql

echo "Embulk installation complete."
java -jar "$EMBULK_JAR" --version
```

- [ ] **Step 3: 执行安装 Embulk**

```bash
cd E:/pxg_project
bash scripts/install-embulk.sh
```

验证: `java -jar scripts/embulk.jar --version` 输出 `0.11.5`

- [ ] **Step 4: 提交**

```bash
git add scripts/install-embulk.sh
git commit -m "feat: add Embulk installation script"
```

---

### Task 2: 配置项 + DTO

**Files:**
- Modify: `maidc-data/src/main/resources/application-dev.yml`
- Create: `maidc-data/src/main/java/com/maidc/data/dto/ImportTask.java`
- Create: `maidc-data/src/main/java/com/maidc/data/dto/ImportStatusVO.java`

- [ ] **Step 1: 添加 ETL 配置到 application-dev.yml**

在 `maidc-data/src/main/resources/application-dev.yml` 末尾追加:

```yaml
maidc:
  jwt:
    secret: maidc-jwt-secret-key-2026-change-in-production
  etl:
    embulk-path: scripts/embulk.jar
    csv-base-dir: E:/pxg_project/data/icu-datasets
    parallel: 3
    batch-size: 10000
    db-url: jdbc:postgresql://localhost:5432/maidc
    db-schema: ods
    db-user: maidc
    db-password: maidc123
```

- [ ] **Step 2: 创建 ImportTask DTO**

Create `maidc-data/src/main/java/com/maidc/data/dto/ImportTask.java`:

```java
package com.maidc.data.dto;

import lombok.Data;

@Data
public class ImportTask {
    private String tableName;
    private String csvPath;
    private String status;       // PENDING / RUNNING / SUCCESS / FAILED
    private Long csvRows;
    private Long dbRows;
    private Boolean rowMatch;
    private Integer durationSec;
    private String errorMsg;
    private String batchId;
}
```

- [ ] **Step 3: 创建 ImportStatusVO**

Create `maidc-data/src/main/java/com/maidc/data/dto/ImportStatusVO.java`:

```java
package com.maidc.data.dto;

import lombok.Data;
import java.util.List;

@Data
public class ImportStatusVO {
    private String batchId;
    private int total;
    private int success;
    private int failed;
    private int running;
    private int pending;
    private List<TableStatus> tables;

    @Data
    public static class TableStatus {
        private String table;
        private String status;
        private Long csvRows;
        private Long dbRows;
        private Boolean match;
        private Integer duration;
        private String startedAt;
        private String error;
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
cd E:/pxg_project/maidc-parent && mvn compile -pl maidc-data -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
cd E:/pxg_project
git add maidc-parent/maidc-data/src/main/resources/application-dev.yml \
        maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/
git commit -m "feat: add ETL config and import DTOs"
```

---

### Task 3: TableMapping — 57 张表映射定义

**Files:**
- Create: `maidc-data/src/main/java/com/maidc/data/etl/TableMapping.java`

- [ ] **Step 1: 创建 TableMapping**

Create `maidc-data/src/main/java/com/maidc/data/etl/TableMapping.java`:

```java
package com.maidc.data.etl;

import lombok.Data;
import java.util.List;

@Data
public class TableMapping {
    private String tableName;          // o3_patients
    private String csvFile;            // iii/PATIENTS.csv/PATIENTS.csv
    private boolean largeTable;        // >= 500MB
    private List<ColumnDef> columns;

    @Data
    public static class ColumnDef {
        private String name;
        private String embulkType;     // long, string, timestamp, double
        private String format;         // timestamp format, e.g. "%Y-%m-%d %H:%M:%S"
    }
}
```

- [ ] **Step 2: 在 TableMapping 中添加静态工厂方法 `allMappings()`，返回全部 57 张表映射**

在 TableMapping 类中添加:

```java
import java.util.ArrayList;
import java.util.Map;

public static List<TableMapping> allMappings() {
    List<TableMapping> list = new ArrayList<>();
    // --- MIMIC-III (o3_ prefix, 26 tables) ---
    list.add(of("o3_caregivers", "iii/CAREGIVERS.csv/CAREGIVERS.csv", false,
        col("row_id","long"), col("cgid","long"), col("label","string"), col("description","string")));
    list.add(of("o3_d_items", "iii/D_ITEMS.csv/D_ITEMS.csv", false,
        col("row_id","long"), col("itemid","long"), col("label","string"), col("abbreviation","string"),
        col("dbsource","string"), col("linksto","string"), col("category","string"),
        col("unitname","string"), col("param_type","string"), col("conceptid","long")));
    list.add(of("o3_d_labitems", "iii/D_LABITEMS.csv/D_LABITEMS.csv", false,
        col("row_id","long"), col("itemid","long"), col("label","string"), col("fluid","string"), col("category","string"), col("loinc_code","string")));
    list.add(of("o3_d_icd_diagnoses", "iii/D_ICD_DIAGNOSES.csv/D_ICD_DIAGNOSES.csv", false,
        col("row_id","long"), col("icd9_code","string"), col("short_title","string"), col("long_title","string")));
    list.add(of("o3_d_icd_procedures", "iii/D_ICD_PROCEDURES.csv/D_ICD_PROCEDURES.csv", false,
        col("row_id","long"), col("icd9_code","string"), col("short_title","string"), col("long_title","string")));
    list.add(of("o3_d_cpt", "iii/D_CPT.csv/D_CPT.csv", false,
        col("row_id","long"), col("category","long"), col("sectionrange","string"), col("sectionheader","string"),
        col("subsectionrange","string"), col("subsectionheader","string"), col("codesuffix","string"),
        col("mincodeinsubsection","long"), col("maxcodeinsubsection","long")));
    list.add(of("o3_patients", "iii/PATIENTS.csv/PATIENTS.csv", false,
        col("row_id","long"), col("subject_id","long"), col("gender","string"),
        ts("dob"), ts("dod"), ts("dod_hosp"), ts("dod_ssn"), col("expire_flag","long")));
    list.add(of("o3_admissions", "iii/ADMISSIONS.csv/ADMISSIONS.csv", false,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"),
        ts("admittime"), ts("dischtime"), ts("deathtime"), col("admission_type","string"),
        col("admission_location","string"), col("discharge_location","string"), col("insurance","string"),
        col("language","string"), col("religion","string"), col("marital_status","string"),
        col("ethnicity","string"), ts("edregtime"), ts("edouttime"), col("diagnosis","string"),
        col("hospital_expire_flag","long"), col("has_chartevents_data","long")));
    list.add(of("o3_icustays", "iii/ICUSTAYS.csv/ICUSTAYS.csv", false,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), col("icustay_id","long"),
        col("dbsource","string"), col("first_careunit","string"), col("last_careunit","string"),
        ts("intime"), ts("outtime"), col("los","double")));
    list.add(of("o3_services", "iii/SERVICES.csv/SERVICES.csv", false,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), ts("transfertime"),
        col("prev_service","string"), col("curr_service","string")));
    list.add(of("o3_transfers", "iii/TRANSFERS.csv/TRANSFERS.csv", false,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), col("icustay_id","long"),
        ts("intime"), ts("outtime"), col("los","double"), col("eventtype","string"),
        col("prev_careunit","string"), col("curr_careunit","string")));
    list.add(of("o3_diagnoses_icd", "iii/DIAGNOSES_ICD.csv/DIAGNOSES_ICD.csv", false,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), col("seq_num","long"), col("icd9_code","string")));
    list.add(of("o3_procedures_icd", "iii/PROCEDURES_ICD.csv/PROCEDURES_ICD.csv", false,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), col("seq_num","long"), col("icd9_code","string")));
    list.add(of("o3_drgcodes", "iii/DRGCODES.csv/DRGCODES.csv", false,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), col("drg_type","string"),
        col("drg_code","string"), col("description","string"), col("drg_severity","long"), col("drg_mortality","long")));
    list.add(of("o3_cptevents", "iii/CPTEVENTS.csv/CPTEVENTS.csv", false,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), col("costcenter","string"),
        col("chartdate","timestamp","%Y-%m-%d"), col("cpt_cd","string"), col("cpt_number","long"),
        col("cpt_suffix","string"), col("ticket_id_seq","long"), col("sectionheader","string")));
    list.add(of("o3_callout", "iii/CALLOUT.csv/CALLOUT.csv", false,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), col("submit_wardid","long"),
        col("submit_careunit","string"), col("curr_wardid","long"), col("curr_careunit","string"),
        col("callout_wardid","long"), col("callout_service","string"), col("request_tele","long"),
        col("request_resp","long"), col("request_cdiff","long"), col("request_mrsa","long"),
        col("request_vre","long"), ts("callout_status"), ts("callout_outcome"),
        ts("acknowledge_status"), col("acknowledgetime","timestamp","%Y-%m-%d %H:%M:%S"),
        col("createtime","timestamp","%Y-%m-%d %H:%M:%S"), col("updatetime","timestamp","%Y-%m-%d %H:%M:%S"),
        col("confirmtime","timestamp","%Y-%m-%d %H:%M:%S")));
    list.add(of("o3_chartevents", "iii/CHARTEVENTS.csv/CHARTEVENTS.csv", true,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), col("icustay_id","long"),
        col("itemid","long"), ts("charttime"), col("storetime","timestamp","%Y-%m-%d %H:%M:%S"),
        col("cgid","long"), col("value","string"), col("valuenum","double"),
        col("valueuom","string"), col("warning","long"), col("error","long"),
        col("resultstatus","string"), col("stopped","string")));
    list.add(of("o3_labevents", "iii/LABEVENTS.csv/LABEVENTS.csv", true,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), col("itemid","long"),
        ts("charttime"), col("value","string"), col("valuenum","double"),
        col("valueuom","string"), col("ref_range_lower","double"), col("ref_range_upper","double"),
        col("flag","string")));
    list.add(of("o3_prescriptions", "iii/PRESCRIPTIONS.csv/PRESCRIPTIONS.csv", false,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), col("icustay_id","long"),
        ts("startdate"), ts("enddate"), col("drug_type","string"), col("drug","string"),
        col("drug_name_poe","string"), col("drug_name_generic","string"), col("formulary_drug_cd","string"),
        col("gsn","string"), col("ndc","string"), col("prod_strength","string"),
        col("form_rx","string"), col("dose_val_rx","string"), col("dose_unit_rx","string"),
        col("form_val_disp","string"), col("form_unit_disp","string"), col("doses_per_24_hrs","long"),
        col("route","string")));
    list.add(of("o3_inputevents_cv", "iii/INPUTEVENTS_CV.csv/INPUTEVENTS_CV.csv", true,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), col("icustay_id","long"),
        ts("charttime"), col("itemid","long"), col("amount","double"), col("amountuom","string"),
        col("rate","double"), col("rateuom","string"), ts("starttime"), ts("endtime"),
        col("cgid","long"), col("orderid","long"), col("linkorderid","long"),
        col("ordercategoryname","string"), col("secondaryordercategoryname","string"),
        col("ordercomponenttypedescription","string"), col("ordercategorydescription","string"),
        col("patientweight","double"), col("totalamount","double"), col("totalamountuom","string"),
        col("isopenbag","long"), col("continueinnextdept","long"), col("statusdescription","string"),
        col("originalamount","double"), col("originalrate","double")));
    list.add(of("o3_inputevents_mv", "iii/INPUTEVENTS_MV.csv/INPUTEVENTS_MV.csv", false,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), col("icustay_id","long"),
        ts("starttime"), ts("endtime"), col("itemid","long"), col("amount","double"),
        col("amountuom","string"), col("rate","double"), col("rateuom","string"),
        ts("storetime"), col("cgid","long"), col("orderid","long"), col("linkorderid","long"),
        col("ordercategoryname","string"), col("secondaryordercategoryname","string"),
        col("ordercomponenttypedescription","string"), col("ordercategorydescription","string"),
        col("patientweight","double"), col("totalamount","double"), col("totalamountuom","string"),
        col("isopenbag","long"), col("continueinnextdept","long"), col("statusdescription","string"),
        col("originalamount","double"), col("originalrate","double")));
    list.add(of("o3_outputevents", "iii/OUTPUTEVENTS.csv/OUTPUTEVENTS.csv", false,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), col("icustay_id","long"),
        ts("charttime"), col("itemid","long"), col("value","double"), col("valueuom","string"),
        col("cgid","long"), col("stopped","string"), col("iserror","long")));
    list.add(of("o3_noteevents", "iii/NOTEEVENTS.csv/NOTEEVENTS.csv", true,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), ts("chartdate"),
        ts("charttime"), col("storetime","timestamp","%Y-%m-%d %H:%M:%S"), col("category","string"),
        col("description","string"), col("cgid","long"), col("iserror","long"), col("text","string")));
    list.add(of("o3_microbiologyevents", "iii/MICROBIOLOGYEVENTS.csv/MICROBIOLOGYEVENTS.csv", false,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), col("chartdate","timestamp","%Y-%m-%d"),
        col("charttime","timestamp","%Y-%m-%d %H:%M:%S"), col("spec_itemid","long"), col("spec_type_desc","string"),
        col("org_itemid","long"), col("org_name","string"), col("isolate_num","long"),
        col("ab_itemid","long"), col("ab_name","string"), col("dilution_text","string"),
        col("dilution_comparison","string"), col("dilution_value","double"), col("interpretation","string")));
    list.add(of("o3_datetimeevents", "iii/DATETIMEEVENTS.csv/DATETIMEEVENTS.csv", false,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), col("icustay_id","long"),
        col("itemid","long"), ts("charttime"), col("storetime","timestamp","%Y-%m-%d %H:%M:%S"),
        ts("value"), col("valueuom","string"), col("cgid","long"), col("error","long")));
    list.add(of("o3_procedureevents_mv", "iii/PROCEDUREEVENTS_MV.csv/PROCEDUREEVENTS_MV.csv", false,
        col("row_id","long"), col("subject_id","long"), col("hadm_id","long"), col("icustay_id","long"),
        ts("starttime"), ts("endtime"), col("itemid","long"), col("value","double"),
        col("valueuom","string"), col("location","string"), col("locationcategory","string"),
        ts("storetime"), col("cgid","long"), col("orderid","long"), col("linkorderid","long"),
        col("ordercategoryname","string"), col("secondaryordercategoryname","string"),
        col("ordercategorydescription","string"), col("isopenbag","long"), col("continueinnextdept","long"),
        col("statusdescription","string"), col("originalamount","double"), col("originalrate","double")));

    // --- MIMIC-IV (o4_ prefix, 31 tables) ---
    list.add(of("o4_patients", "iv/content/mimic-iv-3.1/hosp/patients.csv", false,
        col("subject_id","long"), col("gender","string"), col("anchor_age","long"),
        col("anchor_year","long"), col("anchor_year_group","string"), ts("dod")));
    list.add(of("o4_admissions", "iv/content/mimic-iv-3.1/hosp/admissions.csv", false,
        col("subject_id","long"), col("hadm_id","long"), ts("admittime"), ts("dischtime"), ts("deathtime"),
        col("admission_type","string"), col("admit_provider_id","string"), col("admission_location","string"),
        col("discharge_location","string"), col("insurance","string"), col("language","string"),
        col("marital_status","string"), col("race","string"), ts("edregtime"), ts("edouttime"),
        col("hospital_expire_flag","long")));
    list.add(of("o4_provider", "iv/content/mimic-iv-3.1/hosp/provider.csv", false,
        col("provider_id","string")));
    list.add(of("o4_d_hcpcs", "iv/content/mimic-iv-3.1/hosp/d_hcpcs.csv", false,
        col("code","string"), col("category","string"), col("long_description","string"), col("short_description","string")));
    list.add(of("o4_d_icd_diagnoses", "iv/content/mimic-iv-3.1/hosp/d_icd_diagnoses.csv", false,
        col("icd_code","string"), col("icd_version","long"), col("long_title","string")));
    list.add(of("o4_d_icd_procedures", "iv/content/mimic-iv-3.1/hosp/d_icd_procedures.csv", false,
        col("icd_code","string"), col("icd_version","long"), col("long_title","string")));
    list.add(of("o4_d_labitems", "iv/content/mimic-iv-3.1/hosp/d_labitems.csv", false,
        col("itemid","long"), col("label","string"), col("fluid","string"), col("category","string")));
    list.add(of("o4_diagnoses_icd", "iv/content/mimic-iv-3.1/hosp/diagnoses_icd.csv", false,
        col("subject_id","long"), col("hadm_id","long"), col("seq_num","long"), col("icd_code","string"), col("icd_version","long")));
    list.add(of("o4_drgcodes", "iv/content/mimic-iv-3.1/hosp/drgcodes.csv", false,
        col("subject_id","long"), col("hadm_id","long"), col("drg_type","string"), col("drg_code","string"),
        col("description","string"), col("drg_severity","long"), col("drg_mortality","long")));
    list.add(of("o4_hcpcsevents", "iv/content/mimic-iv-3.1/hosp/hcpcsevents.csv", false,
        col("subject_id","long"), col("hadm_id","long"), col("hcpcs_cd","string"), col("seq_num","long"),
        col("short_description","string")));
    list.add(of("o4_procedures_icd", "iv/content/mimic-iv-3.1/hosp/procedures_icd.csv", false,
        col("subject_id","long"), col("hadm_id","long"), col("seq_num","long"), col("icd_code","string"), col("icd_version","long")));
    list.add(of("o4_services", "iv/content/mimic-iv-3.1/hosp/services.csv", false,
        col("subject_id","long"), col("hadm_id","long"), ts("transfertime"), col("prev_service","string"), col("curr_service","string")));
    list.add(of("o4_transfers", "iv/content/mimic-iv-3.1/hosp/transfers.csv", false,
        col("subject_id","long"), col("hadm_id","long"), ts("intime"), ts("outtime"), col("eventtype","string"),
        col("careunit","string"), col("losetw","double")));
    list.add(of("o4_omr", "iv/content/mimic-iv-3.1/hosp/omr.csv", false,
        col("subject_id","long"), col("chartdate","timestamp","%Y-%m-%d"), col("result_name","string"), col("result_value","string")));
    list.add(of("o4_labevents", "iv/content/mimic-iv-3.1/hosp/labevents.csv", true,
        col("labevent_id","long"), col("subject_id","long"), col("hadm_id","long"), col("itemid","long"),
        ts("charttime"), col("value","string"), col("valuenum","double"), col("valueuom","string"),
        col("ref_range_lower","double"), col("ref_range_upper","double"), col("flag","string"), col("priority","string"),
        col("comments","string")));
    list.add(of("o4_microbiologyevents", "iv/content/mimic-iv-3.1/hosp/microbiologyevents.csv", false,
        col("microevent_id","long"), col("subject_id","long"), col("hadm_id","long"),
        col("chartdate","timestamp","%Y-%m-%d"), ts("charttime"), col("spec_itemid","long"),
        col("spec_type_desc","string"), col("test_itemid","long"), col("test_name","string"),
        col("org_itemid","long"), col("org_name","string"), col("isolate_num","long"),
        col("quantity","string"), col("ab_itemid","long"), col("ab_name","string"),
        col("dilution_text","string"), col("dilution_comparison","string"), col("dilution_value","double"),
        col("interpretation","string"), col("comments","string")));
    list.add(of("o4_pharmacy", "iv/content/mimic-iv-3.1/hosp/pharmacy.csv", true,
        col("subject_id","long"), col("hadm_id","long"), col("pharmacy_id","long"), ts("starttime"),
        ts("stoptime"), col("medication","string"), col("proc_type","string"), col("route","string"),
        col("frequency","string"), col("doses_per_24_hrs","double"), col("dose","string"),
        col("dose_unit","string"), col("form","string"), col("doses_per_24_hrs_overlay","string")));
    list.add(of("o4_poe", "iv/content/mimic-iv-3.1/hosp/poe.csv", true,
        col("poe_id","string"), col("poe_seq","long"), col("subject_id","long"), col("hadm_id","long"),
        ts("ordertime"), col("order_type","string"), col("order_subtype","string"),
        col("transaction_type","string"), col("discontinue_of_poe_id","string"), col("discontinued_by_poe_id","string")));
    list.add(of("o4_poe_detail", "iv/content/mimic-iv-3.1/hosp/poe_detail.csv", false,
        col("poe_id","string"), col("poe_seq","long"), col("subject_id","long"), ts("formulary_drug_cd"),
        col("gsn","string"), col("ndc","string"), col("prod_strength","string"),
        col("form_rx","string"), col("dose_val_rx","string"), col("dose_unit_rx","string"),
        col("form_val_disp","string"), col("form_unit_disp","string"), col("doses_per_24_hrs","long"),
        col("route","string")));
    list.add(of("o4_prescriptions", "iv/content/mimic-iv-3.1/hosp/prescriptions.csv", true,
        col("subject_id","long"), col("hadm_id","long"), col("pharmacy_id","long"), ts("starttime"),
        ts("stoptime"), col("drug_type","string"), col("drug","string"), col("gsn","string"),
        col("ndc","string"), col("prod_strength","string"), col("form_rx","string"),
        col("dose_val_rx","string"), col("dose_unit_rx","string"), col("form_val_disp","string"),
        col("form_unit_disp","string"), col("doses_per_24_hrs","long"), col("route","string")));
    list.add(of("o4_emar", "iv/content/mimic-iv-3.1/hosp/emar.csv", true,
        col("subject_id","long"), col("hadm_id","long"), col("emar_id","string"), col("emar_seq","long"),
        col("poe_id","string"), col("pharmacy_id","long"), ts("charttime"), col("medication","string"),
        col("event_txt","string"), col("scheduletime","timestamp","%Y-%m-%d %H:%M:%S"),
        col("administration_type","string"), col("pharmacy_id","long")));
    list.add(of("o4_emar_detail", "iv/content/mimic-iv-3.1/hosp/emar_detail.csv", true,
        col("subject_id","long"), col("emar_id","string"), col("parent_field_ordinal","string"),
        col("administration_type","string"), col("pharmacy_id","long"), col("barcode_type","string"),
        col("reason_for_no_barcode","string"), col("complete_dose_not_given","string"),
        col("dose_due","string"), col("dose_given","string"), col("dose_given_unit","string"),
        col("will_remainder_of_dose_be_given","string"), col("product_amount_given","string"),
        col("product_unit","string"), col("product_code","string"), col("product_description","string"),
        col("product_description_other","string"), col("prior_infusion_rate","string"),
        col("infusion_rate","string"), col("infusion_rate_adjustment","string"),
        col("infusion_rate_adjustment_amount","string"), col("infusion_rate_unit","string"),
        col("route","string"), col("infusion_complete","string"), col("completion_interval","string"),
        col("new_iv_bag_hung","string"), col("continued_infusion_in_other_location","string"),
        col("restart_interval","string"), col("side","string"), col("site","string"),
        col("non_formulary_visual_verification","string")));
    list.add(of("o4_caregiver", "iv/content/mimic-iv-3.1/icu/caregiver.csv", false,
        col("cgid","long"), col("label","string"), col("description","string")));
    list.add(of("o4_d_items", "iv/content/mimic-iv-3.1/icu/d_items.csv", false,
        col("itemid","long"), col("label","string"), col("abbreviation","string"),
        col("linksto","string"), col("category","string"), col("unitname","string"),
        col("param_type","string"), col("lownormalvalue","double"), col("highnormalvalue","double")));
    list.add(of("o4_icustays", "iv/content/mimic-iv-3.1/icu/icustays.csv", false,
        col("subject_id","long"), col("hadm_id","long"), col("stay_id","long"),
        ts("intime"), ts("outtime"), col("los","double")));
    list.add(of("o4_chartevents", "iv/content/mimic-iv-3.1/icu/chartevents.csv", true,
        col("subject_id","long"), col("hadm_id","long"), col("stay_id","long"), col("caregiver_id","long"),
        ts("charttime"), ts("storetime"), col("itemid","long"), col("value","string"),
        col("valuenum","double"), col("valueuom","string"), col("warning","long")));
    list.add(of("o4_datetimeevents", "iv/content/mimic-iv-3.1/icu/datetimeevents.csv", false,
        col("subject_id","long"), col("hadm_id","long"), col("stay_id","long"), col("caregiver_id","long"),
        ts("charttime"), col("itemid","long"), ts("value")));
    list.add(of("o4_ingredientevents", "iv/content/mimic-iv-3.1/icu/ingredientevents.csv", true,
        col("subject_id","long"), col("hadm_id","long"), col("stay_id","long"), col("caregiver_id","long"),
        ts("starttime"), ts("endtime"), col("itemid","long"), col("amount","double"),
        col("amountuom","string"), col("rate","double"), col("rateuom","string"),
        col("orderid","long"), col("linkorderid","long"), col("ordercategoryname","string"),
        col("secondaryordercategoryname","string"), col("ordercomponenttypedescription","string"),
        col("ordercategorydescription","string"), col("patientweight","double"), col("totalamount","double"),
        col("totalamountuom","string"), col("isopenbag","long"), col("continueinnextdept","long"),
        col("statusdescription","string")));
    list.add(of("o4_inputevents", "iv/content/mimic-iv-3.1/icu/inputevents.csv", true,
        col("subject_id","long"), col("hadm_id","long"), col("stay_id","long"), col("caregiver_id","long"),
        ts("starttime"), ts("endtime"), col("storetime","timestamp","%Y-%m-%d %H:%M:%S"),
        col("itemid","long"), col("amount","double"), col("amountuom","string"),
        col("rate","double"), col("rateuom","string"), col("orderid","long"), col("linkorderid","long"),
        col("ordercategoryname","string"), col("secondaryordercategoryname","string"),
        col("ordercomponenttypedescription","string"), col("ordercategorydescription","string"),
        col("patientweight","double"), col("totalamount","double"), col("totalamountuom","string"),
        col("isopenbag","long"), col("continueinnextdept","long"), col("statusdescription","string"),
        col("originalamount","double"), col("originalrate","double")));
    list.add(of("o4_outputevents", "iv/content/mimic-iv-3.1/icu/outputevents.csv", false,
        col("subject_id","long"), col("hadm_id","long"), col("stay_id","long"), col("caregiver_id","long"),
        ts("charttime"), ts("storetime"), col("itemid","long"), col("value","double"),
        col("valueuom","string")));
    list.add(of("o4_procedureevents", "iv/content/mimic-iv-3.1/icu/procedureevents.csv", false,
        col("subject_id","long"), col("hadm_id","long"), col("stay_id","long"), col("caregiver_id","long"),
        ts("starttime"), ts("endtime"), col("storetime","timestamp","%Y-%m-%d %H:%M:%S"),
        col("itemid","long"), col("value","double"), col("valueuom","string"),
        col("location","string"), col("locationcategory","string"), col("orderid","long"),
        col("linkorderid","long"), col("ordercategoryname","string"),
        col("secondaryordercategoryname","string"), col("ordercategorydescription","string"),
        col("patientweight","double"), col("isopenbag","long"), col("continueinnextdept","long"),
        col("statusdescription","string"), col("originalamount","double"), col("originalrate","double")));

    return list;
}

private static TableMapping of(String tableName, String csvFile, boolean largeTable, ColumnDef... cols) {
    TableMapping m = new TableMapping();
    m.setTableName(tableName);
    m.setCsvFile(csvFile);
    m.setLargeTable(largeTable);
    m.setColumns(List.of(cols));
    return m;
}

private static ColumnDef col(String name, String embulkType) {
    ColumnDef c = new ColumnDef();
    c.setName(name);
    c.setEmbulkType(embulkType);
    return c;
}

private static ColumnDef ts(String name) {
    ColumnDef c = new ColumnDef();
    c.setName(name);
    c.setEmbulkType("timestamp");
    c.setFormat("%Y-%m-%d %H:%M:%S");
    return c;
}
```

- [ ] **Step 3: 编译验证**

```bash
cd E:/pxg_project/maidc-parent && mvn compile -pl maidc-data -q
```

- [ ] **Step 4: 提交**

```bash
cd E:/pxg_project
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/etl/TableMapping.java
git commit -m "feat: add TableMapping with all 57 MIMIC table definitions"
```

---

### Task 4: EmbulkConfigGenerator — YAML 配置生成

**Files:**
- Create: `maidc-data/src/main/java/com/maidc/data/etl/EmbulkConfigGenerator.java`

- [ ] **Step 1: 创建 EmbulkConfigGenerator**

Create `maidc-data/src/main/java/com/maidc/data/etl/EmbulkConfigGenerator.java`:

```java
package com.maidc.data.etl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class EmbulkConfigGenerator {

    @Value("${maidc.etl.csv-base-dir}")
    private String csvBaseDir;

    @Value("${maidc.etl.db-url}")
    private String dbUrl;

    @Value("${maidc.etl.db-schema}")
    private String dbSchema;

    @Value("${maidc.etl.db-user}")
    private String dbUser;

    @Value("${maidc.etl.db-password}")
    private String dbPassword;

    public Path generateYaml(TableMapping mapping, String batchId) throws IOException {
        Path tmpDir = Files.createTempDirectory("embulk-");
        Path yamlPath = tmpDir.resolve(mapping.getTableName() + ".yml");

        // Extract host/port/database from JDBC URL
        // jdbc:postgresql://localhost:5432/maidc
        String[] parts = dbUrl.replace("jdbc:postgresql://", "").split("/");
        String hostPort = parts[0];
        String database = parts.length > 1 ? parts[1] : "maidc";
        String[] hp = hostPort.split(":");
        String host = hp[0];
        String port = hp.length > 1 ? hp[1] : "5432";

        try (BufferedWriter w = Files.newBufferedWriter(yamlPath)) {
            w.write("in:\n");
            w.write("  type: file\n");
            w.write("  path_prefix: \"" + csvBaseDir + "/" + removeExtension(mapping.getCsvFile()) + "\"\n");
            w.write("  parser:\n");
            w.write("    type: csv\n");
            w.write("    delimiter: \",\"\n");
            w.write("    quote: '\"'\n");
            w.write("    header_line: true\n");
            w.write("    columns:\n");
            for (TableMapping.ColumnDef col : mapping.getColumns()) {
                if ("timestamp".equals(col.getEmbulkType()) && col.getFormat() != null) {
                    w.write("      - {name: " + col.getName() + ", type: timestamp, format: \"" + col.getFormat() + "\"}\n");
                } else {
                    w.write("      - {name: " + col.getName() + ", type: " + col.getEmbulkType() + "}\n");
                }
            }
            w.write("filters:\n");
            w.write("  - type: add_columns\n");
            w.write("    columns:\n");
            w.write("      - {name: _batch_id, value: \"" + batchId + "\"}\n");
            w.write("      - {name: _source_file, value: \"" + mapping.getCsvFile() + "\"}\n");
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            w.write("      - {name: _loaded_at, value: \"" + now + "\", type: timestamp}\n");
            w.write("      - {name: _is_valid, value: \"true\"}\n");
            w.write("out:\n");
            w.write("  type: postgresql\n");
            w.write("  host: " + host + "\n");
            w.write("  port: " + port + "\n");
            w.write("  user: " + dbUser + "\n");
            w.write("  password: " + dbPassword + "\n");
            w.write("  database: " + database + "\n");
            w.write("  schema: " + dbSchema + "\n");
            w.write("  table: " + mapping.getTableName() + "\n");
            w.write("  mode: insert\n");
            w.write("  insert_method: copy\n");
        }
        return yamlPath;
    }

    /**
     * Remove .csv extension from path for Embulk path_prefix.
     * PATIENTS.csv/PATIENTS.csv -> PATIENTS.csv/PATIENTS
     * hosp/patients.csv -> hosp/patients
     */
    private String removeExtension(String path) {
        if (path.toLowerCase().endsWith(".csv")) {
            return path.substring(0, path.length() - 4);
        }
        return path;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd E:/pxg_project/maidc-parent && mvn compile -pl maidc-data -q
```

- [ ] **Step 3: 提交**

```bash
cd E:/pxg_project
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/etl/EmbulkConfigGenerator.java
git commit -m "feat: add Embulk YAML config generator"
```

---

### Task 5: EmbulkRunner — ProcessBuilder 封装

**Files:**
- Create: `maidc-data/src/main/java/com/maidc/data/etl/EmbulkRunner.java`

- [ ] **Step 1: 创建 EmbulkRunner**

Create `maidc-data/src/main/java/com/maidc/data/etl/EmbulkRunner.java`:

```java
package com.maidc.data.etl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class EmbulkRunner {

    @Value("${maidc.etl.embulk-path}")
    private String embulkPath;

    /**
     * Run embulk with the given YAML config. Returns true on success.
     */
    public boolean run(Path yamlPath, long timeoutMinutes) {
        ProcessBuilder pb = new ProcessBuilder(
                "java", "-jar", embulkPath, "run", yamlPath.toString()
        );
        pb.redirectErrorStream(true);

        log.info("Starting Embulk: {} -> {}", yamlPath.getFileName(), yamlPath);

        try {
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (line.contains("error") || line.contains("Error") || line.contains("ERROR")) {
                        log.warn("[embulk] {}", line);
                    } else {
                        log.debug("[embulk] {}", line);
                    }
                }
            }

            boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                log.error("Embulk timed out after {} minutes for {}", timeoutMinutes, yamlPath.getFileName());
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Embulk exited with code {} for {}. Output:\n{}", exitCode, yamlPath.getFileName(), output);
                return false;
            }

            log.info("Embulk completed successfully: {}", yamlPath.getFileName());
            return true;
        } catch (IOException | InterruptedException e) {
            log.error("Embulk execution failed for {}", yamlPath.getFileName(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd E:/pxg_project/maidc-parent && mvn compile -pl maidc-data -q
```

- [ ] **Step 3: 提交**

```bash
cd E:/pxg_project
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/etl/EmbulkRunner.java
git commit -m "feat: add Embulk ProcessBuilder runner"
```

---

### Task 6: OdsImportRepository — 元数据表操作

**Files:**
- Create: `maidc-data/src/main/java/com/maidc/data/service/OdsImportRepository.java`

- [ ] **Step 1: 创建 OdsImportRepository**

Create `maidc-data/src/main/java/com/maidc/data/service/OdsImportRepository.java`:

```java
package com.maidc.data.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OdsImportRepository {

    private final JdbcTemplate jdbc;

    public void ensureSchemaAndTables() {
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS ods");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS ods.ods_import_log (
                id              BIGSERIAL   PRIMARY KEY,
                table_name      VARCHAR(64) NOT NULL,
                source_file     VARCHAR(256) NOT NULL,
                status          VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                csv_rows        BIGINT,
                db_rows         BIGINT,
                row_match       BOOLEAN,
                started_at      TIMESTAMP,
                finished_at     TIMESTAMP,
                duration_sec    INT,
                error_msg       TEXT,
                batch_id        VARCHAR(32) NOT NULL
            )
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS ods.ods_import_check (
                id              BIGSERIAL   PRIMARY KEY,
                batch_id        VARCHAR(32) NOT NULL,
                table_name      VARCHAR(64) NOT NULL,
                check_type      VARCHAR(16) NOT NULL,
                check_result    VARCHAR(8) NOT NULL,
                expected        BIGINT,
                actual          BIGINT,
                diff            BIGINT,
                checked_at      TIMESTAMP   NOT NULL DEFAULT NOW()
            )
            """);
    }

    public void initImportLog(String batchId, String tableName, String sourceFile) {
        jdbc.update("INSERT INTO ods.ods_import_log (table_name, source_file, status, batch_id) VALUES (?, ?, 'PENDING', ?)",
                tableName, sourceFile, batchId);
    }

    public void markRunning(String tableName, String batchId) {
        jdbc.update("UPDATE ods.ods_import_log SET status='RUNNING', started_at=NOW() WHERE table_name=? AND batch_id=?",
                tableName, batchId);
    }

    public void markSuccess(String tableName, String batchId, long csvRows, long dbRows, boolean match, int durationSec) {
        jdbc.update("UPDATE ods.ods_import_log SET status='SUCCESS', csv_rows=?, db_rows=?, row_match=?, finished_at=NOW(), duration_sec=? WHERE table_name=? AND batch_id=?",
                csvRows, dbRows, match, durationSec, tableName, batchId);
    }

    public void markFailed(String tableName, String batchId, String errorMsg, int durationSec) {
        jdbc.update("UPDATE ods.ods_import_log SET status='FAILED', error_msg=?, finished_at=NOW(), duration_sec=? WHERE table_name=? AND batch_id=?",
                errorMsg, durationSec, tableName, batchId);
    }

    public long countTable(String schema, String tableName) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + schema + "." + tableName, Long.class);
        return count != null ? count : 0;
    }

    public void clearTable(String schema, String tableName, String batchId) {
        jdbc.update("DELETE FROM " + schema + "." + tableName + " WHERE _batch_id = ?", batchId);
    }

    public void insertCheck(String batchId, String tableName, long expected, long actual) {
        long diff = actual - expected;
        String result = (diff == 0) ? "PASS" : "FAIL";
        jdbc.update("INSERT INTO ods.ods_import_check (batch_id, table_name, check_type, check_result, expected, actual, diff) VALUES (?, ?, 'ROW_COUNT', ?, ?, ?, ?)",
                batchId, tableName, result, expected, actual, diff);
    }

    public List<Map<String, Object>> getImportStatus(String batchId) {
        return jdbc.queryForList("SELECT table_name, status, csv_rows, db_rows, row_match, duration_sec, started_at, error_msg FROM ods.ods_import_log WHERE batch_id = ? ORDER BY id", batchId);
    }

    public String getLatestBatchId() {
        List<String> ids = jdbc.queryForList("SELECT DISTINCT batch_id FROM ods.ods_import_log ORDER BY batch_id DESC LIMIT 1", String.class);
        return ids.isEmpty() ? null : ids.get(0);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd E:/pxg_project/maidc-parent && mvn compile -pl maidc-data -q
```

- [ ] **Step 3: 提交**

```bash
cd E:/pxg_project
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/OdsImportRepository.java
git commit -m "feat: add ODS import metadata repository"
```

---

### Task 7: OdsImportService — 核心编排服务

**Files:**
- Create: `maidc-data/src/main/java/com/maidc/data/service/OdsImportService.java`

- [ ] **Step 1: 创建 OdsImportService**

Create `maidc-data/src/main/java/com/maidc/data/service/OdsImportService.java`:

```java
package com.maidc.data.service;

import com.maidc.data.etl.EmbulkConfigGenerator;
import com.maidc.data.etl.EmbulkRunner;
import com.maidc.data.etl.TableMapping;
import com.maidc.data.dto.ImportStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OdsImportService {

    private final OdsImportRepository importRepo;
    private final EmbulkConfigGenerator configGenerator;
    private final EmbulkRunner embulkRunner;

    @Value("${maidc.etl.csv-base-dir}")
    private String csvBaseDir;

    @Value("${maidc.etl.parallel}")
    private int parallel;

    @Value("${maidc.etl.db-schema}")
    private String dbSchema;

    private volatile String currentBatchId;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public String startImport() {
        if (currentBatchId != null) {
            throw new IllegalStateException("Import already running with batch: " + currentBatchId);
        }

        currentBatchId = "batch_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        log.info("Starting ODS import batch: {}", currentBatchId);

        // Ensure metadata tables exist
        importRepo.ensureSchemaAndTables();

        // Get all mappings
        List<TableMapping> mappings = TableMapping.allMappings();

        // Init import log for each table
        for (TableMapping m : mappings) {
            importRepo.initImportLog(currentBatchId, m.getTableName(), m.getCsvFile());
        }

        // Submit tasks to thread pool
        for (TableMapping m : mappings) {
            executor.submit(() -> importOneTable(m, currentBatchId));
        }

        return currentBatchId;
    }

    private void importOneTable(TableMapping mapping, String batchId) {
        String tableName = mapping.getTableName();
        log.info("[{}] Starting import for {}", batchId, tableName);

        importRepo.markRunning(tableName, batchId);
        long startTime = System.currentTimeMillis();

        try {
            // Count CSV rows (wc -l minus header)
            long csvRows = countCsvRows(mapping.getCsvFile());

            // Generate Embulk YAML config
            Path yamlPath = configGenerator.generateYaml(mapping, batchId);

            // Run Embulk (large tables get 120min timeout, small tables 30min)
            long timeoutMin = mapping.isLargeTable() ? 120 : 30;
            boolean success = embulkRunner.run(yamlPath, timeoutMin);

            int durationSec = (int) ((System.currentTimeMillis() - startTime) / 1000);

            if (success) {
                // Count DB rows
                long dbRows = importRepo.countTable(dbSchema, tableName);
                boolean match = (csvRows == dbRows);

                importRepo.markSuccess(tableName, batchId, csvRows, dbRows, match, durationSec);
                importRepo.insertCheck(batchId, tableName, csvRows, dbRows);

                log.info("[{}] {} done: CSV={} DB={} match={} in {}s", batchId, tableName, csvRows, dbRows, match, durationSec);
            } else {
                importRepo.markFailed(tableName, batchId, "Embulk execution failed", durationSec);
                log.error("[{}] {} FAILED after {}s", batchId, tableName, durationSec);
            }

            // Clean up temp YAML
            Files.deleteIfExists(yamlPath);

        } catch (Exception e) {
            int durationSec = (int) ((System.currentTimeMillis() - startTime) / 1000);
            importRepo.markFailed(tableName, batchId, e.getMessage(), durationSec);
            log.error("[{}] {} EXCEPTION: {}", batchId, tableName, e.getMessage(), e);
        }
    }

    /**
     * Count CSV rows by reading line count minus 1 (header).
     */
    private long countCsvRows(String csvRelativePath) throws Exception {
        Path path = Paths.get(csvBaseDir, csvRelativePath);
        if (!Files.exists(path)) {
            // Try without double extension (e.g., PATIENTS.csv/PATIENTS.csv)
            log.warn("CSV file not found at {}, returning 0", path);
            return 0;
        }
        long lines = Files.lines(path).count();
        return Math.max(0, lines - 1); // subtract header
    }

    public ImportStatusVO getStatus() {
        String batchId = currentBatchId != null ? currentBatchId : importRepo.getLatestBatchId();
        ImportStatusVO vo = new ImportStatusVO();
        vo.setBatchId(batchId);

        if (batchId == null) {
            return vo;
        }

        List<Map<String, Object>> rows = importRepo.getImportStatus(batchId);
        int success = 0, failed = 0, running = 0, pending = 0;

        for (Map<String, Object> row : rows) {
            ImportStatusVO.TableStatus ts = new ImportStatusVO.TableStatus();
            ts.setTable((String) row.get("table_name"));
            String status = (String) row.get("status");
            ts.setStatus(status);
            ts.setCsvRows(row.get("csv_rows") != null ? ((Number) row.get("csv_rows")).longValue() : null);
            ts.setDbRows(row.get("db_rows") != null ? ((Number) row.get("db_rows")).longValue() : null);
            ts.setMatch(row.get("row_match") != null ? (Boolean) row.get("row_match") : null);
            ts.setDuration(row.get("duration_sec") != null ? ((Number) row.get("duration_sec")).intValue() : null);
            ts.setError((String) row.get("error_msg"));

            switch (status) {
                case "SUCCESS" -> success++;
                case "FAILED" -> failed++;
                case "RUNNING" -> running++;
                default -> pending++;
            }
            vo.getTables().add(ts);
        }

        vo.setTotal(rows.size());
        vo.setSuccess(success);
        vo.setFailed(failed);
        vo.setRunning(running);
        vo.setPending(pending);
        return vo;
    }

    public void retryTable(String tableName) {
        String batchId = currentBatchId != null ? currentBatchId : importRepo.getLatestBatchId();
        if (batchId == null) throw new IllegalStateException("No import batch found");

        // Find the mapping
        TableMapping mapping = TableMapping.allMappings().stream()
                .filter(m -> m.getTableName().equals(tableName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown table: " + tableName));

        // Clear existing data for this batch
        importRepo.clearTable(dbSchema, tableName, batchId);

        // Re-import
        executor.submit(() -> importOneTable(mapping, batchId));
        log.info("Retrying import for {} in batch {}", tableName, batchId);
    }
}
```

Note: `vo.getTables()` may NPE. Add tables list initialization in ImportStatusVO:

```java
// In ImportStatusVO.java, change:
private List<TableStatus> tables;
// To:
private List<TableStatus> tables = new java.util.ArrayList<>();
```

- [ ] **Step 2: 编译验证**

```bash
cd E:/pxg_project/maidc-parent && mvn compile -pl maidc-data -q
```

- [ ] **Step 3: 提交**

```bash
cd E:/pxg_project
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/OdsImportService.java \
        maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/ImportStatusVO.java
git commit -m "feat: add ODS import service with orchestration, validation, retry"
```

---

### Task 8: EtlImportController — REST API

**Files:**
- Create: `maidc-data/src/main/java/com/maidc/data/controller/EtlImportController.java`

- [ ] **Step 1: 创建 EtlImportController**

Create `maidc-data/src/main/java/com/maidc/data/controller/EtlImportController.java`:

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.dto.ImportStatusVO;
import com.maidc.data.service.OdsImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/etl/import")
@RequiredArgsConstructor
public class EtlImportController {

    private final OdsImportService importService;

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/start")
    public R<Map<String, String>> startImport() {
        String batchId = importService.startImport();
        return R.ok(Map.of("batchId", batchId, "message", "Import started"));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/status")
    public R<ImportStatusVO> getStatus() {
        return R.ok(importService.getStatus());
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping("/retry/{tableName}")
    public R<Map<String, String>> retryTable(@PathVariable String tableName) {
        importService.retryTable(tableName);
        return R.ok(Map.of("table", tableName, "message", "Retry started"));
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd E:/pxg_project/maidc-parent && mvn compile -pl maidc-data -q
```

- [ ] **Step 3: 提交**

```bash
cd E:/pxg_project
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/EtlImportController.java
git commit -m "feat: add ETL import REST API controller"
```

---

### Task 9: ODS Schema 初始化 — 执行 DDL 建表

**Files:** None new — execute existing SQL files

- [ ] **Step 1: 创建 ODS schema + 57 张表**

```bash
docker exec -i maidc-postgres psql -U maidc -d maidc -c "CREATE SCHEMA IF NOT EXISTS ods;"
docker exec -i maidc-postgres psql -U maidc -d maidc < docker/init-db/07-ods-schema.sql
docker exec -i maidc-postgres psql -U maidc -d maidc < docker/init-db/08-ods-mimic3.sql
docker exec -i maidc-postgres psql -U maidc -d maidc < docker/init-db/09-ods-mimic4.sql
```

- [ ] **Step 2: 验证表创建**

```bash
docker exec -i maidc-postgres psql -U maidc -d maidc -c "SELECT tablename FROM pg_tables WHERE schemaname='ods' ORDER BY tablename;"
```

Expected: 57+ rows (26 o3_* + 31 o4_* + partition tables + metadata tables)

---

### Task 10: 端到端测试 — 重启服务 + 导入一张小表验证

- [ ] **Step 1: 重启 maidc-data 服务**

```bash
# Kill existing
taskkill //PID $(netstat -ano | grep ":8082 " | head -1 | awk '{print $5}') //F
# Rebuild and start
cd E:/pxg_project/maidc-parent/maidc-data && mvn spring-boot:run -Dspring-boot.run.profiles=dev &
```

- [ ] **Step 2: 调用 API 触发导入**

```bash
curl -X POST http://localhost:8082/api/v1/etl/import/status
```

Verify: returns batch ID and table list.

- [ ] **Step 3: 检查导入结果**

```bash
# Wait a few minutes, then check status
curl http://localhost:8082/api/v1/etl/import/status | python3 -m json.tool

# Check DB directly for a small table
docker exec -i maidc-postgres psql -U maidc -d maidc -c "SELECT COUNT(*) FROM ods.o3_d_labitems;"
docker exec -i maidc-postgres psql -U maidc -d maidc -c "SELECT * FROM ods.ods_import_log LIMIT 5;"
docker exec -i maidc-postgres psql -U maidc -d maidc -c "SELECT * FROM ods.ods_import_check WHERE check_result='FAIL';"
```

Expected: import_log shows SUCCESS for completed tables, import_check shows PASS.

- [ ] **Step 4: 提交全部代码（如有修复）**

```bash
cd E:/pxg_project
git add -A maidc-parent/maidc-data/
git commit -m "fix: post-integration fixes for ODS import"
```
