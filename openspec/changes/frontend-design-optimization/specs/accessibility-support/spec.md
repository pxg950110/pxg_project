# Accessibility Support

## ADDED Requirements

### Requirement: Semantic HTML structure
The system SHALL use semantic HTML elements to convey document structure and meaning.

#### Scenario: Main layout uses semantic landmarks
- **WHEN** the BasicLayout renders
- **THEN** the sidebar uses `<aside>` element
- **AND** the header uses `<header>` element
- **AND** the main content area uses `<main>` element
- **AND** each landmark has an appropriate `role` attribute if needed

#### Scenario: Navigation uses semantic list
- **WHEN** the SidebarMenu renders navigation items
- **THEN** the menu items are wrapped in `<nav>` element
- **AND** each menu item uses `<a>` or `<button>` element (not `<div>` or `<span>`)

#### Scenario: Tables use semantic markup
- **WHEN** a data table renders
- **THEN** the table uses `<thead>` and `<tbody>` elements
- **AND** header cells use `<th>` with `scope="col"` attribute
- **AND** data cells use `<td>` elements

### Requirement: ARIA labels for interactive elements
The system SHALL provide ARIA labels for all interactive elements (buttons, links, form controls).

#### Scenario: Icon-only buttons have aria-label
- **WHEN** a button contains only an icon (no text)
- **THEN** the button has an `aria-label` attribute describing the action
- **EXAMPLE** `<button aria-label="关闭侧边栏"><CloseOutlined /></button>`

#### Scenario: Form inputs have associated labels
- **WHEN** a form input renders
- **THEN** the input has an associated `<label>` element
- **OR** the input has an `aria-label` attribute if a visible label is not present

#### Scenario: MetricCard provides accessible name
- **WHEN** MetricCard renders with title "患者总数"
- **THEN** the card element has `role="article"` and `aria-label="患者总数: 1234人"`

### Requirement: Keyboard navigation support
The system SHALL support full keyboard navigation for all interactive elements.

#### Scenario: Tab key navigates through interactive elements
- **WHEN** user presses Tab key
- **THEN** focus moves to the next interactive element in logical order
- **AND** the focused element has a visible focus indicator (outline or ring)

#### Scenario: Enter/Space activates buttons and links
- **WHEN** a button or link has focus and user presses Enter or Space
- **THEN** the button/link performs its action (equivalent to mouse click)

#### Scenario: Escape closes modals and dropdowns
- **WHEN** a modal or dropdown is open and user presses Escape
- **THEN** the modal/dropdown closes
- **AND** focus returns to the element that triggered it

#### Scenario: Arrow keys navigate menus
- **WHEN** the sidebar menu has focus and user presses Down Arrow
- **THEN** focus moves to the next menu item
- **WHEN** user presses Up Arrow
- **THEN** focus moves to the previous menu item

### Requirement: Focus management
The system SHALL provide clear visual focus indicators and manage focus appropriately.

#### Scenario: Focused elements show visible outline
- **WHEN** an element receives keyboard focus
- **THEN** the element displays a visible focus ring using `var(--color-primary)`
- **AND** the focus ring has at least 3:1 contrast ratio against background

#### Scenario: Focus trap in modals
- **WHEN** a modal opens
- **THEN** focus moves to the first focusable element in the modal
- **AND** Tab key cycles through focusable elements within the modal only
- **AND** focus cannot escape the modal until it closes

#### Scenario: Focus returns after modal close
- **WHEN** a modal closes
- **THEN** focus returns to the element that opened the modal
- **EXAMPLE** if user clicked "Edit" button to open modal, focus returns to "Edit" button after close

### Requirement: Screen reader support
The system SHALL provide appropriate ARIA attributes for screen reader compatibility.

#### Scenario: Dynamic content updates announce to screen readers
- **WHEN** metric values update on the Dashboard
- **THEN** the MetricCard uses `aria-live="polite"` to announce the change
- **AND** the announcement says "患者总数更新为 1234人"

#### Scenario: Loading states are announced
- **WHEN** a component enters loading state
- **THEN** a visually hidden element with `role="status"` and `aria-live="polite"` announces "加载中"
- **WHEN** loading completes
- **THEN** the announcement says "加载完成"

#### Scenario: Error messages are announced
- **WHEN** a form validation error occurs
- **THEN** the error message element has `role="alert"` and `aria-live="assertive"`
- **AND** the screen reader immediately announces the error

### Requirement: Color contrast compliance
The system SHALL ensure all text and interactive elements meet WCAG 2.0 AA color contrast requirements (4.5:1 for normal text, 3:1 for large text).

#### Scenario: Primary text meets contrast ratio
- **WHEN** the application renders with light theme
- **THEN** primary text color (rgba(0, 0, 0, 0.88)) against white background has at least 4.5:1 contrast ratio

#### Scenario: Status badge colors meet contrast ratio
- **WHEN** StatusBadge renders with "成功" status
- **THEN** the green text color against the background has at least 4.5:1 contrast ratio
- **WHEN** StatusBadge renders with "错误" status
- **THEN** the red text color against the background has at least 4.5:1 contrast ratio

#### Scenario: Focus indicator has sufficient contrast
- **WHEN** an element shows focus ring
- **THEN** the focus ring color against adjacent colors has at least 3:1 contrast ratio

### Requirement: Automated accessibility testing
The system SHALL integrate automated accessibility testing (axe-core) into the development workflow.

#### Scenario: axe-core scans for violations
- **WHEN** the test suite runs
- **THEN** axe-core scans each page for WCAG 2.0 AA violations
- **AND** the test fails if any critical or serious violations are found

#### Scenario: axe-core reports issues with remediation guidance
- **WHEN** axe-core detects an accessibility issue
- **THEN** the test output includes:
  - Issue description (e.g., "Button must have discernible text")
  - Affected element selector
  - Remediation guidance (e.g., "Add aria-label attribute")

#### Scenario: axe-core runs on all key pages
- **WHEN** the CI/CD pipeline executes
- **THEN** axe-core scans the following pages:
  - Dashboard (DataDashboard.vue)
  - Patient List (PatientList.vue)
  - Encounter Detail (EncounterDetail.vue)
  - Model Management (ModelList.vue)
  - Login page (LoginPage.vue)