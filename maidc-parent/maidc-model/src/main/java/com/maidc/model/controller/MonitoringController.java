package com.maidc.model.controller;

import com.maidc.common.core.result.PageResult;
import com.maidc.common.core.result.R;
import com.maidc.model.service.MonitoringService;
import com.maidc.model.vo.MetricDataVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping("/metrics")
    public R<Map<String, Object>> getMetricsOverview() {
        return R.ok(Map.of(
                "totalDeployments", 0,
                "activeDeployments", 0,
                "totalInferences", 0,
                "avgLatency", 0.0
        ));
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping("/deployments/{id}/logs")
    public R<PageResult<Map<String, Object>>> getInferenceLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start_time,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end_time,
            @RequestParam(required = false) String status) {
        return R.ok(monitoringService.getInferenceLogs(id, page, pageSize, start_time, end_time, status));
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping("/deployments/{id}/metrics")
    public R<MetricDataVO> getMetrics(
            @PathVariable Long id,
            @RequestParam(required = false) String metric_name,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start_time,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end_time,
            @RequestParam(defaultValue = "5m") String interval) {
        return R.ok(monitoringService.getMetrics(id, metric_name, start_time, end_time, interval));
    }
}
