# Visual ETL Designer - Implementation Plan (Master Index)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Transform the ETL pipeline config page into a Kettle-like visual drag-and-drop designer using @vue-flow/core

**Architecture:** Three-panel layout (Palette + Vue Flow Canvas + Property Panel). Components dragged onto canvas, connected via edges. Backend stores graph structure and generates Embulk YAML for execution.

**Tech Stack:** @vue-flow/core + @vue-flow/background + @vue-flow/controls + Vue 3 + Ant Design Vue 4 + TypeScript

**Spec:** `docs/superpowers/specs/2026-04-19-visual-etl-designer-design.md`

---

## Execution Order

Tasks must be completed in order. Tasks within the same phase can run in parallel.

### Phase 1: Foundation (sequential - types needed by everything)

| # | File | Description |
|---|------|-------------|
| 01 | [install-vue-flow.md](01-install-vue-flow.md) | Install @vue-flow/core + peer deps |
| 02 | [types-and-enums.md](02-types-and-enums.md) | TypeScript types, enums, component registry |

### Phase 2: Frontend Components (sequential - each builds on previous)

| # | File | Description |
|---|------|-------------|
| 03 | [custom-nodes.md](03-custom-nodes.md) | Custom Vue Flow node components |
| 04 | [designer-layout.md](04-designer-layout.md) | EtlDesigner three-panel layout shell |
| 05 | [palette-panel.md](05-palette-panel.md) | Left sidebar: component palette with drag |
| 06 | [canvas-component.md](06-canvas-component.md) | Center: Vue Flow canvas with drop/connect |
| 07 | [property-panel.md](07-property-panel.md) | Right sidebar: node config forms |
| 08 | [field-mapping-modal.md](08-field-mapping-modal.md) | Edge double-click field mapping editor |

### Phase 3: Backend (can start after Phase 1)

| # | File | Description |
|---|------|-------------|
| 09 | [backend-edge-and-api.md](09-backend-edge-and-api.md) | r_etl_edge DDL + graph API endpoints |

### Phase 4: Integration (sequential - depends on Phase 2 + 3)

| # | File | Description |
|---|------|-------------|
| 10 | [graph-composable.md](10-graph-composable.md) | useDesignerGraph composable |
| 11 | [api-integration.md](11-api-integration.md) | Graph API client + save/load wiring |
| 12 | [rewrite-config-page.md](12-rewrite-config-page.md) | Rewrite EtlPipelineConfig.vue to use designer |
| 13 | [embulk-generator.md](13-embulk-generator.md) | Update EtlConfigGenerator for graph topology |

---

## Key Files Summary

**New files (frontend):**
- `src/views/data-etl/types/etl-designer.ts` - Types and enums
- `src/views/data-etl/components/EtlDesigner.vue` - Main designer
- `src/views/data-etl/components/EtlPalette.vue` - Palette panel
- `src/views/data-etl/components/EtlCanvas.vue` - Canvas panel
- `src/views/data-etl/components/EtlPropertyPanel.vue` - Property panel
- `src/views/data-etl/components/FieldMappingModal.vue` - Field mapping
- `src/views/data-etl/components/nodes/EtlNode.vue` - Base node
- `src/views/data-etl/composables/useDesignerGraph.ts` - Graph state

**Modified files (frontend):**
- `src/views/data-etl/EtlPipelineConfig.vue` - Rewrite to use designer
- `src/api/etl.ts` - Add graph API endpoints

**New files (backend):**
- SQL: `docker/init-db/12-etl-edge.sql` - r_etl_edge DDL
- Java: `EtlEdge` entity, `EtlEdgeRepository`, graph API in controller

**Modified files (backend):**
- `EtlPipelineController.java` - Add graph endpoints
- `EtlConfigGenerator.java` - Support graph topology
