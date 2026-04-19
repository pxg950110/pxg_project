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
@Table(name = "r_etl_field_mapping", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.r_etl_field_mapping SET is_deleted = true WHERE id = ?")
public class EtlFieldMappingEntity extends BaseEntity {

    @Column(name = "step_id", nullable = false)
    private Long stepId;

    @Column(name = "source_column", length = 64)
    private String sourceColumn;

    @Column(name = "source_table_alias", length = 32)
    private String sourceTableAlias;

    @Column(name = "target_column", nullable = false, length = 64)
    private String targetColumn;

    @Column(name = "transform_type", nullable = false, length = 16)
    private String transformType = "DIRECT";

    @Column(name = "transform_expr", columnDefinition = "TEXT")
    private String transformExpr;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
