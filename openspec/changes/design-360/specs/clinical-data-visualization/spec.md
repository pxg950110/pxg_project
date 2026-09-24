## ADDED Requirements

### Requirement: Lab test trend visualization
The system SHALL provide interactive line charts displaying lab test result trends over time for a patient across multiple encounters.

#### Scenario: Display lab trend chart
- **WHEN** user clicks a "View Trend" button for a specific lab test item (e.g., hemoglobin)
- **THEN** system displays an ECharts line chart with:
  - X-axis: sample time across encounters
  - Y-axis: test result value
  - Reference range bands shaded in background
  - Abnormal values marked with red dots
  - Tooltip showing exact value and date on hover

#### Scenario: Zoom into trend period
- **WHEN** user selects a date range on the chart using the data zoom slider
- **THEN** system redraws the chart to show only data within the selected period, preserving reference range bands

#### Scenario: Compare multiple lab items
- **WHEN** user selects multiple related lab items (e.g., WBC, RBC, Hemoglobin) to compare
- **THEN** system displays a multi-series chart with each item as a separate line, using distinct colors and legend

### Requirement: Vital signs visualization
The system SHALL provide charts displaying vital signs measurements (temperature, blood pressure, pulse, respiration rate) from clinical documents.

#### Scenario: Display vital signs chart
- **WHEN** user views encounter detail with vital signs data available
- **THEN** system displays an ECharts combination chart with:
  - Blood pressure shown as dual-series (systolic/diastolic) with area fill
  - Temperature as line chart
  - Pulse as line chart
  - Time axis aligned with encounter timeline
  - Interactive tooltips on hover

#### Scenario: Export chart as image
- **WHEN** user clicks "Export Chart" button on any visualization
- **THEN** system downloads the current chart view as a PNG image with 2x resolution

### Requirement: Chart performance with large datasets
The system SHALL render charts efficiently when displaying >100 data points without blocking the UI.

#### Scenario: Load chart with 200 lab results
- **WHEN** patient has 200+ lab test results across all encounters
- **THEN** system renders the trend chart within 2 seconds using canvas rendering mode, with lazy-loaded data series

### Requirement: Chart accessibility
The system SHALL ensure charts are accessible to users with visual impairments.

#### Scenario: Screen reader announces chart data
- **WHEN** screen reader user focuses on a chart component
- **THEN** system provides aria-label describing the chart title, data series, and key insights (e.g., "Hemoglobin trend chart showing values from 10.5 to 14.2 g/dL across 15 encounters, with 3 abnormal values below reference range")

#### Scenario: High contrast mode support
- **WHEN** user enables system high contrast mode
- **THEN** chart colors adapt to use high-contrast palette with distinct patterns for data series