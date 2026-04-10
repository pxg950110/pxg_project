-- MAIDC Data Integrity Verification
-- Run after full DDL execution to verify all tables exist

-- Check schema count
SELECT schema_name, COUNT(*) as table_count
FROM information_schema.tables
WHERE schema_name IN ('system', 'cdr', 'rdr', 'model', 'audit')
  AND table_type = 'BASE TABLE'
GROUP BY schema_name
ORDER BY schema_name;

-- Expected: system(7), cdr(28), rdr(19), model(11), audit(3) = 68 tables

-- Verify system tables
SELECT 'system.' || table_name as table_name
FROM information_schema.tables
WHERE schema_name = 'system' AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- Verify model tables
SELECT 'model.' || table_name as table_name
FROM information_schema.tables
WHERE schema_name = 'model' AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- Verify initial data
SELECT 'Users' as check_name, COUNT(*) as count FROM system.s_user WHERE is_deleted = false
UNION ALL
SELECT 'Roles', COUNT(*) FROM system.s_role WHERE is_deleted = false
UNION ALL
SELECT 'Permissions', COUNT(*) FROM system.s_permission
UNION ALL
SELECT 'Dictionaries', COUNT(*) FROM system.s_dict WHERE is_deleted = false
UNION ALL
SELECT 'Configs', COUNT(*) FROM system.s_config WHERE is_deleted = false;

-- Expected: admin user, 6 roles, permission tree, dictionaries, configs

-- Verify foreign key constraints
SELECT tc.constraint_name, tc.table_schema || '.' || tc.table_name as table_name,
       kcu.column_name, ccu.table_schema || '.' || ccu.table_name AS foreign_table
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_schema IN ('system', 'cdr', 'rdr', 'model', 'audit')
ORDER BY tc.table_schema, tc.table_name;

-- Verify indexes
SELECT schemaname || '.' || tablename as table_name, indexname
FROM pg_indexes
WHERE schemaname IN ('system', 'cdr', 'rdr', 'model', 'audit')
ORDER BY schemaname, tablename, indexname;
