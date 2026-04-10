package com.maidc.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalCreateDTO {

    private Long modelVersionId;

    @NotBlank(message = "审批类型不能为空")
    private String approvalType;

    private JsonNode evidenceDocs;

    private String riskAssessment;
}
