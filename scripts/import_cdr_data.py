"""Import generated CDR CSV data into PostgreSQL.

Usage:
    python scripts/import_cdr_data.py              # Import all tables
    python scripts/import_cdr_data.py --truncate    # Truncate + Import
    python scripts/import_cdr_data.py --verify      # Verify row counts only
    python scripts/import_cdr_data.py --tables c_patient c_encounter  # Import specific tables
"""

import argparse
import csv
import os
import sys
import psycopg2
from psycopg2 import sql

DB_CONFIG = {
    'host': 'localhost',
    'port': 5432,
    'dbname': 'maidc',
    'user': 'maidc',
    'password': 'maidc123',
    'options': '-c search_path=cdr',
}

DATA_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                        'data', 'cdr-synthetic')

# Import order (respects FK dependencies)
TABLE_ORDER = [
    # Tier 0: Dictionary (no FK)
    'c_dict_icd_diagnosis',
    'c_dict_icd_procedure',
    'c_dict_lab_item',
    'c_dict_item',
    'c_dict_procedure_code',
    # Tier 1: Foundation (no FK except org parent_id self-ref)
    'c_org',
    'c_caregiver',
    'c_patient',
    # Tier 2: Patient-level (FK to c_patient)
    'c_patient_contact',
    'c_patient_insurance',
    'c_allergy',
    'c_family_history',
    # Tier 3: Encounters (FK to c_patient, c_org)
    'c_encounter',
    'c_patient_bed',
    # Tier 4: Clinical (FK to c_encounter, c_patient)
    'c_diagnosis',
    'c_lab_test',
    'c_lab_panel',
    'c_medication',
    'c_vital_sign',
    # Tier 5: Extended (FK to c_encounter, c_patient, c_imaging_exam)
    'c_imaging_exam',
    'c_imaging_finding',
    'c_pathology',
    'c_operation',
    'c_clinical_note',
    # Tier 6: Care & Admin
    'c_nursing_record',
    'c_blood_transfusion',
    'c_transfer',
    'c_fee_record',
    'c_discharge_summary',
    # Tier 7: Checkup
    'c_health_checkup',
    'c_checkup_package',
    'c_checkup_item_result',
    'c_checkup_summary',
    'c_checkup_comparison',
    # Tier 8: ICU (FK to c_encounter, c_patient)
    'c_icu_stay',
    'c_input_event',
    'c_output_event',
    'c_microbiology',
    'c_procedure_event',
    'c_datetime_event',
    'c_ingredient_event',
    # Tier 9: Pharmacy/Orders/Billing
    'c_pharmacy_order',
    'c_provider_order',
    'c_med_admin',
    'c_drg_code',
    'c_cpt_event',
]


def get_connection():
    return psycopg2.connect(**DB_CONFIG)


def get_conn_str():
    return f"postgresql://{DB_CONFIG['user']}:{DB_CONFIG['password']}@{DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['dbname']}"


def truncate_all(cur):
    """Truncate all CDR tables in reverse dependency order."""
    print('Truncating all CDR tables...')
    reversed_order = list(reversed(TABLE_ORDER))
    tables_sql = ', '.join(f'cdr.{t}' for t in reversed_order)
    cur.execute(f'TRUNCATE TABLE {tables_sql} CASCADE;')
    print(f'  Truncated {len(TABLE_ORDER)} tables.')


def import_table(cur, table_name):
    """Import a single CSV file into a table using PostgreSQL COPY."""
    csv_path = os.path.join(DATA_DIR, f'{table_name}.csv')
    if not os.path.exists(csv_path):
        print(f'  SKIP {table_name}: CSV not found')
        return 0

    with open(csv_path, 'r', encoding='utf-8-sig') as f:
        reader = csv.reader(f)
        headers = next(reader)

        # Check CSV has data rows
        first_data = None
        rows = []
        for row in reader:
            if first_data is None:
                first_data = row
            rows.append(row)

    if not rows:
        print(f'  SKIP {table_name}: 0 data rows')
        return 0

    # Build COPY statement
    cols = ', '.join(headers)
    copy_sql = f'COPY cdr.{table_name} ({cols}) FROM STDIN WITH (FORMAT csv)'

    # Use copy_expert with the actual data
    with open(csv_path, 'r', encoding='utf-8-sig') as f:
        # Skip header
        next(f)
        cur.copy_expert(copy_sql, f)

    return len(rows)


def verify_counts(cur):
    """Verify row counts in all CDR tables."""
    print('\n=== Row Count Verification ===')
    total = 0
    for table in TABLE_ORDER:
        try:
            cur.execute(f'SELECT COUNT(*) FROM cdr.{table}')
            count = cur.fetchone()[0]
            total += count

            # Compare with CSV
            csv_path = os.path.join(DATA_DIR, f'{table}.csv')
            csv_count = 0
            if os.path.exists(csv_path):
                with open(csv_path, 'r', encoding='utf-8-sig') as f:
                    csv_count = sum(1 for _ in f) - 1  # subtract header

            status = 'OK' if count == csv_count else f'MISMATCH (csv={csv_count})'
            if count > 0:
                print(f'  {table}: {count:,} {status}')
        except Exception as e:
            print(f'  {table}: ERROR - {e}')

    print(f'\n  Total rows in DB: {total:,}')


def main():
    parser = argparse.ArgumentParser(description='Import CDR synthetic data into PostgreSQL')
    parser.add_argument('--truncate', action='store_true', help='Truncate tables before import')
    parser.add_argument('--verify', action='store_true', help='Only verify row counts, no import')
    parser.add_argument('--tables', nargs='+', help='Import specific tables only')
    args = parser.parse_args()

    if not os.path.exists(DATA_DIR):
        print(f'Data directory not found: {DATA_DIR}')
        print('Run "python scripts/generate_cdr/main.py" first.')
        sys.exit(1)

    try:
        conn = get_connection()
        conn.autocommit = False
        cur = conn.cursor()
        print(f'Connected to {DB_CONFIG["host"]}:{DB_CONFIG["port"]}/{DB_CONFIG["dbname"]}')
    except Exception as e:
        print(f'Connection failed: {e}')
        print('Make sure PostgreSQL is running and the maidc database exists.')
        sys.exit(1)

    try:
        if args.verify:
            verify_counts(cur)
            return

        if args.truncate:
            truncate_all(cur)
            conn.commit()

        # Import tables
        tables = args.tables if args.tables else TABLE_ORDER
        print(f'\nImporting {len(tables)} tables...')
        total_rows = 0
        for table in tables:
            t0 = __import__('time').time()
            try:
                n = import_table(cur, table)
                total_rows += n
                elapsed = __import__('time').time() - t0
                if n > 0:
                    print(f'  {table}: {n:,} rows ({elapsed:.1f}s)')
                conn.commit()
            except Exception as e:
                conn.rollback()
                print(f'  {table}: FAILED - {e}')

        print(f'\nImport complete. Total: {total_rows:,} rows.')

        # Verify
        verify_counts(cur)

    finally:
        cur.close()
        conn.close()


if __name__ == '__main__':
    main()
