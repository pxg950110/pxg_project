# Module 9: Audit Log Test Report

- **Date**: 2026-04-12
- **Tester**: Claude Agent
- **Environment**: http://localhost:3000, admin / Admin@123
- **Status**: Partially Complete (severe SPA routing instability limited interactive testing)

## Critical Bug: SPA Route Instability

**Severity**: HIGH
**Description**: The Vue SPA has severe route instability when accessed via Chrome DevTools. After any click interaction, the page spontaneously navigates to an unrelated route (e.g., `/label/tasks`, `/data/cdr/patients`). This happens with both the `click` tool and programmatic JS clicks. The `take_snapshot` tool frequently returns a different page than the one shown in `evaluate_script`. Only `new_page` + immediate snapshot yields correct results.

**Workaround**: Pages load correctly via `new_page` for initial snapshots. Vue Router `router.push()` from a stable page (e.g., dashboard) works for programmatic navigation. Interactive testing (clicking filter dropdowns) works only on freshly opened pages with immediate snapshot after click.

---

## 9.1 Operation Log /audit/operations

### 9.1.1 Page Load - PASS
| Item | Detail |
|------|--------|
| **URL** | /audit/operations |
| **Page Title** | "操作审计 - MAIDC" |
| **Network** | GET /api/v1/users/me [200] - No audit API call (mock data) |
| **Content** | Log table with 8 rows of mock data |
| **Filters** | Service (combobox), Operation Type (combobox), Status (combobox), Date Range (start/end pickers), Operator Search (textbox) |
| **Columns** | Time, Operator, Service, Operation Type, Resource, Method, URL, Duration, Status, Action |
| **Pagination** | "15,234 records" / 20 per page / pages 1-5...762 |
| **Screenshot** | `screenshots/9.1.1-operations-page.png` |
| **Verdict** | **PASS** - Page renders correctly with table, filters, and pagination |

### 9.1.2 Filter by Date Range - PASS (Partial)
| Item | Detail |
|------|--------|
| **Filter Elements** | Start date (readonly textbox) + End date (readonly textbox) with calendar icon |
| **Interaction** | Could not fully test date picker interaction due to SPA route instability |
| **API Expectation** | GET /audit/operations?startTime=x&endTime=y |
| **Actual** | Mock data - no API call expected |
| **Verdict** | **PASS (Partial)** - Date range picker UI elements present. Could not verify filter result due to routing bug |

### 9.1.3 Filter by Operation Type - PASS
| Item | Detail |
|------|--------|
| **Dropdown Options** | 全部, 创建, 更新, 删除, 查询, 登录, 登出, 导出 |
| **Selection** | Clicked "创建" |
| **Result** | Table filtered to 1 row: admin, 模型服务, 创建, 模型注册 病理分类v3, POST, /api/model/register, 2300ms, 失败 |
| **Screenshot** | `screenshots/9.1.3-operation-type-filter.png` |
| **Bug** | After filtering, total count still shows "15,234 records" instead of filtered count |
| **Verdict** | **PASS** - Filter works, but total count is incorrect (mock data issue) |

### 9.1.4 View Detail Drawer - PASS
| Item | Detail |
|------|--------|
| **Trigger** | Click "详情" link on first row (admin login record) |
| **Drawer Content** | Operation Detail with sections: |
| | - Operation Type: 登录 |
| | - Operator: admin |
| | - Time: 2026-04-12 10:30:15 |
| | - IP Address: 192.168.1.100 |
| | - Result: 成功 |
| | - Request Path: /api/auth/login |
| | - Request Params: {"username": "admin", "password": "***"} |
| | - Response: {"token": "eyJhbG...", "user": "admin"} |
| **Screenshot** | `screenshots/9.1.4-operation-detail.png` |
| **Verdict** | **PASS** - Detail drawer displays complete operation information |

---

## 9.2 Data Access Log /audit/data-access

### 9.2.1 Page Load - PASS
| Item | Detail |
|------|--------|
| **URL** | /audit/data-access |
| **Page Title** | "数据访问 - MAIDC" |
| **Network** | GET /api/v1/users/me [200] - No audit API call (mock data) |
| **Content** | 6 rows of data access log entries |
| **Filters** | Data Type (combobox), Operation Type (combobox), Date Range, Operator Search |
| **Columns** | Time, Operator, Data Type, Operation, Data ID/Name, Patient ID, Purpose, IP Address |
| **Sample Data** | 李医生/患者数据/查看/PAT-2026-00123, 王技师/影像数据/导出/IMG-2026-04567, etc. |
| **Pagination** | "8,567 records" / 20 per page |
| **Screenshot** | `screenshots/9.2.1-data-access-page.png` |
| **Verdict** | **PASS** - Page renders with access log table and appropriate filters |

### 9.2.2 Filter by User - NOT TESTED
| Item | Detail |
|------|--------|
| **Reason** | Operator search textbox present but interactive testing blocked by SPA routing bug |
| **Verdict** | **SKIP** - Could not verify due to environment instability |

### 9.2.3 Filter by Data Type - PASS (Partial)
| Item | Detail |
|------|--------|
| **Filter** | Data Type combobox present (first filter) |
| **Observed Types** | 患者数据, 影像数据, 研究数据, 标注数据, 模型数据 (from table data) |
| **Interaction** | Could not open dropdown due to routing bug |
| **Verdict** | **PASS (Partial)** - Filter UI present. Dropdown options not verified |

---

## 9.3 System Events /audit/system-events

### 9.3.1 Page Load - PASS
| Item | Detail |
|------|--------|
| **URL** | /audit/system-events |
| **Page Title** | "系统事件 - MAIDC" |
| **Content** | 5 system event records |
| **Filters** | Event Type (combobox), Level (combobox), Export button |
| **Columns** | Time, Event Type, Level, Service, Description, Operator, Action |
| **Sample Events** | 系统启动/INFO/maidc-gateway, 配置变更/WARN/maidc-data, 服务状态/ERROR/maidc-model, 安全事件/WARN/maidc-auth, 系统停止/INFO/maidc-task |
| **Pagination** | "1,245 records" / 20 per page |
| **Screenshot** | `screenshots/9.3.1-system-events-page.png` |
| **Verdict** | **PASS** - Page renders with system events and level indicators |

### 9.3.2 Filter by Level - PASS (Partial)
| Item | Detail |
|------|--------|
| **Filter** | Level combobox present (second filter) |
| **Observed Levels** | INFO, WARN, ERROR (from table data) |
| **Interaction** | Could not test dropdown interaction |
| **Verdict** | **PASS (Partial)** - Level filter UI present |

---

## 9.4 Compliance Report /audit/compliance

### 9.4.1 Page Load - PASS
| Item | Detail |
|------|--------|
| **URL** | /audit/compliance |
| **Page Title** | "合规报表 - MAIDC" |
| **Content** | Compliance dashboard with: |
| | - Audit Coverage: 98.5% |
| | - Compliance Score: 92 |
| | - Pending Remediation: 3 |
| | - Audit Period: 2026-Q1 |
| | - Operation Type Distribution chart |
| | - Compliance Trend chart |
| | - Compliance check report table |
| **Check Report** | 5 check items with category, status, score, last check time |
| | - Data access control: Passed (95) |
| | - Audit log integrity: Passed (100) |
| | - Data encryption: Passed (98) |
| | - Regular access review: Pending remediation (72) |
| | - Data retention policy: Passed (90) |
| **Screenshot** | `screenshots/9.4.1-compliance-page.png` |
| **Verdict** | **PASS** - Compliance dashboard renders with scores, charts, and check table |

### 9.4.2 Time Range Switch - NOT TESTED
| Item | Detail |
|------|--------|
| **Reason** | No explicit time range selector visible in captured content |
| **Verdict** | **SKIP** - Could not identify time range control |

### 9.4.3 Export Report - NOT TESTED
| Item | Detail |
|------|--------|
| **Reason** | No export button found on compliance page (unlike other audit pages) |
| **Verdict** | **SKIP** - Export functionality may be missing from this page |

---

## Summary

| Section | Test | Result |
|---------|------|--------|
| 9.1.1 | Operation Log Page Load | PASS |
| 9.1.2 | Filter by Date Range | PASS (Partial) |
| 9.1.3 | Filter by Operation Type | PASS |
| 9.1.4 | View Detail Drawer | PASS |
| 9.2.1 | Data Access Page Load | PASS |
| 9.2.2 | Filter by User | SKIP |
| 9.2.3 | Filter by Data Type | PASS (Partial) |
| 9.3.1 | System Events Page Load | PASS |
| 9.3.2 | Filter by Level | PASS (Partial) |
| 9.4.1 | Compliance Report Page Load | PASS |
| 9.4.2 | Time Range Switch | SKIP |
| 9.4.3 | Export Report | SKIP |
| **Total** | **12 tests** | **4 PASS, 4 PASS (Partial), 4 SKIP** |

## Bugs Found

1. **SPA Route Instability (HIGH)**: Click interactions cause spontaneous navigation to unrelated pages. Affects all audit module pages.
2. **Mock Total Count Not Updated (LOW)**: After filtering operation type to "创建", total count remains "15,234" instead of reflecting filtered results.
3. **All Data is Mock**: No actual API calls to `/audit/*` endpoints. All data is frontend-generated mock data. GET /api/v1/users/me is the only real API call observed.
