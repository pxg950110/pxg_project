-- ============================================================
-- ODS (Operational Data Store) Schema Utilities
-- Raw MIMIC data staging layer - 1:1 CSV mapping
-- ============================================================

-- Helper function: create monthly partitions for a parent table
-- Covers MIMIC shifted date range (2100-01 through 2210-12)
CREATE OR REPLACE FUNCTION ods.create_monthly_partitions(
    parent_table TEXT,
    start_year   INT DEFAULT 2100,
    end_year     INT DEFAULT 2210
) RETURNS VOID AS $$
DECLARE
    partition_name TEXT;
    partition_start DATE;
    partition_end   DATE;
BEGIN
    FOR y IN start_year..end_year LOOP
        FOR m IN 1..12 LOOP
            partition_name := parent_table || '_' || to_char(make_date(y, m, 1), 'YYYY_MM');
            partition_start := make_date(y, m, 1);

            IF m = 12 THEN
                partition_end := make_date(y + 1, 1, 1);
            ELSE
                partition_end := make_date(y, m + 1, 1);
            END IF;

            EXECUTE format(
                'CREATE TABLE IF NOT EXISTS %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
                partition_name, parent_table, partition_start, partition_end
            );
        END LOOP;
    END LOOP;

    -- Default partition for any out-of-range data
    EXECUTE format(
        'CREATE TABLE IF NOT EXISTS %I PARTITION OF %I DEFAULT',
        parent_table || '_default', parent_table
    );
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION ods.create_monthly_partitions IS '为 ODS 分区表按月生成子分区，覆盖 MIMIC 偏移日期范围 2100-2210';
