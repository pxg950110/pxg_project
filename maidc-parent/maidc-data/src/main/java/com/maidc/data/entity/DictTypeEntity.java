package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据字典类型（字典分组）。
 * 字典项仍存储于 system.s_dict（按 dict_type = type_code 关联）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "s_dict_type", schema = "system")
public class DictTypeEntity extends BaseEntity {

    /** 类型编码（业务唯一键，字典项通过 s_dict.dict_type 引用） */
    @Column(name = "type_code", nullable = false, length = 64)
    private String typeCode;

    /** 类型名称 */
    @Column(name = "type_name", nullable = false, length = 128)
    private String typeName;

    @Column(name = "remark", length = 256)
    private String remark;
}
