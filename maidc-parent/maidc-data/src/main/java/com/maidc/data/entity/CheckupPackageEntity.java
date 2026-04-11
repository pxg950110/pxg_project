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
@Table(name = "c_checkup_package", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_checkup_package SET is_deleted = true WHERE id = ?")
public class CheckupPackageEntity extends BaseEntity {

    @Column(name = "checkup_id", nullable = false)
    private Long checkupId;

    @Column(name = "package_name", length = 128)
    private String packageName;

    @Column(name = "category", length = 32)
    private String category;
}
