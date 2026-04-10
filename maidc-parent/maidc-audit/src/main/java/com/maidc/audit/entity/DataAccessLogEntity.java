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
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "org_id", length = 36)
    private String orgId;

    @Column(name = "data_type", length = 50)
    private String dataType;

    @Column(name = "data_id", length = 36)
    private String dataId;

    @Column(name = "action", length = 50)
    private String action;

    @Column(name = "patient_id", length = 36)
    private String patientId;

    @Column(name = "access_purpose", length = 200)
    private String accessPurpose;

    @Column(name = "ip", length = 50)
    private String ip;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
