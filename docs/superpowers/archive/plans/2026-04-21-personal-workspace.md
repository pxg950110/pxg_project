# Personal Workspace (个人工作台) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform `/dashboard/overview` into a unified personal workspace entry point with aggregated API, todo management, notifications, and quick actions.

**Architecture:** Aggregated backend API (`WorkspaceController` in maidc-task) queries personal_task table + cross-schema model/notification data. Frontend splits into 6 focused Vue components under `dashboard/workspace/`. Route `/dashboard` redirects to new workspace page.

**Tech Stack:** Vue 3 + TypeScript + Ant Design Vue 4.x (frontend) / Spring Boot + JPA + RabbitMQ (backend) / PostgreSQL multi-schema

---

## File Map

### Backend (maidc-parent/maidc-task)

| Action | Path | Responsibility |
|--------|------|---------------|
| Create | `docker/init-db/16-personal-task.sql` | Database migration for personal_task table |
| Create | `maidc-task/src/main/java/com/maidc/task/entity/PersonalTaskEntity.java` | JPA entity for personal_task |
| Create | `maidc-task/src/main/java/com/maidc/task/repository/PersonalTaskRepository.java` | Repository for personal_task CRUD |
| Create | `maidc-task/src/main/java/com/maidc/task/dto/PersonalTaskCreateDTO.java` | DTO for creating personal tasks |
| Create | `maidc-task/src/main/java/com/maidc/task/vo/PersonalTaskVO.java` | VO for personal task responses |
| Create | `maidc-task/src/main/java/com/maidc/task/vo/WorkspaceDashboardVO.java` | Aggregated workspace response VO |
| Create | `maidc-task/src/main/java/com/maidc/task/mapper/PersonalTaskMapper.java` | MapStruct mapper |
| Create | `maidc-task/src/main/java/com/maidc/task/service/PersonalTaskService.java` | Personal task business logic |
| Create | `maidc-task/src/main/java/com/maidc/task/service/WorkspaceService.java` | Workspace aggregation logic |
| Create | `maidc-task/src/main/java/com/maidc/task/controller/WorkspaceController.java` | REST endpoint |
| Create | `maidc-task/src/main/java/com/maidc/task/consumer/PersonalTaskConsumer.java` | MQ consumer for task creation |
| Create | `maidc-task/src/test/java/com/maidc/task/service/PersonalTaskServiceTest.java` | Unit tests |
| Create | `maidc-task/src/test/java/com/maidc/task/service/WorkspaceServiceTest.java` | Unit tests |

### Frontend (maidc-portal)

| Action | Path | Responsibility |
|--------|------|---------------|
| Create | `src/api/workspace.ts` | Workspace API definitions |
| Create | `src/stores/workspace.ts` | Pinia store for workspace state |
| Create | `src/views/dashboard/workspace/WorkspaceView.vue` | Main page container |
| Create | `src/views/dashboard/workspace/WelcomeSection.vue` | User greeting |
| Create | `src/views/dashboard/workspace/MetricCards.vue` | 4 metric cards |
| Create | `src/views/dashboard/workspace/TodoSection.vue` | Todo list with filters |
| Create | `src/views/dashboard/workspace/NotifySection.vue` | Notification list |
| Create | `src/views/dashboard/workspace/QuickActions.vue` | Quick action buttons |
| Modify | `src/router/asyncRoutes.ts` | Add workspace route, change redirect |

---

## Task 1: Database Migration — personal_task Table

**Files:**
- Create: `docker/init-db/16-personal-task.sql`

- [ ] **Step 1: Write the SQL migration**

```sql
-- 16-personal-task.sql — 个人待办任务表
CREATE TABLE system.t_personal_task (
    id              BIGSERIAL      PRIMARY KEY,
    title           VARCHAR(200)   NOT NULL,
    description     TEXT,
    task_type       VARCHAR(20)    NOT NULL,
    priority        VARCHAR(10)    NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(10)    NOT NULL DEFAULT 'PENDING',
    assignee_id     BIGINT         NOT NULL,
    source_id       BIGINT,
    source_type     VARCHAR(20),
    due_date        TIMESTAMP,
    created_by      VARCHAR(64)    NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN        NOT NULL DEFAULT FALSE,
    org_id          BIGINT         NOT NULL DEFAULT 1,
    CONSTRAINT chk_pt_type     CHECK (task_type IN ('APPROVAL','LABELING','DATA_QUERY','OTHER')),
    CONSTRAINT chk_pt_priority CHECK (priority IN ('HIGH','MEDIUM','LOW')),
    CONSTRAINT chk_pt_status   CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','CANCELLED'))
);

CREATE INDEX idx_pt_assignee_status ON system.t_personal_task(assignee_id, status);
CREATE INDEX idx_pt_source ON system.t_personal_task(source_type, source_id);

COMMENT ON TABLE system.t_personal_task IS '个人待办任务表';
```

- [ ] **Step 2: Execute the migration**

Run: `psql -h localhost -U postgres -d maidc -f docker/init-db/16-personal-task.sql`
Expected: `CREATE TABLE`, `CREATE INDEX` success messages

- [ ] **Step 3: Commit**

```bash
git add docker/init-db/16-personal-task.sql
git commit -m "feat(task): add personal_task table migration"
```

---

## Task 2: Backend Entity — PersonalTaskEntity

**Files:**
- Create: `maidc-parent/maidc-task/src/main/java/com/maidc/task/entity/PersonalTaskEntity.java`

- [ ] **Step 1: Write the entity class**

```java
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
```

- [ ] **Step 2: Verify compilation**

Run: `cd maidc-parent && mvn compile -pl maidc-task -am -q`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add maidc-parent/maidc-task/src/main/java/com/maidc/task/entity/PersonalTaskEntity.java
git commit -m "feat(task): add PersonalTaskEntity"
```

---

## Task 3: Backend DTO, VO, and Mapper

**Files:**
- Create: `maidc-parent/maidc-task/src/main/java/com/maidc/task/dto/PersonalTaskCreateDTO.java`
- Create: `maidc-parent/maidc-task/src/main/java/com/maidc/task/vo/PersonalTaskVO.java`
- Create: `maidc-parent/maidc-task/src/main/java/com/maidc/task/vo/WorkspaceDashboardVO.java`
- Create: `maidc-parent/maidc-task/src/main/java/com/maidc/task/mapper/PersonalTaskMapper.java`

- [ ] **Step 1: Write PersonalTaskCreateDTO**

```java
package com.maidc.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PersonalTaskCreateDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200)
    private String title;

    private String description;

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    private String priority;

    @NotNull(message = "指派人不能为空")
    private Long assigneeId;

    private Long sourceId;
    private String sourceType;
    private String dueDate;
}
```

- [ ] **Step 2: Write PersonalTaskVO**

```java
package com.maidc.task.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PersonalTaskVO {
    private Long id;
    private String title;
    private String description;
    private String taskType;
    private String priority;
    private String status;
    private Long assigneeId;
    private Long sourceId;
    private String sourceType;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: Write WorkspaceDashboardVO (aggregated response)**

```java
package com.maidc.task.vo;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class WorkspaceDashboardVO {

    private WelcomeInfo welcome;
    private MetricsInfo metrics;
    private List<PersonalTaskVO> todos;
    private List<NotificationItem> notifications;
    private List<QuickAction> quickActions;

    @Data
    @Builder
    public static class WelcomeInfo {
        private String userName;
        private String date;
        private String role;
    }

    @Data
    @Builder
    public static class MetricsInfo {
        private long modelCount;
        private long activeDeployments;
        private long dailyInferences;
        private long pendingApprovals;
    }

    @Data
    @Builder
    public static class NotificationItem {
        private Long id;
        private String type;
        private String title;
        private String content;
        private boolean isRead;
        private String createdAt;
    }

    @Data
    @Builder
    public static class QuickAction {
        private String key;
        private String label;
        private String icon;
        private String route;
    }
}
```

- [ ] **Step 4: Write PersonalTaskMapper**

```java
package com.maidc.task.mapper;

import com.maidc.task.dto.PersonalTaskCreateDTO;
import com.maidc.task.entity.PersonalTaskEntity;
import com.maidc.task.vo.PersonalTaskVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface PersonalTaskMapper {

    PersonalTaskVO toVO(PersonalTaskEntity entity);

    @Mapping(target = "priority", defaultValue = "MEDIUM")
    @Mapping(target = "dueDate", dateFormat = "yyyy-MM-dd HH:mm:ss")
    PersonalTaskEntity toEntity(PersonalTaskCreateDTO dto);
}
```

- [ ] **Step 5: Verify compilation**

Run: `cd maidc-parent && mvn compile -pl maidc-task -am -q`
Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add maidc-parent/maidc-task/src/main/java/com/maidc/task/dto/PersonalTaskCreateDTO.java \
       maidc-parent/maidc-task/src/main/java/com/maidc/task/vo/PersonalTaskVO.java \
       maidc-parent/maidc-task/src/main/java/com/maidc/task/vo/WorkspaceDashboardVO.java \
       maidc-parent/maidc-task/src/main/java/com/maidc/task/mapper/PersonalTaskMapper.java
git commit -m "feat(task): add workspace DTOs, VOs, and mapper"
```

---

## Task 4: Backend Repository

**Files:**
- Create: `maidc-parent/maidc-task/src/main/java/com/maidc/task/repository/PersonalTaskRepository.java`

- [ ] **Step 1: Write the repository**

```java
package com.maidc.task.repository;

import com.maidc.task.entity.PersonalTaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonalTaskRepository extends JpaRepository<PersonalTaskEntity, Long> {

    List<PersonalTaskEntity> findByAssigneeIdAndStatusInOrderByPriorityDescCreatedAtDesc(
            Long assigneeId, List<String> statuses);

    Page<PersonalTaskEntity> findByAssigneeIdAndStatusInOrderByPriorityDescCreatedAtDesc(
            Long assigneeId, List<String> statuses, Pageable pageable);

    long countByAssigneeIdAndStatus(Long assigneeId, String status);
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd maidc-parent && mvn compile -pl maidc-task -am -q`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add maidc-parent/maidc-task/src/main/java/com/maidc/task/repository/PersonalTaskRepository.java
git commit -m "feat(task): add PersonalTaskRepository"
```

---

## Task 5: Backend Service — PersonalTaskService

**Files:**
- Create: `maidc-parent/maidc-task/src/main/java/com/maidc/task/service/PersonalTaskService.java`
- Create: `maidc-parent/maidc-task/src/test/java/com/maidc/task/service/PersonalTaskServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.maidc.task.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.task.dto.PersonalTaskCreateDTO;
import com.maidc.task.entity.PersonalTaskEntity;
import com.maidc.task.mapper.PersonalTaskMapper;
import com.maidc.task.repository.PersonalTaskRepository;
import com.maidc.task.vo.PersonalTaskVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalTaskServiceTest {

    @Mock
    private PersonalTaskRepository personalTaskRepository;

    @Mock
    private PersonalTaskMapper personalTaskMapper;

    @InjectMocks
    private PersonalTaskService personalTaskService;

    @Test
    void createTask_validInput_returnsVO() {
        PersonalTaskCreateDTO dto = PersonalTaskCreateDTO.builder()
                .title("审批模型 v2.1")
                .taskType("APPROVAL")
                .assigneeId(1L)
                .priority("HIGH")
                .build();

        PersonalTaskEntity entity = new PersonalTaskEntity();
        entity.setTitle(dto.getTitle());
        entity.setTaskType(dto.getTaskType());

        PersonalTaskVO vo = new PersonalTaskVO();
        vo.setId(1L);
        vo.setTitle(dto.getTitle());

        when(personalTaskMapper.toEntity(dto)).thenReturn(entity);
        when(personalTaskRepository.save(any(PersonalTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(personalTaskMapper.toVO(any(PersonalTaskEntity.class))).thenReturn(vo);

        PersonalTaskVO result = personalTaskService.createTask(dto);

        assertNotNull(result);
        assertEquals("审批模型 v2.1", result.getTitle());
        verify(personalTaskRepository).save(any(PersonalTaskEntity.class));
    }

    @Test
    void completeTask_pendingTask_changesToCompleted() {
        PersonalTaskEntity entity = new PersonalTaskEntity();
        entity.setId(1L);
        entity.setStatus("PENDING");
        entity.setTitle("Test task");

        PersonalTaskVO vo = new PersonalTaskVO();
        vo.setId(1L);
        vo.setStatus("COMPLETED");

        when(personalTaskRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(personalTaskRepository.save(any(PersonalTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(personalTaskMapper.toVO(any(PersonalTaskEntity.class))).thenReturn(vo);

        PersonalTaskVO result = personalTaskService.completeTask(1L);

        assertEquals("COMPLETED", result.getStatus());
        verify(personalTaskRepository).save(argThat(e -> "COMPLETED".equals(e.getStatus())));
    }

    @Test
    void completeTask_nonExisting_throws() {
        when(personalTaskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> personalTaskService.completeTask(999L));
    }

    @Test
    void getPendingTasks_returnsList() {
        Long assigneeId = 1L;
        List<String> pendingStatuses = List.of("PENDING", "IN_PROGRESS");

        PersonalTaskEntity entity = new PersonalTaskEntity();
        entity.setId(1L);
        entity.setTitle("Task 1");
        entity.setStatus("PENDING");

        when(personalTaskRepository.findByAssigneeIdAndStatusInOrderByPriorityDescCreatedAtDesc(
                assigneeId, pendingStatuses)).thenReturn(List.of(entity));

        PersonalTaskVO vo = new PersonalTaskVO();
        vo.setId(1L);
        vo.setTitle("Task 1");
        when(personalTaskMapper.toVO(entity)).thenReturn(vo);

        List<PersonalTaskVO> result = personalTaskService.getPendingTasks(assigneeId);

        assertEquals(1, result.size());
        assertEquals("Task 1", result.get(0).getTitle());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd maidc-parent && mvn test -pl maidc-task -Dtest=PersonalTaskServiceTest -q`
Expected: Compilation errors — `PersonalTaskService` class not found

- [ ] **Step 3: Write PersonalTaskService implementation**

```java
package com.maidc.task.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.task.dto.PersonalTaskCreateDTO;
import com.maidc.task.entity.PersonalTaskEntity;
import com.maidc.task.mapper.PersonalTaskMapper;
import com.maidc.task.repository.PersonalTaskRepository;
import com.maidc.task.vo.PersonalTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalTaskService {

    private final PersonalTaskRepository personalTaskRepository;
    private final PersonalTaskMapper personalTaskMapper;

    @Transactional
    public PersonalTaskVO createTask(PersonalTaskCreateDTO dto) {
        PersonalTaskEntity entity = personalTaskMapper.toEntity(dto);
        entity.setIsDeleted(false);
        PersonalTaskEntity saved = personalTaskRepository.save(entity);
        log.info("Personal task created: id={}, title={}, assignee={}", saved.getId(), saved.getTitle(), saved.getAssigneeId());
        return personalTaskMapper.toVO(saved);
    }

    @Transactional
    public PersonalTaskVO completeTask(Long id) {
        PersonalTaskEntity entity = getTaskOrThrow(id);
        entity.setStatus("COMPLETED");
        personalTaskRepository.save(entity);
        return personalTaskMapper.toVO(entity);
    }

    public List<PersonalTaskVO> getPendingTasks(Long assigneeId) {
        List<String> pendingStatuses = List.of("PENDING", "IN_PROGRESS");
        List<PersonalTaskEntity> entities = personalTaskRepository
                .findByAssigneeIdAndStatusInOrderByPriorityDescCreatedAtDesc(assigneeId, pendingStatuses);
        return entities.stream().map(personalTaskMapper::toVO).toList();
    }

    private PersonalTaskEntity getTaskOrThrow(Long id) {
        return personalTaskRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd maidc-parent && mvn test -pl maidc-task -Dtest=PersonalTaskServiceTest -q`
Expected: All 4 tests PASS

- [ ] **Step 5: Commit**

```bash
git add maidc-parent/maidc-task/src/main/java/com/maidc/task/service/PersonalTaskService.java \
       maidc-parent/maidc-task/src/test/java/com/maidc/task/service/PersonalTaskServiceTest.java
git commit -m "feat(task): add PersonalTaskService with tests"
```

---

## Task 6: Backend Service — WorkspaceService (Aggregation)

**Files:**
- Create: `maidc-parent/maidc-task/src/main/java/com/maidc/task/service/WorkspaceService.java`
- Create: `maidc-parent/maidc-task/src/test/java/com/maidc/task/service/WorkspaceServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.maidc.task.service;

import com.maidc.task.vo.WorkspaceDashboardVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private PersonalTaskService personalTaskService;

    @Mock
    private WorkspaceMetricsRepository workspaceMetricsRepository;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    void getDashboard_returnsAllSections() {
        when(workspaceMetricsRepository.countModelByOrgId(anyLong())).thenReturn(28L);
        when(workspaceMetricsRepository.countActiveDeploymentsByOrgId(anyLong())).thenReturn(8L);
        when(workspaceMetricsRepository.countTodayInferencesByOrgId(anyLong())).thenReturn(12456L);
        when(workspaceMetricsRepository.countPendingApprovalsByOrgId(anyLong())).thenReturn(5L);
        when(personalTaskService.getPendingTasks(anyLong())).thenReturn(java.util.List.of());

        WorkspaceDashboardVO result = workspaceService.getDashboard(1L, 1L);

        assertNotNull(result);
        assertNotNull(result.getWelcome());
        assertNotNull(result.getMetrics());
        assertNotNull(result.getTodos());
        assertNotNull(result.getQuickActions());
        assertEquals(28L, result.getMetrics().getModelCount());
        assertEquals(8L, result.getMetrics().getActiveDeployments());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd maidc-parent && mvn test -pl maidc-task -Dtest=WorkspaceServiceTest -q`
Expected: Compilation errors

- [ ] **Step 3: Write WorkspaceMetricsRepository (cross-schema native queries)**

```java
package com.maidc.task.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WorkspaceMetricsRepository {

    @PersistenceContext
    private EntityManager em;

    public long countModelByOrgId(long orgId) {
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM model.m_model WHERE org_id = :orgId AND is_deleted = false")
                .setParameter("orgId", orgId).getSingleResult()).longValue();
    }

    public long countActiveDeploymentsByOrgId(long orgId) {
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM model.m_deployment WHERE org_id = :orgId AND status = 'RUNNING' AND is_deleted = false")
                .setParameter("orgId", orgId).getSingleResult()).longValue();
    }

    public long countTodayInferencesByOrgId(long orgId) {
        return ((Number) em.createNativeQuery(
                "SELECT COALESCE(SUM(invocation_count), 0) FROM model.m_model_daily_stats WHERE org_id = :orgId AND stat_date = CURRENT_DATE")
                .setParameter("orgId", orgId).getSingleResult()).longValue();
    }

    public long countPendingApprovalsByOrgId(long orgId) {
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM model.m_approval WHERE org_id = :orgId AND status = 'PENDING' AND is_deleted = false")
                .setParameter("orgId", orgId).getSingleResult()).longValue();
    }
}
```

- [ ] **Step 4: Write WorkspaceService implementation**

```java
package com.maidc.task.service;

import com.maidc.task.vo.WorkspaceDashboardVO;
import com.maidc.task.repository.WorkspaceMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final PersonalTaskService personalTaskService;
    private final WorkspaceMetricsRepository metricsRepository;

    public WorkspaceDashboardVO getDashboard(Long userId, Long orgId) {
        return WorkspaceDashboardVO.builder()
                .welcome(buildWelcome(userId))
                .metrics(buildMetrics(orgId))
                .todos(personalTaskService.getPendingTasks(userId))
                .notifications(List.of())
                .quickActions(buildQuickActions())
                .build();
    }

    private WorkspaceDashboardVO.WelcomeInfo buildWelcome(Long userId) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE", Locale.CHINA));
        return WorkspaceDashboardVO.WelcomeInfo.builder()
                .userName("") // filled by frontend from auth store
                .date(today)
                .role("")
                .build();
    }

    private WorkspaceDashboardVO.MetricsInfo buildMetrics(Long orgId) {
        return WorkspaceDashboardVO.MetricsInfo.builder()
                .modelCount(metricsRepository.countModelByOrgId(orgId))
                .activeDeployments(metricsRepository.countActiveDeploymentsByOrgId(orgId))
                .dailyInferences(metricsRepository.countTodayInferencesByOrgId(orgId))
                .pendingApprovals(metricsRepository.countPendingApprovalsByOrgId(orgId))
                .build();
    }

    private List<WorkspaceDashboardVO.QuickAction> buildQuickActions() {
        return List.of(
                WorkspaceDashboardVO.QuickAction.builder().key("new_model").label("新建模型").icon("plus-outlined").route("/model/list").build(),
                WorkspaceDashboardVO.QuickAction.builder().key("patient_query").label("患者查询").icon("search-outlined").route("/data/cdr/patients").build(),
                WorkspaceDashboardVO.QuickAction.builder().key("new_evaluation").label("新建评估").icon("experiment-outlined").route("/model/evaluations").build(),
                WorkspaceDashboardVO.QuickAction.builder().key("etl_task").label("ETL任务").icon("thunderbolt-outlined").route("/data/etl/pipelines").build()
        );
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd maidc-parent && mvn test -pl maidc-task -Dtest=WorkspaceServiceTest -q`
Expected: Test PASS

- [ ] **Step 6: Commit**

```bash
git add maidc-parent/maidc-task/src/main/java/com/maidc/task/service/WorkspaceService.java \
       maidc-parent/maidc-task/src/main/java/com/maidc/task/repository/WorkspaceMetricsRepository.java \
       maidc-parent/maidc-task/src/test/java/com/maidc/task/service/WorkspaceServiceTest.java
git commit -m "feat(task): add WorkspaceService with aggregation and metrics queries"
```

---

## Task 7: Backend Controller — WorkspaceController

**Files:**
- Create: `maidc-parent/maidc-task/src/main/java/com/maidc/task/controller/WorkspaceController.java`

- [ ] **Step 1: Write the controller**

```java
package com.maidc.task.controller;

import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.task.service.PersonalTaskService;
import com.maidc.task.service.WorkspaceService;
import com.maidc.task.vo.PersonalTaskVO;
import com.maidc.task.vo.WorkspaceDashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspace")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final PersonalTaskService personalTaskService;

    @PreAuthorize("hasPermission('workspace:read')")
    @GetMapping("/dashboard")
    public R<WorkspaceDashboardVO> getDashboard(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Org-Id") Long orgId) {
        return R.ok(workspaceService.getDashboard(userId, orgId));
    }

    @OperLog(module = "workspace", operation = "completeTask")
    @PreAuthorize("hasPermission('workspace:write')")
    @PutMapping("/todos/{id}/complete")
    public R<PersonalTaskVO> completeTask(@PathVariable Long id) {
        return R.ok(personalTaskService.completeTask(id));
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd maidc-parent && mvn compile -pl maidc-task -am -q`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add maidc-parent/maidc-task/src/main/java/com/maidc/task/controller/WorkspaceController.java
git commit -m "feat(task): add WorkspaceController with dashboard endpoint"
```

---

## Task 8: Backend MQ Consumer — PersonalTaskConsumer

**Files:**
- Create: `maidc-parent/maidc-task/src/main/java/com/maidc/task/consumer/PersonalTaskConsumer.java`

- [ ] **Step 1: Write the MQ consumer**

Follows existing `AlertNotifyConsumer` pattern — listens on `approval.notify` and `label.notify` queues, creates personal_task entries.

```java
package com.maidc.task.consumer;

import com.maidc.common.mq.model.MaidcMessage;
import com.maidc.task.dto.PersonalTaskCreateDTO;
import com.maidc.task.service.PersonalTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersonalTaskConsumer {

    private final PersonalTaskService personalTaskService;

    @RabbitListener(queues = "approval.notify")
    public void onApprovalNotify(MaidcMessage message) {
        log.info("Received approval notify: eventType={}", message.getEventType());
        handleNotify(message, "APPROVAL");
    }

    @RabbitListener(queues = "label.notify")
    public void onLabelNotify(MaidcMessage message) {
        log.info("Received label notify: eventType={}", message.getEventType());
        handleNotify(message, "LABELING");
    }

    private void handleNotify(MaidcMessage message, String taskType) {
        Map<String, Object> payload = message.getPayload();
        Long assigneeId = extractLong(payload, "assigneeId");
        String title = extractString(payload, "title");
        Long sourceId = extractLong(payload, "bizId");

        if (assigneeId == null || title == null) {
            log.warn("Missing required fields in message: {}", message);
            return;
        }

        PersonalTaskCreateDTO dto = PersonalTaskCreateDTO.builder()
                .title(title)
                .taskType(taskType)
                .assigneeId(assigneeId)
                .sourceId(sourceId)
                .sourceType(taskType)
                .priority(extractString(payload, "priority"))
                .build();

        personalTaskService.createTask(dto);
        log.info("Created personal task: type={}, assignee={}", taskType, assigneeId);
    }

    private Long extractLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return null; }
    }

    private String extractString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value != null ? value.toString() : null;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd maidc-parent && mvn compile -pl maidc-task -am -q`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add maidc-parent/maidc-task/src/main/java/com/maidc/task/consumer/PersonalTaskConsumer.java
git commit -m "feat(task): add PersonalTaskConsumer for MQ-driven task creation"
```

---

## Task 9: Backend Full Test Suite

**Files:**
- Modify: `maidc-parent/maidc-task/src/test/java/com/maidc/task/service/WorkspaceServiceTest.java` (update mock type)

- [ ] **Step 1: Run all maidc-task tests**

Run: `cd maidc-parent && mvn test -pl maidc-task -q`
Expected: `BUILD SUCCESS`, all tests pass (existing + new)

- [ ] **Step 2: Commit (if any test fixes needed)**

```bash
git add -u
git commit -m "test(task): fix and verify all workspace tests"
```

---

## Task 10: Frontend API — workspace.ts

**Files:**
- Create: `maidc-portal/src/api/workspace.ts`

- [ ] **Step 1: Write the API file**

Follows existing pattern in `api/model.ts` and `api/task.ts`.

```typescript
import request from '@/utils/request'
import type { ApiResponse } from '@/api/types'

export interface WelcomeInfo {
  userName: string
  date: string
  role: string
}

export interface MetricsInfo {
  modelCount: number
  activeDeployments: number
  dailyInferences: number
  pendingApprovals: number
}

export interface PersonalTaskVO {
  id: number
  title: string
  description: string
  taskType: string
  priority: string
  status: string
  assigneeId: number
  sourceId: number
  sourceType: string
  dueDate: string
  createdAt: string
}

export interface NotificationItem {
  id: number
  type: string
  title: string
  content: string
  isRead: boolean
  createdAt: string
}

export interface QuickAction {
  key: string
  label: string
  icon: string
  route: string
}

export interface WorkspaceDashboardVO {
  welcome: WelcomeInfo
  metrics: MetricsInfo
  todos: PersonalTaskVO[]
  notifications: NotificationItem[]
  quickActions: QuickAction[]
}

export function getWorkspaceDashboard() {
  return request.get<ApiResponse<WorkspaceDashboardVO>>('/workspace/dashboard')
}

export function completeTodo(id: number) {
  return request.put<ApiResponse<PersonalTaskVO>>(`/workspace/todos/${id}/complete`)
}
```

- [ ] **Step 2: Verify no TypeScript errors**

Run: `cd maidc-portal && npx vue-tsc --noEmit 2>&1 | head -20`
Expected: No errors related to workspace.ts

- [ ] **Step 3: Commit**

```bash
git add maidc-portal/src/api/workspace.ts
git commit -m "feat(portal): add workspace API definitions"
```

---

## Task 11: Frontend Store — useWorkspaceStore

**Files:**
- Create: `maidc-portal/src/stores/workspace.ts`

- [ ] **Step 1: Write the Pinia store**

Follows existing pattern in `stores/auth.ts`.

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getWorkspaceDashboard, completeTodo } from '@/api/workspace'
import type { WorkspaceDashboardVO, PersonalTaskVO } from '@/api/workspace'

export const useWorkspaceStore = defineStore('workspace', () => {
  const dashboard = ref<WorkspaceDashboardVO | null>(null)
  const loading = ref(false)

  async function fetchDashboard() {
    loading.value = true
    try {
      const res = await getWorkspaceDashboard()
      dashboard.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function completeTask(id: number) {
    await completeTodo(id)
    if (dashboard.value) {
      dashboard.value.todos = dashboard.value.todos.filter(t => t.id !== id)
    }
  }

  function $reset() {
    dashboard.value = null
    loading.value = false
  }

  return { dashboard, loading, fetchDashboard, completeTask, $reset }
})
```

- [ ] **Step 2: Commit**

```bash
git add maidc-portal/src/stores/workspace.ts
git commit -m "feat(portal): add useWorkspaceStore"
```

---

## Task 12: Frontend Components — WelcomeSection + MetricCards

**Files:**
- Create: `maidc-portal/src/views/dashboard/workspace/WelcomeSection.vue`
- Create: `maidc-portal/src/views/dashboard/workspace/MetricCards.vue`

- [ ] **Step 1: Write WelcomeSection**

```vue
<template>
  <div class="welcome-section">
    <div class="welcome-info">
      <h2 class="welcome-greeting">{{ greeting }}，{{ userName }}</h2>
      <p class="welcome-date">{{ date }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  userName: string
  date: string
}>()

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})
</script>

<style scoped lang="scss">
.welcome-section {
  padding: 24px;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  border-radius: 8px;
  color: #fff;
  margin-bottom: 16px;
}

.welcome-greeting {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 4px;
}

.welcome-date {
  font-size: 14px;
  opacity: 0.85;
  margin: 0;
}
</style>
```

- [ ] **Step 2: Write MetricCards**

```vue
<template>
  <a-row :gutter="[16, 16]">
    <a-col v-for="item in cards" :key="item.title" :span="6">
      <MetricCard
        :title="item.title"
        :value="item.value"
        :suffix="item.suffix"
        :loading="loading"
      >
        <template #icon>
          <component :is="item.icon" />
        </template>
      </MetricCard>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ExperimentOutlined, RocketOutlined, ThunderboltOutlined, AuditOutlined } from '@ant-design/icons-vue'
import MetricCard from '@/components/MetricCard/index.vue'
import type { MetricsInfo } from '@/api/workspace'

const props = defineProps<{
  metrics: MetricsInfo | null
  loading: boolean
}>()

const cards = computed(() => [
  { title: '模型总数', value: props.metrics?.modelCount ?? 0, suffix: '个', icon: ExperimentOutlined },
  { title: '活跃部署', value: props.metrics?.activeDeployments ?? 0, suffix: '个', icon: RocketOutlined },
  { title: '今日推理', value: props.metrics?.dailyInferences ?? 0, suffix: '次', icon: ThunderboltOutlined },
  { title: '待审批', value: props.metrics?.pendingApprovals ?? 0, suffix: '项', icon: AuditOutlined },
])
</script>
```

- [ ] **Step 3: Commit**

```bash
git add maidc-portal/src/views/dashboard/workspace/WelcomeSection.vue \
       maidc-portal/src/views/dashboard/workspace/MetricCards.vue
git commit -m "feat(portal): add WelcomeSection and MetricCards components"
```

---

## Task 13: Frontend Components — TodoSection

**Files:**
- Create: `maidc-portal/src/views/dashboard/workspace/TodoSection.vue`

- [ ] **Step 1: Write TodoSection**

```vue
<template>
  <a-card title="待办任务" :bordered="false" :loading="loading">
    <template #extra>
      <a-radio-group v-model:value="activeFilter" size="small" button-style="solid">
        <a-radio-button value="ALL">全部</a-radio-button>
        <a-radio-button value="APPROVAL">审批</a-radio-button>
        <a-radio-button value="LABELING">标注</a-radio-button>
        <a-radio-button value="OTHER">其他</a-radio-button>
      </a-radio-group>
    </template>

    <div v-if="filteredTodos.length === 0" class="empty-state">
      <a-empty description="暂无待办任务" />
    </div>

    <a-list v-else :data-source="filteredTodos" size="small">
      <template #renderItem="{ item }">
        <a-list-item>
          <a-list-item-meta>
            <template #title>
              <a class="todo-title" @click="handleNavigate(item)">{{ item.title }}</a>
            </template>
            <template #description>
              <span>{{ formatType(item.taskType) }}</span>
              <span v-if="item.dueDate" style="margin-left: 12px; color: #ff4d4f">
                截止: {{ item.dueDate }}
              </span>
            </template>
            <template #avatar>
              <a-tag :color="priorityColor(item.priority)">{{ item.priority }}</a-tag>
            </template>
          </a-list-item-meta>
          <template #actions>
            <a-button type="link" size="small" @click="handleComplete(item)">完成</a-button>
          </template>
        </a-list-item>
      </template>
    </a-list>
  </a-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import type { PersonalTaskVO } from '@/api/workspace'

const props = defineProps<{
  todos: PersonalTaskVO[]
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'complete', id: number): void
}>()

const router = useRouter()
const activeFilter = ref('ALL')

const filteredTodos = computed(() => {
  if (activeFilter.value === 'ALL') return props.todos
  return props.todos.filter(t => t.taskType === activeFilter.value)
})

function formatType(type: string) {
  const map: Record<string, string> = { APPROVAL: '审批', LABELING: '标注', DATA_QUERY: '数据查询', OTHER: '其他' }
  return map[type] ?? type
}

function priorityColor(priority: string) {
  const map: Record<string, string> = { HIGH: 'red', MEDIUM: 'orange', LOW: 'blue' }
  return map[priority] ?? 'default'
}

function handleNavigate(item: PersonalTaskVO) {
  const routeMap: Record<string, string> = {
    APPROVAL: `/model/approvals/${item.sourceId}`,
    LABELING: `/label/workspace/${item.sourceId}`,
  }
  const route = routeMap[item.sourceType]
  if (route) router.push(route)
}

function handleComplete(item: PersonalTaskVO) {
  emit('complete', item.id)
}
</script>

<style scoped lang="scss">
.empty-state {
  padding: 32px 0;
}

.todo-title {
  cursor: pointer;
  &:hover { color: #1890ff; }
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add maidc-portal/src/views/dashboard/workspace/TodoSection.vue
git commit -m "feat(portal): add TodoSection with filter and complete actions"
```

---

## Task 14: Frontend Components — NotifySection

**Files:**
- Create: `maidc-portal/src/views/dashboard/workspace/NotifySection.vue`

- [ ] **Step 1: Write NotifySection**

```vue
<template>
  <a-card title="消息通知" :bordered="false" :loading="loading">
    <template #extra>
      <a-button type="link" size="small" @click="handleMarkAllRead">全部已读</a-button>
    </template>

    <div v-if="notifications.length === 0" class="empty-state">
      <a-empty description="暂无消息" />
    </div>

    <a-list v-else :data-source="notifications" size="small">
      <template #renderItem="{ item }">
        <a-list-item :class="{ 'unread-item': !item.isRead }">
          <a-list-item-meta>
            <template #title>
              <a class="notify-title" @click="handleClick(item)">{{ item.title }}</a>
            </template>
            <template #description>
              <span>{{ item.createdAt }}</span>
            </template>
            <template #avatar>
              <a-avatar :size="32" :style="{ backgroundColor: typeColor(item.type) }">
                <template #icon>
                  <NotificationOutlined />
                </template>
              </a-avatar>
            </template>
          </a-list-item-meta>
        </a-list-item>
      </template>
    </a-list>
  </a-card>
</template>

<script setup lang="ts">
import { NotificationOutlined } from '@ant-design/icons-vue'
import type { NotificationItem } from '@/api/workspace'

const props = defineProps<{
  notifications: NotificationItem[]
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'markAllRead'): void
  (e: 'click', item: NotificationItem): void
}>()

function typeColor(type: string) {
  const map: Record<string, string> = { SYSTEM: '#1890ff', ALERT: '#ff4d4f', APPROVAL: '#52c41a' }
  return map[type] ?? '#999'
}

function handleMarkAllRead() {
  emit('markAllRead')
}

function handleClick(item: NotificationItem) {
  emit('click', item)
}
</script>

<style scoped lang="scss">
.empty-state { padding: 32px 0; }
.unread-item { border-left: 3px solid #1890ff; padding-left: 8px; }
.notify-title { cursor: pointer; &:hover { color: #1890ff; } }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add maidc-portal/src/views/dashboard/workspace/NotifySection.vue
git commit -m "feat(portal): add NotifySection with unread indicators"
```

---

## Task 15: Frontend Components — QuickActions

**Files:**
- Create: `maidc-portal/src/views/dashboard/workspace/QuickActions.vue`

- [ ] **Step 1: Write QuickActions**

```vue
<template>
  <a-card title="快捷操作" :bordered="false">
    <a-row :gutter="[16, 16]">
      <a-col v-for="action in actions" :key="action.key" :span="6">
        <a-button type="primary" ghost block size="large" @click="handleClick(action)">
          <template #icon>
            <component :is="iconMap[action.icon]" />
          </template>
          {{ action.label }}
        </a-button>
      </a-col>
    </a-row>
  </a-card>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { PlusOutlined, SearchOutlined, ExperimentOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import type { QuickAction } from '@/api/workspace'

defineProps<{
  actions: QuickAction[]
}>()

const router = useRouter()

const iconMap: Record<string, any> = {
  'plus-outlined': PlusOutlined,
  'search-outlined': SearchOutlined,
  'experiment-outlined': ExperimentOutlined,
  'thunderbolt-outlined': ThunderboltOutlined,
}

function handleClick(action: QuickAction) {
  router.push(action.route)
}
</script>
```

- [ ] **Step 2: Commit**

```bash
git add maidc-portal/src/views/dashboard/workspace/QuickActions.vue
git commit -m "feat(portal): add QuickActions component"
```

---

## Task 16: Frontend Page — WorkspaceView

**Files:**
- Create: `maidc-portal/src/views/dashboard/workspace/WorkspaceView.vue`

- [ ] **Step 1: Write the main page container**

```vue
<template>
  <PageContainer title="个人工作台" subtitle="MAIDC 医疗 AI 数据中心">
    <a-spin :spinning="store.loading">
      <!-- Welcome Banner -->
      <WelcomeSection
        :user-name="store.dashboard?.welcome?.userName ?? userName"
        :date="store.dashboard?.welcome?.date ?? ''"
      />

      <!-- Metric Cards -->
      <MetricCards
        :metrics="store.dashboard?.metrics ?? null"
        :loading="store.loading"
      />

      <!-- Todo + Notifications Split -->
      <a-row :gutter="[16, 16]" style="margin-top: 16px">
        <a-col :span="14">
          <TodoSection
            :todos="store.dashboard?.todos ?? []"
            :loading="store.loading"
            @complete="handleComplete"
          />
        </a-col>
        <a-col :span="10">
          <NotifySection
            :notifications="store.dashboard?.notifications ?? []"
            :loading="store.loading"
            @mark-all-read="handleMarkAllRead"
            @click="handleNotifyClick"
          />
        </a-col>
      </a-row>

      <!-- Quick Actions -->
      <div style="margin-top: 16px">
        <QuickActions :actions="store.dashboard?.quickActions ?? []" />
      </div>
    </a-spin>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useWorkspaceStore } from '@/stores/workspace'
import { useAuthStore } from '@/stores/auth'
import PageContainer from '@/components/PageContainer/index.vue'
import WelcomeSection from './WelcomeSection.vue'
import MetricCards from './MetricCards.vue'
import TodoSection from './TodoSection.vue'
import NotifySection from './NotifySection.vue'
import QuickActions from './QuickActions.vue'
import type { NotificationItem } from '@/api/workspace'

const store = useWorkspaceStore()
const authStore = useAuthStore()

const userName = computed(() => authStore.userInfo?.realName ?? '')

onMounted(() => {
  store.fetchDashboard()
})

function handleComplete(id: number) {
  store.completeTask(id)
}

function handleMarkAllRead() {
  // TODO: wire to message API
}

function handleNotifyClick(_item: NotificationItem) {
  // TODO: navigate based on notification type
}
</script>
```

- [ ] **Step 2: Commit**

```bash
git add maidc-portal/src/views/dashboard/workspace/WorkspaceView.vue
git commit -m "feat(portal): add WorkspaceView main page"
```

---

## Task 17: Frontend Route Configuration

**Files:**
- Modify: `maidc-portal/src/router/asyncRoutes.ts`

- [ ] **Step 1: Update route configuration**

Find the `dashboard` children array in `asyncRoutes.ts`. Add the workspace route as the first child and change the parent redirect.

Changes to make:
1. Change `redirect: '/dashboard/overview'` → `redirect: '/dashboard/workspace'`
2. Add new route entry as first child:

```typescript
{
  path: 'workspace',
  name: 'DashboardWorkspace',
  meta: { title: '个人工作台' },
  component: () => import('@/views/dashboard/workspace/WorkspaceView.vue')
},
```

The full dashboard section should look like:

```typescript
{
  path: 'dashboard',
  name: 'Dashboard',
  meta: { title: '仪表盘', icon: 'DashboardOutlined', sort: 1 },
  redirect: '/dashboard/workspace',
  children: [
    {
      path: 'workspace',
      name: 'DashboardWorkspace',
      meta: { title: '个人工作台' },
      component: () => import('@/views/dashboard/workspace/WorkspaceView.vue')
    },
    {
      path: 'overview',
      name: 'DashboardOverview',
      meta: { title: '系统总览' },
      component: () => import('@/views/dashboard/Overview.vue')
    },
    {
      path: 'model',
      name: 'ModelDashboard',
      meta: { title: '模型看板' },
      component: () => import('@/views/dashboard/ModelDashboard.vue')
    },
    {
      path: 'data',
      name: 'DataDashboard',
      meta: { title: '数据看板' },
      component: () => import('@/views/dashboard/DataDashboard.vue')
    },
  ],
},
```

- [ ] **Step 2: Verify dev server starts**

Run: `cd maidc-portal && npx vite build --mode development 2>&1 | tail -5`
Expected: Build succeeds with no errors

- [ ] **Step 3: Commit**

```bash
git add maidc-portal/src/router/asyncRoutes.ts
git commit -m "feat(portal): add workspace route, set as default dashboard"
```

---

## Task 18: Integration Smoke Test

**Files:** No new files

- [ ] **Step 1: Run full backend tests**

Run: `cd maidc-parent && mvn test -pl maidc-task -q`
Expected: `BUILD SUCCESS`

- [ ] **Step 2: Run frontend build**

Run: `cd maidc-portal && npx vite build 2>&1 | tail -10`
Expected: Build succeeds

- [ ] **Step 3: Verify the route works in browser**

1. Start backend: `cd maidc-parent && mvn spring-boot:run -pl maidc-task`
2. Start frontend: `cd maidc-portal && npm run dev`
3. Navigate to `http://localhost:5173/dashboard`
4. Expect: Redirects to `/dashboard/workspace`, shows workspace page layout
5. Verify: Welcome section, metric cards, todo/notify sections, quick actions all render

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "feat: complete personal workspace with backend API and frontend components"
```
