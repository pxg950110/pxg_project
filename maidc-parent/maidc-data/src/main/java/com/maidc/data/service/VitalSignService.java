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

import java.time.LocalDateTime;
import java.util.List;
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
     */
    @Transactional(readOnly = true)
    public List<VitalSignDTO> getPatientVitalSigns(Long patientId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("获取患者生命体征数据: patientId={}, startDate={}, endDate={}", patientId, startDate, endDate);

        List<VitalSignEntity> vitalSigns;

        if (startDate != null && endDate != null) {
            vitalSigns = vitalSignRepository.findByPatientIdAndRecordTimeBetweenOrderByRecordTimeDesc(patientId, startDate, endDate);
        } else {
            vitalSigns = vitalSignRepository.findByPatientIdOrderByRecordTimeDesc(patientId);
        }

        return vitalSigns.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取遇诊的生命体征数据(用于图表可视化)
     */
    @Transactional(readOnly = true)
    public List<VitalSignDTO> getEncounterVitalSigns(Long encounterId) {
        log.info("获取遇诊生命体征数据: encounterId={}", encounterId);

        List<VitalSignEntity> vitalSigns = vitalSignRepository.findByEncounterIdOrderByRecordTimeDesc(encounterId);

        return vitalSigns.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private VitalSignDTO convertToDTO(VitalSignEntity entity) {
        VitalSignDTO dto = new VitalSignDTO();
        dto.setEncounterId(entity.getEncounterId());
        dto.setRecordTime(entity.getRecordTime());
        dto.setTemperature(entity.getTemperature());
        dto.setSystolicBP(entity.getSystolicBP());
        dto.setDiastolicBP(entity.getDiastolicBP());
        dto.setPulse(entity.getPulse());
        dto.setRespirationRate(entity.getRespirationRate());
        dto.setOxygenSaturation(entity.getOxygenSaturation());
        dto.setWeight(entity.getWeight());
        dto.setHeight(entity.getHeight());
        dto.setBmi(calculateBMI(entity.getWeight(), entity.getHeight()));
        dto.setDeptName(entity.getDeptName());
        dto.setNurseName(entity.getNurseName());
        return dto;
    }

    private Double calculateBMI(Double weight, Double height) {
        if (weight == null || height == null || height == 0) {
            return null;
        }
        double heightInMeters = height / 100.0;
        return Math.round(weight / (heightInMeters * heightInMeters) * 100.0) / 100.0;
    }
}
