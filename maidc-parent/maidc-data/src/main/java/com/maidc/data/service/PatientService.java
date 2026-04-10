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
        entity.setName(dto.getName());
        entity.setGender(dto.getGender());
        entity.setBirthDate(dto.getBirthDate());
        entity.setIdCardHash(dto.getIdCardHash());
        entity.setPhoneHash(dto.getPhoneHash());
        entity.setAddress(dto.getAddress());
        entity.setOrgId(dto.getOrgId() != null ? dto.getOrgId() : 0L);

        entity = patientRepository.save(entity);
        log.info("患者创建成功: id={}, name={}", entity.getId(), entity.getName());
        return dataMapper.toPatientVO(entity);
    }

    public PatientVO getPatient(Long id) {
        PatientEntity entity = patientRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PATIENT_NOT_FOUND));
        return dataMapper.toPatientVO(entity);
    }

    public PageResult<PatientVO> listPatients(PatientQueryDTO query) {
        Specification<PatientEntity> spec = PatientSpecification.buildSearchSpec(
                query.getOrgId(), query.getKeyword(), query.getGender());

        Page<PatientEntity> page = patientRepository.findAll(spec,
                PageRequest.of(query.getPage() - 1, query.getPageSize()));

        Page<PatientVO> voPage = page.map(dataMapper::toPatientVO);
        return PageResult.of(voPage);
    }

    @Transactional
    public void deletePatient(Long id) {
        PatientEntity entity = patientRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PATIENT_NOT_FOUND));
        patientRepository.delete(entity);
        log.info("患者已删除: id={}", id);
    }
}
