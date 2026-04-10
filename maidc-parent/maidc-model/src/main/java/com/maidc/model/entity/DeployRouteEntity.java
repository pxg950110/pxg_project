package com.maidc.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.maidc.common.jpa.base.BaseEntity;
import com.maidc.common.jpa.converter.JsonNodeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "m_deploy_route", schema = "model")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE model.m_deploy_route SET is_deleted = true WHERE id = ?")
public class DeployRouteEntity extends BaseEntity {

    @Column(name = "route_name", nullable = false, length = 128)
    private String routeName;

    @Column(name = "route_type", length = 32)
    private String routeType;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "config", columnDefinition = "jsonb")
    private JsonNode config;

    @Column(name = "active_deployment_id")
    private Long activeDeploymentId;

    @Column(name = "canary_deployment_id")
    private Long canaryDeploymentId;

    @Column(name = "canary_weight")
    private Integer canaryWeight = 0;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";
}
