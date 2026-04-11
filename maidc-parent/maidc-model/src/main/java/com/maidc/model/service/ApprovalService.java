package com.maidc.model.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.model.dto.ApprovalCreateDTO;
import com.maidc.model.dto.ApprovalReviewDTO;
import com.maidc.model.entity.ApprovalEntity;
import com.maidc.model.mapper.ModelMapper;
import com.maidc.model.repository.ApprovalRepository;
import com.maidc.model.vo.ApprovalVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public ApprovalVO submitApproval(ApprovalCreateDTO dto) {
        ApprovalEntity approval = new ApprovalEntity();
        approval.setVersionId(dto.getModelVersionId());
        approval.setApprovalType(dto.getApprovalType());
        approval.setEvidenceDocs(dto.getEvidenceDocs());
        approval.setRiskAssessment(dto.getRiskAssessment() != null
                ? new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
                        .put("assessment", dto.getRiskAssessment())
                : null);
        approval.setStatus("PENDING");
        approval.setCurrentLevel(1);
        approval.setSubmittedAt(LocalDateTime.now());

        approval = approvalRepository.save(approval);
        log.info("审批已提交: id={}, type={}", approval.getId(), dto.getApprovalType());
        return modelMapper.toApprovalVO(approval);
    }

    @Transactional
    public ApprovalVO reviewApproval(Long id, ApprovalReviewDTO dto, Long reviewerId) {
        ApprovalEntity approval = approvalRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVAL_NOT_FOUND));

        if (!"PENDING".equals(approval.getStatus())) {
            throw new BusinessException(ErrorCode.APPROVAL_NOT_PENDING);
        }

        approval.setReviewedBy(reviewerId);
        approval.setReviewedAt(LocalDateTime.now());
        approval.setReviewComment(dto.getResultComment());

        if ("APPROVED".equals(dto.getResult())) {
            approval.setStatus("APPROVED");
        } else {
            approval.setStatus("REJECTED");
        }

        approval = approvalRepository.save(approval);
        log.info("审批完成: id={}, result={}", id, dto.getResult());
        return modelMapper.toApprovalVO(approval);
    }

    public ApprovalVO getApprovalDetail(Long id) {
        ApprovalEntity approval = approvalRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVAL_NOT_FOUND));
        return modelMapper.toApprovalVO(approval);
    }
}
