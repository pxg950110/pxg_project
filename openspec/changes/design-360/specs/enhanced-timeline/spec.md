## ADDED Requirements

### Requirement: Timeline visual status indicators
The system SHALL display visual status indicators on the encounter timeline to show encounter status and severity.

#### Scenario: Current vs. historical encounter indicator
- **WHEN** user views encounter timeline
- **THEN** current active encounter displays solid blue circle (●) on timeline
- Historical encounters display hollow gray circle (○) on timeline
- Current encounter node is larger than historical nodes (40px vs 24px diameter)

#### Scenario: Severity color coding
- **WHEN** encounter has severity level recorded (critical, serious, stable)
- **THEN** timeline node displays with color-coded border:
  - Critical: Red border (#FF0000)
  - Serious: Orange border (#FFA500)
  - Stable: Green border (#4CAF50)
- Hovering over node shows tooltip with severity level text

### Requirement: Timeline mini-preview
The system SHALL display a preview tooltip when hovering over encounter nodes on the timeline.

#### Scenario: Encounter mini-preview on hover
- **WHEN** user hovers mouse over an encounter node on timeline
- **THEN** system displays a preview card (300px wide) showing:
  - Encounter date (e.g., "2023-05-15")
  - Department name
  - Primary diagnosis
  - Encounter type icon (🏥 for inpatient, 🏥 for outpatient, 🚑 for emergency)
  - Quick stats: "3 diagnoses, 5 lab tests, 2 imaging"
- Preview appears within 200ms of hover start

#### Scenario: Preview positioning
- **WHEN** user hovers over an encounter node near the top of the viewport
- **THEN** preview card appears below the node to avoid clipping
- When hovering over a node near the bottom, preview appears above the node

### Requirement: Timeline navigation improvements
The system SHALL provide enhanced navigation controls on the encounter timeline.

#### Scenario: Jump to first/last encounter
- **WHEN** user clicks "Jump to Oldest" button on timeline
- **THEN** timeline scrolls to show the earliest encounter and selects it
- "Jump to Newest" button scrolls to and selects the most recent encounter

#### Scenario: Timeline zoom controls
- **WHEN** user clicks "Zoom In" button on timeline
- **THEN** timeline expands vertically to show more details for each encounter node (larger nodes, expanded preview)
- "Zoom Out" button collapses timeline to show more encounters in the same viewport

#### Scenario: Timeline date scale display
- **WHEN** user views timeline
- **THEN** vertical date scale is displayed on the left side showing: year markers, month markers for current year, and day markers for current month

### Requirement: Timeline filtering feedback
The system SHALL provide clear visual feedback when filters are applied to the timeline.

#### Scenario: Filtered timeline indicator
- **WHEN** user applies filters to encounters (e.g., by diagnosis keyword)
- **THEN** timeline header displays indicator: "Showing 12 of 45 encounters (filtered)"
- Non-matching encounter nodes are dimmed (opacity: 0.3) but remain visible
- Matching nodes are highlighted with subtle glow effect

#### Scenario: Clear filters from timeline
- **WHEN** filters are active and user clicks "Clear Filters" button on timeline header
- **THEN** timeline immediately restores all encounters to full visibility and removes filter indicator

### Requirement: Timeline chronological grouping
The system SHALL optionally group encounters by year or month when there are many encounters.

#### Scenario: Group encounters by year
- **WHEN** patient has >30 encounters and user selects "Group by Year" view
- **THEN** timeline displays collapsible year groups (e.g., "2024 (15 encounters)", "2023 (20 encounters)")
- Clicking a year group expands to show individual encounters for that year
- Current year group is expanded by default

#### Scenario: Group encounters by month
- **WHEN** user selects "Group by Month" view
- **THEN** timeline displays collapsible month groups (e.g., "May 2024 (3 encounters)", "April 2024 (5 encounters)")
- Most recent month is expanded by default

### Requirement: Timeline accessibility
The system SHALL ensure timeline is fully navigable via keyboard and screen readers.

#### Scenario: Keyboard navigation
- **WHEN** user presses Tab key on timeline
- **THEN** focus moves between encounter nodes in chronological order
- Arrow keys navigate to previous/next encounter node
- Enter key selects the focused encounter

#### Scenario: Screen reader announces encounter details
- **WHEN** screen reader user navigates to an encounter node
- **THEN** screen reader announces: "Encounter on [date] at [department], primary diagnosis: [diagnosis], [encounter type], [encounters remaining] of [total]"

### Requirement: Timeline performance with many encounters
The system SHALL render timeline efficiently when patient has >100 encounters.

#### Scenario: Virtualized timeline rendering
- **WHEN** patient has 150 encounters
- **THEN** timeline uses virtual scrolling to render only visible nodes (approximately 20-30 nodes)
- Scrolling performance remains smooth (60fps) without jank
- All encounter data is loaded in background for instant preview display