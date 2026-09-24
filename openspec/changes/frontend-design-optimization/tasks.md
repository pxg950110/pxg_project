# Frontend Design Optimization - Implementation Tasks

## 1. Design Token System Setup

- [ ] 1.1 Create `src/assets/styles/design-tokens.css` with CSS variables for colors, typography, spacing, shadows, and border radius
- [ ] 1.2 Define light theme CSS variables in `:root` selector
- [ ] 1.3 Define dark theme CSS variables in `[data-theme='dark']` selector
- [ ] 1.4 Create `src/assets/styles/design-tokens.md` documentation with token names, values, and usage guidelines
- [ ] 1.5 Update `App.vue` to sync CSS variables with Ant Design ConfigProvider tokens
- [ ] 1.6 Add theme switching toggle in `SettingsDrawer.vue` (light/dark mode)
- [ ] 1.7 Store theme preference in localStorage or user settings API

## 2. Responsive Layout Infrastructure

- [ ] 2.1 Create `src/composables/useResponsive.ts` composable with breakpoint detection (sm/md/lg)
- [ ] 2.2 Add window resize event listener and reactive `currentBreakpoint` state
- [ ] 2.3 Implement cleanup logic in `useResponsive` to remove event listeners on unmount
- [ ] 2.4 Create `src/assets/styles/responsive.css` with media queries for 1440px/1920px/2560px breakpoints
- [ ] 2.5 Update `BasicLayout.vue` sidebar width with CSS media queries (200px/220px/260px)
- [ ] 2.6 Update `BasicLayout.vue` content padding with CSS media queries (16px/24px/32px)
- [ ] 2.7 Add auto-collapse logic for sidebar when viewport < 1200px
- [ ] 2.8 Update Dashboard metric card grid with responsive column spans (span="8"/"6")

## 3. Component Migration to Design Tokens

- [ ] 3.1 Migrate `MetricCard/index.vue` to use Design Token colors (icon background, trend colors)
- [ ] 3.2 Migrate `MetricCard/index.vue` to use Design Token text colors (title, value)
- [ ] 3.3 Migrate `StatusBadge/index.vue` to use Design Token status colors (success/error/warning/info)
- [ ] 3.4 Migrate `PageContainer/index.vue` to use Design Token spacing and typography
- [ ] 3.5 Migrate `EmptyState/index.vue` to use Design Token colors and spacing
- [ ] 3.6 Migrate `Card` hover effects to use Design Token shadows
- [ ] 3.7 Migrate `Table` components to use Design Token spacing and typography
- [ ] 3.8 Migrate `SearchForm/index.vue` to use Design Token spacing
- [ ] 3.9 Migrate `HeaderActions.vue` and `TabBar.vue` to use Design Token colors

## 4. Skeleton Loading Components

- [ ] 4.1 Create `src/components/Skeleton/MetricCardSkeleton.vue` component
- [ ] 4.2 Create `src/components/Skeleton/TableSkeleton.vue` component
- [ ] 4.3 Create `src/components/Skeleton/ChartSkeleton.vue` component
- [ ] 4.4 Add pulsing animation CSS for skeleton placeholders
- [ ] 4.5 Integrate skeletons into `DataDashboard.vue` using Vue 3 Suspense
- [ ] 4.6 Integrate skeletons into `PatientList.vue` and `EncounterDetail.vue`
- [ ] 4.7 Add loading state detection for ETL task table in Dashboard

## 5. Micro-interactions Implementation

- [ ] 5.1 Add ripple effect CSS for primary button clicks (300ms animation)
- [ ] 5.2 Add hover state CSS for buttons (brightness filter, pointer cursor)
- [ ] 5.3 Add loading spinner and disabled state for async button actions
- [ ] 5.4 Add shake animation for invalid form field validation (300ms)
- [ ] 5.5 Add success indicator checkmark animation for valid form fields
- [ ] 5.6 Add smooth sidebar collapse transition (200ms ease-in-out)
- [ ] 5.7 Add card hover shadow transition (150ms)
- [ ] 5.8 Add tab switch fade animation (150ms fade-in/fade-out)
- [ ] 5.9 Implement progress bar component with semantic color transitions
- [ ] 5.10 Configure Ant Design message/toast notifications with slide animations
- [ ] 5.11 Add toast auto-dismiss logic (3 seconds for success, manual for error)

## 6. Accessibility Enhancements

- [ ] 6.1 Add semantic HTML landmarks to `BasicLayout.vue` (aside/header/main)
- [ ] 6.2 Add `aria-label` to icon-only buttons in `HeaderActions.vue` and `SidebarMenu.vue`
- [ ] 6.3 Add `role="article"` and `aria-label` to `MetricCard` components
- [ ] 6.4 Add `role="status"` and `aria-label` to `StatusBadge` components
- [ ] 6.5 Implement keyboard navigation for sidebar menu (Arrow keys, Tab, Enter)
- [ ] 6.6 Add visible focus ring CSS for keyboard focus (3:1 contrast ratio)
- [ ] 6.7 Implement focus trap for modal dialogs (Ant Design Modal already supports this)
- [ ] 6.8 Add `aria-live="polite"` for dynamic metric updates in Dashboard
- [ ] 6.9 Add `role="alert"` for error messages in form validation
- [ ] 6.10 Add `aria-live` announcements for loading states ("加载中" → "加载完成")
- [ ] 6.11 Install and configure axe-core for automated accessibility testing
- [ ] 6.12 Create axe-core test script for key pages (Dashboard, PatientList, EncounterDetail)
- [ ] 6.13 Add axe-core to CI/CD pipeline with violation reporting

## 7. Error Pages Optimization

- [ ] 7.1 Update `403.vue` with centered layout, icon (80px), title, message, and navigation buttons
- [ ] 7.2 Update `404.vue` with centered layout, icon (80px), title, message, and navigation buttons
- [ ] 7.3 Update `500.vue` with centered layout, icon (80px), title, message, and navigation buttons
- [ ] 7.4 Add "返回首页" button navigation logic (router.push('/dashboard'))
- [ ] 7.5 Add "返回上一页" button navigation logic (router.back() with fallback)
- [ ] 7.6 Add "刷新页面" button logic (window.location.reload())
- [ ] 7.7 Add "联系管理员" button email link with pre-filled subject/body
- [ ] 7.8 Add `role="alert"` and `aria-live="assertive"` to error page titles
- [ ] 7.9 Add dark theme support for error pages (CSS variables adaptation)

## 8. Performance Optimization

- [ ] 8.1 Compress and optimize logo.svg and icon assets (target 30% size reduction)
- [ ] 8.2 Implement lazy loading for images using Intersection Observer API
- [ ] 8.3 Add critical CSS inlining in Vite config (design-tokens.css, global.css)
- [ ] 8.4 Configure async CSS loading for non-critical styles (responsive.css)
- [ ] 8.5 Optimize CSS animations to use transform instead of position/layout properties
- [ ] 8.6 Add Vite bundle analyzer to monitor CSS/JS bundle size
- [ ] 8.7 Set Lighthouse CI performance thresholds (FCP < 1.5s)
- [ ] 8.8 Add performance monitoring script for production builds

## 9. Testing and Validation

- [ ] 9.1 Write E2E tests for responsive breakpoints (Playwright tests for 1440px/1920px/2560px)
- [ ] 9.2 Write visual regression tests with Percy/BackstopJS for Dashboard layout
- [ ] 9.3 Write unit tests for `useResponsive` composable
- [ ] 9.4 Write integration tests for theme switching (light/dark toggle)
- [ ] 9.5 Write accessibility tests for keyboard navigation and ARIA attributes
- [ ] 9.6 Manual testing checklist: verify all responsive breakpoints with core workflows
- [ ] 9.7 Manual testing checklist: verify color contrast ratios with contrast checker tool
- [ ] 9.8 Manual usability testing: invite doctor users to test keyboard navigation
- [ ] 9.9 Performance validation: measure FCP/TTI before and after optimization

## 10. Documentation and Deployment

- [ ] 10.1 Create design system documentation (Design Tokens reference guide)
- [ ] 10.2 Create accessibility usage guide (keyboard shortcuts, screen reader tips)
- [ ] 10.3 Update README.md with responsive design support notes
- [ ] 10.4 Update component library documentation with Design Token usage examples
- [ ] 10.5 Create rollback plan documentation (feature flags, style fallback)
- [ ] 10.6 Prepare deployment checklist (CSS variables, theme toggle, responsive tests)
- [ ] 10.7 Monitor production performance for 2 weeks post-deployment
- [ ] 10.8 Collect user feedback on visual consistency and responsiveness