## ADDED Requirements

### Requirement: Encounter search by keyword
The system SHALL allow users to search within encounter data using keywords for diagnoses, medications, and procedures.

#### Scenario: Search encounters by diagnosis keyword
- **WHEN** user types "diabetes" in the encounter search box
- **THEN** system filters the encounter timeline to show only encounters with diagnosis containing "diabetes" in diagnosis name or ICD code
- Timeline displays result count: "Showing 5 of 23 encounters matching 'diabetes'"

#### Scenario: Search encounters by medication name
- **WHEN** user types "aspirin" in the encounter search box
- **THEN** system filters encounter timeline to show only encounters where patient was prescribed aspirin or related medications
- Filtered results include encounters with "Aspirin", "ASA", or brand names containing "aspirin"

#### Scenario: Clear search filter
- **WHEN** user clicks "Clear" button or deletes search text
- **THEN** system removes filter and displays all encounters for the patient

### Requirement: Encounter filtering by date range
The system SHALL allow users to filter encounters by admission date range.

#### Scenario: Filter encounters by date range
- **WHEN** user selects date range "2023-01-01" to "2023-12-31" in the date filter
- **THEN** system filters encounter timeline to show only encounters with admission date within the selected range
- Date filter persists in URL as query parameter: `?from=2023-01-01&to=2023-12-31`

#### Scenario: Quick date preset selection
- **WHEN** user clicks "Last 6 Months" preset button
- **THEN** system automatically sets date range to current date minus 6 months to current date, and filters encounters accordingly

### Requirement: Encounter filtering by department
The system SHALL allow users to filter encounters by department name.

#### Scenario: Filter encounters by department
- **WHEN** user selects "Cardiology" from department dropdown filter
- **THEN** system filters encounter timeline to show only encounters where department name matches "Cardiology"
- Dropdown displays all unique departments from patient's encounter history

### Requirement: Encounter filtering by encounter type
The system SHALL allow users to filter encounters by type (inpatient, outpatient, emergency).

#### Scenario: Filter encounters by type
- **WHEN** user checks "Inpatient" checkbox in encounter type filter
- **THEN** system filters encounter timeline to show only encounters with encounterType = "INPATIENT"
- User can select multiple types (e.g., both Inpatient and Emergency)

### Requirement: Combined filtering
The system SHALL allow users to apply multiple filters simultaneously with AND logic.

#### Scenario: Apply combined filters
- **WHEN** user filters by: diagnosis keyword "diabetes" AND date range "2023" AND encounter type "Inpatient"
- **THEN** system filters encounter timeline to show only encounters that meet ALL criteria:
  - Diagnosis contains "diabetes"
  - Admission date within 2023
  - Encounter type is inpatient

#### Scenario: URL persistence for combined filters
- **WHEN** user applies combined filters and copies the browser URL
- **THEN** URL contains all filter parameters: `?search=diabetes&from=2023-01-01&to=2023-12-31&type=INPATIENT`
- Opening the URL in a new tab restores all filters

### Requirement: Filter performance
The system SHALL apply filters to encounter lists within 500ms for typical datasets (<200 encounters).

#### Scenario: Filter response time
- **WHEN** user applies any single filter or combined filter on patient with 150 encounters
- **THEN** system displays filtered results within 500ms
- Loading spinner is shown if filtering takes >200ms

### Requirement: Filter UI placement
The system SHALL display filter controls prominently above the encounter timeline panel.

#### Scenario: Filter controls visibility
- **WHEN** user opens Patient Encounter 360 view
- **THEN** filter controls (search box, date picker, department dropdown, type checkboxes) are displayed at the top of the left sidebar, above the encounter timeline
- Active filters are shown as removable tags below the filter controls