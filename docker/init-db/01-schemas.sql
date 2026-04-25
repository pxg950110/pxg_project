-- MAIDC Schema 初始化
-- 5 个业务 Schema + 扩展

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE SCHEMA IF NOT EXISTS system;
CREATE SCHEMA IF NOT EXISTS ods;
CREATE SCHEMA IF NOT EXISTS cdr;
CREATE SCHEMA IF NOT EXISTS rdr;
CREATE SCHEMA IF NOT EXISTS model;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS masterdata;

COMMENT ON SCHEMA system IS '系统管理（用户/角色/权限/字典/配置）';
COMMENT ON SCHEMA ods     IS '操作数据存储层（MIMIC原始数据1:1映射）';
COMMENT ON SCHEMA cdr     IS '临床数据仓库（患者/就诊/检验/影像等）';
COMMENT ON SCHEMA rdr     IS '研究数据仓库（项目/队列/数据集/ETL）';
COMMENT ON SCHEMA model   IS '模型管理（注册/版本/评估/部署/监控）';
COMMENT ON SCHEMA audit   IS '审计日志（操作/数据访问/系统事件）';
COMMENT ON SCHEMA masterdata IS '主数据管理（编码体系/标准概念/映射）';

GRANT USAGE ON SCHEMA system, ods, cdr, rdr, model, audit, masterdata TO maidc;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA system, ods, cdr, rdr, model, audit, masterdata TO maidc;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA system, ods, cdr, rdr, model, audit, masterdata TO maidc;
ALTER DEFAULT PRIVILEGES IN SCHEMA system, ods, cdr, rdr, model, audit, masterdata GRANT ALL PRIVILEGES ON TABLES TO maidc;
ALTER DEFAULT PRIVILEGES IN SCHEMA system, ods, cdr, rdr, model, audit, masterdata GRANT ALL PRIVILEGES ON SEQUENCES TO maidc;
