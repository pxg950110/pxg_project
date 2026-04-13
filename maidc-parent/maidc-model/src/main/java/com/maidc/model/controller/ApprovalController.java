package com.maidc.model.controller;

import com.maidc.common.core.result.R;
import com.maidc.common.core.result.PageResult;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.model.dto.ApprovalCreateDTO;
import com.maidc.model.dto.ApprovalReviewDTO;
import com.maidc.model.service.ApprovalService;
import com.maidc.model.vo.ApprovalVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping
    public R<PageResult<ApprovalVO>> listApprovals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(approvalService.listApprovals(page, pageSize));
    }

    @OperLog(module = "model", operation = "submitApproval")
    @PreAuthorize("hasPermission('model:approve')")
    @PostMapping
    public R<ApprovalVO> submitApproval(@RequestBody @Valid ApprovalCreateDTO dto) {
        return R.ok(approvalService.submitApproval(dto));
    }

    @OperLog(module = "model", operation = "reviewApproval")
    @PreAuthorize("hasPermission('model:approve')")
    @PutMapping("/{id}/review")
    public R<ApprovalVO> reviewApproval(@PathVariable Long id,
                                          @RequestBody @Valid ApprovalReviewDTO dto,
                                          @RequestHeader("X-User-Id") Long userId) {
        return R.ok(approvalService.reviewApproval(id, dto, userId));
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping("/{id}")
    public R<ApprovalVO> getApproval(@PathVariable Long id) {
        return R.ok(approvalService.getApprovalDetail(id));
    }
}
