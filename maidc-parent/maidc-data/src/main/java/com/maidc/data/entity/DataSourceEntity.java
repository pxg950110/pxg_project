package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "r_data_source", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.r_data_source SET is_deleted = true WHERE id = ?")
public class DataSourceEntity extends BaseEntity {

    @Column(name = "source_name", nullable = false, length = 128)
    private String sourceName;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "host", length = 256)
    private String host;

    @Column(name = "port")
    private Integer port;

    @Column(name = "database_name", length = 128)
    private String databaseName;

    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "password", length = 256)
    private String password;

    @Column(name = "config", columnDefinition = "jsonb")
    private String config;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "source_type_code", length = 64)
    private String sourceTypeCode;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "connection_params", columnDefinition = "jsonb")
    private JsonNode connectionParams;
}
