package com.maidc.task.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_personal_task", schema = "system")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE system.t_personal_task SET is_deleted = true WHERE id = ?")
public class PersonalTaskEntity extends BaseEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "task_type", nullable = false, length = 20)
    private String taskType;

    @Column(name = "priority", nullable = false, length = 10)
    private String priority = "MEDIUM";

    @Column(name = "status", nullable = false, length = 10)
    private String status = "PENDING";

    @Column(name = "assignee_id", nullable = false)
    private Long assigneeId;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_type", length = 20)
    private String sourceType;

    @Column(name = "due_date")
    private LocalDateTime dueDate;
}
