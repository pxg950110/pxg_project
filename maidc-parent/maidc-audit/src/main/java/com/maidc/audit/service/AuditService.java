package com.maidc.audit.service;

import com.maidc.audit.dto.AuditLogQueryDTO;
import com.maidc.audit.dto.DataAccessQueryDTO;
import com.maidc.audit.dto.EventQueryDTO;
import com.maidc.audit.entity.AuditLogEntity;
import com.maidc.audit.entity.DataAccessLogEntity;
import com.maidc.audit.entity.SystemEventEntity;
import com.maidc.audit.mapper.AuditMapper;
import com.maidc.audit.repository.AuditLogRepository;
import com.maidc.audit.repository.AuditLogSpecification;
import com.maidc.audit.repository.DataAccessLogRepository;
import com.maidc.audit.repository.DataAccessLogSpecification;
import com.maidc.audit.repository.SystemEventRepository;
import com.maidc.audit.repository.SystemEventSpecification;
import com.maidc.audit.vo.AuditLogVO;
import com.maidc.audit.vo.DataAccessLogVO;
import com.maidc.audit.vo.SystemEventVO;
import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final DataAccessLogRepository dataAccessLogRepository;
    private final SystemEventRepository systemEventRepository;
    private final AuditMapper auditMapper;

    /**
     * Query audit logs with dynamic filters and pagination
     */
    public PageResult<AuditLogVO> queryAuditLogs(AuditLogQueryDTO queryDTO) {
        Page<AuditLogEntity> page = auditLogRepository.findAll(
                AuditLogSpecification.buildSearchSpec(
                        queryDTO.getServiceName(),
                        queryDTO.getOperation(),
                        queryDTO.getUsername(),
                        queryDTO.getStartTime(),
                        queryDTO.getEndTime(),
                        queryDTO.getStatus()
                ),
                PageRequest.of(queryDTO.getPage() - 1, queryDTO.getPageSize())
        );

        Page<AuditLogVO> voPage = page.map(auditMapper::toAuditLogVO);
        return PageResult.of(voPage);
    }

    /**
     * Get audit log detail by id
     */
    public AuditLogVO getAuditLogDetail(Long id) {
        AuditLogEntity entity = auditLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return auditMapper.toAuditLogVO(entity);
    }

    /**
     * Query data access logs with dynamic filters and pagination
     */
    public PageResult<DataAccessLogVO> queryDataAccessLogs(DataAccessQueryDTO queryDTO) {
        Page<DataAccessLogEntity> page = dataAccessLogRepository.findAll(
                DataAccessLogSpecification.buildSearchSpec(
                        queryDTO.getUserId(),
                        queryDTO.getDataDomain(),
                        queryDTO.getPatientId(),
                        queryDTO.getStartTime(),
                        queryDTO.getEndTime()
                ),
                PageRequest.of(queryDTO.getPage() - 1, queryDTO.getPageSize())
        );

        Page<DataAccessLogVO> voPage = page.map(auditMapper::toDataAccessLogVO);
        return PageResult.of(voPage);
    }

    /**
     * Query system events with dynamic filters and pagination
     */
    public PageResult<SystemEventVO> querySystemEvents(EventQueryDTO queryDTO) {
        Page<SystemEventEntity> page = systemEventRepository.findAll(
                SystemEventSpecification.buildSearchSpec(
                        queryDTO.getEventType(),
                        queryDTO.getEventLevel(),
                        queryDTO.getStartTime(),
                        queryDTO.getEndTime()
                ),
                PageRequest.of(queryDTO.getPage() - 1, queryDTO.getPageSize())
        );

        Page<SystemEventVO> voPage = page.map(auditMapper::toSystemEventVO);
        return PageResult.of(voPage);
    }

    /**
     * Generate compliance report summary for a given time range
     */
    public Map<String, Object> getComplianceReport(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> report = new LinkedHashMap<>();

        // Total operation count
        long totalOperations = auditLogRepository.count(
                AuditLogSpecification.buildSearchSpec(null, null, null, startTime, endTime, null)
        );
        report.put("totalOperations", totalOperations);

        // Failed operation count (status = FAILURE)
        long failedOperations = auditLogRepository.count(
                AuditLogSpecification.buildSearchSpec(null, null, null, startTime, endTime, "FAILURE")
        );
        report.put("failedOperations", failedOperations);

        // Success rate
        double successRate = totalOperations > 0
                ? Math.round((1.0 - (double) failedOperations / totalOperations) * 10000.0) / 100.0
                : 100.0;
        report.put("successRate", successRate + "%");

        // Data access count
        long totalDataAccess = dataAccessLogRepository.count(
                DataAccessLogSpecification.buildSearchSpec(null, null, null, startTime, endTime)
        );
        report.put("totalDataAccess", totalDataAccess);

        // System event count
        long totalEvents = systemEventRepository.count(
                SystemEventSpecification.buildSearchSpec(null, null, startTime, endTime)
        );
        report.put("totalSystemEvents", totalEvents);

        // Critical event count (eventLevel = CRITICAL)
        long criticalEvents = systemEventRepository.count(
                SystemEventSpecification.buildSearchSpec(null, "CRITICAL", startTime, endTime)
        );
        report.put("criticalEvents", criticalEvents);

        // Time range info
        report.put("startTime", startTime);
        report.put("endTime", endTime);

        log.info("合规报表生成: startTime={}, endTime={}, totalOps={}, dataAccess={}, events={}",
                startTime, endTime, totalOperations, totalDataAccess, totalEvents);

        return report;
    }

    public void exportAuditLogsCsv(AuditLogQueryDTO queryDTO, HttpServletResponse response) throws IOException {
        List<AuditLogEntity> logs = auditLogRepository.findAll(
                AuditLogSpecification.buildSearchSpec(
                        queryDTO.getServiceName(), queryDTO.getOperation(),
                        queryDTO.getUsername(), queryDTO.getStartTime(),
                        queryDTO.getEndTime(), queryDTO.getStatus()
                )
        );

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=operation_logs.csv");
        PrintWriter writer = response.getWriter();
        writer.write("\uFEFF");
        writer.println("时间,操作人,模块,操作,请求方法,请求路径,IP地址,耗时(ms),状态");
        for (AuditLogEntity e : logs) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    e.getCreatedAt(), e.getUsername(), e.getServiceName(),
                    e.getOperation(), e.getRequestMethod(), e.getRequestUrl(),
                    e.getIpAddress(), e.getDurationMs(), e.getStatus());
        }
        writer.flush();
    }

    public void exportDataAccessLogsCsv(DataAccessQueryDTO queryDTO, HttpServletResponse response) throws IOException {
        List<DataAccessLogEntity> logs = dataAccessLogRepository.findAll(
                DataAccessLogSpecification.buildSearchSpec(
                        queryDTO.getUserId(), queryDTO.getDataDomain(),
                        queryDTO.getPatientId(), queryDTO.getStartTime(),
                        queryDTO.getEndTime()
                )
        );

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=data_access_logs.csv");
        PrintWriter writer = response.getWriter();
        writer.write("\uFEFF");
        writer.println("时间,操作人ID,访问类型,数据域,表名,记录ID,患者ID,目的,IP地址");
        for (DataAccessLogEntity e : logs) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    e.getCreatedAt(), e.getUserId(), e.getAccessType(),
                    e.getDataDomain(), e.getTableName(), e.getRecordId(),
                    e.getPatientId(), e.getPurpose(), e.getIpAddress());
        }
        writer.flush();
    }

    public void exportSystemEventsCsv(EventQueryDTO queryDTO, HttpServletResponse response) throws IOException {
        List<SystemEventEntity> events = systemEventRepository.findAll(
                SystemEventSpecification.buildSearchSpec(
                        queryDTO.getEventType(), queryDTO.getEventLevel(),
                        queryDTO.getStartTime(), queryDTO.getEndTime()
                )
        );

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=system_events.csv");
        PrintWriter writer = response.getWriter();
        writer.write("\uFEFF");
        writer.println("时间,事件类型,级别,来源,标题,详情,已解决");
        for (SystemEventEntity e : events) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s%n",
                    e.getCreatedAt(), e.getEventType(), e.getEventLevel(),
                    e.getSource(), e.getEventTitle(),
                    e.getEventDetail() != null ? e.getEventDetail().replace(",", "，") : "",
                    e.getResolved());
        }
        writer.flush();
    }
}
