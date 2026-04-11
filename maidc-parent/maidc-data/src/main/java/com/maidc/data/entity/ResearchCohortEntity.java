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
@Table(name = "r_research_cohort", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.r_research_cohort SET is_deleted = true WHERE id = ?")
public class ResearchCohortEntity extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "cohort_name", nullable = false, length = 128)
    private String cohortName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "inclusion_criteria", columnDefinition = "jsonb")
    private String inclusionCriteria;

    @Column(name = "exclusion_criteria", columnDefinition = "jsonb")
    private String exclusionCriteria;

    @Column(name = "target_size")
    private Integer targetSize;

    @Column(name = "current_size", nullable = false)
    private Integer currentSize = 0;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "DRAFT";
}
