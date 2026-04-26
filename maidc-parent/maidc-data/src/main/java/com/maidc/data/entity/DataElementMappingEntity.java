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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "m_data_element_mapping", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_data_element_mapping SET is_deleted = true WHERE id = ?")
public class DataElementMappingEntity extends BaseEntity {

    @Column(name = "data_element_id", nullable = false)
    private Long dataElementId;

    @Column(name = "schema_name", nullable = false, length = 64)
    private String schemaName;

    @Column(name = "table_name", nullable = false, length = 128)
    private String tableName;

    @Column(name = "column_name", nullable = false, length = 128)
    private String columnName;

    @Column(name = "mapping_type", nullable = false, length = 16)
    private String mappingType = "MANUAL";

    @Column(name = "confidence", precision = 3, scale = 2)
    private BigDecimal confidence;

    @Column(name = "mapping_status", nullable = false, length = 16)
    private String mappingStatus = "PENDING";

    @Column(name = "transform_rule", columnDefinition = "TEXT")
    private String transformRule;

    @Column(name = "mapped_by", length = 64)
    private String mappedBy;

    @Column(name = "mapped_at")
    private LocalDateTime mappedAt;
}
