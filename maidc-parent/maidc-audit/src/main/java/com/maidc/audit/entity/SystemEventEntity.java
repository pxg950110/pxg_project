package com.maidc.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "a_system_event", schema = "audit")
public class SystemEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 对应 DDL BIGSERIAL，消费侧首次写表
    @Column(name = "id")
    private Long id;

    @Column(name = "event_type", length = 32)
    private String eventType;

    @Column(name = "event_level", length = 16)
    private String eventLevel;

    @Column(name = "source", length = 64)
    private String source;

    @Column(name = "event_title", length = 128)
    private String eventTitle;

    @Column(name = "event_detail", columnDefinition = "TEXT")
    private String eventDetail;

    @Column(name = "event_data", columnDefinition = "JSONB")
    private String eventData;

    @Column(name = "resolved")
    private Boolean resolved = false;

    @Column(name = "resolved_by", length = 64)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "org_id")
    private Long orgId;
}
