package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import com.maidc.data.dto.PatientCreateDTO;
import com.maidc.data.dto.PatientQueryDTO;
import com.maidc.data.entity.PatientEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.PatientRepository;
import com.maidc.data.repository.PatientSpecification;
import com.maidc.data.vo.PatientVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final DataMapper dataMapper;

    @Transactional
    public PatientVO createPatient(PatientCreateDTO dto) {
        PatientEntity entity = new PatientEntity();
        // patient_no 为 DDL NOT NULL 列：调用方未提供时自动生成
        entity.setPatientNo(dto.getPatientNo() != null && !dto.getPatientNo().isBlank()
                ? dto.getPatientNo() : "P" + System.currentTimeMillis());
        entity.setName(dto.getName());
        entity.setGender(dto.getGender());
        entity.setBirthDate(dto.getBirthDate());
        entity.setIdCardNo(dto.getIdCardNo());
        entity.setBloodType(dto.getBloodType());
        entity.setPhone(dto.getPhone());
        entity.setAddress(dto.getAddress());
        entity.setSourceSystem("MANUAL");
        entity.setOrgId(dto.getOrgId() != null ? dto.getOrgId() : 0L);

        entity = patientRepository.save(entity);
        log.info("患者创建成功: id={}, name={}", entity.getId(), entity.getName());
        return maskSensitive(dataMapper.toPatientVO(entity));
    }

    public PatientVO getPatient(Long id) {
        PatientEntity entity = patientRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PATIENT_NOT_FOUND));
        return maskSensitive(dataMapper.toPatientVO(entity));
    }

    public PageResult<PatientVO> listPatients(PatientQueryDTO query) {
        Specification<PatientEntity> spec = PatientSpecification.buildSearchSpec(
                query.getOrgId(), query.getKeyword(), query.getGender());

        Page<PatientEntity> page = patientRepository.findAll(spec,
                PageRequest.of(query.getPage() - 1, query.getPageSize()));

        Page<PatientVO> voPage = page.map(dataMapper::toPatientVO);
        voPage.forEach(this::maskSensitive);
        return PageResult.of(voPage);
    }

    /**
     * 出口脱敏：列表/详情不回明文证件号/手机号（对齐 PatientEncounterService 就诊流约定）。
     */
    private PatientVO maskSensitive(PatientVO vo) {
        if (vo == null) {
            return null;
        }
        if (vo.getIdCardNo() != null && vo.getIdCardNo().length() >= 8) {
            int len = vo.getIdCardNo().length();
            vo.setIdCardNo(vo.getIdCardNo().substring(0, 3) + "***********" + vo.getIdCardNo().substring(len - 4));
        }
        if (vo.getPhone() != null && vo.getPhone().length() >= 7) {
            int len = vo.getPhone().length();
            vo.setPhone(vo.getPhone().substring(0, 3) + "****" + vo.getPhone().substring(len - 4));
        }
        return vo;
    }

    @Transactional
    public void deletePatient(Long id) {
        PatientEntity entity = patientRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PATIENT_NOT_FOUND));
        patientRepository.delete(entity);
        log.info("患者已删除: id={}", id);
    }
}
