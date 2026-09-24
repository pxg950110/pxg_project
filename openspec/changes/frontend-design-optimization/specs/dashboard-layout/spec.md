# Dashboard Layout Optimization

## MODIFIED Requirements

### Requirement: Metric card spacing and visual hierarchy
The Dashboard SHALL display metric cards with optimized spacing and visual hierarchy to improve readability and information density.

#### Scenario: Metric cards use consistent spacing
- **WHEN** the Dashboard renders metric cards
- **THEN** the cards use 16px gap between columns and rows on medium breakpoint
- **AND** the cards use `var(--spacing-md)` for padding inside the card
- **AND** the card border radius uses `var(--radius-lg)` (8px)

#### Scenario: Metric value font size adapts to importance
- **WHEN** MetricCard renders the value number
- **THEN** the font size is 28px for primary metrics (患者总数, 研究项目)
- **AND** the font weight is 600
- **AND** the color uses `var(--color-text)` for high contrast

#### Scenario: Metric card shows trend indicator
- **WHEN** MetricCard displays a trend (上升/下降)
- **THEN** the trend icon and percentage use `var(--color-success)` for upward trends
- **AND** the trend icon and percentage use `var(--color-error)` for downward trends
- **AND** the trend text is smaller than the main value (14px vs 28px)

### Requirement: Dashboard section layout optimization
The Dashboard SHALL organize sections with clear visual hierarchy and appropriate spacing.

#### Scenario: Dashboard sections use consistent vertical spacing
- **WHEN** the Dashboard renders multiple sections (metrics, charts, table)
- **THEN** each section uses 24px top margin (using `var(--spacing-lg)`)
- **AND** section cards use `var(--shadow-sm)` for subtle depth

#### Scenario: Chart cards maintain aspect ratio
- **WHEN** the Dashboard displays metric charts (data growth, source distribution)
- **THEN** the chart height is 320px on medium breakpoint
- **AND** the chart cards use equal width (span="12")
- **AND** the chart cards use `var(--radius-lg)` for border radius

#### Scenario: ETL task table shows recent tasks
- **WHEN** the Dashboard displays the ETL task table
- **THEN** the table shows the 5 most recent tasks
- **AND** the table uses compact row height (size="middle")
- **AND** the status column uses StatusBadge component with semantic colors

## ADDED Requirements

### Requirement: Dashboard skeleton loading
The Dashboard SHALL display skeleton placeholders during initial data loading.

#### Scenario: Metric cards show skeleton during load
- **WHEN** the Dashboard is fetching metric data
- **THEN** each MetricCard displays a skeleton with:
  - Gray placeholder for the title (width: 60%)
  - Gray placeholder for the value (width: 40%, height: 28px)
  - Pulsing animation (opacity 0.3 → 0.6)
- **AND** the skeleton matches the actual card dimensions

#### Scenario: Charts show skeleton during load
- **WHEN** chart data is loading
- **THEN** the chart area displays a gray rectangle with pulsing animation
- **AND** the chart skeleton maintains the 320px height

### Requirement: Dashboard responsive grid
The Dashboard SHALL adapt the card grid layout based on the responsive breakpoint.

#### Scenario: Medium breakpoint shows 4-column metric grid
- **WHEN** viewport width is 1920px (medium breakpoint)
- **THEN** the metric cards render in 4 columns (each span="6")
- **AND** the row gutter is [16, 16] (horizontal, vertical)

#### Scenario: Small breakpoint shows 3-column metric grid
- **WHEN** viewport width is less than 1440px
- **THEN** the metric cards render in 3 columns (each span="8")
- **AND** the fourth card wraps to the second row

#### Scenario: Large breakpoint shows 4-column grid with larger cards
- **WHEN** viewport width is 2560px or greater
- **THEN** the metric cards render in 4 columns
- **AND** the cards scale up proportionally to fill extra space
- **AND** the row gutter increases to [24, 24]