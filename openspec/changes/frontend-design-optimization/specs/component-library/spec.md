# Component Library Optimization

## MODIFIED Requirements

### Requirement: MetricCard uses Design Tokens
MetricCard components SHALL use Design Token CSS variables instead of hardcoded color values.

#### Scenario: MetricCard icon background uses Design Token
- **WHEN** MetricCard renders with an icon
- **THEN** the icon background color uses `rgba(var(--color-primary-rgb), 0.08)`
- **AND** the icon color uses `var(--color-primary)`
- **AND** hardcoded values like `#1677ff` are removed

#### Scenario: MetricCard text colors use Design Tokens
- **WHEN** MetricCard renders the title and value
- **THEN** the title color uses `var(--color-text-secondary)` (rgba(0, 0, 0, 0.45))
- **AND** the value color uses `var(--color-text)` (rgba(0, 0, 0, 0.88))

#### Scenario: MetricCard trend colors use Design Tokens
- **WHEN** MetricCard displays a trend indicator
- **THEN** upward trend color uses `var(--color-success)`
- **AND** downward trend color uses `var(--color-error)`
- **AND** hardcoded values like `#52c41a` and `#ff4d4f` are removed

### Requirement: StatusBadge uses Design Tokens
StatusBadge components SHALL use Design Token CSS variables for all status colors.

#### Scenario: Success badge uses Design Token
- **WHEN** StatusBadge renders with status="成功"
- **THEN** the text color uses `var(--color-success)`
- **AND** the background color uses `var(--color-success-bg)` (derived token)
- **AND** hardcoded values like `#52c41a` are removed

#### Scenario: Error badge uses Design Token
- **WHEN** StatusBadge renders with status="错误"
- **THEN** the text color uses `var(--color-error)`
- **AND** the background color uses `var(--color-error-bg)`
- **AND** hardcoded values like `#ff4d4f` are removed

#### Scenario: Warning badge uses Design Token
- **WHEN** StatusBadge renders with status="警告"
- **THEN** the text color uses `var(--color-warning)`
- **AND** the background color uses `var(--color-warning-bg)`
- **AND** hardcoded values like `#faad14` are removed

#### Scenario: Info badge uses Design Token
- **WHEN** StatusBadge renders with status="信息"
- **THEN** the text color uses `var(--color-info)` (if defined)
- **AND** the background color uses `var(--color-info-bg)`
- **OR** falls back to `var(--color-primary)` if `--color-info` is not defined

### Requirement: PageContainer uses Design Tokens
PageContainer components SHALL use Design Token spacing and colors.

#### Scenario: PageContainer uses consistent padding
- **WHEN** PageContainer renders the content wrapper
- **THEN** the inner padding uses `var(--spacing-md)` (16px)
- **AND** the title margin-bottom uses `var(--spacing-sm)` (8px)

#### Scenario: PageContainer title typography uses Design Tokens
- **WHEN** PageContainer renders the page title
- **THEN** the font size uses `var(--font-size-lg)` (16px)
- **AND** the font weight uses 600
- **AND** the color uses `var(--color-text)`

#### Scenario: PageContainer subtitle uses secondary text color
- **WHEN** PageContainer renders the subtitle
- **THEN** the font size uses `var(--font-size-base)` (14px)
- **AND** the color uses `var(--color-text-secondary)`
- **AND** the margin-top uses `var(--spacing-xs)` (4px)

## ADDED Requirements

### Requirement: Card hover effects
Card components SHALL provide subtle hover effects to indicate interactivity.

#### Scenario: Card shows shadow on hover
- **WHEN** user hovers over a card (MetricCard, PageContainer card wrapper)
- **THEN** the card shadow transitions from `var(--shadow-sm)` to `var(--shadow-md)`
- **AND** the transition duration is 150ms
- **AND** the transition timing function is ease-out

#### Scenario: Card does not show hover if not interactive
- **WHEN** a card is static (no click actions)
- **THEN** the hover effect is not applied to avoid misleading visual cues

### Requirement: EmptyState component
EmptyState components SHALL display clear empty states with guidance.

#### Scenario: EmptyState shows icon and message
- **WHEN** EmptyState renders for a list with no items
- **THEN** a centered icon (size 64px) is displayed using `var(--color-text-secondary)`
- **AND** a message text appears below the icon with `var(--color-text-secondary)` color
- **AND** a primary action button is displayed if applicable (using `var(--color-primary)`)

#### Scenario: EmptyState uses semantic icon
- **WHEN** EmptyState renders for a table search with no results
- **THEN** the icon is a "空" or "搜索" icon
- **WHEN** EmptyState renders for an empty list
- **THEN** the icon is a "列表" or "空" icon

### Requirement: Component accessibility attributes
All interactive components SHALL include appropriate ARIA attributes.

#### Scenario: MetricCard has role and aria-label
- **WHEN** MetricCard renders
- **THEN** the card element has `role="article"`
- **AND** the card element has `aria-label="[title]: [value][suffix]"`

#### Scenario: StatusBadge has role and aria-label
- **WHEN** StatusBadge renders with status="成功"
- **THEN** the badge element has `role="status"`
- **AND** the badge element has `aria-label="状态: 成功"`

#### Scenario: Buttons have aria-label if icon-only
- **WHEN** a button contains only an icon (no visible text)
- **THEN** the button has an `aria-label` describing the action
- **EXAMPLE** `aria-label="关闭"` for a close button with X icon