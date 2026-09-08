package com.maidc.auth.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@Table(name = "s_role", schema = "system")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE system.s_role SET is_deleted = true WHERE id = ?")
public class RoleEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_code", nullable = false, length = 32)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 64)
    private String roleName;

    @Column(name = "description")
    private String description;

    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    /** 数据范围 ALL/DEPT/SELF/PROJECT，枚举存字符串（system.s_role.data_scope，DB 默认 'SELF'） */
    @Column(name = "data_scope", nullable = false, length = 16)
    private String dataScope = "SELF";
}
