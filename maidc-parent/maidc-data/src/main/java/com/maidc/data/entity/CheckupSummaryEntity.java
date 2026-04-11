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
@Table(name = "c_checkup_summary", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_checkup_summary SET is_deleted = true WHERE id = ?")
public class CheckupSummaryEntity extends BaseEntity {

    @Column(name = "checkup_id", nullable = false)
    private Long checkupId;

    @Column(name = "summary_type", length = 32)
    private String summaryType;

    @Column(name = "summary_content", columnDefinition = "TEXT")
    private String summaryContent;
}
