package com.maidc.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalVO {

    private Long id;

    private String approvalType;

    private String status;

    private String submittedBy;

    private LocalDateTime submittedAt;

    private String reviewedBy;

    private LocalDateTime reviewedAt;

    private String reviewComment;
}
