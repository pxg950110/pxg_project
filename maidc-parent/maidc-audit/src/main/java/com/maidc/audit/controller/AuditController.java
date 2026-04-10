package com.maidc.audit.controller;

import com.maidc.audit.dto.AuditLogQueryDTO;
import com.maidc.audit.dto.DataAccessQueryDTO;
import com.maidc.audit.dto.EventQueryDTO;
import com.maidc.audit.service.AuditService;
import com.maidc.audit.vo.AuditLogVO;
import com.maidc.audit.vo.DataAccessLogVO;
import com.maidc.audit.vo.SystemEventVO;
import com.maidc.common.core.result.PageResult;
import com.maidc.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * Query audit logs with pagination and filters
     */
    @PreAuthorize("hasPermission('audit:read')")
    @GetMapping("/operations")
    public R<PageResult<AuditLogVO>> queryAuditLogs(AuditLogQueryDTO queryDTO) {
        return R.ok(auditService.queryAuditLogs(queryDTO));
    }

    /**
     * Get audit log detail by id
     */
    @PreAuthorize("hasPermission('audit:read')")
    @GetMapping("/operations/{id}")
    public R<AuditLogVO> getAuditLogDetail(@PathVariable String id) {
        return R.ok(auditService.getAuditLogDetail(id));
    }

    /**
     * Query data access logs with pagination and filters
     */
    @PreAuthorize("hasPermission('audit:read')")
    @GetMapping("/data-access")
    public R<PageResult<DataAccessLogVO>> queryDataAccessLogs(DataAccessQueryDTO queryDTO) {
        return R.ok(auditService.queryDataAccessLogs(queryDTO));
    }

    /**
     * Query system events with pagination and filters
     */
    @PreAuthorize("hasPermission('audit:read')")
    @GetMapping("/events")
    public R<PageResult<SystemEventVO>> querySystemEvents(EventQueryDTO queryDTO) {
        return R.ok(auditService.querySystemEvents(queryDTO));
    }

    /**
     * Generate compliance report summary for a given time range
     */
    @PreAuthorize("hasPermission('audit:read')")
    @GetMapping("/reports/compliance")
    public R<Map<String, Object>> getComplianceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return R.ok(auditService.getComplianceReport(startTime, endTime));
    }
}
