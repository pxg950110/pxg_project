package com.maidc.data.entity;

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
@Table(name = "c_cohort_event", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_cohort_event SET is_deleted = true WHERE id = ?")
public class DiseaseCohortEventEntity extends BaseEntity {

    /** 关联队列；KB 全局发布等无队列关联时为空 */
    @Column(name = "cohort_id")
    private Long cohortId;

    /** SYNC_DONE / KB_ITEM_PUBLISHED / AI_SUGGEST_PENDING */
    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "event_title", nullable = false, length = 256)
    private String eventTitle;

    @Column(name = "event_detail", columnDefinition = "jsonb")
    private String eventDetail;
}
