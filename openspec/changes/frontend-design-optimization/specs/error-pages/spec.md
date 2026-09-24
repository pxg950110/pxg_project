# Error Pages Optimization

## MODIFIED Requirements

### Requirement: Error page visual design
Error pages (403, 404, 500) SHALL provide clear, user-friendly designs with action guidance.

#### Scenario: 403 Forbidden page shows access denied message
- **WHEN** user navigates to a forbidden page
- **THEN** the page displays a centered icon (lock or stop icon, size 80px)
- **AND** the icon color uses `var(--color-error)`
- **AND** a title "无访问权限" appears below the icon in `var(--color-text)`
- **AND** a message explains the reason (e.g., "您没有权限访问此页面，请联系管理员")
- **AND** a primary button "返回首页" is displayed using `var(--color-primary)`
- **AND** a secondary button "联系管理员" is displayed using outline style

#### Scenario: 404 Not Found page shows missing resource message
- **WHEN** user navigates to a non-existent page
- **THEN** the page displays a centered icon (question mark or broken link icon, size 80px)
- **AND** the icon color uses `var(--color-warning)`
- **AND** a title "页面不存在" appears below the icon in `var(--color-text)`
- **AND** a message explains the situation (e.g., "您访问的页面不存在或已被移除")
- **AND** a primary button "返回首页" is displayed
- **AND** a secondary button "返回上一页" is displayed

#### Scenario: 500 Internal Server Error page shows error message
- **WHEN** a server error occurs
- **THEN** the page displays a centered icon (server error or warning icon, size 80px)
- **AND** the icon color uses `var(--color-error)`
- **AND** a title "服务器错误" appears below the icon in `var(--color-text)`
- **AND** a message explains the situation (e.g., "服务器暂时无法处理您的请求，请稍后再试")
- **AND** a primary button "刷新页面" is displayed
- **AND** a secondary button "返回首页" is displayed

### Requirement: Error page layout consistency
All error pages SHALL use consistent layout and spacing.

#### Scenario: Error pages use centered layout
- **WHEN** any error page renders
- **THEN** the content is centered vertically and horizontally in the viewport
- **AND** the icon, title, message, and buttons are stacked vertically
- **AND** the spacing between elements uses `var(--spacing-md)` (16px)

#### Scenario: Error pages use Design Tokens for typography
- **WHEN** any error page renders
- **THEN** the title font size uses 24px
- **AND** the title font weight uses 600
- **AND** the title color uses `var(--color-text)`
- **AND** the message font size uses `var(--font-size-base)` (14px)
- **AND** the message color uses `var(--color-text-secondary)`

## ADDED Requirements

### Requirement: Error page navigation actions
Error pages SHALL provide clear navigation actions to help users recover.

#### Scenario: "返回首页" button navigates to Dashboard
- **WHEN** user clicks the "返回首页" button on an error page
- **THEN** the application navigates to the Dashboard route (`/dashboard`)
- **AND** the error state is cleared

#### Scenario: "返回上一页" button navigates back in history
- **WHEN** user clicks the "返回上一页" button on 404 page
- **THEN** the browser navigates back one step in history (`router.back()`)
- **AND** if no history exists, the button navigates to Dashboard instead

#### Scenario: "刷新页面" button reloads the current page
- **WHEN** user clicks the "刷新页面" button on 500 page
- **THEN** the browser reloads the current page (`window.location.reload()`)
- **AND** a loading indicator is shown during reload

#### Scenario: "联系管理员" button opens email client
- **WHEN** user clicks the "联系管理员" button on 403 page
- **THEN** the browser opens the default email client with a pre-filled message
- **AND** the email subject is "申请访问权限: [页面路径]"
- **AND** the email body includes the current page URL and user context

### Requirement: Error page accessibility
Error pages SHALL be fully accessible with ARIA labels and keyboard navigation.

#### Scenario: Error page title is announced by screen reader
- **WHEN** screen reader user navigates to an error page
- **THEN** the title element has `role="alert"` and `aria-live="assertive"`
- **AND** the screen reader immediately announces the error title (e.g., "无访问权限")

#### Scenario: Error page navigation buttons are accessible
- **WHEN** error page renders navigation buttons
- **THEN** each button has a descriptive `aria-label` (e.g., "返回首页", "刷新页面")
- **AND** the buttons can be navigated and activated via keyboard (Tab, Enter/Space)

### Requirement: Error page dark theme support
Error pages SHALL adapt to dark theme when active.

#### Scenario: Error page uses dark theme colors
- **WHEN** dark theme is active (`data-theme="dark"`)
- **THEN** the error icon color remains `var(--color-error)` (dark theme value)
- **AND** the title color uses `var(--color-text)` (dark theme value)
- **AND** the background color uses `var(--color-bg)` (dark theme value: #141414)