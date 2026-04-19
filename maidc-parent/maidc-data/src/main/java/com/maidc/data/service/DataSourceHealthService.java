package com.maidc.data.service;

import com.maidc.data.entity.DataSourceHealthEntity;
import com.maidc.data.repository.DataSourceHealthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceHealthService {

    private final DataSourceHealthRepository healthRepository;

    @Transactional
    public DataSourceHealthEntity recordHealth(Long sourceId, String checkType,
                                                String status, Integer latencyMs, String errorMessage) {
        DataSourceHealthEntity entity = new DataSourceHealthEntity();
        entity.setSourceId(sourceId);
        entity.setCheckType(checkType);
        entity.setStatus(status);
        entity.setLatencyMs(latencyMs);
        entity.setErrorMessage(errorMessage);
        entity.setCheckedAt(LocalDateTime.now());
        return healthRepository.save(entity);
    }

    public List<DataSourceHealthEntity> getRecentHealth(Long sourceId, int limit) {
        return healthRepository.findTop50BySourceIdAndIsDeletedFalseOrderByCheckedAtDesc(sourceId)
                .stream().limit(limit).toList();
    }

    public Map<String, Object> getHealthStats(Long sourceId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        long total = healthRepository.countSince(sourceId, since);
        long success = healthRepository.countSuccessSince(sourceId, since);
        Double avgLatency = healthRepository.avgLatencySince(sourceId, since);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalChecks", total);
        stats.put("successCount", success);
        stats.put("failCount", total - success);
        stats.put("availabilityRate", total > 0 ? (double) success / total : 0);
        stats.put("avgLatencyMs", avgLatency != null ? avgLatency : 0);
        stats.put("since", since);
        return stats;
    }
}
