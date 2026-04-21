-- ==================== system schema: 7 tables ====================

-- s_user
CREATE TABLE system.s_user (
    id                BIGSERIAL    PRIMARY KEY,
    username          VARCHAR(64)  NOT NULL,
    password_hash     VARCHAR(256) NOT NULL,
    real_name         VARCHAR(64)  NOT NULL,
    email             VARCHAR(128),
    phone             VARCHAR(32),
    avatar_url        VARCHAR(256),
    status            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    last_login_at     TIMESTAMP,
    last_login_ip     VARCHAR(45),
    password_changed_at TIMESTAMP,
    must_change_pwd   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_username UNIQUE (org_id, username)
);
COMMENT ON TABLE system.s_user IS '用户表';

-- s_role
CREATE TABLE system.s_role (
    id          BIGSERIAL    PRIMARY KEY,
    role_code   VARCHAR(32)  NOT NULL,
    role_name   VARCHAR(64)  NOT NULL,
    description TEXT,
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by  VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by  VARCHAR(64),
    updated_at  TIMESTAMP,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_role_code UNIQUE (org_id, role_code)
);
COMMENT ON TABLE system.s_role IS '角色表';

-- s_permission
CREATE TABLE system.s_permission (
    id              BIGSERIAL    PRIMARY KEY,
    permission_code VARCHAR(64)  NOT NULL,
    permission_name VARCHAR(128) NOT NULL,
    resource_type   VARCHAR(32)  NOT NULL,
    resource_key    VARCHAR(128) NOT NULL,
    action          VARCHAR(32)  NOT NULL,
    parent_id       BIGINT,
    sort_order      INT          NOT NULL DEFAULT 0,
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_perm_code UNIQUE (org_id, permission_code)
);
COMMENT ON TABLE system.s_permission IS '权限表';

-- s_user_role
CREATE TABLE system.s_user_role (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT    NOT NULL,
    role_id     BIGINT    NOT NULL,
    granted_by  BIGINT    NOT NULL DEFAULT 0,
    granted_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMP,
    org_id      BIGINT    NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);
COMMENT ON TABLE system.s_user_role IS '用户角色关联表';

-- s_role_permission
CREATE TABLE system.s_role_permission (
    id            BIGSERIAL PRIMARY KEY,
    role_id       BIGINT    NOT NULL,
    permission_id BIGINT    NOT NULL,
    org_id        BIGINT    NOT NULL DEFAULT 0,
    CONSTRAINT uk_role_perm UNIQUE (role_id, permission_id)
);
COMMENT ON TABLE system.s_role_permission IS '角色权限关联表';

-- s_dict
CREATE TABLE system.s_dict (
    id          BIGSERIAL    PRIMARY KEY,
    dict_type   VARCHAR(32)  NOT NULL,
    dict_code   VARCHAR(32)  NOT NULL,
    dict_label  VARCHAR(128) NOT NULL,
    dict_value  VARCHAR(256),
    sort_order  INT          NOT NULL DEFAULT 0,
    parent_code VARCHAR(32),
    is_enabled  BOOLEAN      NOT NULL DEFAULT TRUE,
    remark      VARCHAR(256),
    created_by  VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by  VARCHAR(64),
    updated_at  TIMESTAMP,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_dict UNIQUE (org_id, dict_type, dict_code)
);
COMMENT ON TABLE system.s_dict IS '数据字典表';

-- s_config
CREATE TABLE system.s_config (
    id           BIGSERIAL    PRIMARY KEY,
    config_key   VARCHAR(128) NOT NULL,
    config_value TEXT         NOT NULL,
    config_type  VARCHAR(16)  NOT NULL DEFAULT 'STRING',
    description  VARCHAR(256),
    is_encrypted BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by   VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by   VARCHAR(64),
    updated_at   TIMESTAMP,
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_config_key UNIQUE (org_id, config_key)
);
COMMENT ON TABLE system.s_config IS '系统配置表';

-- ==================== 初始数据 ====================

-- 超级管理员 (password: admin123)
INSERT INTO system.s_user (username, password_hash, real_name, status, created_by, org_id)
VALUES ('admin', '$2b$10$LhAZUObGniTkqVU6mii9s.Hk/zOtyR8aLS2cj4WBZSR3oZb4LmaE6', '系统管理员', 'ACTIVE', 'system', 0);

-- 6 个内置角色
INSERT INTO system.s_role (role_code, role_name, description, is_system, created_by, org_id) VALUES
('admin',         '系统管理员', '拥有全部权限', true, 'system', 0),
('data_admin',    '数据管理员', '数据接入/ETL/质量管理', true, 'system', 0),
('researcher',    '科研人员',   '科研项目/数据集/标注', true, 'system', 0),
('ai_engineer',   'AI工程师',  '模型注册/训练/评估/部署', true, 'system', 0),
('doctor',        '临床医生',  '患者数据查看/模型推理', true, 'system', 0),
('auditor',       '审计员',    '操作审计/合规检查', true, 'system', 0);

-- admin 用户赋予 admin 角色
INSERT INTO system.s_user_role (user_id, role_id, granted_by, org_id)
SELECT u.id, r.id, 0, 0
FROM system.s_user u, system.s_role r
WHERE u.username = 'admin' AND r.role_code = 'admin';

-- 权限树（一级菜单）
INSERT INTO system.s_permission (permission_code, permission_name, resource_type, resource_key, action, parent_id, sort_order, created_by, org_id) VALUES
('dashboard',         '仪表盘',       'MENU', '/dashboard',         'READ',  NULL, 1,  'system', 0),
('model',             '模型管理',     'MENU', '/model',             'READ',  NULL, 2,  'system', 0),
('model:create',      '注册模型',     'BUTTON','/model/create',     'CREATE',NULL, 1,  'system', 0),
('model:evaluate',    '模型评估',     'BUTTON','/model/evaluate',   'CREATE',NULL, 2,  'system', 0),
('model:approve',     '模型审批',     'BUTTON','/model/approve',    'UPDATE',NULL, 3,  'system', 0),
('model:deploy',      '模型部署',     'BUTTON','/model/deploy',     'CREATE',NULL, 4,  'system', 0),
('data',              '数据管理',     'MENU', '/data',              'READ',  NULL, 3,  'system', 0),
('data:import',       '数据导入',     'BUTTON','/data/import',      'CREATE',NULL, 1,  'system', 0),
('data:export',       '数据导出',     'BUTTON','/data/export',      'EXPORT',NULL, 2,  'system', 0),
('data:desensitize',  '数据脱敏',     'BUTTON','/data/desensitize', 'UPDATE',NULL, 3,  'system', 0),
('label',             '标注管理',     'MENU', '/label',             'READ',  NULL, 4,  'system', 0),
('audit',             '审计日志',     'MENU', '/audit',             'READ',  NULL, 5,  'system', 0),
('system',            '系统设置',     'MENU', '/system',            'READ',  NULL, 6,  'system', 0),
('system:user',       '用户管理',     'BUTTON','/system/user',      'READ',  NULL, 1,  'system', 0),
('system:role',       '角色管理',     'BUTTON','/system/role',      'READ',  NULL, 2,  'system', 0);

-- 数据字典
INSERT INTO system.s_dict (dict_type, dict_code, dict_label, dict_value, sort_order, created_by, org_id) VALUES
('model_type',   'IMAGING',     '影像分析',     'IMAGING',     1, 'system', 0),
('model_type',   'NLP',         '自然语言处理', 'NLP',         2, 'system', 0),
('model_type',   'GENOMIC',     '基因组分析',   'GENOMIC',     3, 'system', 0),
('model_type',   'STRUCTURED',  '结构化数据',   'STRUCTURED',  4, 'system', 0),
('model_type',   'MULTIMODAL',  '多模态',       'MULTIMODAL',  5, 'system', 0),
('model_status', 'DRAFT',       '草稿',         'DRAFT',       1, 'system', 0),
('model_status', 'REGISTERED',  '已注册',       'REGISTERED',  2, 'system', 0),
('model_status', 'PUBLISHED',   '已发布',       'PUBLISHED',   3, 'system', 0),
('model_status', 'DEPRECATED',  '已弃用',       'DEPRECATED',  4, 'system', 0),
('framework',    'PYTORCH',     'PyTorch',      'PYTORCH',     1, 'system', 0),
('framework',    'TENSORFLOW',  'TensorFlow',   'TENSORFLOW',  2, 'system', 0),
('framework',    'SKLEARN',     'scikit-learn', 'SKLEARN',     3, 'system', 0),
('framework',    'XGBOOST',     'XGBoost',      'XGBOOST',     4, 'system', 0),
('framework',    'ONNX',        'ONNX',         'ONNX',        5, 'system', 0),
('task_type',    'CLASSIFICATION',         '分类',         'CLASSIFICATION',         1, 'system', 0),
('task_type',    'SEGMENTATION',           '分割',         'SEGMENTATION',           2, 'system', 0),
('task_type',    'OBJECT_DETECTION',       '目标检测',     'OBJECT_DETECTION',       3, 'system', 0),
('task_type',    'NAMED_ENTITY_RECOGNITION','命名实体识别','NAMED_ENTITY_RECOGNITION',4, 'system', 0),
('task_type',    'REGRESSION',             '回归',         'REGRESSION',             5, 'system', 0),
('gender',       'M',   '男', 'M', 1, 'system', 0),
('gender',       'F',   '女', 'F', 2, 'system', 0),
('gender',       'O',   '其他','O', 3, 'system', 0),
('encounter_type','OUTPATIENT','门诊','OUTPATIENT',1,'system',0),
('encounter_type','INPATIENT', '住院','INPATIENT', 2,'system',0),
('encounter_type','EMERGENCY', '急诊','EMERGENCY', 3,'system',0);

-- 系统配置
INSERT INTO system.s_config (config_key, config_value, config_type, description, created_by, org_id) VALUES
('jwt.secret',           'maidc-jwt-secret-key-2026-change-in-production', 'STRING', 'JWT签名密钥',     'system', 0),
('jwt.access-expiration','7200000',  'NUMBER', 'Access Token 有效期(ms)', 'system', 0),
('jwt.refresh-expiration','604800000','NUMBER', 'Refresh Token 有效期(ms)','system', 0),
('password.minLength',   '8',        'NUMBER', '密码最小长度',             'system', 0),
('file.maxSize',         '524288000','NUMBER', '文件上传最大值(bytes)',     'system', 0),
('login.maxAttempts',    '5',        'NUMBER', '登录最大尝试次数',         'system', 0),
('login.lockDuration',   '1800000',  'NUMBER', '锁定时长(ms)',             'system', 0);
