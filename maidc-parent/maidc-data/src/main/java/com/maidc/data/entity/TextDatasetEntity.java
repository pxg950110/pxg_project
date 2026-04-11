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
@Table(name = "r_text_dataset", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.r_text_dataset SET is_deleted = true WHERE id = ?")
public class TextDatasetEntity extends BaseEntity {

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "language", nullable = false, length = 8)
    private String language = "zh";
}
