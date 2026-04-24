"""Main orchestrator for CDR synthetic data generation."""

import os
import sys
import time

# Add parent directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from generate_cdr.utils import Ctx, write_csv
from generate_cdr.config import OUTPUT_DIR, NUM_PATIENTS
from generate_cdr.gen_dictionary import generate_dict_tables
from generate_cdr.gen_foundation import generate_foundation
from generate_cdr.gen_encounter import generate_encounters
from generate_cdr.gen_clinical import generate_clinical
from generate_cdr.gen_extended import generate_extended
from generate_cdr.gen_care import generate_care
from generate_cdr.gen_checkup import generate_checkup
from generate_cdr.gen_icu import generate_icu
from generate_cdr.gen_pharmacy import generate_pharmacy


def main():
    start = time.time()
    output_dir = os.path.abspath(os.path.join(os.path.dirname(os.path.dirname(
        os.path.abspath(__file__))), OUTPUT_DIR))
    os.makedirs(output_dir, exist_ok=True)

    ctx = Ctx()

    print(f'=== MAIDC CDR Synthetic Data Generator ===')
    print(f'Patients: {NUM_PATIENTS}')
    print(f'Output:   {output_dir}')
    print()

    # Generate in dependency order
    steps = [
        ('1. Dictionary tables', generate_dict_tables),
        ('2. Foundation (org/patient)', generate_foundation),
        ('3. Encounters', generate_encounters),
        ('4. Clinical (diag/lab/med/vital)', generate_clinical),
        ('5. Extended (imaging/path/op/note)', generate_extended),
        ('6. Care & Admin', generate_care),
        ('7. Health Checkup', generate_checkup),
        ('8. ICU', generate_icu),
        ('9. Pharmacy/Orders', generate_pharmacy),
    ]

    for label, func in steps:
        t0 = time.time()
        print(f'[{label}]')
        func(ctx, output_dir)
        elapsed = time.time() - t0
        print(f'  ({elapsed:.1f}s)')
        print()

    total_time = time.time() - start
    total_ids = ctx.seq._id
    print(f'=== Done ===')
    print(f'Total rows generated: {total_ids}')
    print(f'Total time: {total_time:.1f}s')
    print(f'Next sequential ID: {total_ids + 1}')


if __name__ == '__main__':
    main()
