## Context

The Patient Encounter 360 view is a core feature of the MAIDC system, providing a comprehensive view of patient clinical data organized around encounters. The current implementation (completed 2026-05-13) includes:

**Current Architecture:**
- Backend: Spring Boot 3.x with JPA, PostgreSQL
- Frontend: Vue 3 + Element Plus + Pinia
- Layout: Three-zone design (patient card, timeline, detail)
- Data: 6 clinical modules (diagnosis, lab tests, imaging, medication, operations, basic info)

**Current Limitations:**
1. No data visualization — lab trends and vital signs shown only as raw tables
2. No clinical alerts — critical values and allergies only shown as static text
3. No filtering — no way to search within encounter data
4. No export — clinicians cannot generate reports from the 360 view
5. Timeline is basic — no visual indicators or mini-previews
6. Performance degrades with >50 encounters due to N+1 queries

**Stakeholders:** Clinical doctors (primary), researchers, hospital administrators

## Goals / Non-Goals

**Goals:**
- Add interactive chart visualization for lab trends and vital signs
- Implement clinical alert system for critical values and allergies
- Provide advanced filtering and search within encounter details
- Enable PDF report export of patient encounter data
- Enhance timeline with visual status indicators and mini-previews
- Optimize backend queries for large encounter datasets

**Non-Goals:**
- Complete UI/UX redesign of the existing 360 view layout
- Real-time data streaming (WebSocket-based live updates)
- AI-powered clinical decision support or diagnostic suggestions
- Integration with external PACS/DICOM viewers for imaging
- Multi-language (i18n) implementation in this phase
- Mobile app version (responsive optimization only)

## Decisions

### 1. Visualization Library: ECharts over Chart.js

**Decision:** Use Apache ECharts for data visualization.

**Rationale:**
- ECharts is already listed in the project tech stack
- Superior support for medical chart types (reference range bands, multi-axis)
- Built-in data zoom and brush selection for exploring trends
- Better performance with large datasets (canvas rendering with GPU acceleration)
- Rich theming support for dark mode integration

**Alternatives considered:**
- Chart.js: Simpler API but lacks medical-specific features and zoom/brush
- D3.js: Too low-level for our needs; would require building chart primitives from scratch

### 2. Alert Architecture: Client-side Rules Engine

**Decision:** Implement clinical alerts as a client-side rules engine with configurable thresholds, backed by a lightweight server-side alert configuration API.

**Rationale:**
- Clinical alert rules are institution-specific and change frequently
- Client-side evaluation avoids round-trips for simple threshold checks
- Server stores threshold configurations; client evaluates against fetched data
- Allows real-time visual feedback as data loads

**Alternatives considered:**
- Server-side evaluation only: Adds latency, requires redeployment for rule changes
- Pure client-side with hardcoded thresholds: Not configurable per institution

### 3. Filtering Architecture: URL-driven Filter State

**Decision:** Use URL query parameters to persist filter state, enabling shareable filtered views and browser back/forward navigation.

**Rationale:**
- Clinicians frequently share specific filtered views with colleagues
- URL state survives page refreshes
- Vue Router already supports query parameter binding
- Consistent with the existing route-driven architecture (`/cdr/patient/:patientId/encounter/:encounterId`)

### 4. PDF Export: Server-side Generation

**Decision:** Generate PDF reports on the server using JasperReports or iText, served as a downloadable file.

**Rationale:**
- Server-side generation ensures consistent formatting across browsers
- Can include hospital letterhead, headers/footers, and page numbers
- Better handling of CJK characters and medical symbols
- Larger reports (>50 pages) would freeze the browser if generated client-side

**Alternatives considered:**
- Client-side jsPDF: Inconsistent rendering, CJK font issues, blocks main thread
- Headless Chrome PDF: Good fidelity but requires running a browser process on server

### 5. Timeline Enhancement: SVG-based Rendering

**Decision:** Enhance the existing HTML-based timeline with SVG elements for visual indicators, keeping the current click interaction model.

**Rationale:**
- SVG integrates well with the existing Vue component structure
- Better visual fidelity for status indicators and mini-charts
- Maintains accessibility (ARIA attributes on SVG)
- No need for a heavy timeline library; the current component is simple enough

### 6. Performance: Batch Loading with EntityGraph

**Decision:** Replace N+1 queries with JPA EntityGraph and implement cursor-based pagination for encounter lists.

**Rationale:**
- Current implementation fetches encounters then lazy-loads related entities one-by-one
- EntityGraph allows specifying fetch joins at query time
- Cursor-based pagination (using `admitTime` as cursor) avoids OFFSET performance issues
- Batch loading reduces API calls from O(n) to O(1) for encounter detail

## Risks / Trade-offs

**ECharts bundle size (~800KB minified)** → Use tree-shaking and lazy-load chart components only when their tab is active. Expected impact: ~200KB gzipped for the chart modules we use.

**Client-side alert evaluation may miss cross-encounter patterns** → Phase 2 can add server-side complex rule evaluation. For now, single-encounter alert coverage is sufficient for the most critical alerts (allergies, critical lab values).

**Server-side PDF generation adds a dependency on JasperReports/iText** → Start with iText (lighter weight, AGPL for internal use). If commercial distribution is needed, migrate to Apache PDFBox (Apache 2.0 license).

**URL-driven filter state may expose sensitive patient data in URLs** → Use opaque filter tokens instead of raw parameters. E.g., `?f=enc` instead of `?diagnosis=diabetes`. Decode server-side when sharing.

**SVG timeline enhancements may conflict with existing CSS** → Use CSS scoped styling (already in place) and test visual regression before merging.

**EntityGraph changes require testing against existing data** → Add integration tests before deploying. The current lazy-loading behavior must be preserved for other consumers of the repositories.
