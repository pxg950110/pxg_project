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
@Table(name = "c_org", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_org SET is_deleted = true WHERE id = ?")
public class OrgEntity extends BaseEntity {

    @Column(name = "org_code", nullable = false, length = 32)
    private String orgCode;

    @Column(name = "org_name", length = 128)
    private String orgName;

    @Column(name = "org_type", length = 16)
    private String orgType;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";
}
