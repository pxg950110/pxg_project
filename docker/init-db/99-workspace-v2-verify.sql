-- 工作台 v2 验证用临时种子数据（验证后可清理）
-- doctor1 密码 = admin123（复用 admin 的 bcrypt hash，仅本地 dev）
INSERT INTO system.s_user (username, password_hash, real_name, status, created_by, org_id)
SELECT 'doctor1', u.password_hash, '张伟', 'ACTIVE', 'system', 0
FROM system.s_user u WHERE u.username = 'admin'
AND NOT EXISTS (SELECT 1 FROM system.s_user WHERE username = 'doctor1');

INSERT INTO system.s_user_role (user_id, role_id, granted_by, org_id)
SELECT u.id, r.id, 0, 0
FROM system.s_user u, system.s_role r
WHERE u.username = 'doctor1' AND r.role_code = 'doctor' AND r.org_id = 0
AND NOT EXISTS (
  SELECT 1 FROM system.s_user_role ur
  WHERE ur.user_id = (SELECT id FROM system.s_user WHERE username='doctor1')
    AND ur.role_id = r.id);

-- 专病队列 + 患者 + 随访档案 + 任务（doctor1 名下）
INSERT INTO cdr.c_disease_cohort (name, description, inclusion_rules, patient_count, status, created_by, org_id)
SELECT '慢性鼻窦炎验证队列', '工作台验证用', '[{"type":"diagnosis","code":"J32"}]'::jsonb, 0, 'ACTIVE', 'system', 0
WHERE NOT EXISTS (SELECT 1 FROM cdr.c_disease_cohort WHERE name = '慢性鼻窦炎验证队列');

INSERT INTO cdr.c_patient (name, gender, birth_date, created_by, org_id)
SELECT '王建国', 'M', '1965-03-12', 'system', 0
WHERE NOT EXISTS (SELECT 1 FROM cdr.c_patient WHERE name = '王建国' AND is_deleted = false);

INSERT INTO cdr.c_patient_followup (cohort_id, patient_id, protocol_id, protocol_version, doctor_id, status, enroll_date, created_by, org_id)
SELECT c.id, p.id, 0, 1, u.id, 'ACTIVE', CURRENT_DATE - 90, 'system', 0
FROM cdr.c_disease_cohort c, cdr.c_patient p, system.s_user u
WHERE c.name = '慢性鼻窦炎验证队列' AND p.name = '王建国' AND u.username = 'doctor1'
AND NOT EXISTS (
  SELECT 1 FROM cdr.c_patient_followup f
  WHERE f.cohort_id = c.id AND f.patient_id = p.id AND f.is_deleted = false);

INSERT INTO cdr.c_followup_task (followup_id, stage_code, stage_name, due_date, status, required_scales, completed_by, completed_at, created_by, org_id)
SELECT f.id, v.stage_code, v.stage_name, CURRENT_DATE + v.due_offset, v.status, v.scales::jsonb, v.completed_by, v.completed_at, 'system', 0
FROM cdr.c_patient_followup f,
(VALUES
  ('POST_1M',  '术后1月',  -8, 'PENDING', '["SNOT-22","VAS"]'::jsonb, NULL::bigint, NULL::timestamp),
  ('POST_3M',  '术后3月',  -2, 'PENDING', '["SNOT-22"]'::jsonb,       NULL::bigint, NULL::timestamp),
  ('BASELINE', '基线评估',   0, 'PENDING', '["SNOT-22","Lund-Kennedy"]'::jsonb, NULL::bigint, NULL::timestamp),
  ('POST_6M',  '术后6月',   3, 'PENDING', '["SNOT-22"]'::jsonb,       NULL::bigint, NULL::timestamp),
  ('POST_12M', '术后12月', -5, 'DONE',    '["SNOT-22"]'::jsonb,       (SELECT id FROM system.s_user WHERE username='doctor1'), date_trunc('day', date_trunc('week', CURRENT_DATE))::timestamp + interval '1 day' + interval '2 hours')
) AS v(stage_code, stage_name, due_offset, status, scales, completed_by, completed_at)
WHERE NOT EXISTS (SELECT 1 FROM cdr.c_followup_task t WHERE t.followup_id = f.id AND t.stage_code = v.stage_code);

-- 核对输出
SELECT u.id AS doctor_user_id, u.username FROM system.s_user u WHERE u.username IN ('admin','doctor1');
SELECT t.id, t.stage_code, t.due_date, t.status FROM cdr.c_followup_task t WHERE t.is_deleted = false ORDER BY t.id;
