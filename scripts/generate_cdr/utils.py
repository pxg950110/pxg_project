"""Utility functions: ID generator, CSV writer, date helpers."""

import csv
import os
import random
from datetime import datetime, date, timedelta


class Seq:
    """Sequential ID generator simulating BIGSERIAL."""
    def __init__(self, start=1):
        self._id = start - 1

    def next(self):
        self._id += 1
        return self._id


class Ctx:
    """Shared context for passing generated IDs between generators."""
    def __init__(self):
        self.seq = Seq()
        self.orgs = []          # [{id, org_code, org_id}]
        self.patients = []      # [{id, org_id, gender, birth_date, ...}]
        self.caregivers = []    # [{id, code}]
        self.encounters = []    # [{id, patient_id, encounter_type, org_id, admit, discharge, dept_code, ...}]
        self.icu_stays = []     # [{id, encounter_id, patient_id, org_id}]
        self.lab_tests = []     # [{id, encounter_id, patient_id, org_id}]
        self.imaging_exams = [] # [{id, encounter_id, patient_id, org_id}]
        self.checkups = []      # [{id, patient_id, org_id, checkup_date}]
        self.checkup_pkgs = []  # [{id, checkup_id, org_id}]


def rand_date(start=date(2020, 1, 1), end=date(2025, 12, 31)):
    delta = (end - start).days
    return start + timedelta(days=random.randint(0, delta))


def rand_datetime(start=date(2020, 1, 1), end=date(2025, 12, 31)):
    d = rand_date(start, end)
    return datetime(d.year, d.month, d.day,
                    random.randint(0, 23), random.randint(0, 59), random.randint(0, 59))


def rand_date_between(d1, d2):
    if d1 > d2:
        d1, d2 = d2, d1
    delta = (d2 - d1).days
    return d1 + timedelta(days=random.randint(0, max(delta, 1)))


def rand_datetime_after(dt, min_hours=1, max_hours=72):
    hours = random.randint(min_hours, max_hours)
    return dt + timedelta(hours=hours)


def fmt_dt(dt):
    if dt is None:
        return ''
    return dt.strftime('%Y-%m-%d %H:%M:%S')


def fmt_date(d):
    if d is None:
        return ''
    return d.strftime('%Y-%m-%d')


def fmt_bool(b):
    return 't' if b else 'f'


def rand_float(mean, std, lo=None, hi=None, decimals=2):
    v = random.gauss(mean, std)
    if lo is not None:
        v = max(lo, v)
    if hi is not None:
        v = min(hi, v)
    return round(v, decimals)


def choice_weighted(options_weights):
    """Pick from options with given weights. [(option, weight), ...]"""
    options, weights = zip(*options_weights)
    total = sum(weights)
    r = random.uniform(0, total)
    cumsum = 0
    for opt, w in zip(options, weights):
        cumsum += w
        if r <= cumsum:
            return opt
    return options[-1]


def write_csv(filename, headers, rows, output_dir):
    os.makedirs(output_dir, exist_ok=True)
    path = os.path.join(output_dir, filename)
    with open(path, 'w', newline='', encoding='utf-8-sig') as f:
        w = csv.writer(f)
        w.writerow(headers)
        w.writerows(rows)
    print(f'  {filename}: {len(rows)} rows')
    return path
