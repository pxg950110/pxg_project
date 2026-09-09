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
@Table(name = "c_disease_kb_qa_session", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_disease_kb_qa_session SET is_deleted = true WHERE id = ?")
public class DiseaseKbQaSessionEntity extends BaseEntity {

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    /** 首问截断生成 */
    @Column(name = "title", length = 256)
    private String title;
}
