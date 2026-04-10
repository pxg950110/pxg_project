package com.maidc.msg.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.Column;
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
@Table(name = "m_notification_setting", schema = "model")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE model.m_notification_setting SET is_deleted = true WHERE id = ?")
public class NotificationSettingEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "channel", nullable = false, length = 32)
    private String channel;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
}
