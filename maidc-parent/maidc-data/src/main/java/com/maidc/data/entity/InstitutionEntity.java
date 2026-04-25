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
@Table(name = "m_institution", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_institution SET is_deleted = true WHERE id = ?")
public class InstitutionEntity extends BaseEntity {

    @Column(name = "inst_code", nullable = false, length = 32)
    private String instCode;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "short_name", length = 64)
    private String shortName;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";
}
