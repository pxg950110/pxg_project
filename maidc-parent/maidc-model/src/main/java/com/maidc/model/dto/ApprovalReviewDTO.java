package com.maidc.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalReviewDTO {

    @NotBlank(message = "审批结果不能为空")
    private String result; // APPROVED / REJECTED

    private String resultComment;
}
