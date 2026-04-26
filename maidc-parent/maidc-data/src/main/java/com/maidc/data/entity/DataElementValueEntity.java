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
@Table(name = "m_data_element_value", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_data_element_value SET is_deleted = true WHERE id = ?")
public class DataElementValueEntity extends BaseEntity {

    @Column(name = "data_element_id", nullable = false)
    private Long dataElementId;

    @Column(name = "value_code", nullable = false, length = 64)
    private String valueCode;

    @Column(name = "value_meaning", nullable = false, length = 256)
    private String valueMeaning;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
