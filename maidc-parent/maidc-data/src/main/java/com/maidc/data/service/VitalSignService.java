package com.maidc.data.service;

import com.maidc.data.dto.VitalSignDTO;
import com.maidc.data.entity.VitalSignEntity;
import com.maidc.data.repository.VitalSignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VitalSignService {

    private final VitalSignRepository vitalSignRepository;

    public VitalSignEntity getVitalSign(Long id) {
        return vitalSignRepository.findById(id).orElse(null);
    }

    public Page<VitalSignEntity> listVitalSigns(Long encounterId, int page, int size) {
        return vitalSignRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public VitalSignEntity createVitalSign(VitalSignEntity entity) {
        return vitalSignRepository.save(entity);
    }

    @Transactional
    public void deleteVitalSign(Long id) {
        vitalSignRepository.deleteById(id);
    }

    /**
     * 获取患者的生命体征数据(用于图表可视化)
     * c_vital_sign为EAV结构(sign_type/sign_value), 按测量时间透视成宽表DTO
     */
    @Transactional(readOnly = true)
    public List<VitalSignDTO> getPatientVitalSigns(Long patientId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("获取患者生命体征数据: patientId={}, startDate={}, endDate={}", patientId, startDate, endDate);

        List<VitalSignEntity> vitalSigns = vitalSignRepository.findByPatientIdAndIsDeletedFalse(patientId);

        if (startDate != null || endDate != null) {
            vitalSigns = vitalSigns.stream()
                    .filter(sign -> {
                        if (sign.getMeasuredAt() == null) {
                            return true;
                        }
                        if (startDate != null && sign.getMeasuredAt().isBefore(startDate)) {
                            return false;
                        }
                        if (endDate != null && sign.getMeasuredAt().isAfter(endDate)) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        return pivotToDTOs(vitalSigns);
    }

    /**
     * 获取遇诊的生命体征数据(用于图表可视化)
     */
    @Transactional(readOnly = true)
    public List<VitalSignDTO> getEncounterVitalSigns(Long encounterId) {
        log.info("获取遇诊生命体征数据: encounterId={}", encounterId);

        List<VitalSignEntity> vitalSigns = vitalSignRepository.findByEncounterIdAndIsDeletedFalse(encounterId);

        return pivotToDTOs(vitalSigns);
    }

    /**
     * 将同一测量时间点的多条体征记录合并为一条宽表DTO
     */
    private List<VitalSignDTO> pivotToDTOs(List<VitalSignEntity> vitalSigns) {
        Map<String, List<VitalSignEntity>> byTimePoint = vitalSigns.stream()
                .filter(sign -> sign.getMeasuredAt() != null)
                .collect(Collectors.groupingBy(
                        sign -> sign.getEncounterId() + "@" + sign.getMeasuredAt(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        return byTimePoint.values().stream()
                .map(group -> {
                    VitalSignDTO dto = new VitalSignDTO();
                    dto.setRecordTime(group.get(0).getMeasuredAt());
                    dto.setEncounterId(group.get(0).getEncounterId());
                    for (VitalSignEntity sign : group) {
                        applySignValue(dto, sign.getSignType(), sign.getSignValue());
                        if (dto.getNurseName() == null && sign.getMeasuredBy() != null) {
                            dto.setNurseName(sign.getMeasuredBy());
                        }
                    }
                    dto.setBmi(calculateBMI(dto.getWeight(), dto.getHeight()));
                    return dto;
                })
                .sorted(Comparator.comparing(VitalSignDTO::getRecordTime))
                .collect(Collectors.toList());
    }

    private void applySignValue(VitalSignDTO dto, String signType, BigDecimal value) {
        if (signType == null || value == null) {
            return;
        }
        double v = value.doubleValue();
        switch (signType) {
            case "TEMPERATURE" -> dto.setTemperature(v);
            case "PULSE" -> dto.setPulse((int) v);
            case "BP_SYSTOLIC" -> dto.setSystolicBP((int) v);
            case "BP_DIASTOLIC" -> dto.setDiastolicBP((int) v);
            case "RESPIRATION" -> dto.setRespirationRate((int) v);
            case "SPO2" -> dto.setOxygenSaturation((int) v);
            case "HEIGHT" -> dto.setHeight(v);
            case "WEIGHT" -> dto.setWeight(v);
            default -> log.debug("未知体征类型: {}", signType);
        }
    }

    private Double calculateBMI(Double weight, Double height) {
        if (weight == null || height == null || height == 0) {
            return null;
        }
        double heightInMeters = height / 100.0;
        return Math.round(weight / (heightInMeters * heightInMeters) * 100.0) / 100.0;
    }
}
