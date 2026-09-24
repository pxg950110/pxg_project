## ADDED Requirements

### Requirement: PDF report generation
The system SHALL allow users to generate comprehensive PDF reports of patient encounter data.

#### Scenario: Generate single encounter report
- **WHEN** user clicks "Export PDF" button on encounter detail view and selects "Current Encounter"
- **THEN** system generates a PDF document containing:
  - Patient demographics header (name, age, gender, patient ID)
  - Encounter basic information (admission date, department, attending physician)
  - All diagnosis records in tabular format
  - Lab test results with abnormal values highlighted
  - Medication list with dosages and frequencies
  - Procedure/surgery records if applicable
  - Footer with generation timestamp and user ID

#### Scenario: Generate full patient history report
- **WHEN** user clicks "Export PDF" button and selects "All Encounters"
- **THEN** system generates a multi-page PDF containing all encounters in chronological order with:
  - Summary page listing all encounters with dates and primary diagnoses
  - Detailed sections for each encounter following the single-encounter format
  - Table of contents with page numbers
  - Page headers showing patient name and patient ID

#### Scenario: Generate filtered encounters report
- **WHEN** user has applied filters to the encounter timeline and clicks "Export PDF"
- **THEN** system generates PDF containing only the currently filtered encounters
- PDF header indicates filter criteria applied (e.g., "Encounters from 2023 with diabetes diagnosis")

### Requirement: Report customization
The system SHALL allow users to select which data sections to include in the PDF report.

#### Scenario: Select report sections
- **WHEN** user clicks "Export PDF" and opens report options dialog
- **THEN** system displays checkboxes for each data section:
  - ☑ Basic Information (always included)
  - ☑ Diagnoses
  - ☑ Lab Tests
  - ☐ Imaging Exams (unchecked by default)
  - ☑ Medications
  - ☐ Procedures (unchecked by default)
- User can toggle sections before generating report

#### Scenario: Include charts in report
- **WHEN** user checks "Include Charts" option in report settings
- **THEN** generated PDF includes embedded trend charts for lab tests with >3 data points, rendered as static images

### Requirement: Report branding
The system SHALL include hospital branding elements in PDF reports.

#### Scenario: Hospital letterhead
- **WHEN** PDF report is generated
- **THEN** report includes hospital logo, name, and address in the header
- Hospital branding is configurable in system settings

#### Scenario: Confidentiality notice
- **WHEN** PDF report is generated
- **THEN** report includes confidentiality notice in footer: "CONFIDENTIAL - For authorized medical personnel only"

### Requirement: Report file naming
The system SHALL generate PDF files with descriptive names based on patient and date.

#### Scenario: Single encounter report filename
- **WHEN** user exports encounter dated 2023-05-15 for patient "Zhang San"
- **THEN** PDF file is named: `Patient_ZhangSan_Encounter_20230515_20260605.pdf` (with generation date)

#### Scenario: Full history report filename
- **WHEN** user exports all encounters for patient "Zhang San"
- **THEN** PDF file is named: `Patient_ZhangSan_FullHistory_20260605.pdf`

### Requirement: Report generation performance
The system SHALL generate PDF reports within acceptable time limits and provide progress feedback.

#### Scenario: Progress indicator for large reports
- **WHEN** user generates report for patient with >50 encounters
- **THEN** system displays progress indicator: "Generating report... (Encounter 15 of 52)"
- Report generation completes within 30 seconds

#### Scenario: Report generation failure handling
- **WHEN** PDF generation fails due to data inconsistency or system error
- **THEN** system displays error message: "Report generation failed. Please try again or contact support if the problem persists."
- Error details are logged for troubleshooting

### Requirement: Report access control
The system SHALL enforce access control on PDF report generation.

#### Scenario: Unauthorized access prevention
- **WHEN** user without "Export Patient Data" permission attempts to generate PDF report
- **THEN** system disables the "Export PDF" button and displays tooltip: "You do not have permission to export patient data"

#### Scenario: Audit log entry
- **WHEN** user successfully generates a PDF report
- **THEN** system creates audit log entry recording: user ID, patient ID, report type (single/full), timestamp, and IP address