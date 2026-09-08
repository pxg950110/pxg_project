package com.maidc.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "a_data_access_log", schema = "audit")
public class DataAccessLogEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "access_type", length = 32)
    private String accessType;

    @Column(name = "data_domain", length = 32)
    private String dataDomain;

    @Column(name = "table_name", length = 64)
    private String tableName;

    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "purpose", columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "data_volume")
    private Long dataVolume;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "org_id")
    private Long orgId;
}
