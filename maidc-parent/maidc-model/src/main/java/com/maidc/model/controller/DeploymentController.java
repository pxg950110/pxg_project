package com.maidc.model.controller;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.model.dto.DeploymentCreateDTO;
import com.maidc.model.dto.DeploymentScaleDTO;
import com.maidc.model.dto.RouteUpsertDTO;
import com.maidc.model.entity.DeployRouteEntity;
import com.maidc.model.repository.DeployRouteRepository;
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
    private final DeployRouteRepository deployRouteRepository;

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

    /** 前端 model.ts 契约：GET /deployments/{id} 详情（复用状态聚合视图） */
    @PreAuthorize("hasPermission('model:read')")
    @GetMapping("/{id}")
    public R<DeploymentVO> getDeployment(@PathVariable Long id) {
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
    @GetMapping("/routes")
    public R<List<DeployRouteEntity>> listRoutes() {
        return R.ok(deployRouteRepository.findAll());
    }

    /** 创建路由（{name, type, rules[]} → route_name/route_type/config） */
    @OperLog(module = "model", operation = "createRoute")
    @PreAuthorize("hasPermission('model:deploy')")
    @PostMapping("/routes")
    public R<DeployRouteEntity> createRoute(@RequestBody RouteUpsertDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        DeployRouteEntity route = new DeployRouteEntity();
        route.setRouteName(dto.getName());
        if (dto.getType() != null) {
            route.setRouteType(dto.getType());
        }
        route.setConfig(dto.getRules());
        return R.ok(deployRouteRepository.save(route));
    }

    /** 更新路由（null 字段不动） */
    @OperLog(module = "model", operation = "updateRoute")
    @PreAuthorize("hasPermission('model:deploy')")
    @PutMapping("/routes/{id}")
    public R<DeployRouteEntity> updateRoute(@PathVariable Long id, @RequestBody RouteUpsertDTO dto) {
        DeployRouteEntity route = deployRouteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (dto.getName() != null && !dto.getName().isBlank()) {
            route.setRouteName(dto.getName());
        }
        if (dto.getType() != null) {
            route.setRouteType(dto.getType());
        }
        if (dto.getRules() != null) {
            route.setConfig(dto.getRules());
        }
        return R.ok(deployRouteRepository.save(route));
    }

    @PreAuthorize("hasPermission('model:read')")
    @GetMapping
    public R<List<DeploymentVO>> list(@RequestParam(required = false) String status) {
        return R.ok(deploymentService.listDeployments(status));
    }
}
