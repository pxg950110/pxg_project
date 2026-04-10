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
@Table(name = "m_message_template", schema = "model")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE model.m_message_template SET is_deleted = true WHERE id = ?")
public class MessageTemplateEntity extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 128)
    private String code;

    @Column(name = "title_template", nullable = false, length = 512)
    private String titleTemplate;

    @Column(name = "content_template", nullable = false, columnDefinition = "TEXT")
    private String contentTemplate;

    @Column(name = "channel", length = 32)
    private String channel;

    @Column(name = "event_type", length = 64)
    private String eventType;
}
