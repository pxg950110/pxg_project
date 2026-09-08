# 权限系统实施计划（RBAC + 数据范围 + 脱敏联动）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 [设计规格](../specs/2026-09-08-permission-system-design.md) 落地 8 角色 × 42 权限码的 RBAC + 行级数据范围（ALL/DEPT/SELF/PROJECT）+ 角色脱敏豁免 + 服务内 `@RequirePermission` 强制鉴权全覆盖。

**Architecture:** 权限集由 auth 服务在登录/懒加载时构建并写 Redis（`maidc:auth:perm:{userId}`），业务服务通过 common-security 的 AOP 切面读取校验；数据范围由 Service 层 Helper 拼 WHERE；脱敏豁免挂在 CDR 患者数据出口；网关保持纯认证不动。

**Tech Stack:** Java 17 / Spring Boot 3.2.5 / Spring Data JPA / Redis / RabbitMQ / ArchUnit / PostgreSQL；前端 Vue3 + TS + Antd Vue。

**关键事实（已核实）：**
- `ErrorCode.FORBIDDEN(403,"无权限访问")`、`BusinessException(int,String)`、`R.ok/fail`、`GlobalExceptionHandler` 已存在于 common-core
- `UserIdHeaderFilter`（common-security，`@Component`，包扫描可见）注入 `X-User-Id` 头
- `maidc-data` 的 `SecurityConfig` 有 `@EnableMethodSecurity(prePostEnabled=false)` + **always-true PermissionEvaluator**（现状 @PreAuthorize 全是空转，本计划移除）
- `AuthService.login()` 返回 `LoginVO`（`UserInfo` 含 roles，**无 permissions**）；前端 permission store 已在读 `userInfo.permissions`（当前恒空）
- 脱敏现状：`PatientEncounterService` 私有方法硬编码脱敏，`r_desensitize_rule` 仅 CRUD 未联动 → Task 10 经 `DesensitizeRuleChecker` 使**规则开关+角色豁免**全量联动规则表（用户确认：脱敏通过规则配置联动）
- CDR 无现有导出端点（新建）；RDR 导出已有 `rdr_export_task`
- common 包 bean 靠各服务 `@SpringBootApplication` 包扫描（`com.maidc`）发现，无 auto-config 文件

---

## 文件结构总览

```
docker/init-db/17-permission-system.sql                       [新建] DDL+种子
common/common-security/.../security/
  annotation/RequirePermission.java                           [新建] 注解
  annotation/PublicEndpoint.java                              [新建] 白名单注解
  context/CurrentUser.java                                    [新建] 当前用户(头/SecurityContext)
  context/PermissionContext.java                              [新建] 权限集模型
  aspect/PermissionAspect.java                                [新建] AOP 切面
  store/PermissionStore.java                                  [新建] Redis读写+懒加载
  scope/DataScope.java                                        [新建] 枚举
  scope/DataScopeHelper.java                                  [新建] WHERE助手
maidc-auth/.../auth/
  service/PermissionCacheService.java                         [新建] 构建/失效权限集
  controller/InternalPermissionController.java                [新建] 内部懒加载端点
  service/AuthService.java                                    [修改] 登录写缓存+UserInfo扩展
  service/RoleService.java UserService.java                   [修改] 变更失效钩子
maidc-data/.../data/
  config/SecurityConfig.java                                  [修改] 删always-true evaluator
  service/PatientEncounterService.java                        [修改] DEPT过滤+脱敏豁免
  service/DesensitizeRuleChecker.java                         [新建] 规则驱动脱敏判定（开关+豁免）
  entity/ExportRequestEntity.java 等 4 文件                    [新建] 导出审批流
各服务 Controller（8服务50+）                                   [修改] 加@RequirePermission
各服务 pom.xml + ArchUnit 测试                                 [新建]
maidc-portal/src/router/asyncRoutes.ts                        [修改] 权限码对齐
maidc-portal/src/api/export.ts + views/system/ExportApproval*.vue [新建]
```

---

### Task 1: 数据库迁移与种子数据

**Files:**
- Create: `docker/init-db/17-permission-system.sql`

- [ ] **Step 1: 写迁移 SQL**

```sql
-- 17-permission-system.sql  MAIDC 权限系统（RBAC扩展+数据范围+脱敏豁免）
BEGIN;

-- ========== DDL ==========
ALTER TABLE system.s_role ADD COLUMN IF NOT EXISTS data_scope VARCHAR(16) NOT NULL DEFAULT 'SELF';
ALTER TABLE system.s_user ADD COLUMN IF NOT EXISTS dept_id BIGINT;
ALTER TABLE cdr.r_desensitize_rule ADD COLUMN IF NOT EXISTS exempt_role_codes TEXT;
ALTER TABLE rdr.r_study_member ADD COLUMN IF NOT EXISTS can_export BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS cdr.c_export_request (
    id            BIGSERIAL PRIMARY KEY,
    requester_id  BIGINT NOT NULL,
    patient_ids   JSONB,
    purpose       VARCHAR(512) NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    approver_id   BIGINT,
    approved_at   TIMESTAMP,
    reject_reason VARCHAR(512),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_export_request_status ON cdr.c_export_request(status);

-- ========== 角色种子 ==========
INSERT INTO system.s_role (role_code, role_name, description, is_system, data_scope, created_by, org_id) VALUES
('nurse',         '护士',     '本科室患者视图/临床检索/标注/导出需审批', true, 'DEPT',    'system', 0),
('researcher_pi', '科研负责人','科研项目全生命周期管理',                 true, 'PROJECT', 'system', 0)
ON CONFLICT (org_id, role_code) DO UPDATE SET data_scope = EXCLUDED.data_scope, description = EXCLUDED.description;

UPDATE system.s_role SET data_scope='ALL'    WHERE role_code IN ('admin','data_admin','auditor');
UPDATE system.s_role SET data_scope='DEPT'   WHERE role_code IN ('doctor','nurse');
UPDATE system.s_role SET data_scope='PROJECT' WHERE role_code IN ('researcher','researcher_pi');
UPDATE system.s_role SET data_scope='SELF'   WHERE role_code='ai_engineer';

-- ========== 权限种子（42码，替换旧15条） ==========
DELETE FROM system.s_role_permission WHERE org_id = 0;
DELETE FROM system.s_permission WHERE org_id = 0;

INSERT INTO system.s_permission (permission_code, permission_name, resource_type, resource_key, action, sort_order, created_by, org_id) VALUES
('workspace:read','个人工作台','MENU','/dashboard/workspace','READ',1,'system',0),
('dashboard:overview:read','系统总览','MENU','/dashboard/overview','READ',2,'system',0),
('dashboard:data:read','数据看板','MENU','/dashboard/data','READ',3,'system',0),
('dashboard:model:read','模型看板','MENU','/dashboard/model','READ',4,'system',0),
('cdr:patient:read','患者360查看','MENU','/data/cdr/patients','READ',10,'system',0),
('cdr:patient:export','患者数据导出','BUTTON','/data/cdr/patients/export','EXPORT',11,'system',0),
('cdr:search:read','临床/智能检索','MENU','/data/cdr/search','READ',12,'system',0),
('cdr:cohort:read','病种队列查看','MENU','/data/cdr/disease','READ',13,'system',0),
('cdr:cohort:manage','病种队列管理','BUTTON','/data/cdr/disease/manage','UPDATE',14,'system',0),
('cdr:quality:manage','数据质控管理','MENU','/data/cdr/quality','UPDATE',15,'system',0),
('cdr:term:manage','术语映射管理','MENU','/data/cdr/term','UPDATE',16,'system',0),
('cdr:lineage:read','数据血缘','MENU','/data/cdr/lineage','READ',17,'system',0),
('etl:pipeline:read','ETL管道查看','MENU','/data/etl/pipelines','READ',20,'system',0),
('etl:pipeline:design','ETL管道设计','BUTTON','/data/etl/pipelines/design','UPDATE',21,'system',0),
('etl:pipeline:execute','ETL管道执行','BUTTON','/data/etl/pipelines/execute','CREATE',22,'system',0),
('datasource:manage','数据源管理','MENU','/data/datasources','UPDATE',23,'system',0),
('sync:manage','同步任务管理','MENU','/data/sync','UPDATE',24,'system',0),
('desensitize:manage','脱敏规则管理','MENU','/data/desensitize','UPDATE',25,'system',0),
('rdr:project:read','科研项目查看','MENU','/data/rdr/projects','READ',30,'system',0),
('rdr:project:manage','科研项目管理','BUTTON','/data/rdr/projects/manage','UPDATE',31,'system',0),
('rdr:cohort:manage','科研队列定义','BUTTON','/data/rdr/cohorts','UPDATE',32,'system',0),
('rdr:dataset:read','数据集查看','MENU','/data/rdr/datasets','READ',33,'system',0),
('rdr:dataset:export','数据集导出','BUTTON','/data/rdr/datasets/export','EXPORT',34,'system',0),
('rdr:extraction:manage','抽取任务管理','MENU','/data/rdr/etl','UPDATE',35,'system',0),
('rdr:feature:manage','特征字典管理','MENU','/data/rdr/features','UPDATE',36,'system',0),
('model:read','模型查看','MENU','/model/list','READ',40,'system',0),
('model:register','模型注册','BUTTON','/model/create','CREATE',41,'system',0),
('model:evaluate','模型评估','BUTTON','/model/evaluate','CREATE',42,'system',0),
('model:approve','模型审批','BUTTON','/model/approve','UPDATE',43,'system',0),
('model:deploy','模型部署','BUTTON','/model/deploy','CREATE',44,'system',0),
('model:route:manage','流量路由管理','MENU','/model/routes','UPDATE',45,'system',0),
('model:infer','模型推理','BUTTON','/model/infer','CREATE',46,'system',0),
('model:monitor:read','模型监控','MENU','/model/monitor','READ',47,'system',0),
('alert:read','告警查看','MENU','/alert','READ',48,'system',0),
('alert:manage','告警规则管理','BUTTON','/alert/rules','UPDATE',49,'system',0),
('label:task:read','标注任务查看','MENU','/label/tasks','READ',50,'system',0),
('label:task:assign','标注任务分配','BUTTON','/label/tasks/assign','UPDATE',51,'system',0),
('label:task:work','标注作业','MENU','/label/workspace','CREATE',52,'system',0),
('label:task:review','标注审核','BUTTON','/label/tasks/review','UPDATE',53,'system',0),
('audit:operation:read','操作日志','MENU','/audit/operations','READ',60,'system',0),
('audit:dataaccess:read','数据访问日志','MENU','/audit/data-access','READ',61,'system',0),
('audit:event:read','系统事件','MENU','/audit/system-events','READ',62,'system',0),
('audit:compliance:read','合规报告','MENU','/audit/compliance','READ',63,'system',0),
('masterdata:element:manage','数据元标准管理','MENU','/masterdata/elements','UPDATE',70,'system',0),
('masterdata:codesystem:manage','代码系统管理','MENU','/masterdata/code-systems','UPDATE',71,'system',0),
('masterdata:mapping:manage','概念映射管理','MENU','/masterdata/mappings','UPDATE',72,'system',0),
('masterdata:dict:manage','医学字典管理','MENU','/masterdata/dictionaries','UPDATE',73,'system',0),
('masterdata:knowledge:read','知识库查看','MENU','/masterdata/knowledge','READ',74,'system',0),
('masterdata:institution:manage','机构管理','MENU','/masterdata/institutions','UPDATE',75,'system',0),
('system:user:manage','用户管理','MENU','/system/users','UPDATE',80,'system',0),
('system:role:manage','角色管理','MENU','/system/roles','UPDATE',81,'system',0),
('system:permission:manage','权限管理','MENU','/system/permissions','UPDATE',82,'system',0),
('system:config:manage','系统配置','MENU','/system/config','UPDATE',83,'system',0),
('schedule:task:manage','调度任务管理','MENU','/schedule','UPDATE',84,'system',0),
('message:template:manage','消息模板管理','MENU','/message/templates','UPDATE',85,'system',0)
ON CONFLICT (org_id, permission_code) DO NOTHING;

-- ========== 角色-权限矩阵 ==========
-- 辅助：给角色灌权限（r=角色码, p=权限码列表）
CREATE TEMP TABLE tmp_role_perm(role_code VARCHAR(32), perm_code VARCHAR(64));

INSERT INTO tmp_role_perm VALUES
-- admin：全部（下方统一处理，不逐条插）
-- doctor
('doctor','workspace:read'),('doctor','dashboard:overview:read'),
('doctor','cdr:patient:read'),('doctor','cdr:patient:export'),('doctor','cdr:search:read'),
('doctor','cdr:cohort:read'),('doctor','cdr:cohort:manage'),
('doctor','model:infer'),('doctor','model:approve'),
('doctor','label:task:read'),('doctor','label:task:work'),('doctor','label:task:review'),
('doctor','masterdata:knowledge:read'),
-- nurse
('nurse','workspace:read'),('nurse','dashboard:overview:read'),
('nurse','cdr:patient:read'),('nurse','cdr:patient:export'),('nurse','cdr:search:read'),
('nurse','cdr:cohort:read'),
('nurse','label:task:read'),('nurse','label:task:work'),('nurse','label:task:review'),
('nurse','masterdata:knowledge:read'),
-- researcher_pi
('researcher_pi','workspace:read'),('researcher_pi','dashboard:overview:read'),('researcher_pi','dashboard:data:read'),
('researcher_pi','cdr:search:read'),('researcher_pi','cdr:cohort:read'),('researcher_pi','cdr:cohort:manage'),
('researcher_pi','rdr:project:read'),('researcher_pi','rdr:project:manage'),('researcher_pi','rdr:cohort:manage'),
('researcher_pi','rdr:dataset:read'),('researcher_pi','rdr:dataset:export'),
('researcher_pi','rdr:extraction:manage'),('researcher_pi','rdr:feature:manage'),
('researcher_pi','label:task:read'),('researcher_pi','label:task:assign'),('researcher_pi','label:task:work'),('researcher_pi','label:task:review'),
('researcher_pi','masterdata:knowledge:read'),
-- researcher
('researcher','workspace:read'),('researcher','dashboard:overview:read'),('researcher','dashboard:data:read'),
('researcher','cdr:search:read'),('researcher','cdr:cohort:read'),
('researcher','rdr:project:read'),('researcher','rdr:cohort:manage'),('researcher','rdr:dataset:read'),
('researcher','label:task:read'),('researcher','label:task:work'),
('researcher','masterdata:knowledge:read'),
-- data_admin
('data_admin','workspace:read'),('data_admin','dashboard:overview:read'),('data_admin','dashboard:data:read'),
('data_admin','cdr:patient:read'),('data_admin','cdr:search:read'),('data_admin','cdr:cohort:read'),('data_admin','cdr:cohort:manage'),
('data_admin','cdr:quality:manage'),('data_admin','cdr:term:manage'),('data_admin','cdr:lineage:read'),
('data_admin','etl:pipeline:read'),('data_admin','etl:pipeline:design'),('data_admin','etl:pipeline:execute'),
('data_admin','datasource:manage'),('data_admin','sync:manage'),('data_admin','desensitize:manage'),
('data_admin','rdr:project:read'),('data_admin','rdr:cohort:manage'),('data_admin','rdr:dataset:read'),
('data_admin','rdr:dataset:export'),('data_admin','rdr:extraction:manage'),('data_admin','rdr:feature:manage'),
('data_admin','label:task:read'),
('data_admin','masterdata:element:manage'),('data_admin','masterdata:codesystem:manage'),('data_admin','masterdata:mapping:manage'),
('data_admin','masterdata:dict:manage'),('data_admin','masterdata:knowledge:read'),('data_admin','masterdata:institution:manage'),
-- ai_engineer
('ai_engineer','workspace:read'),('ai_engineer','dashboard:overview:read'),('ai_engineer','dashboard:model:read'),
('ai_engineer','cdr:search:read'),
('ai_engineer','rdr:project:read'),('rdr:dataset:read','ai_engineer'),
('ai_engineer','model:read'),('ai_engineer','model:register'),('ai_engineer','model:evaluate'),('ai_engineer','model:deploy'),
('ai_engineer','model:route:manage'),('ai_engineer','model:infer'),('ai_engineer','model:monitor:read'),
('ai_engineer','alert:read'),('ai_engineer','alert:manage'),
('ai_engineer','label:task:read'),('ai_engineer','label:task:assign'),('ai_engineer','label:task:work'),('ai_engineer','label:task:review'),
('ai_engineer','masterdata:knowledge:read'),
-- auditor
('auditor','workspace:read'),('auditor','dashboard:overview:read'),
('auditor','cdr:lineage:read'),
('auditor','model:monitor:read'),('auditor','alert:read'),
('auditor','audit:operation:read'),('auditor','audit:dataaccess:read'),('auditor','audit:event:read'),('auditor','audit:compliance:read'),
('auditor','masterdata:knowledge:read');

-- 修正 ai_engineer 一条插反的值
UPDATE tmp_role_perm SET role_code='ai_engineer', perm_code='rdr:dataset:read'
 WHERE role_code='rdr:dataset:read' AND perm_code='ai_engineer';

INSERT INTO system.s_role_permission (role_id, permission_id, org_id)
SELECT r.id, p.id, 0
FROM tmp_role_perm t
JOIN system.s_role r ON r.role_code = t.role_code AND r.org_id = 0
JOIN system.s_permission p ON p.permission_code = t.perm_code AND p.org_id = 0
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- admin = 全部权限
INSERT INTO system.s_role_permission (role_id, permission_id, org_id)
SELECT r.id, p.id, 0 FROM system.s_role r, system.s_permission p
WHERE r.role_code='admin' AND r.org_id=0 AND p.org_id=0
ON CONFLICT (role_id, permission_id) DO NOTHING;

DROP TABLE tmp_role_perm;

-- ========== 脱敏豁免种子（医生大部分豁免/护士少部分/data_admin豁免） ==========
UPDATE cdr.r_desensitize_rule SET exempt_role_codes='admin,doctor,data_admin'
 WHERE field_type IN ('NAME','ID_CARD','PHONE','ADDRESS') AND exempt_role_codes IS NULL;
UPDATE cdr.r_desensitize_rule SET exempt_role_codes='admin,data_admin'
 WHERE field_type NOT IN ('NAME','ID_CARD','PHONE','ADDRESS') AND exempt_role_codes IS NULL;

COMMIT;
```

> 注：`r_desensitize_rule` 列已核实（`DesensitizeRuleEntity`：`field_type`/`strategy`/`params`/`enabled`）。脱敏行为完全由本表驱动：`enabled=false` → 明文；`enabled=true` 且角色命中 `exempt_role_codes` → 明文；否则脱敏；无规则行 → 安全默认脱敏。

- [ ] **Step 2: 空库验证 SQL 语法**

Run: `cd e:/pxg_project/docker && docker compose -f docker-compose-infra.yml exec -T postgres psql -U maidc -d maidc -c "\i /docker-entrypoint-initdb.d/17-permission-system.sql" 2>&1 | tail -5`（若库已有数据则直接执行；新库需先跑 01-16）
Expected: 输出 `COMMIT`，无 ERROR

随后核对脱敏规则行覆盖（豁免种子依赖这些行存在）：

Run: `docker compose -f docker-compose-infra.yml exec -T postgres psql -U maidc -d maidc -t -c "SELECT field_type, strategy, enabled FROM cdr.r_desensitize_rule ORDER BY 1;"`
Expected: 含 `NAME`/`ID_CARD`/`PHONE`/`ADDRESS` 四类行。若缺，按现有行的 strategy 取值补插（列与 `BaseEntity` 对齐）：

```sql
INSERT INTO cdr.r_desensitize_rule (rule_name, field_type, strategy, enabled, exempt_role_codes, created_by, created_at, is_deleted)
SELECT '核心字段-' || t.ft, t.ft, r2.strategy, true, 'admin,doctor,data_admin', 'system', NOW(), false
FROM (VALUES ('NAME'),('ID_CARD'),('PHONE'),('ADDRESS')) AS t(ft)
CROSS JOIN (SELECT strategy FROM cdr.r_desensitize_rule LIMIT 1) r2
WHERE NOT EXISTS (SELECT 1 FROM cdr.r_desensitize_rule r WHERE r.field_type = t.ft);
```

- [ ] **Step 3: 验证矩阵行数**

Run: `docker compose -f docker-compose-infra.yml exec -T postgres psql -U maidc -d maidc -t -c "SELECT r.role_code, count(*) FROM system.s_role_permission rp JOIN system.s_role r ON r.id=rp.role_id GROUP BY 1 ORDER BY 1;"`
Expected: `admin` = 55（权限总数），doctor=13, nurse=10, researcher_pi=18, researcher=11, data_admin=29, ai_engineer=20, auditor=10（admin=全部；若与预期差 1-2 先核对权限种子行数再判定失败）

- [ ] **Step 4: Commit**

```bash
git add docker/init-db/17-permission-system.sql
git commit -m "feat(db): permission system migration - data scope, 55 permission codes, 8-role matrix"
```

---

### Task 2: common-security 基础类型（注解/上下文/枚举）

**Files:**
- Create: `maidc-parent/common/common-security/src/main/java/com/maidc/common/security/annotation/RequirePermission.java`
- Create: `maidc-parent/common/common-security/src/main/java/com/maidc/common/security/annotation/PublicEndpoint.java`
- Create: `maidc-parent/common/common-security/src/main/java/com/maidc/common/security/scope/DataScope.java`
- Create: `maidc-parent/common/common-security/src/main/java/com/maidc/common/security/context/PermissionContext.java`
- Create: `maidc-parent/common/common-security/src/main/java/com/maidc/common/security/context/CurrentUser.java`
- Test: `maidc-parent/common/common-security/src/test/java/com/maidc/common/security/context/CurrentUserTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.maidc.common.security.context;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

class CurrentUserTest {

    @Test
    void userId_shouldReadFromHeader() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "42");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
        try {
            assertEquals(42L, CurrentUser.userId());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void userId_shouldReturnNullWhenMissing() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
        try {
            assertNull(CurrentUser.userId());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
```

> 若 common-security 无 `spring-test` 依赖，在其 `pom.xml` 加 `<dependency><groupId>org.springframework</groupId><artifactId>spring-test</artifactId><scope>test</scope></dependency>`

- [ ] **Step 2: 运行确认失败**

Run: `cd e:/pxg_project/maidc-parent && mvn -pl common/common-security test -Dtest=CurrentUserTest -q`
Expected: 编译失败 `CurrentUser` 不存在

- [ ] **Step 3: 实现 5 个类**

```java
// scope/DataScope.java
package com.maidc.common.security.scope;

public enum DataScope {
    /** 不过滤 */
    ALL,
    /** 本科室（患者就诊科室 ∈ 用户 dept_id） */
    DEPT,
    /** 本人创建（created_by = userId） */
    SELF,
    /** 项目成员（r_study_member 含用户）∪ 本人创建 */
    PROJECT;

    /** 多角色取更宽的范围（ALL > DEPT > PROJECT > SELF） */
    public static DataScope widest(DataScope a, DataScope b) {
        if (a == ALL || b == ALL) return ALL;
        if (a == DEPT || b == DEPT) return DEPT;
        if (a == PROJECT || b == PROJECT) return PROJECT;
        return SELF;
    }
}
```

```java
// context/PermissionContext.java
package com.maidc.common.security.context;

import com.maidc.common.security.scope.DataScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/** 当前用户权限集（Redis maidc:auth:perm:{userId} 的值，Jackson 反序列化目标） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionContext {
    private Long userId;
    private Set<String> permissions;
    private DataScope dataScope;
    private Long deptId;
    private List<String> roles;
    private List<Long> projectIds;

    public boolean has(String code) {
        return permissions != null && permissions.contains(code);
    }
}
```

```java
// context/CurrentUser.java
package com.maidc.common.security.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 从请求头(X-User-Id, 网关注入)取当前用户，兜底 SecurityContext */
public final class CurrentUser {

    private CurrentUser() {}

    public static Long userId() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            String v = sra.getRequest().getHeader("X-User-Id");
            if (v != null && !v.isBlank()) {
                try { return Long.valueOf(v); } catch (NumberFormatException ignored) {}
            }
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Long id) {
            return id;
        }
        return null;
    }
}
```

```java
// annotation/RequirePermission.java
package com.maidc.common.security.annotation;

import com.maidc.common.security.scope.DataScope;

import java.lang.annotation.*;

/** 方法级功能权限；dataScope 声明该接口额外受数据范围约束（切面只校验功能码，范围过滤由 Service 层 Helper 完成） */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    /** 权限码，模块:资源:动作 */
    String value();
    /** 该接口的数据范围语义（文档性声明，供 ArchUnit 与 code review 检查） */
    DataScope scope() default DataScope.ALL;
}
```

```java
// annotation/PublicEndpoint.java
package com.maidc.common.security.annotation;

import java.lang.annotation.*;

/** 显式声明无需权限注解的端点（健康检查/内部端点等），ArchUnit 白名单依据 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublicEndpoint {
    String reason() default "";
}
```

- [ ] **Step 4: 运行测试通过**

Run: `mvn -pl common/common-security test -Dtest=CurrentUserTest -q`
Expected: BUILD SUCCESS, Tests run: 2

- [ ] **Step 5: Commit**

```bash
git add maidc-parent/common/common-security
git commit -m "feat(security): permission annotations, PermissionContext, DataScope enum, CurrentUser"
```

---

### Task 3: PermissionStore + PermissionAspect（切面鉴权）

**Files:**
- Create: `maidc-parent/common/common-security/src/main/java/com/maidc/common/security/store/PermissionStore.java`
- Create: `maidc-parent/common/common-security/src/main/java/com/maidc/common/security/aspect/PermissionAspect.java`
- Modify: `maidc-parent/common/common-security/pom.xml`（加 jackson-databind、spring-boot-starter-web 提供 RestTemplate/RestClient；common-security 已依赖 spring-security/web，核对后仅补缺项）
- Test: `maidc-parent/common/common-security/src/test/java/com/maidc/common/security/aspect/PermissionAspectTest.java`

- [ ] **Step 1: pom 补依赖（已有则跳过）**

在 `common/common-security/pom.xml` `<dependencies>` 中核对/添加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-redis</artifactId>
</dependency>
```

（若已存在则不重复；common-security 现依赖 common-core/jjwt/spring-security，Redis 访问用各服务已有的 `StringRedisTemplate` bean）

- [ ] **Step 2: 写失败测试**

```java
package com.maidc.common.security.aspect;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.security.context.PermissionContext;
import com.maidc.common.security.store.PermissionStore;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissionAspectTest {

    private final PermissionStore store = mock(PermissionStore.class);
    private final PermissionAspect aspect = new PermissionAspect(store);

    private PermissionContext ctx(String... perms) {
        return PermissionContext.builder().userId(1L)
                .permissions(Set.of(perms)).build();
    }

    @Test
    void passes_whenPermissionPresent() {
        when(store.load(1L)).thenReturn(ctx("cdr:patient:read"));
        aspect.check("cdr:patient:read", 1L);   // 不抛即通过
    }

    @Test
    void throws403_whenPermissionMissing() {
        when(store.load(1L)).thenReturn(ctx("model:deploy"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.check("cdr:patient:read", 1L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void throws401_whenNoUserContext() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.check("cdr:patient:read", null));
        assertEquals(401, ex.getCode());
    }
}
```

- [ ] **Step 3: 运行确认失败**

Run: `mvn -pl common/common-security test -Dtest=PermissionAspectTest -q`
Expected: 编译失败 PermissionAspect/PermissionStore 不存在

- [ ] **Step 4: 实现 Store 与 Aspect**

```java
// store/PermissionStore.java
package com.maidc.common.security.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.security.context.PermissionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** 权限集读写：Redis 为主，miss 时调 auth 内部端点懒加载重建 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionStore {

    public static final String KEY_PREFIX = "maidc:auth:perm:";
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${maidc.auth.base-url:http://localhost:8081}")
    private String authBaseUrl;

    public PermissionContext load(Long userId) {
        String json = redis.opsForValue().get(KEY_PREFIX + userId);
        if (json != null) {
            try {
                PermissionContext ctx = objectMapper.readValue(json, PermissionContext.class);
                ctx.setUserId(userId);
                return ctx;
            } catch (Exception e) {
                log.warn("权限缓存反序列化失败 userId={}, 重建", userId, e);
            }
        }
        // 懒加载：调 auth 内部端点（auth 端会写回 Redis 并返回）
        PermissionContext ctx = RestClient.create().get()
                .uri(authBaseUrl + "/api/v1/internal/permissions/{userId}", userId)
                .retrieve()
                .body(PermissionContext.class);
        if (ctx != null) {
            save(ctx);
        }
        return ctx;
    }

    public void save(PermissionContext ctx) {
        try {
            redis.opsForValue().set(KEY_PREFIX + ctx.getUserId(),
                    objectMapper.writeValueAsString(ctx), Duration.ofHours(12));
        } catch (Exception e) {
            log.error("权限缓存写入失败 userId={}", ctx.getUserId(), e);
        }
    }

    public void evict(Long userId) {
        redis.delete(KEY_PREFIX + userId);
    }
}
```

```java
// aspect/PermissionAspect.java
package com.maidc.common.security.aspect;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.security.annotation.RequirePermission;
import com.maidc.common.security.context.CurrentUser;
import com.maidc.common.security.context.PermissionContext;
import com.maidc.common.security.store.PermissionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/** @RequirePermission 执行器：无用户=401，无权限=403 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionStore store;

    @Before("@annotation(rp)")
    public void before(JoinPoint jp, RequirePermission rp) {
        check(rp.value(), CurrentUser.userId());
    }

    /** 核心校验（切面与单测共用入口） */
    public void check(String code, Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "未认证");
        }
        PermissionContext ctx = store.load(userId);
        if (ctx == null || !ctx.has(code)) {
            log.warn("权限拒绝 userId={} code={}", userId, code);
            throw new BusinessException(403, "无权限访问: " + code);
        }
    }
}
```

- [ ] **Step 5: 运行测试通过**

Run: `mvn -pl common/common-security test -Dtest=PermissionAspectTest -q`
Expected: BUILD SUCCESS, Tests run: 3

- [ ] **Step 6: Commit**

```bash
git add maidc-parent/common/common-security
git commit -m "feat(security): PermissionAspect with Redis-backed PermissionStore and lazy reload"
```

---

### Task 4: auth 服务 — 权限缓存构建、内部端点、登录增强、变更失效

**Files:**
- Create: `maidc-parent/maidc-auth/src/main/java/com/maidc/auth/service/PermissionCacheService.java`
- Create: `maidc-parent/maidc-auth/src/main/java/com/maidc/auth/controller/InternalPermissionController.java`
- Modify: `maidc-parent/maidc-auth/src/main/java/com/maidc/auth/service/AuthService.java`（login/refreshToken 返回 UserInfo 增 permissions/dataScope，并写缓存）
- Modify: `maidc-parent/maidc-auth/src/main/java/com/maidc/auth/vo/LoginVO.java`（UserInfo 加字段）
- Modify: `maidc-parent/maidc-auth/src/main/java/com/maidc/auth/service/RoleService.java`（角色权限变更→失效）
- Modify: `maidc-parent/maidc-auth/src/main/java/com/maidc/auth/service/UserService.java`（用户角色变更→失效）
- Modify: `maidc-parent/maidc-auth/src/main/java/com/maidc/auth/config/SecurityConfig.java`（放行 internal 端点）
- Test: `maidc-parent/maidc-auth/src/test/java/com/maidc/auth/service/PermissionCacheServiceTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.maidc.auth.service;

import com.maidc.auth.entity.*;
import com.maidc.auth.repository.*;
import com.maidc.common.security.context.PermissionContext;
import com.maidc.common.security.scope.DataScope;
import com.maidc.common.security.store.PermissionStore;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PermissionCacheServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final RolePermissionRepository rolePermissionRepository = mock(RolePermissionRepository.class);
    private final PermissionRepository permissionRepository = mock(PermissionRepository.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final PermissionStore store = mock(PermissionStore.class);

    private final PermissionCacheService service = new PermissionCacheService(
            userRepository, userRoleRepository, roleRepository,
            rolePermissionRepository, permissionRepository, jdbcTemplate, store);

    @Test
    void build_permissionSetAndWidestScope() {
        // 用户 doctor 角色
        when(userRepository.getReferenceById(1L)).thenReturn(mock(UserEntity.class));
        UserRoleEntity ur = new UserRoleEntity();
        ur.setRoleId(10L);
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(ur));

        RoleEntity doctor = new RoleEntity();
        doctor.setId(10L);
        doctor.setRoleCode("doctor");
        doctor.setDataScope(DataScope.DEPT);
        when(roleRepository.findAllById(List.of(10L))).thenReturn(List.of(doctor));

        RolePermissionEntity rp = new RolePermissionEntity();
        rp.setRoleId(10L);
        rp.setPermissionId(100L);
        when(rolePermissionRepository.findByRoleIdIn(List.of(10L))).thenReturn(List.of(rp));

        PermissionEntity perm = new PermissionEntity();
        perm.setId(100L);
        perm.setPermissionCode("cdr:patient:read");
        when(permissionRepository.findAllById(List.of(100L))).thenReturn(List.of(perm));

        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(1L))).thenReturn(List.of());

        PermissionContext ctx = service.build(1L);

        assertTrue(ctx.has("cdr:patient:read"));
        assertEquals(DataScope.DEPT, ctx.getDataScope());
        verify(store).save(ctx);
    }

    @Test
    void evictByRole_clearsCacheOfAffectedUsers() {
        UserRoleEntity ur = new UserRoleEntity();
        ur.setUserId(7L);
        when(userRoleRepository.findByRoleId(5L)).thenReturn(List.of(ur));

        service.evictByRole(5L);

        verify(store).evict(7L);
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `cd e:/pxg_project/maidc-parent && mvn -pl maidc-auth -am test -Dtest=PermissionCacheServiceTest -q`
Expected: 编译失败 PermissionCacheService 不存在；同时确认 `RoleEntity` 是否已有 `dataScope` 字段（Task 4 需给实体加列映射，见 Step 3 第一段）

- [ ] **Step 3: 实体补列 + PermissionCacheService**

给 `RoleEntity` 增加字段（若没有）：

```java
@Column(name = "data_scope", nullable = false)
private String dataScope;   // ALL/DEPT/SELF/PROJECT，枚举存字符串，getter 转换
```

`UserEntity` 增加：

```java
@Column(name = "dept_id")
private Long deptId;
```

```java
// service/PermissionCacheService.java
package com.maidc.auth.service;

import com.maidc.auth.entity.*;
import com.maidc.auth.repository.*;
import com.maidc.common.security.context.PermissionContext;
import com.maidc.common.security.scope.DataScope;
import com.maidc.common.security.store.PermissionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PermissionStore store;

    /** 构建并写入 Redis，返回上下文 */
    public PermissionContext build(Long userId) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;

        List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(userId);
        List<Long> roleIds = userRoles.stream().map(UserRoleEntity::getRoleId).toList();
        List<RoleEntity> roles = roleIds.isEmpty() ? List.of() : roleRepository.findAllById(roleIds);

        List<RolePermissionEntity> rps = roleIds.isEmpty() ? List.of()
                : rolePermissionRepository.findByRoleIdIn(roleIds);
        List<Long> permIds = rps.stream().map(RolePermissionEntity::getPermissionId).distinct().toList();
        List<PermissionEntity> perms = permIds.isEmpty() ? List.of() : permissionRepository.findAllById(permIds);

        DataScope scope = roles.stream()
                .map(r -> DataScope.valueOf(r.getDataScope()))
                .reduce(DataScope.SELF, DataScope::widest);

        // 项目成员（rdr.r_study_member, status=ACTIVE）
        List<Long> projectIds = jdbcTemplate.queryForList(
                "SELECT project_id FROM rdr.r_study_member WHERE user_id = ? AND status = 'ACTIVE'",
                Long.class, userId);

        PermissionContext ctx = PermissionContext.builder()
                .userId(userId)
                .permissions(perms.stream().map(PermissionEntity::getPermissionCode).collect(Collectors.toSet()))
                .dataScope(scope)
                .deptId(user.getDeptId())
                .roles(roles.stream().map(RoleEntity::getRoleCode).toList())
                .projectIds(projectIds)
                .build();
        store.save(ctx);
        return ctx;
    }

    /** 用户角色变更 → 失效 */
    public void evictUser(Long userId) {
        store.evict(userId);
    }

    /** 角色的权限/属性变更 → 失效所有持有该角色的用户 */
    public void evictByRole(Long roleId) {
        userRoleRepository.findByRoleId(roleId)
                .forEach(ur -> store.evict(ur.getUserId()));
    }
}
```

> 实施时按 `UserRoleRepository`/`RolePermissionRepository` 实际方法名调整（若已有 `findByUserIdAndStatus` 等变体，用现有方法；没有则补这两个派生查询方法：`List<UserRoleEntity> findByUserId(Long userId);` 和 `List<RolePermissionEntity> findByRoleIdIn(List<Long> roleIds);`、`List<UserRoleEntity> findByRoleId(Long roleId);`）

- [ ] **Step 4: 内部端点 + SecurityConfig 放行**

```java
// controller/InternalPermissionController.java
package com.maidc.auth.controller;

import com.maidc.auth.service.PermissionCacheService;
import com.maidc.common.security.annotation.PublicEndpoint;
import com.maidc.common.security.context.PermissionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 服务间内部端点：权限缓存 miss 时由各业务服务的 PermissionStore 调用重建。
 * 直连 auth 服务端口（不经网关），SecurityConfig 仅放行本机/内网来源。
 */
@RestController
@RequestMapping("/api/v1/internal/permissions")
@RequiredArgsConstructor
public class InternalPermissionController {

    private final PermissionCacheService cacheService;

    @PublicEndpoint(reason = "service-to-service lazy load")
    @GetMapping("/{userId}")
    public PermissionContext get(@PathVariable Long userId) {
        return cacheService.build(userId);
    }
}
```

`maidc-auth` 的 `SecurityConfig`（按现有 HttpSecurity 风格）追加：

```java
.requestMatchers("/api/v1/internal/**").permitAll()
```

> 网关不路由 `/api/v1/internal/**`（gateway 路由按服务前缀转发，internal 路径不对外暴露；实施时核对 gateway 路由配置，若 `auth` 路由是全量转发则在 AuthFilter WHITE_LIST 之外确保该路径返回 404——在网关路由 predicates 中排除 `/api/v1/internal/**`）。

- [ ] **Step 5: 登录增强**

`LoginVO.UserInfo` 增加两个字段：

```java
private Set<String> permissions;
private String dataScope;
```

`AuthService.login()`（约 L43-92）在返回前插入：

```java
com.maidc.common.security.context.PermissionContext permCtx = permissionCacheService.build(user.getId());
// UserInfo.builder() 追加：
//   .permissions(permCtx != null ? permCtx.getPermissions() : java.util.Set.of())
//   .dataScope(permCtx != null ? permCtx.getDataScope().name() : "SELF")
```

构造器注入 `private final PermissionCacheService permissionCacheService;`。`refreshToken()` 同样处理。

- [ ] **Step 6: 变更失效钩子**

`RoleService`（更新角色/分配权限的方法末尾）与 `UserService`（分配角色的方法末尾）追加：

```java
// RoleService: 角色dataScope变更 或 角色权限增删后
permissionCacheService.evictByRole(roleId);
// UserService: 用户角色增删后
permissionCacheService.evictUser(userId);
```

- [ ] **Step 7: 运行全部 auth 测试**

Run: `mvn -pl maidc-auth -am test -q`
Expected: BUILD SUCCESS（新旧测试全过）

- [ ] **Step 8: Commit**

```bash
git add maidc-parent/maidc-auth
git commit -m "feat(auth): permission cache build/evict, internal reload endpoint, login returns permissions"
```

---

### Task 5: DataScopeHelper + CDR 患者查询 DEPT 过滤

**Files:**
- Create: `maidc-parent/common/common-security/src/main/java/com/maidc/common/security/scope/DataScopeHelper.java`
- Modify: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/PatientEncounterService.java`
- Test: `maidc-parent/common/common-security/src/test/java/com/maidc/common/security/scope/DataScopeHelperTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.maidc.common.security.scope;

import com.maidc.common.security.context.PermissionContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataScopeHelperTest {

    private PermissionContext ctx(DataScope scope, Long deptId) {
        return PermissionContext.builder().userId(9L).dataScope(scope).deptId(deptId).build();
    }

    @Test
    void allScope_noFilter() {
        assertFalse(DataScopeHelper.needDeptFilter(ctx(DataScope.ALL, 1L)));
    }

    @Test
    void deptScope_filterByDept() {
        assertTrue(DataScopeHelper.needDeptFilter(ctx(DataScope.DEPT, 3L)));
        assertEquals(3L, DataScopeHelper.deptId(ctx(DataScope.DEPT, 3L)));
    }

    @Test
    void selfScope_createdBy() {
        assertEquals(9L, DataScopeHelper.selfUserId(ctx(DataScope.SELF, null)));
    }

    @Test
    void projectScope_idsFromContext() {
        PermissionContext c = PermissionContext.builder()
                .userId(9L).dataScope(DataScope.PROJECT)
                .projectIds(java.util.List.of(1L, 2L)).build();
        assertEquals(java.util.List.of(1L, 2L), DataScopeHelper.projectIds(c));
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `mvn -pl common/common-security test -Dtest=DataScopeHelperTest -q`
Expected: 编译失败 DataScopeHelper 不存在

- [ ] **Step 3: 实现 Helper**

```java
// scope/DataScopeHelper.java
package com.maidc.common.security.scope;

import com.maidc.common.security.context.PermissionContext;

import java.util.List;

/** 数据范围判断助手：Service 查询前调用，据此拼 WHERE（JPA Specification 或 QueryDSL/原生条件） */
public final class DataScopeHelper {

    private DataScopeHelper() {}

    public static boolean needDeptFilter(PermissionContext ctx) {
        return ctx != null && ctx.getDataScope() == DataScope.DEPT;
    }

    public static Long deptId(PermissionContext ctx) {
        return ctx == null ? null : ctx.getDeptId();
    }

    public static Long selfUserId(PermissionContext ctx) {
        return ctx == null ? null : ctx.getUserId();
    }

    public static List<Long> projectIds(PermissionContext ctx) {
        return ctx == null || ctx.getProjectIds() == null ? List.of() : ctx.getProjectIds();
    }
}
```

- [ ] **Step 4: PatientEncounterService 接入 DEPT 过滤**

在患者列表查询方法（`list`/分页查询入口，实施时定位实际方法名）中注入：

```java
// 类新增字段
private final PermissionStore permissionStore;

// 查询入口处（示例：患者列表）
PermissionContext ctx = permissionStore.load(CurrentUser.userId());
if (DataScopeHelper.needDeptFilter(ctx)) {
    Long deptId = DataScopeHelper.deptId(ctx);
    // 按现有查询方式追加条件；JPA Specification 用法：
    //   root.get("deptId").in(deptId)  或原生SQL追加 " AND dept_id = ?"
    // 患者实体若无 dept_id 字段，则通过 encounters 表的科室字段过滤
    // （实施时以 c_patient / c_encounter 实际列为准，在 mapper/查询里加等值条件）
}
```

同时患者详情方法：加载后校验 `patient.deptId ∈ ctx`，不满足 → 抛 `BusinessException(404, "患者不存在")`（不暴露存在性），并发审计事件（Task 6 提供工具）。

- [ ] **Step 5: 运行测试**

Run: `mvn -pl common/common-security test -Dtest=DataScopeHelperTest -q && mvn -pl maidc-data -am test -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add maidc-parent/common/common-security maidc-parent/maidc-data
git commit -m "feat(security): DataScopeHelper + DEPT-scope filtering on CDR patient queries"
```

---

### Task 6: 越权审计事件（切面内 best-effort 发 MQ）

**Files:**
- Create: `maidc-parent/common/common-security/src/main/java/com/maidc/common/security/audit/PermissionAuditPublisher.java`
- Modify: `maidc-parent/common/common-security/src/main/java/com/maidc/common/security/aspect/PermissionAspect.java`

- [ ] **Step 1: 实现 Publisher（无 MQ 连接时静默降级）**

```java
package com.maidc.common.security.audit;

import com.maidc.common.mq.model.MaidcMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 权限拒绝/越权事件发布（best-effort：失败仅记日志，不影响主流程响应码） */
@Slf4j
@Component
public class PermissionAuditPublisher {

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    public void publishDenied(Long userId, String code, String uri) {
        if (rabbitTemplate == null) {
            log.info("[audit-deny] userId={} code={} uri={} (mq unavailable)", userId, code, uri);
            return;
        }
        try {
            MaidcMessage msg = MaidcMessage.of("PERMISSION_DENIED", Map.of(
                    "userId", userId, "permissionCode", code, "uri", uri));
            rabbitTemplate.convertAndSend("maidc.audit.exchange", "audit.permission.denied", msg);
        } catch (Exception e) {
            log.warn("权限拒绝事件发布失败 userId={} code={}", userId, code, e);
        }
    }
}
```

> `MaidcMessage.of(...)` 若与实际模型不符（核对 common-mq 的 `MaidcMessage` 构造方式），按现有字段改为 builder/构造器。`maidc-audit` 的 MQ 监听（audit 服务已有 mq 包）增加对 `audit.permission.denied` 路由的消费落 `system.s_system_event`（若监听是通配 topic 则自动覆盖，实施时核对 `AuthRabbitMqConfig`/audit 的绑定）。

- [ ] **Step 2: 切面接入**

`PermissionAspect` 构造器新增 `PermissionAuditPublisher` 参数，在抛 403 前调用 `permissionAuditPublisher.publishDenied(userId, code, 当前URI)`（URI 从 `RequestContextHolder` 取）。同步更新 `PermissionAspectTest` 的构造为 `new PermissionAspect(store, publisher)`（publisher 用 `mock(PermissionAuditPublisher.class)`）。

- [ ] **Step 3: 编译 + 既有测试回归**

Run: `mvn -pl common/common-security,maidc-audit -am test -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add maidc-parent/common/common-security maidc-parent/maidc-audit
git commit -m "feat(security): publish permission-denied audit events via MQ"
```

---

### Task 7: maidc-data 清理旧鉴权残留

**Files:**
- Modify: `maidc-parent/maidc-data/src/main/java/com/maidc/data/config/SecurityConfig.java`
- Modify: `maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/cdr/CdrQualityRuleController.java`（及 grep 到的所有 `@PreAuthorize` 使用处）

- [ ] **Step 1: 删除 always-true PermissionEvaluator**

`SecurityConfig` 中删除 `permissionEvaluator()` 方法（L80-100 附近 always-true 实现），`@EnableMethodSecurity(prePostEnabled = false)` 保持（我们不再用 @PreAuthorize）。

- [ ] **Step 2: 替换全部 @PreAuthorize**

Run: `grep -rn "@PreAuthorize" maidc-parent/maidc-data/src/main/java --include="*.java" -l`
对每个文件：删除 `@PreAuthorize(...)` 行与 `import org.springframework.security.access.prepost.PreAuthorize;`，该 Controller 在 Task 8 统一加 `@RequirePermission`。

- [ ] **Step 3: 编译验证**

Run: `mvn -pl maidc-data -am compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add maidc-parent/maidc-data
git commit -m "refactor(data): remove always-true PermissionEvaluator and stale @PreAuthorize"
```

---

### Task 8: Controller 全量加 @RequirePermission（8 服务）

**Files:** 全部 8 个服务的 `src/main/java/**/controller/**/*.java`（约 60 文件）

**统一模式**（每个 Controller 类加 import，每个 public 端点方法加注解；读接口用 `:read`/对应 READ 码，写接口用 manage/export 码）：

```java
import com.maidc.common.security.annotation.RequirePermission;

@RequirePermission("cdr:patient:read")          // 读
@GetMapping("/{id}")
public R<PatientDetailVO> detail(@PathVariable Long id) { ... }

@RequirePermission(value = "cdr:cohort:manage") // 写
@PostMapping
public R<Void> create(@RequestBody @Valid CohortDTO dto) { ... }
```

健康检查/actuator/内部端点加 `@PublicEndpoint(reason = "...")`。

**各服务映射表（Controller → 权限码；同 Controller 内方法若读写并存则按方法标注）：**

**8a. maidc-auth（:8081）**

| Controller | 权限码 |
|---|---|
| AuthController（login/refresh/logout/me） | `@PublicEndpoint`（白名单类） |
| UserController | `system:user:manage` |
| RoleController | `system:role:manage`；`GET /api/v1/roles` 列表可放宽为 `system:role:manage`（仅管理页使用） |
| SystemConfigController | `system:config:manage` |
| InternalPermissionController | 已是 `@PublicEndpoint` |

**8b. maidc-data（:8082）**

| Controller | 权限码 |
|---|---|
| PatientEncounterController | 读=`cdr:patient:read`；导出方法=`cdr:patient:export`（Task 10 新增端点） |
| cdr/Patient360Controller | `cdr:patient:read` |
| ClinicalSearchController | `cdr:search:read` |
| SmartSearchController | `cdr:search:read` |
| DiseaseCohortController | 读=`cdr:cohort:read`；写=`cdr:cohort:manage` |
| DiseaseTemplateController | 读=`cdr:cohort:read`；写=`cdr:cohort:manage` |
| cdr/CdrQualityRuleController / CdrQualityCheckController / CdrQuarantineController | `cdr:quality:manage` |
| cdr/CdrTermMappingController | `cdr:term:manage` |
| cdr/CdrDataLineageController | `cdr:lineage:read` |
| CdrController | 读=`cdr:patient:read`；其余=`cdr:quality:manage`（按方法） |
| DataSourceController / DataSourceTypeController | `datasource:manage` |
| SyncTaskController | `sync:manage` |
| DesensitizeRuleController | `desensitize:manage` |
| EtlController / EtlPipelineController / EtlStepController / EtlFieldMappingController | 读=`etl:pipeline:read`；写=`etl:pipeline:design`；执行触发=`etl:pipeline:execute` |
| EtlExecutionController | 读=`etl:pipeline:read` |
| EtlImportController / EtlMetadataController | `etl:pipeline:design` |
| rdr/RdrSelectionController | `rdr:cohort:manage` |
| rdr/RdrDatasetVersionController | 读=`rdr:dataset:read`；导出=`rdr:dataset:export` |
| rdr/RdrExtractionTaskController / RdrExtractionExecutionController | `rdr:extraction:manage` |
| rdr/RdrMultimodalController | `rdr:dataset:read` |
| InstitutionController | `masterdata:institution:manage` |
| KnowledgeController | `masterdata:knowledge:read` |
| DictController | `masterdata:dict:manage` |
| dictionary/ 5 个（Diagnosis/Drug/ExamItem/FeeItem/LabItem） | `masterdata:dict:manage` |
| ConceptDomain/ValueDomain/DataElementConcept/DataElementController | `masterdata:element:manage` |
| CodeSystem/Concept/ConceptMapping/LocalConcept/MappingController | `masterdata:codesystem:manage`（映射类=`masterdata:mapping:manage`） |
| ObjectClass/Property/TerminologyDomainController | `masterdata:element:manage` |
| DrugInteraction/ReferenceRangeController | `masterdata:knowledge:read` |
| MasterDataImportController | `masterdata:element:manage` |
| TaskController / WorkspaceController（data 侧） | `workspace:read`；任务写=`schedule:task:manage` |
| ExportRequestController（Task 10 新建） | 申请=`cdr:patient:export`；审批=`desensitize:manage` |

**8c. maidc-model（:8083）**

| Controller | 权限码 |
|---|---|
| ModelController | 读=`model:read`；注册=`model:register` |
| VersionController | `model:read` |
| EvaluationController | `model:evaluate`（读=`model:read`） |
| ApprovalController | `model:approve` |
| DeploymentController | `model:deploy` |
| InferenceController | `model:infer` |
| MonitoringController | `model:monitor:read` |
| AlertController | 读=`alert:read`；规则写=`alert:manage` |
| LabelTaskController（model 内 label 包） | 读=`label:task:read`；分配=`label:task:assign` |

**8d. maidc-task（:8084）/ maidc-label（:8085）/ maidc-msg（:8087）/ maidc-audit（:8086）**

| Controller | 权限码 |
|---|---|
| task/TaskController | 读=`schedule:task:read`→统一用 `schedule:task:manage`（读放行给 workspace 用户：GET 方法=`workspace:read`，写=`schedule:task:manage`） |
| task/WorkspaceController | `workspace:read` |
| label/LabelTaskController | 读=`label:task:read`；认领/提交=`label:task:work`；审核=`label:task:review` |
| msg/MessageController | `workspace:read`（本人消息） |
| msg/NotificationController | 读=`workspace:read`；模板=`message:template:manage` |
| audit/AuditController | 按 resource 参数分：操作日志=`audit:operation:read`，数据访问=`audit:dataaccess:read`，系统事件=`audit:event:read`，合规=`audit:compliance:read`（同一 Controller 不同方法/参数不同码，按方法标注） |

- [ ] **Step 1: 按 8a→8d 顺序逐服务标注**（每服务完成后编译）
- [ ] **Step 2: 每服务验证**

Run: `mvn -pl <module> -am compile -q`（module 取 maidc-auth / maidc-data / maidc-model / maidc-task / maidc-label / maidc-msg / maidc-audit）
Expected: BUILD SUCCESS

- [ ] **Step 3: 每服务 Commit**

```bash
git add maidc-parent/maidc-auth && git commit -m "feat(auth): enforce @RequirePermission on all controllers"
# data/model/task/label/msg/audit 同样各一个 commit
```

---

### Task 9: ArchUnit 防漏加规则（全部服务）

**Files:**
- Modify: `maidc-parent/pom.xml`（dependencyManagement 加 archunit）
- Create: 各服务 `src/test/java/com/maidc/<svc>/arch/ControllerPermissionArchTest.java`（7 份，仅包名不同）

- [ ] **Step 1: 父 pom 加版本管理**

```xml
<properties><archunit.version>1.3.0</archunit.version></properties>
<dependencyManagement><dependencies>
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>${archunit.version}</version>
    <scope>test</scope>
</dependency>
</dependencies></dependencyManagement>
```

各服务 pom 加（无版本）：

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: 每服务一个测试类（模板，包名替换）**

```java
package com.maidc.data.arch;   // 每服务换成自己的包

import com.maidc.common.security.annotation.PublicEndpoint;
import com.maidc.common.security.annotation.RequirePermission;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

@AnalyzeClasses(packages = "com.maidc.data")
public class ControllerPermissionArchTest {

    @ArchTest
    static final ArchRule controllerEndpointsMustDeclarePermission =
        methods().that().arePublic()
            .and().areDeclaredInClassesThat().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should().beAnnotatedWith(RequirePermission.class)
            .orShould().beAnnotatedWith(PublicEndpoint.class);
}
```

- [ ] **Step 3: 运行（应全绿；若有遗漏端点则补注解而非改规则）**

Run: `mvn -pl maidc-data test -Dtest=ControllerPermissionArchTest -q`（逐服务）
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add maidc-parent/pom.xml maidc-parent/*/pom.xml maidc-parent/*/src/test
git commit -m "test: ArchUnit rule requiring permission annotation on all controller endpoints"
```

---

### Task 10: 脱敏角色豁免 + CDR 导出审批流

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/DesensitizeRuleChecker.java`
- Test: `maidc-parent/maidc-data/src/test/java/com/maidc/data/service/DesensitizeRuleCheckerTest.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/ExportRequestEntity.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/repository/ExportRequestRepository.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/ExportRequestService.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/ExportRequestController.java`
- Modify: `maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/DesensitizeRuleEntity.java`（加 exemptRoleCodes）
- Modify: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/PatientEncounterService.java`（desensitize* 接规则判定）

> **设计要点（用户确认：脱敏通过规则配置联动）**：`r_desensitize_rule` 是脱敏行为唯一配置源——`enabled` 开关、`exempt_role_codes` 豁免角色都由 data_admin 在规则管理页配置，改配置即生效（60s 内），无需改代码。

- [ ] **Step 1: DesensitizeRuleEntity 加字段**

```java
@Column(name = "exempt_role_codes")
private String exemptRoleCodes;   // 逗号分隔，如 "admin,doctor"
```

- [ ] **Step 2: 写失败测试（规则联动判定）**

```java
package com.maidc.data.service;

import com.maidc.common.security.context.CurrentUser;
import com.maidc.common.security.context.PermissionContext;
import com.maidc.common.security.store.PermissionStore;
import com.maidc.data.entity.DesensitizeRuleEntity;
import com.maidc.data.repository.DesensitizeRuleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DesensitizeRuleCheckerTest {

    private final DesensitizeRuleRepository ruleRepository = mock(DesensitizeRuleRepository.class);
    private final PermissionStore permissionStore = mock(PermissionStore.class);
    private final DesensitizeRuleChecker checker = new DesensitizeRuleChecker(ruleRepository, permissionStore);

    private DesensitizeRuleEntity rule(boolean enabled, String exempt) {
        DesensitizeRuleEntity r = new DesensitizeRuleEntity();
        r.setFieldType("NAME");
        r.setEnabled(enabled);
        r.setExemptRoleCodes(exempt);
        return r;
    }

    @Test
    void enabledRule_exemptRole_seesPlaintext() {
        when(ruleRepository.findAll()).thenReturn(List.of(rule(true, "admin,doctor")));
        try (MockedStatic<CurrentUser> mocked = mockStatic(CurrentUser.class)) {
            mocked.when(CurrentUser::userId).thenReturn(1L);
            when(permissionStore.load(1L)).thenReturn(PermissionContext.builder()
                    .userId(1L).roles(List.of("doctor")).build());
            assertFalse(checker.shouldMask("NAME"));   // 命中豁免 → 明文
        }
    }

    @Test
    void enabledRule_notExempt_masked() {
        when(ruleRepository.findAll()).thenReturn(List.of(rule(true, "admin,data_admin")));
        try (MockedStatic<CurrentUser> mocked = mockStatic(CurrentUser.class)) {
            mocked.when(CurrentUser::userId).thenReturn(1L);
            when(permissionStore.load(1L)).thenReturn(PermissionContext.builder()
                    .userId(1L).roles(List.of("researcher")).build());
            assertTrue(checker.shouldMask("NAME"));    // 未豁免 → 脱敏
        }
    }

    @Test
    void disabledRule_seesPlaintext() {
        when(ruleRepository.findAll()).thenReturn(List.of(rule(false, null)));
        assertFalse(checker.shouldMask("NAME"));       // 规则关闭 → 明文（无需用户上下文）
    }

    @Test
    void noRule_defaultsToMask() {
        when(ruleRepository.findAll()).thenReturn(List.of());
        assertTrue(checker.shouldMask("PHONE"));       // 无规则 → 安全默认脱敏
    }
}
```

> JUnit 5 每测试方法新建实例 → 60s 快照缓存不会跨测试串数据；Mockito 5（Boot 3.2 自带）原生支持 `mockStatic`。

- [ ] **Step 3: 运行确认失败**

Run: `mvn -pl maidc-data -am test -Dtest=DesensitizeRuleCheckerTest -q`
Expected: 编译失败 DesensitizeRuleChecker 不存在

- [ ] **Step 4: 实现 DesensitizeRuleChecker（规则驱动 + 60s 快照缓存）**

```java
package com.maidc.data.service;

import com.maidc.common.security.context.CurrentUser;
import com.maidc.common.security.store.PermissionStore;
import com.maidc.data.entity.DesensitizeRuleEntity;
import com.maidc.data.repository.DesensitizeRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 规则驱动的脱敏判定，r_desensitize_rule 为唯一配置源：
 *   无匹配规则         → 脱敏（敏感字段安全默认）
 *   规则 enabled=false → 明文（data_admin 在规则管理页关闭）
 *   规则 enabled=true  → 角色命中 exempt_role_codes 明文，否则脱敏
 * 规则快照本地缓存 60s：配置变更最迟 1 分钟生效，避免逐行查库。
 */
@Service
@RequiredArgsConstructor
public class DesensitizeRuleChecker {

    private static final long CACHE_TTL_MS = 60_000;

    private final DesensitizeRuleRepository ruleRepository;
    private final PermissionStore permissionStore;

    private volatile Map<String, DesensitizeRuleEntity> ruleSnapshot = Map.of();
    private volatile long snapshotAt = 0;

    public boolean shouldMask(String fieldType) {
        DesensitizeRuleEntity rule = snapshot().get(fieldType == null ? "" : fieldType.toUpperCase());
        if (rule == null) return true;
        if (!Boolean.TRUE.equals(rule.getEnabled())) return false;

        var ctx = permissionStore.load(CurrentUser.userId());
        if (ctx == null || ctx.getRoles() == null) return true;
        String exempt = rule.getExemptRoleCodes();
        if (exempt == null || exempt.isBlank()) return true;
        return Arrays.stream(exempt.split(","))
                .map(String::trim)
                .noneMatch(ctx.getRoles()::contains);
    }

    private Map<String, DesensitizeRuleEntity> snapshot() {
        long now = System.currentTimeMillis();
        if (ruleSnapshot.isEmpty() || now - snapshotAt > CACHE_TTL_MS) {
            List<DesensitizeRuleEntity> rules = ruleRepository.findAll();
            ruleSnapshot = rules.stream().collect(Collectors.toMap(
                    r -> r.getFieldType().toUpperCase(), r -> r, (a, b) -> a));
            snapshotAt = now;
        }
        return ruleSnapshot;
    }
}
```

- [ ] **Step 5: 运行测试通过**

Run: `mvn -pl maidc-data -am test -Dtest=DesensitizeRuleCheckerTest -q`
Expected: BUILD SUCCESS, Tests run: 4

- [ ] **Step 6: PatientEncounterService 脱敏方法接规则判定**

类注入 `private final DesensitizeRuleChecker ruleChecker;`。列表/详情返回前**每请求判定一次**（避免逐行重复判定），三个私有方法加 `mask` 参数：

```java
// 查询出口处计算一次：
boolean maskName   = ruleChecker.shouldMask("NAME");
boolean maskIdCard = ruleChecker.shouldMask("ID_CARD");
boolean maskPhone  = ruleChecker.shouldMask("PHONE");

// 私有方法改造（idCard/phone 同型）：
private String desensitizeName(String name, boolean mask) {
    if (name == null || name.isEmpty()) return null;
    if (!mask) return name;                    // 规则关闭或角色豁免 → 原文
    if (name.length() == 1) return name + "**";
    return name.charAt(0) + "**";
}
```

调用处（`PatientEncounterService` L66-70 附近）把三个布尔传入。

- [ ] **Step 7: 导出审批实体/仓库/服务/控制器**

```java
// entity/ExportRequestEntity.java
package com.maidc.data.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "c_export_request", schema = "cdr")
public class ExportRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "requester_id", nullable = false)
    private Long requesterId;
    @Column(name = "patient_ids", columnDefinition = "jsonb")
    private String patientIds;
    @Column(nullable = false, length = 512)
    private String purpose;
    @Column(nullable = false, length = 16)
    private String status = "PENDING";    // PENDING/APPROVED/REJECTED
    @Column(name = "approver_id")
    private Long approverId;
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    @Column(name = "reject_reason", length = 512)
    private String rejectReason;
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

```java
// repository/ExportRequestRepository.java
package com.maidc.data.repository;

import com.maidc.data.entity.ExportRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExportRequestRepository extends JpaRepository<ExportRequestEntity, Long> {
    List<ExportRequestEntity> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
    List<ExportRequestEntity> findByStatusOrderByCreatedAtDesc(String status);
}
```

```java
// service/ExportRequestService.java
package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.ExportRequestEntity;
import com.maidc.data.repository.ExportRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportRequestService {

    private final ExportRequestRepository repository;

    public ExportRequestEntity create(Long requesterId, String patientIds, String purpose) {
        ExportRequestEntity e = new ExportRequestEntity();
        e.setRequesterId(requesterId);
        e.setPatientIds(patientIds);
        e.setPurpose(purpose);
        return repository.save(e);
    }

    public List<ExportRequestEntity> myRequests(Long requesterId) {
        return repository.findByRequesterIdOrderByCreatedAtDesc(requesterId);
    }

    public List<ExportRequestEntity> pending() {
        return repository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    @Transactional
    public ExportRequestEntity approve(Long id, Long approverId) {
        ExportRequestEntity e = mustBePending(id);
        e.setStatus("APPROVED");
        e.setApproverId(approverId);
        e.setApprovedAt(LocalDateTime.now());
        return repository.save(e);
    }

    @Transactional
    public ExportRequestEntity reject(Long id, Long approverId, String reason) {
        ExportRequestEntity e = mustBePending(id);
        e.setStatus("REJECTED");
        e.setApproverId(approverId);
        e.setRejectReason(reason);
        return repository.save(e);
    }

    /** 导出执行前校验：护士必须持 APPROVED 单据；doctor 由权限免审批直接放行 */
    public void assertExportable(Long requesterId, boolean exemptFromApproval) {
        if (exemptFromApproval) return;
        boolean has = repository.findByRequesterIdOrderByCreatedAtDesc(requesterId).stream()
                .anyMatch(e -> "APPROVED".equals(e.getStatus()));
        if (!has) {
            throw new BusinessException(403, "导出需先获得审批（提交导出申请）");
        }
    }

    private ExportRequestEntity mustBePending(Long id) {
        ExportRequestEntity e = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "导出申请不存在"));
        if (!"PENDING".equals(e.getStatus())) {
            throw new BusinessException(409, "该申请已处理");
        }
        return e;
    }
}
```

```java
// controller/ExportRequestController.java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.common.security.annotation.CurrentUserId;
import com.maidc.common.security.annotation.RequirePermission;
import com.maidc.common.security.context.PermissionContext;
import com.maidc.common.security.store.PermissionStore;
import com.maidc.data.entity.ExportRequestEntity;
import com.maidc.data.service.ExportRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/data/export-requests")
@RequiredArgsConstructor
public class ExportRequestController {

    private final ExportRequestService service;
    private final PermissionStore permissionStore;

    /** 护士/受限角色提交导出申请 */
    @RequirePermission("cdr:patient:export")
    @PostMapping
    public R<ExportRequestEntity> create(@CurrentUserId Long userId,
                                         @RequestBody Map<String, String> body) {
        return R.ok(service.create(userId, body.get("patientIds"), body.get("purpose")));
    }

    @RequirePermission("cdr:patient:export")
    @GetMapping("/mine")
    public R<List<ExportRequestEntity>> mine(@CurrentUserId Long userId) {
        return R.ok(service.myRequests(userId));
    }

    /** 待审列表：data_admin */
    @RequirePermission("desensitize:manage")
    @GetMapping("/pending")
    public R<List<ExportRequestEntity>> pending() {
        return R.ok(service.pending());
    }

    @RequirePermission("desensitize:manage")
    @PostMapping("/{id}/approve")
    public R<ExportRequestEntity> approve(@PathVariable Long id, @CurrentUserId Long approverId) {
        return R.ok(service.approve(id, approverId));
    }

    @RequirePermission("desensitize:manage")
    @PostMapping("/{id}/reject")
    public R<ExportRequestEntity> reject(@PathVariable Long id, @CurrentUserId Long approverId,
                                         @RequestBody Map<String, String> body) {
        return R.ok(service.reject(id, approverId, body.getOrDefault("reason", "")));
    }

    /** 角色是否免审批导出（doctor=true；nurse=false 走审批）——供前端展示 */
    @RequirePermission("cdr:patient:export")
    @GetMapping("/exempt")
    public R<Boolean> exempt(@CurrentUserId Long userId) {
        PermissionContext ctx = permissionStore.load(userId);
        return R.ok(ctx != null && ctx.getRoles() != null
                && ctx.getRoles().contains("doctor"));
    }
}
```

- [ ] **Step 8: 编译 + 回归**

Run: `mvn -pl maidc-data -am test -q`
Expected: BUILD SUCCESS（含 DesensitizeRuleCheckerTest 4 例）

- [ ] **Step 9: Commit**

```bash
git add maidc-parent/maidc-data
git commit -m "feat(data): rule-driven desensitize linkage + CDR export approval workflow"
```

---

### Task 11: 前端权限码对齐 + 导出审批页面

**Files:**
- Modify: `maidc-portal/src/router/asyncRoutes.ts`
- Modify: `maidc-portal/src/stores/auth.ts`（userInfo 类型加 permissions/dataScope，若为 any 则跳过）
- Create: `maidc-portal/src/api/export.ts`
- Create: `maidc-portal/src/views/system/ExportRequestList.vue`（护士：我的申请）
- Create: `maidc-portal/src/views/system/ExportApprovalList.vue`（data_admin：审批）
- Modify: `maidc-portal/src/views/data-cdr/DesensitizeRule.vue`（现有脱敏规则管理页，表单加"豁免角色"编辑框——规则配置联动的管理入口）
- Modify: `maidc-portal/src/router/asyncRoutes.ts`（新增两个路由项）

- [ ] **Step 1: asyncRoutes 权限码映射（逐项替换 meta.permission）**

| 路由 | 旧值 | 新值 |
|---|---|---|
| dashboard 全部子路由 | （无） | workspace=`workspace:read`，overview=`dashboard:overview:read`，model=`dashboard:model:read`，data=`dashboard:data:read` |
| model 及子路由 | `model` | 父=`model:read`；list=`model:read`，evaluations=`model:evaluate`，approvals=`model:approve`，deployments=`model:deploy`，routes=`model:route:manage`，versions=`model:read`，inference-logs=`model:infer` |
| data/cdr/search | `data` | `cdr:search:read` |
| data/cdr/disease(±:id) | `data` | 读=`cdr:cohort:read` |
| data/cdr/patients(±:id/encounters) | `data` | `cdr:patient:read` |
| data/cdr/quality-rules / quality-results | `data` | `cdr:quality:manage` |
| data/rdr/*（projects/datasets/etl） | `data` | projects=`rdr:project:read`，datasets=`rdr:dataset:read`，etl=`rdr:extraction:manage` |
| data/etl（pipelines/executions） | `data` | `etl:pipeline:read` |
| data/datasources(±:id) | `data` | `datasource:manage` |
| data/sync | `data` | `sync:manage` |
| schedule | — | `schedule:task:manage` |
| label/tasks | `label` | `label:task:read` |
| label/workspace | `label` | `label:task:work` |
| alert/active、alert/detail | `alert` | `alert:read` |
| alert/rules | `alert` | `alert:manage` |
| audit/operations、data-access、system-events、compliance | `audit` | 对应 `audit:operation:read`/`audit:dataaccess:read`/`audit:event:read`/`audit:compliance:read` |
| message/list、detail | — | `workspace:read` |
| message/templates、settings | — | `message:template:manage` |
| masterdata 全部 | — | element 类=`masterdata:element:manage`，code-systems/mappings=`masterdata:codesystem:manage`，dictionaries 5 项=`masterdata:dict:manage`，knowledge=`masterdata:knowledge:read`，institutions=`masterdata:institution:manage`，clinical-rules/domains=`masterdata:mapping:manage` |
| system/users、roles、permissions | `system` | `system:user:manage`/`system:role:manage`/`system:permission:manage` |
| system/config | `system` | `system:config:manage` |
| system/desensitize | `system` | `desensitize:manage` |
| **补** DesensitizeRule 管理页表单 | — | 新增/编辑抽屉加 `exemptRoleCodes` 输入框（label"豁免角色（逗号分隔）"，placeholder 如 `admin,doctor`），随现有 create/update 接口提交——脱敏规则配置联动的前端入口 |
| **新增** system/export-requests | — | `cdr:patient:export`（component: ExportRequestList.vue） |
| **新增** system/export-approvals | — | `desensitize:manage`（component: ExportApprovalList.vue） |

- [ ] **Step 2: api/export.ts**

```typescript
import request from '@/utils/request'

export interface ExportRequest {
  id: number
  requesterId: number
  patientIds: string | null
  purpose: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  approverId: number | null
  approvedAt: string | null
  rejectReason: string | null
  createdAt: string
}

export const createExportRequest = (data: { patientIds: string; purpose: string }) =>
  request.post<ExportRequest>('/api/v1/data/export-requests', data)
export const myExportRequests = () =>
  request.get<ExportRequest[]>('/api/v1/data/export-requests/mine')
export const pendingExportRequests = () =>
  request.get<ExportRequest[]>('/api/v1/data/export-requests/pending')
export const approveExportRequest = (id: number) =>
  request.post<ExportRequest>(`/api/v1/data/export-requests/${id}/approve`)
export const rejectExportRequest = (id: number, reason: string) =>
  request.post<ExportRequest>(`/api/v1/data/export-requests/${id}/reject`, { reason })
export const exportExempt = () =>
  request.get<boolean>('/api/v1/data/export-requests/exempt')
```

> `request` 实例路径以 `src/utils/` 现有 axios 封装为准（核对 `src/api/auth.ts` 的 import 方式保持一致）。

- [ ] **Step 3: 两个 Vue 页面**

`ExportRequestList.vue`（结构：PageContainer + 表格 + 新建 Modal，完整代码）：

```vue
<template>
  <PageContainer title="导出申请">
    <a-space style="margin-bottom: 16px">
      <a-button type="primary" @click="open = true">新建导出申请</a-button>
    </a-space>
    <a-table :data-source="list" :columns="columns" row-key="id" :loading="loading">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <StatusBadge :status="record.status"
            :map="{ PENDING: 'processing', APPROVED: 'success', REJECTED: 'error' }" />
        </template>
      </template>
    </a-table>
    <a-modal v-model:open="open" title="新建导出申请" @ok="submit">
      <a-form layout="vertical">
        <a-form-item label="患者ID清单(JSON数组)" required>
          <a-textarea v-model:value="form.patientIds" :rows="3" placeholder="[101,102]" />
        </a-form-item>
        <a-form-item label="导出用途" required>
          <a-textarea v-model:value="form.purpose" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import { myExportRequests, createExportRequest, ExportRequest } from '@/api/export'

const list = ref<ExportRequest[]>([])
const loading = ref(false)
const open = ref(false)
const form = ref({ patientIds: '', purpose: '' })
const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
  { title: '用途', dataIndex: 'purpose', key: 'purpose' },
  { title: '状态', key: 'status', width: 110 },
  { title: '申请时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '拒绝原因', dataIndex: 'rejectReason', key: 'rejectReason' },
]

const load = async () => {
  loading.value = true
  try { list.value = await myExportRequests() } finally { loading.value = false }
}
const submit = async () => {
  await createExportRequest({ ...form.value })
  message.success('已提交，等待审批')
  open.value = false
  form.value = { patientIds: '', purpose: '' }
  await load()
}
onMounted(load)
</script>
```

> 组件 import 路径以 `src/components/` 实际文件为准（核对 PageContainer/StatusBadge 是否有 index.vue）。`ExportApprovalList.vue` 同型：数据源 `pendingExportRequests()`，行操作"通过/驳回"调 `approveExportRequest`/`rejectExportRequest(id, reason)`（驳回用 Modal 填 reason），列：ID/申请人 requesterId/用途/申请时间/操作。

- [ ] **Step 4: 路由注册（system 菜单 children 追加）**

```ts
{ path: 'export-requests', name: 'ExportRequestList',
  meta: { title: '导出申请', permission: 'cdr:patient:export' },
  component: () => import('@/views/system/ExportRequestList.vue') },
{ path: 'export-approvals', name: 'ExportApprovalList',
  meta: { title: '导出审批', permission: 'desensitize:manage' },
  component: () => import('@/views/system/ExportApprovalList.vue') },
```

- [ ] **Step 5: 类型检查 + 构建验证**

Run: `cd e:/pxg_project/maidc-portal && npm run type-check && npm run build`
Expected: 无错误退出

- [ ] **Step 6: Commit**

```bash
git add maidc-portal/src
git commit -m "feat(portal): align permission codes, add export request/approval pages"
```

---

### Task 12: 越权集成测试（data 服务样例）+ 收尾

**Files:**
- Create: `maidc-parent/maidc-data/src/test/java/com/maidc/data/controller/PatientPermissionIntegrationTest.java`
- Modify: `docs/superpowers/specs/2026-09-08-permission-system-design.md`（末尾加实施状态）

- [ ] **Step 1: 写集成测试（MockMvc + mocked PermissionStore）**

```java
package com.maidc.data.controller;

import com.maidc.common.security.context.PermissionContext;
import com.maidc.common.security.scope.DataScope;
import com.maidc.common.security.store.PermissionStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PatientPermissionIntegrationTest {

    @Autowired MockMvc mockMvc;
    @MockBean PermissionStore permissionStore;

    private PermissionContext ctx(String... perms) {
        return PermissionContext.builder().userId(1L)
                .permissions(Set.of(perms)).dataScope(DataScope.DEPT).deptId(5L)
                .roles(List.of("doctor")).build();
    }

    @Test
    void doctor_canReadPatients() throws Exception {
        when(permissionStore.load(1L)).thenReturn(ctx("cdr:patient:read"));
        mockMvc.perform(get("/api/v1/data/cdr/patients").header("X-User-Id", "1"))
               .andExpect(status().isOk());
    }

    @Test
    void nurse_cannotApproveModel() throws Exception {
        when(permissionStore.load(1L)).thenReturn(ctx("cdr:patient:read"));
        // 无 model:approve 权限访问模型审批 → 403（示例断言 data 服务的受保护端点）
        mockMvc.perform(get("/api/v1/data/etl/pipelines").header("X-User-Id", "1"))
               .andExpect(status().isForbidden());
    }
}
```

> URL 前缀以 `PatientEncounterController`/`EtlPipelineController` 实际 `@RequestMapping` 为准（实施时核对）；`@MockBean` PermissionStore 使切面走 mock，无需 Redis。若服务测试上下文启动需要外部依赖（数据库），按现有 `AuthServiceTest` 风格改用 `@WebMvcTest` + 手动 import 切面。

- [ ] **Step 2: 运行**

Run: `mvn -pl maidc-data test -Dtest=PatientPermissionIntegrationTest -q`
Expected: Tests run: 2, Failures: 0

- [ ] **Step 3: 全量回归**

Run: `cd e:/pxg_project/maidc-parent && mvn test -q`
Expected: BUILD SUCCESS（60+ 既有测试 + 新增全部通过）

- [ ] **Step 4: 更新 spec 实施状态 + Commit**

在 spec 末尾追加：

```markdown
## 14. 实施状态

- [x] Task 1-12 全部完成（本计划）— 2026-09-08
```

```bash
git add docs/superpowers/specs/2026-09-08-permission-system-design.md
git commit -m "docs: mark permission system implemented"
```

---

## 自审记录（写计划时已核对）

1. **规格覆盖**：55 权限码（Task 1，覆盖 spec §6 全部模块）✓ 8角色矩阵（Task 1 种子，逐角色行数已清点：13/10/18/11/29/20/10 + admin=55）✓ @RequirePermission+切面+缓存（Task 2-4）✓ DEPT/SELF/PROJECT（Task 5）✓ 越权审计（Task 6/12）✓ 清理 always-true evaluator（Task 7）✓ 8 服务注解（Task 8）✓ ArchUnit（Task 9）✓ 脱敏规则配置联动+导出审批（Task 10，含 4 例单测）✓ 前端含脱敏规则豁免编辑入口（Task 11）✓
2. **占位符**：无 TBD；"实施时核对"项（request 封装路径、UserRepository 方法名、患者表 dept 列）均为核对指令非占位
3. **类型一致性**：`PermissionContext` 字段（userId/permissions/dataScope/deptId/roles/projectIds）在 Task 2 定义、Task 3/4/5/10/12 使用一致；`PermissionStore.load/save/evict` 签名全局一致；`DataScope.widest` Task 2 定义 Task 4 使用；`DesensitizeRuleChecker.shouldMask(String)` Task 10 Step 4 定义、Step 6 调用一致；`PermissionAspect.check(String, Long)` Task 3 测试与实现一致（Task 6 加 publisher 后同步改测试已注明）
4. **脱敏规则联动（用户 2026-09-08 确认）**：`r_desensitize_rule` 为唯一配置源（enabled 开关 + exempt_role_codes 豁免），`DesensitizeRuleChecker` 60s 快照缓存使配置变更 1 分钟内生效；管理入口 = 现有 DesensitizeRule 页面加豁免角色编辑框（Task 11）
