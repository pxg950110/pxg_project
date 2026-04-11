package com.maidc.model.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.model.dto.DeploymentCreateDTO;
import com.maidc.model.dto.DeploymentScaleDTO;
import com.maidc.model.entity.DeploymentEntity;
import com.maidc.model.entity.ModelVersionEntity;
import com.maidc.model.mapper.ModelMapper;
import com.maidc.model.mq.ModelMessageProducer;
import com.maidc.model.repository.DeploymentRepository;
import com.maidc.model.repository.VersionRepository;
import com.maidc.model.vo.DeploymentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final VersionRepository versionRepository;
    private final ModelMessageProducer messageProducer;
    private final ModelMapper modelMapper;

    @Transactional
    public DeploymentVO createDeployment(DeploymentCreateDTO dto) {
        ModelVersionEntity version = versionRepository.findByIdAndIsDeletedFalse(dto.getModelVersionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND));

        DeploymentEntity deployment = new DeploymentEntity();
        deployment.setModelId(version.getModelId());
        deployment.setVersionId(dto.getModelVersionId());
        deployment.setDeploymentName(dto.getDeploymentName());
        deployment.setEnvironment(dto.getEnvironment());
        deployment.setResourceConfig(dto.getResourceConfig());
        deployment.setEndpointUrl(dto.getEndpointUrl());
        deployment.setReplicas(dto.getReplicas() != null ? dto.getReplicas() : 1);
        deployment.setStatus("CREATING");

        deployment = deploymentRepository.save(deployment);

        // Send deployment message to MQ
        Map<String, Object> configMap = new HashMap<>();
        if (dto.getResourceConfig() != null) {
            dto.getResourceConfig().fields().forEachRemaining(entry ->
                    configMap.put(entry.getKey(), entry.getValue()));
        }
        messageProducer.sendDeploymentTask(deployment.getId(), dto.getModelVersionId(), configMap);

        log.info("部署创建: id={}, name={}", deployment.getId(), dto.getDeploymentName());
        return modelMapper.toDeploymentVO(deployment);
    }

    public DeploymentVO getDeploymentStatus(Long id) {
        DeploymentEntity deployment = deploymentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND));
        return modelMapper.toDeploymentVO(deployment);
    }

    @Transactional
    public DeploymentVO startDeployment(Long id) {
        DeploymentEntity deployment = getDeploymentEntity(id);
        deployment.setStatus("RUNNING");
        deployment.setStartedAt(LocalDateTime.now());
        deployment.setLastHealthCheck(LocalDateTime.now());
        deployment = deploymentRepository.save(deployment);
        log.info("部署已启动: id={}", id);
        return modelMapper.toDeploymentVO(deployment);
    }

    @Transactional
    public DeploymentVO stopDeployment(Long id) {
        DeploymentEntity deployment = getDeploymentEntity(id);
        deployment.setStatus("STOPPED");
        deployment.setStoppedAt(LocalDateTime.now());
        deployment = deploymentRepository.save(deployment);
        log.info("部署已停止: id={}", id);
        return modelMapper.toDeploymentVO(deployment);
    }

    @Transactional
    public DeploymentVO scaleDeployment(Long id, DeploymentScaleDTO dto) {
        DeploymentEntity deployment = getDeploymentEntity(id);
        if (!"RUNNING".equals(deployment.getStatus())) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_NOT_RUNNING);
        }
        deployment.setStatus("SCALING");
        if (dto.getTargetReplicas() != null) deployment.setReplicas(dto.getTargetReplicas());
        if (dto.getResourceConfig() != null) deployment.setResourceConfig(dto.getResourceConfig());
        deployment = deploymentRepository.save(deployment);

        // Simulate scaling complete
        deployment.setStatus("RUNNING");
        deployment = deploymentRepository.save(deployment);
        log.info("部署扩缩容: id={}, replicas={}", id, deployment.getReplicas());
        return modelMapper.toDeploymentVO(deployment);
    }

    @Transactional
    public DeploymentVO restartDeployment(Long id) {
        DeploymentEntity deployment = getDeploymentEntity(id);
        deployment.setStatus("CREATING");
        deployment = deploymentRepository.save(deployment);

        deployment.setStatus("RUNNING");
        deployment.setStartedAt(LocalDateTime.now());
        deployment.setLastHealthCheck(LocalDateTime.now());
        deployment = deploymentRepository.save(deployment);
        log.info("部署已重启: id={}", id);
        return modelMapper.toDeploymentVO(deployment);
    }

    public List<DeploymentVO> listDeployments(String status) {
        if (status != null) {
            return deploymentRepository.findByStatusAndIsDeletedFalse(status).stream()
                    .map(modelMapper::toDeploymentVO).toList();
        }
        return deploymentRepository.findAll().stream()
                .filter(d -> !d.getIsDeleted())
                .map(modelMapper::toDeploymentVO).toList();
    }

    private DeploymentEntity getDeploymentEntity(Long id) {
        return deploymentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND));
    }
}
