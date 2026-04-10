package com.maidc.model.service;

import com.maidc.common.core.result.PageResult;
import com.maidc.model.entity.InferenceLogEntity;
import com.maidc.model.entity.ModelMetricEntity;
import com.maidc.model.repository.InferenceLogRepository;
import com.maidc.model.repository.ModelMetricRepository;
import com.maidc.model.vo.MetricDataVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final InferenceLogRepository inferenceLogRepository;
    private final ModelMetricRepository modelMetricRepository;

    public PageResult<Map<String, Object>> getInferenceLogs(Long deploymentId, int page, int pageSize,
                                                              LocalDateTime startTime, LocalDateTime endTime, String status) {
        Page<InferenceLogEntity> result;
        if (startTime != null && endTime != null) {
            result = inferenceLogRepository.findByDeploymentIdAndCreatedAtBetween(
                    deploymentId, startTime, endTime, PageRequest.of(page - 1, pageSize));
        } else {
            result = inferenceLogRepository.findAll(PageRequest.of(page - 1, pageSize));
        }

        Page<Map<String, Object>> mapped = result.map(log -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("requestId", log.getRequestId());
            map.put("status", log.getStatus());
            map.put("latencyMs", log.getLatencyMs());
            map.put("createdAt", log.getCreatedAt());
            map.put("errorMessage", log.getErrorMessage());
            return map;
        });

        return PageResult.of(mapped);
    }

    public MetricDataVO getMetrics(Long deploymentId, String metricName,
                                    LocalDateTime startTime, LocalDateTime endTime, String interval) {
        List<ModelMetricEntity> metrics = modelMetricRepository
                .findByDeploymentIdAndCollectedAtBetweenOrderByCollectedAt(
                        deploymentId, startTime, endTime);

        List<Map<String, Object>> dataPoints = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (ModelMetricEntity m : metrics) {
            if (metricName == null || metricName.equals(m.getMetricName())) {
                Map<String, Object> point = new HashMap<>();
                point.put("time", m.getCollectedAt().format(fmt));
                point.put("value", m.getMetricValue());
                dataPoints.add(point);
            }
        }

        return MetricDataVO.builder()
                .metricName(metricName)
                .interval(interval != null ? interval : "5m")
                .dataPoints(dataPoints)
                .build();
    }
}
