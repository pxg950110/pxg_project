package com.maidc.model.controller;

import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.model.dto.DeploymentCreateDTO;
import com.maidc.model.dto.DeploymentScaleDTO;
import com.maidc.model.service.DeploymentService;
import com.maidc.model.vo.DeploymentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    @OperLog(module = "model", operation = "createDeployment")
    @PreAuthorize("hasPermission('model:deploy')")
    @PostMapping
    public R<DeploymentVO> createDeployment(@RequestBody @Valid DeploymentCreateDTO dto) {
        return R.ok(deploymentService.createDeployment(dto));
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping("/{id}/status")
    public R<DeploymentVO> getStatus(@PathVariable Long id) {
        return R.ok(deploymentService.getDeploymentStatus(id));
    }

    @OperLog(module = "model", operation = "startDeployment")
    @PreAuthorize("hasPermission('model:deploy')")
    @PutMapping("/{id}/start")
    public R<DeploymentVO> start(@PathVariable Long id) {
        return R.ok(deploymentService.startDeployment(id));
    }

    @OperLog(module = "model", operation = "stopDeployment")
    @PreAuthorize("hasPermission('model:deploy')")
    @PutMapping("/{id}/stop")
    public R<DeploymentVO> stop(@PathVariable Long id) {
        return R.ok(deploymentService.stopDeployment(id));
    }

    @OperLog(module = "model", operation = "scaleDeployment")
    @PreAuthorize("hasPermission('model:deploy')")
    @PutMapping("/{id}/scale")
    public R<DeploymentVO> scale(@PathVariable Long id, @RequestBody DeploymentScaleDTO dto) {
        return R.ok(deploymentService.scaleDeployment(id, dto));
    }

    @OperLog(module = "model", operation = "restartDeployment")
    @PreAuthorize("hasPermission('model:deploy')")
    @PostMapping("/{id}/restart")
    public R<DeploymentVO> restart(@PathVariable Long id) {
        return R.ok(deploymentService.restartDeployment(id));
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping
    public R<List<DeploymentVO>> list(@RequestParam(required = false) String status) {
        return R.ok(deploymentService.listDeployments(status));
    }
}
