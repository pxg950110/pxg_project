package com.maidc.label.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.maidc.common.jpa.base.BaseEntity;
import com.maidc.common.jpa.converter.JsonNodeConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "r_label_task", schema = "rdr")
public class LabelTaskEntity extends BaseEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "task_type", nullable = false, length = 20)
    private String taskType;

    @Column(name = "dataset_id", length = 36)
    private String datasetId;

    @Column(name = "dataset_name", length = 200)
    private String datasetName;

    @Column(name = "assignee_id", length = 36)
    private String assigneeId;

    @Column(name = "assignee_name", length = 100)
    private String assigneeName;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "total_count")
    private Integer totalCount = 0;

    @Column(name = "labeled_count")
    private Integer labeledCount = 0;

    @Column(name = "verified_count")
    private Integer verifiedCount = 0;

    @Column(name = "labels", columnDefinition = "jsonb")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode labels;

    @Column(name = "guidelines", columnDefinition = "TEXT")
    private String guidelines;

    @Column(name = "config", columnDefinition = "jsonb")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode config;
}
