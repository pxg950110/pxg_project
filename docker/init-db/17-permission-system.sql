-- 17-permission-system.sql  MAIDC 权限系统（RBAC扩展+数据范围+脱敏豁免）
BEGIN;

-- ========== DDL ==========
ALTER TABLE system.s_role ADD COLUMN IF NOT EXISTS data_scope VARCHAR(16) NOT NULL DEFAULT 'SELF';
ALTER TABLE system.s_user ADD COLUMN IF NOT EXISTS dept_id BIGINT;
-- r_desensitize_rule 仅存在于已运行库（由JPA建表），init脚本从未创建过；
-- 此处补 CREATE IF NOT EXISTS 保证全新安装时后续 ALTER/回填可执行。
CREATE TABLE IF NOT EXISTS cdr.r_desensitize_rule (
    id          BIGSERIAL     PRIMARY KEY,
    rule_name   VARCHAR(128)  NOT NULL,
    field_type  VARCHAR(32)   NOT NULL,
    strategy    VARCHAR(32)   NOT NULL,
    params      JSONB,
    enabled     BOOLEAN       NOT NULL DEFAULT TRUE,
    description VARCHAR(512),
    created_by  VARCHAR(64)   NOT NULL DEFAULT 'system',
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_by  VARCHAR(64),
    updated_at  TIMESTAMP,
    is_deleted  BOOLEAN       NOT NULL DEFAULT FALSE,
    org_id      BIGINT        NOT NULL DEFAULT 0
);
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

UPDATE system.s_role SET data_scope='ALL'    WHERE role_code IN ('admin','data_admin','auditor') AND org_id = 0;
UPDATE system.s_role SET data_scope='DEPT'   WHERE role_code IN ('doctor','nurse') AND org_id = 0;
UPDATE system.s_role SET data_scope='PROJECT' WHERE role_code IN ('researcher','researcher_pi') AND org_id = 0;
UPDATE system.s_role SET data_scope='SELF'   WHERE role_code='ai_engineer' AND org_id = 0;

-- ========== 权限种子（55码，替换旧15条） ==========
-- 警告：重跑本脚本会重建 org=0 的全部权限与授权（手工添加的权限/授权会丢失，UI 软删除的权限会被恢复并重新授予 admin）
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
CREATE TEMP TABLE tmp_role_perm(role_code VARCHAR(32), perm_code VARCHAR(64));

INSERT INTO tmp_role_perm VALUES
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

-- 修正 ai_engineer 一条插反的值（此 UPDATE 是计划的一部分，保留）。
-- 若将上方插反的值"顺手改正"并删除本 UPDATE，JOIN 会静默丢弃该行，ai_engineer 将失去 rdr:dataset:read。
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

-- ========== 脱敏豁免种子 ==========
-- 核心字段规则回填（表为空时使用显式策略：与 docs/cdr-rdr-sync-flow.md 设计一致）
-- 注：已存在库中 org_id 无默认值，必须显式指定
INSERT INTO cdr.r_desensitize_rule (rule_name, field_type, strategy, enabled, exempt_role_codes, created_by, created_at, is_deleted, org_id)
SELECT '核心字段-' || t.ft, t.ft, t.strategy, true, NULL, 'system', NOW(), false, 0
FROM (VALUES ('NAME','REPLACE'),('ID_CARD','MASK'),('PHONE','MASK'),('ADDRESS','DELETE')) AS t(ft, strategy)
WHERE NOT EXISTS (SELECT 1 FROM cdr.r_desensitize_rule r WHERE r.field_type = t.ft);

-- 注：以下两条 UPDATE 不限 org_id（全局回填应用创建的规则行）是有意为之
UPDATE cdr.r_desensitize_rule SET exempt_role_codes='admin,doctor,data_admin'
 WHERE field_type IN ('NAME','ID_CARD','PHONE','ADDRESS') AND exempt_role_codes IS NULL;
UPDATE cdr.r_desensitize_rule SET exempt_role_codes='admin,data_admin'
 WHERE field_type NOT IN ('NAME','ID_CARD','PHONE','ADDRESS') AND exempt_role_codes IS NULL;

COMMIT;
