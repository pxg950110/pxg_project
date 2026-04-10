package com.maidc.task.entity;

import com.maidc.common.jpa.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Where;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "t_scheduled_task", schema = "system")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE system.t_scheduled_task SET is_deleted = true WHERE id = ?")
public class TaskEntity extends BaseEntity {

    @Column(name = "task_name", nullable = false, length = 200)
    private String taskName;

    @Column(name = "task_type", nullable = false, length = 50)
    private String taskType;

    @Column(name = "cron_expression", length = 50)
    private String cronExpression;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "task_config", columnDefinition = "jsonb")
    private String taskConfig;

    @Column(name = "last_execution_time")
    private java.time.LocalDateTime lastExecutionTime;

    @Column(name = "next_execution_time")
    private java.time.LocalDateTime nextExecutionTime;

    @Column(name = "failure_count")
    private Integer failureCount;
}
