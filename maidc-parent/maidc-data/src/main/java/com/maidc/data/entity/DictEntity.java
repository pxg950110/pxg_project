package com.maidc.data.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "s_dict", schema = "system")
public class DictEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dict_type", nullable = false, length = 64)
    private String dictType;

    @Column(name = "dict_code", nullable = false, length = 64)
    private String dictCode;

    @Column(name = "dict_label", length = 128)
    private String dictLabel;

    @Column(name = "dict_value", length = 256)
    private String dictValue;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "parent_code", length = 64)
    private String parentCode;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(name = "remark", length = 256)
    private String remark;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "org_id")
    private Long orgId = 0L;
}
