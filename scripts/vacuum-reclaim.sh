#!/bin/bash
# PostgreSQL 空间回收脚本
# 用法: ./vacuum-reclaim.sh [full|analyze]
#   full    - 执行 VACUUM FULL 回收磁盘空间给 OS（会锁表）
#   analyze - 仅执行 VACUUM ANALYZE 更新统计信息（不回收空间）
#   无参数  - 显示各表膨胀情况

PGHOST="${PGHOST:-maidc-postgres}"
PGUSER="${PGUSER:-maidc}"
PGDB="${PGDB:-maidc}"
PGPASSWORD="${PGPASSWORD:-maidc123}"
export PGPASSWORD

MODE="${1:-show}"

exec_sql() {
    psql -h "$PGHOST" -U "$PGUSER" -d "$PGDB" -c "$1"
}

show_bloat() {
    echo "=== 表空间与死元组统计 ==="
    psql -h "$PGHOST" -U "$PGUSER" -d "$PGDB" <<'SQL'
SELECT schemaname,
       relname AS table_name,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||relname)) AS total_size,
       n_live_tup AS live_rows,
       n_dead_tup AS dead_rows,
       CASE WHEN n_live_tup > 0
            THEN round(100.0 * n_dead_tup / (n_live_tup + n_dead_tup), 2)
            ELSE 0
       END AS bloat_pct,
       last_vacuum,
       last_autovacuum
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC;
SQL
}

vacuum_analyze() {
    echo "=== 执行 VACUUM ANALYZE 全部用户表 ==="
    psql -h "$PGHOST" -U "$PGUSER" -d "$PGDB" <<'SQL'
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT schemaname, relname FROM pg_stat_user_tables
    LOOP
        RAISE NOTICE 'VACUUM ANALYZE %.%', r.schemaname, r.relname;
        EXECUTE format('VACUUM ANALYZE %I.%I', r.schemaname, r.relname);
    END LOOP;
END;
$$;
SQL
    echo "完成。"
}

vacuum_full() {
    echo "=== 执行 VACUUM FULL 回收磁盘空间 ==="
    echo "注意：VACUUM FULL 会锁表，请在低峰期执行"
    read -p "确认继续？(y/N) " confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        echo "已取消。"
        exit 0
    fi

    psql -h "$PGHOST" -U "$PGUSER" -d "$PGDB" <<'SQL'
DO $$
DECLARE
    r RECORD;
    before_size BIGINT;
    after_size BIGINT;
BEGIN
    FOR r IN SELECT schemaname, relname
             FROM pg_stat_user_tables
             WHERE n_dead_tup > 1000
             ORDER BY pg_total_relation_size(schemaname||'.'||relname) DESC
    LOOP
        before_size := pg_total_relation_size(r.schemaname||'.'||r.relname);
        RAISE NOTICE 'VACUUM FULL %.%  (before: %)',
            r.schemaname, r.relname, pg_size_pretty(before_size);
        EXECUTE format('VACUUM FULL %I.%I', r.schemaname, r.relname);
        after_size := pg_total_relation_size(r.schemaname||'.'||r.relname);
        RAISE NOTICE '  -> after: %  (reclaimed: %)',
            pg_size_pretty(after_size), pg_size_pretty(before_size - after_size);
    END LOOP;
END;
$$;
SQL
    echo "完成。"
}

case "$MODE" in
    show)
        show_bloat
        ;;
    analyze)
        vacuum_analyze
        ;;
    full)
        vacuum_full
        ;;
    *)
        echo "用法: $0 [show|analyze|full]"
        exit 1
        ;;
esac
