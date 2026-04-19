package com.maidc.data.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.maidc.common.jpa.base.BaseEntity;
import com.maidc.common.jpa.converter.JsonNodeConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "r_data_source_type", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.r_data_source_type SET is_deleted = true WHERE id = ?")
public class DataSourceTypeEntity extends BaseEntity {

    @Column(name = "type_code", nullable = false, unique = true, length = 64)
    private String typeCode;

    @Column(name = "type_name", nullable = false, length = 128)
    private String typeName;

    @Column(name = "category", nullable = false, length = 32)
    private String category;

    @Column(name = "icon", length = 64)
    private String icon;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "param_schema", nullable = false, columnDefinition = "jsonb")
    private JsonNode paramSchema;

    @Column(name = "test_command", length = 32)
    private String testCommand;

    @Column(name = "is_builtin", nullable = false)
    private Boolean isBuiltin = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}