# MAIDC Design vs Frontend Diff Report

Generated: 2026-04-12
Updated: After fixes applied

## Methodology
- Design source: `pencil-new.pen` (7 pages exported as PNG)
- Frontend source: `localhost:3000` (7 pages captured via Chrome DevTools)
- Comparison: Visual diff via AI analysis + manual inspection of design variables
- Design variables: `--sidebar-bg: #FFFFFF`, `--primary: #1677FF`, `--background: #F0F2F5`, `--border: #D9D9D9`

---

## Global Issues (affect all pages)

### G1. [P0] Sidebar background color is wrong -- FIXED
- **Design**: White background (`--sidebar-bg: #FFFFFF` in light mode)
- **Before**: Dark navy blue (Ant Design sider default dark theme)
- **Fix**: Added `theme="light"` to `a-layout-sider` in BasicLayout.vue
- **Verified**: `backgroundColor: "rgb(255, 255, 255)"`, width: 220px

### G2. [P0] Logo area does not match design -- FIXED
- **Design**: Blue square icon (32x32, rounded-8, `--primary` bg) with white brain icon + "MAIDC" text (16px, bold, `--foreground` color)
- **Before**: SVG logo image + blue "MAIDC" text
- **Fix**: Wrapped logo SVG in blue icon box (32x32, rounded-8, `#1677FF`), changed text color to dark
- **Verified**: iconBox 32x32px, borderRadius 8px, bg #1677FF, text "MAIDC" 16px bold

### G3. [P1] Header shows breadcrumb instead of page title -- FIXED
- **Design**: Simple page title text (e.g., "工作台", 18px, bold) on white header
- **Before**: Ant Design breadcrumb navigation (e.g., "仪表盘 / 系统总览")
- **Fix**: Replaced BreadcrumbNav with computed pageTitle from route.meta.title
- **Verified**: Header shows "系统总览", "模型列表", "部署管理", etc.

### G4. [P1] Header height mismatch -- FIXED
- **Design**: 60px header height
- **Before**: 56px
- **Fix**: Changed height/line-height from 56px to 60px

### G5. [P1] Content area padding mismatch -- FIXED
- **Design**: 24px padding inside content area
- **Before**: margin: 16px, padding: 20px
- **Fix**: Changed to padding: 24px, removed margin

### G6. [P1] Primary color mismatch on login page -- FIXED
- **Design**: `--primary: #1677FF`
- **Before**: Login page used `#3b82f6` (Tailwind blue)
- **Fix**: Replaced all `#3b82f6` with `#1677ff` in LoginPage.vue

---

## Page-by-Page Differences

### 01. Login Page (nR4Uw)
**Cannot capture frontend** (auto-redirects to dashboard when logged in).

| # | Priority | Issue | Design | Frontend | Status |
|---|----------|-------|--------|----------|--------|
| L1 | P1 | Left panel width | ~50% with gradient dark bg | 50% with gradient, matches | OK |
| L2 | P1 | Primary button color | `#1677FF` | Was `#3b82f6` | FIXED |
| L3 | P2 | Input focus color | `#1677FF` | Was `#3b82f6` | FIXED (with L2) |

### 02. Dashboard (QptC0)
| # | Priority | Issue | Design | Frontend | Status |
|---|----------|-------|--------|----------|--------|
| D1 | P0 | Sidebar bg | White (#FFFFFF) | Was dark navy | FIXED |
| D2 | P0 | Metric cards layout | 2 rows x 3 cards | Was 1 row x 6 cards | FIXED |
| D3 | P1 | Header shows | "工作台" page title | Was breadcrumb | FIXED |
| D4 | P1 | Logo area | Blue icon square + MAIDC text | Was SVG logo | FIXED |
| D5 | P1 | Header height | 60px | Was 56px | FIXED |
| D6 | P2 | Card borders | `--border` (#D9D9D9) | No borders (Ant Card) | SKIP |

### 03. Model List (Syc8a)
| # | Priority | Issue | Design | Frontend | Status |
|---|----------|-------|--------|----------|--------|
| M1 | P0 | Sidebar bg | White | Was dark navy | FIXED |
| M2 | P1 | Header title | Page title | Was breadcrumb | FIXED |
| M3 | P1 | Card grid | Model cards | Similar card grid | OK |
| M4 | P2 | Search placeholder | "搜索..." | "搜索模型名称或编码..." | SKIP |
| M5 | P2 | Model count | "共 28 个模型" | "共 6 个模型" | SKIP (data) |

### 05. Deployment Monitoring (l2c3F)
| # | Priority | Issue | Design | Frontend | Status |
|---|----------|-------|--------|----------|--------|
| P1 | P0 | Sidebar bg | White | Was dark navy | FIXED |
| P2 | P1 | Header title | Page title | Was breadcrumb | FIXED |
| P3 | P1 | Metric cards | 4 cards | 4 similar cards | OK |
| P4 | P2 | Chart type | Bar chart with tabs | Line/bar combo | SKIP |

### 10. Patient List (c2K7C)
| # | Priority | Issue | Design | Frontend | Status |
|---|----------|-------|--------|----------|--------|
| T1 | P0 | Sidebar bg | White | Was dark navy | FIXED |
| T2 | P1 | Header title | Page title | Was breadcrumb | FIXED |
| T3 | P2 | Table columns | Different columns | Different columns | SKIP (data) |
| T4 | P2 | Record count | "共 2,456 条记录" | "共 1 条" | SKIP (data) |

### 11. Research Projects (skaqI)
| # | Priority | Issue | Design | Frontend | Status |
|---|----------|-------|--------|----------|--------|
| R1 | P0 | Sidebar bg | White | Was dark navy | FIXED |
| R2 | P1 | Header title | Page title | Was breadcrumb | FIXED |
| R3 | P1 | Button text | "创建项目" | Was "新建项目" | FIXED |
| R4 | P2 | Card count | 3 cards | 6 cards | SKIP (data) |

### 20. User Management (fltE4)
| # | Priority | Issue | Design | Frontend | Status |
|---|----------|-------|--------|----------|--------|
| U1 | P0 | Sidebar bg | White | Was dark navy | FIXED |
| U2 | P1 | Header title | Page title | Was breadcrumb | FIXED |
| U3 | P1 | Status display | Colored text labels | Colored dot + text | OK (minor) |

---

## Summary

### Fixes Applied: 8 items across 4 files

| # | Priority | Fix | File |
|---|----------|-----|------|
| 1 | P0 | Sidebar white theme | `layouts/BasicLayout.vue` |
| 2 | P0 | Logo blue icon box | `layouts/BasicLayout.vue` |
| 3 | P1 | Header page title (replaced breadcrumb) | `layouts/BasicLayout.vue` |
| 4 | P1 | Header height 56px -> 60px | `layouts/BasicLayout.vue` |
| 5 | P1 | Content padding 24px | `layouts/BasicLayout.vue` |
| 6 | P0 | Dashboard metric cards 1x6 -> 2x3 | `views/dashboard/Overview.vue` |
| 7 | P1 | Login button color #3b82f6 -> #1677FF | `views/login/LoginPage.vue` |
| 8 | P1 | Project button text "新建项目" -> "创建项目" | `views/data-rdr/ProjectList.vue` |

### Not Fixed (data/acceptable differences):
- Model/patient content differences (mock data, not UI issue)
- Pixel-level alignment
- Shadow/border subtleties
- Chart type variations
- Card border style (Ant Card default vs design spec borders)

### Remaining P2 items (not in scope):
- D6: Card borders should use `--border: #D9D9D9`
- M4: Search placeholder text variation
- P4: Chart type difference on deployment page
- U3: Status display format (dots vs text labels)
