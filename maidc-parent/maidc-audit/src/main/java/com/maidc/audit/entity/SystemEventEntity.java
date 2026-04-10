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
@Table(name = "a_system_event", schema = "audit")
public class SystemEventEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "event_source", length = 100)
    private String eventSource;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "operator", length = 100)
    private String operator;

    @Column(name = "ip", length = 50)
    private String ip;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
