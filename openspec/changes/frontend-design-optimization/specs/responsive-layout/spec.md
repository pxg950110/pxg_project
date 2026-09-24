# Responsive Layout

## ADDED Requirements

### Requirement: Three responsive breakpoints
The system SHALL support three responsive breakpoints: 1440px (small), 1920px (medium, default), and 2560px (large).

#### Scenario: Default medium breakpoint is active
- **WHEN** the viewport width is between 1440px and 2560px
- **THEN** the application uses medium breakpoint layout (sidebar 220px, content padding 24px)

#### Scenario: Small breakpoint adjusts layout
- **WHEN** the viewport width is less than 1440px
- **THEN** the sidebar width reduces to 200px
- **AND** the content padding reduces to 16px
- **AND** the `currentBreakpoint` composable returns `'sm'`

#### Scenario: Large breakpoint expands layout
- **WHEN** the viewport width is greater than or equal to 2560px
- **THEN** the sidebar width expands to 260px
- **AND** the content padding increases to 32px
- **AND** the `currentBreakpoint` composable returns `'lg'`

### Requirement: Sidebar responsive adaptation
The system SHALL adapt the sidebar width and behavior based on the responsive breakpoint.

#### Scenario: Sidebar width adjusts on resize
- **WHEN** the user resizes the browser window from 1920px to 1280px
- **THEN** the sidebar smoothly transitions from 220px to 200px width
- **AND** the sidebar content remains readable without horizontal scrolling

#### Scenario: Sidebar auto-collapses on small screens
- **WHEN** the viewport width drops below 1200px
- **THEN** the sidebar automatically collapses to icon-only mode (64px width)
- **AND** the `uiStore.sidebarCollapsed` state is set to `true`

#### Scenario: Sidebar can be manually toggled
- **WHEN** the user clicks the sidebar toggle icon
- **THEN** the sidebar collapses/expands regardless of viewport size
- **AND** the collapsed state persists in `uiStore`

### Requirement: Content area responsive adaptation
The system SHALL adjust the content area padding and layout based on the responsive breakpoint.

#### Scenario: Content padding adjusts on medium breakpoint
- **WHEN** the viewport width is 1920px (medium breakpoint)
- **THEN** the content area uses 24px padding on all sides
- **AND** cards within the content area use 16px gap

#### Scenario: Content padding increases on large breakpoint
- **WHEN** the viewport width is 2560px or greater
- **THEN** the content area uses 32px padding on all sides
- **AND** cards within the content area use 24px gap
- **AND** the content area max-width is constrained to prevent over-stretching

### Requirement: Dashboard card grid responsive adaptation
The system SHALL adjust the Dashboard metric card grid layout based on the responsive breakpoint.

#### Scenario: Dashboard shows 4 cards per row on medium breakpoint
- **WHEN** the viewport width is 1920px (medium breakpoint)
- **THEN** the Dashboard displays metric cards in a 4-column grid (each card span="6")
- **AND** the cards maintain equal width

#### Scenario: Dashboard shows 3 cards per row on small breakpoint
- **WHEN** the viewport width is less than 1440px
- **THEN** the Dashboard displays metric cards in a 3-column grid (each card span="8")
- **AND** the fourth card wraps to the next row

#### Scenario: Dashboard shows 4 cards per row on large breakpoint
- **WHEN** the viewport width is 2560px or greater
- **THEN** the Dashboard displays metric cards in a 4-column grid
- **AND** the card sizes increase proportionally to fill extra space

### Requirement: Table responsive adaptation
The system SHALL adapt table column widths and pagination based on the responsive breakpoint.

#### Scenario: Table columns adjust on small breakpoint
- **WHEN** the viewport width is less than 1440px
- **THEN** the ETL task table hides less important columns (e.g., "Created Time")
- **AND** the pagination shows "5/page" instead of "10/page"

#### Scenario: Table expands on large breakpoint
- **WHEN** the viewport width is 2560px or greater
- **THEN** the table shows all columns with comfortable spacing
- **AND** the table can display up to 15 rows without scrolling

### Requirement: useResponsive composable API
The system SHALL provide a `useResponsive` composable for accessing responsive breakpoint information.

#### Scenario: Composable returns current breakpoint
- **WHEN** a component calls `useResponsive()`
- **THEN** it returns `{ currentBreakpoint: 'sm' | 'md' | 'lg' }`

#### Scenario: Composable updates on window resize
- **WHEN** the user resizes the browser window
- **THEN** the `currentBreakpoint` value updates reactively
- **AND** components using the composable re-render with new breakpoint

#### Scenario: Composable cleans up event listeners
- **WHEN** a component using `useResponsive` is unmounted
- **THEN** the window resize event listener is removed
- **AND** no memory leaks occur