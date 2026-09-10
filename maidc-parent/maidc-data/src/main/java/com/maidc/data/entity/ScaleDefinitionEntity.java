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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "c_scale_definition", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_scale_definition SET is_deleted = true WHERE id = ?")
public class ScaleDefinitionEntity extends BaseEntity {

    @Column(name = "scale_code", nullable = false, length = 32)
    private String scaleCode;
    @Column(name = "name", nullable = false, length = 128)
    private String name;
    @Column(name = "version", nullable = false)
    private Integer version;
    @Column(name = "status", nullable = false, length = 16)
    private String status;
    @Column(name = "definition", nullable = false, columnDefinition = "jsonb")
    private String definition;
}
