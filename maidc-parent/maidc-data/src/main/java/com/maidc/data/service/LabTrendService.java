package com.maidc.data.service;

import com.maidc.data.dto.LabTrendDTO;
import com.maidc.data.entity.LabPanelEntity;
import com.maidc.data.entity.LabTestEntity;
import com.maidc.data.repository.LabPanelRepository;
import com.maidc.data.repository.LabTestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 检验趋势服务 - 提供检验数据的趋势分析和聚合
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabTrendService {

    private final LabTestRepository labTestRepository;
    private final LabPanelRepository labPanelRepository;

    /**
     * 获取患者的检验趋势数据
     *
     * @param patientId 患者ID
     * @param itemCodes 检验项目编码列表(可选,为空则返回所有项目)
     * @param startDate 开始日期(可选)
     * @param endDate 结束日期(可选)
     * @return 按检验项目分组的趋势数据列表
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "labTrends", key = "#patientId + '_' + #itemCodes?.hashCode() + '_' + #startDate + '_' + #endDate")
    public List<LabTrendDTO> getLabTrends(Long patientId, List<String> itemCodes,
                                           LocalDateTime startDate, LocalDateTime endDate) {
        log.info("获取患者检验趋势数据: patientId={}, itemCodes={}, startDate={}, endDate={}",
                patientId, itemCodes, startDate, endDate);

        // 1. 查询患者的所有检验记录
        List<LabTestEntity> labTests = labTestRepository.findByPatientIdAndIsDeletedFalse(patientId);

        // 2. 日期过滤(以采样时间为准)
        final List<LabTestEntity> filteredTests;
        if (startDate != null || endDate != null) {
            filteredTests = labTests.stream()
                    .filter(test -> {
                        LocalDateTime sampleTime = sampleTimeOf(test);
                        if (sampleTime == null) {
                            return true;
                        }
                        if (startDate != null && sampleTime.isBefore(startDate)) {
                            return false;
                        }
                        if (endDate != null && sampleTime.isAfter(endDate)) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        } else {
            filteredTests = labTests;
        }

        // 3. 查询所有检验明细并按项目编码分组
        List<Long> labTestIds = filteredTests.stream()
                .map(LabTestEntity::getId)
                .collect(Collectors.toList());

        List<LabPanelEntity> allPanels = labTestIds.isEmpty()
                ? List.of()
                : labPanelRepository.findByTestIdInAndIsDeletedFalse(labTestIds);

        // 4. 按itemCode分组
        Map<String, List<LabPanelEntity>> panelsByCode = allPanels.stream()
                .filter(panel -> itemCodes == null || itemCodes.isEmpty() || itemCodes.contains(panel.getItemCode()))
                .collect(Collectors.groupingBy(LabPanelEntity::getItemCode));

        // 5. 转换为DTO
        return panelsByCode.entrySet().stream()
                .map(entry -> buildLabTrendDTO(entry.getKey(), entry.getValue(), filteredTests))
                .sorted(Comparator.comparing(LabTrendDTO::getItemName))
                .collect(Collectors.toList());
    }

    /**
     * 采样时间: 依次取标本采集时间、报告时间、开单时间
     */
    private LocalDateTime sampleTimeOf(LabTestEntity test) {
        if (test.getCollectedAt() != null) {
            return test.getCollectedAt();
        }
        if (test.getReportedAt() != null) {
            return test.getReportedAt();
        }
        return test.getOrderedAt();
    }

    /**
     * 构建单个检验项目的趋势DTO
     */
    private LabTrendDTO buildLabTrendDTO(String itemCode, List<LabPanelEntity> panels,
                                          List<LabTestEntity> labTests) {
        LabTrendDTO dto = new LabTrendDTO();
        dto.setItemCode(itemCode);

        if (!panels.isEmpty()) {
            LabPanelEntity firstPanel = panels.get(0);
            dto.setItemName(firstPanel.getItemName());
            dto.setUnit(firstPanel.getResultUnit());
            String[] range = splitReferenceRange(firstPanel.getReferenceRange());
            dto.setReferenceRangeLow(range[0]);
            dto.setReferenceRangeHigh(range[1]);
        }

        // 构建数据点
        Map<Long, LabTestEntity> testMap = labTests.stream()
                .collect(Collectors.toMap(LabTestEntity::getId, test -> test));

        List<LabTrendDTO.LabTrendPointDTO> dataPoints = panels.stream()
                .map(panel -> {
                    LabTestEntity test = testMap.get(panel.getTestId());
                    if (test == null) return null;

                    LabTrendDTO.LabTrendPointDTO point = new LabTrendDTO.LabTrendPointDTO();
                    point.setId(panel.getId());
                    point.setSampleTime(sampleTimeOf(test));
                    point.setResult(panel.getResultValue());
                    point.setAbnormalFlag(Boolean.TRUE.equals(panel.getAbnormalFlag()) ? "Y" : "N");
                    point.setEncounterId(test.getEncounterId());
                    point.setDeptName(null);
                    point.setEncounterType(null);
                    return point;
                })
                .filter(Objects::nonNull)
                .filter(point -> point.getSampleTime() != null)
                .sorted(Comparator.comparing(LabTrendDTO.LabTrendPointDTO::getSampleTime))
                .collect(Collectors.toList());

        dto.setDataPoints(dataPoints);
        return dto;
    }

    /**
     * 拆分参考范围字符串, 如 "3.5-5.5" -> ["3.5", "5.5"]
     */
    private String[] splitReferenceRange(String referenceRange) {
        if (referenceRange == null || referenceRange.isBlank()) {
            return new String[]{null, null};
        }
        String[] parts = referenceRange.split("-", 2);
        if (parts.length == 2) {
            return new String[]{parts[0].trim(), parts[1].trim()};
        }
        return new String[]{referenceRange.trim(), null};
    }

    /**
     * 获取检验项目编码列表(用于前端选择)
     */
    @Transactional(readOnly = true)
    public List<Map<String, String>> getLabItemCodes(Long patientId) {
        List<LabTestEntity> labTests = labTestRepository.findByPatientIdAndIsDeletedFalse(patientId);
        List<Long> labTestIds = labTests.stream()
                .map(LabTestEntity::getId)
                .collect(Collectors.toList());

        List<LabPanelEntity> panels = labTestIds.isEmpty()
                ? List.of()
                : labPanelRepository.findByTestIdInAndIsDeletedFalse(labTestIds);

        return panels.stream()
                .collect(Collectors.groupingBy(
                        LabPanelEntity::getItemCode,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(LabPanelEntity::getCreatedAt,
                                        Comparator.nullsFirst(Comparator.naturalOrder()))),
                                opt -> opt.map(p -> {
                                    Map<String, String> map = new HashMap<>();
                                    map.put("itemCode", p.getItemCode());
                                    map.put("itemName", p.getItemName());
                                    map.put("unit", p.getResultUnit());
                                    return map;
                                }).orElse(null))
                ))
                .values().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(m -> m.get("itemName")))
                .collect(Collectors.toList());
    }
}
