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
public class DeploymentVO {

    private Long id;

    private Long modelId;

    private Long versionId;

    private String deploymentName;

    private String environment;

    private String status;

    private String endpointUrl;

    private Integer replicas;

    private LocalDateTime startedAt;

    private LocalDateTime lastHealthCheck;
}
