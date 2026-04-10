# MAIDC 后端开发指南

> **版本**: v1.0
> **日期**: 2026-04-11
> **状态**: 已确认
> **ORM**: Spring Data JPA + Hibernate
> **关联**: PRD `docs/superpowers/specs/2026-04-08-maidc-design.md`

---

## 目录

1. [工程结构规范](#1-工程结构规范)
2. [分层架构规范](#2-分层架构规范)
3. [JPA 数据访问层](#3-jpa-数据访问层)
4. [消息队列集成](#4-消息队列集成)
5. [统一异常处理](#5-统一异常处理)
6. [编码约定](#6-编码约定)

---

## 1. 工程结构规范

### 1.1 Maven 多模块结构

```
maidc-parent/                          ← 父工程（依赖管理）
├── pom.xml                            ← dependencyManagement 统一版本
│
├── common/                            ← 公共模块
│   ├── common-core/                   ← 统一响应体/异常/枚举
│   ├── common-redis/                  ← Redis工具类/分布式锁
│   ├── common-minio/                  ← MinIO文件上传下载
│   ├── common-mq/                     ← RabbitMQ工具/消息基类
│   ├── common-log/                    ← 操作日志注解+AOP
│   ├── common-security/               ← JWT工具/权限注解
│   └── common-jpa/                    ← JPA配置/审计/多Schema/Specification
│
├── maidc-gateway/                     ← Spring Cloud Gateway 网关
│   ├── src/main/java/com/maidc/gateway/
│   │   ├── filter/                    ← 全局过滤器
│   │   ├── config/                    ← 路由配置
│   │   └── handler/                   ← 异常处理
│   └── src/main/resources/
│       └── application.yml
│
├── maidc-auth/                        ← 用户权限服务 :8081
├── maidc-model/                       ← 模型管理服务 :8083 ★Phase 1
├── maidc-data/                        ← 数据管理服务 :8082
├── maidc-task/                        ← 任务调度服务 :8084
├── maidc-label/                       ← 数据标注服务 :8085
├── maidc-audit/                       ← 审计日志服务 :8086
├── maidc-msg/                         ← 消息通知服务 :8087
│
└── maidc-aiworker/                    ← Python AI Worker (独立)
    ├── app/
    │   ├── main.py                    ← FastAPI 入口
    │   ├── api/                       ← 推理API
    │   ├── tasks/                     ← Celery 任务
    │   ├── models/                    ← 模型加载
    │   └── core/                      ← 配置/工具
    ├── pyproject.toml                 ← Poetry 依赖
    └── Dockerfile
```

### 1.2 单个服务模块标准结构

以 `maidc-model` 为例：

```
maidc-model/
├── src/main/java/com/maidc/model/
│   ├── ModelApplication.java         ← 启动类
│   │
│   ├── controller/                   ← REST 控制器
│   │   ├── ModelController.java
│   │   ├── VersionController.java
│   │   ├── EvaluationController.java
│   │   ├── ApprovalController.java
│   │   ├── DeploymentController.java
│   │   ├── RouteController.java
│   │   ├── MonitoringController.java
│   │   └── AlertController.java
│   │
│   ├── service/                      ← 业务逻辑接口
│   │   ├── ModelService.java
│   │   └── impl/
│   │       └── ModelServiceImpl.java
│   │
│   ├── repository/                   ← JPA Repository
│   │   ├── ModelRepository.java
│   │   └── ModelSpecification.java   ← 动态查询条件
│   │
│   ├── entity/                       ← JPA 实体
│   │   ├── ModelEntity.java
│   │   ├── ModelVersionEntity.java
│   │   └── ...
│   │
│   ├── dto/                          ← 数据传输对象（入参）
│   │   ├── ModelCreateDTO.java
│   │   ├── ModelUpdateDTO.java
│   │   └── ...
│   │
│   ├── vo/                           ← 视图输出对象（出参）
│   │   ├── ModelVO.java
│   │   ├── ModelDetailVO.java
│   │   └── ...
│   │
│   ├── mapper/                       ← MapStruct 对象映射
│   │   └── ModelMapper.java
│   │
│   ├── config/                       ← 服务配置
│   │   ├── JpaConfig.java
│   │   └── RabbitMqConfig.java
│   │
│   ├── mq/                           ← 消息生产者/消费者
│   │   ├── ModelMessageProducer.java
│   │   └── EvaluationResultConsumer.java
│   │
│   ├── feign/                        ← 远程调用客户端
│   │   ├── AuthClient.java
│   │   └── DataClient.java
│   │
│   └── enums/                        ← 业务枚举
│       ├── ModelStatus.java
│       ├── VersionStatus.java
│       └── DeployStatus.java
│
└── src/main/resources/
    ├── application.yml               ← 服务配置
    └── bootstrap.yml                 ← Nacos 配置
```

### 1.3 父工程依赖管理

```xml
<!-- maidc-parent/pom.xml 关键依赖版本 -->
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.2.x</spring-boot.version>
    <spring-cloud.version>2023.0.x</spring-cloud.version>
    <spring-cloud-alibaba.version>2023.0.x</spring-cloud-alibaba.version>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
</properties>
```

---

## 2. 分层架构规范

### 2.1 分层职责

```
┌─────────────────────────────────────────────┐
│              Controller 层                    │
│  · @RestController @RequestMapping           │
│  · 参数校验 @Valid                            │
│  · 权限控制 @PreAuthorize                     │
│  · 调用 Service，返回 R<VO>                   │
├─────────────────────────────────────────────┤
│              Service 层                       │
│  · @Service @Transactional                   │
│  · 业务逻辑/状态机/校验                        │
│  · 调用 Repository/Feign/MQ                  │
│  · DTO → Entity (通过 Mapper)                │
│  · Entity → VO (通过 Mapper)                 │
├─────────────────────────────────────────────┤
│              Repository 层                    │
│  · JpaRepository<Entity, Long>               │
│  · JpaSpecificationExecutor<Entity>          │
│  · @Query 自定义查询                          │
│  · 只操作 Entity                              │
└─────────────────────────────────────────────┘
```

### 2.2 DTO vs VO 规范

| 类型 | 方向 | 用途 | 示例 |
|------|------|------|------|
| **DTO** | 前端 → 后端（入参） | 接收创建/更新参数 | `ModelCreateDTO`, `ModelUpdateDTO` |
| **VO** | 后端 → 前端（出参） | 返回给前端展示 | `ModelVO`, `ModelDetailVO` |
| **Entity** | 内部 | 数据库映射 | `ModelEntity` |

**DTO 示例**:
```java
@Data
public class ModelCreateDTO {
    @NotBlank(message = "模型编码不能为空")
    @Size(max = 32)
    private String modelCode;

    @NotBlank(message = "模型名称不能为空")
    @Size(max = 128)
    private String modelName;

    @NotNull(message = "模型类型不能为空")
    private ModelType modelType;

    @NotNull(message = "任务类型不能为空")
    private TaskType taskType;

    @NotNull(message = "框架不能为空")
    private Framework framework;

    @NotNull(message = "输入Schema不能为空")
    private JsonNode inputSchema;

    @NotNull(message = "输出Schema不能为空")
    private JsonNode outputSchema;

    private String tags;
    private String license;
    private Long projectId;
}
```

**VO 示例**:
```java
@Data
public class ModelVO {
    private Long id;
    private String modelCode;
    private String modelName;
    private String modelType;
    private String taskType;
    private String framework;
    private String status;
    private String latestVersion;
    private List<String> tags;
    private String ownerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 2.3 对象转换：MapStruct

```java
@Mapper(componentModel = "spring")
public interface ModelMapper {
    ModelEntity toEntity(ModelCreateDTO dto);
    void updateEntity(ModelUpdateDTO dto, @MappingTarget ModelEntity entity);
    ModelVO toVO(ModelEntity entity);
    ModelDetailVO toDetailVO(ModelEntity entity);

    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    ModelVO toVOWithStatus(ModelEntity entity);
}
```

### 2.4 Controller 规范

```java
@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
@Tag(name = "模型管理")
public class ModelController {

    private final ModelService modelService;

    @PostMapping
    @PreAuthorize("hasPermission('model:create')")
    @Operation(summary = "注册模型")
    public R<ModelVO> createModel(@RequestBody @Valid ModelCreateDTO dto) {
        return R.ok(modelService.createModel(dto));
    }

    @GetMapping
    @Operation(summary = "模型列表")
    public R<PageResult<ModelVO>> listModels(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return R.ok(modelService.listModels(page, pageSize, modelType, status, keyword));
    }

    @GetMapping("/{id}")
    @Operation(summary = "模型详情")
    public R<ModelDetailVO> getModel(@PathVariable Long id) {
        return R.ok(modelService.getModelDetail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('model:update')")
    @Operation(summary = "更新模型")
    public R<ModelVO> updateModel(@PathVariable Long id,
                                   @RequestBody @Valid ModelUpdateDTO dto) {
        return R.ok(modelService.updateModel(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('model:delete')")
    @Operation(summary = "删除模型")
    public R<Void> deleteModel(@PathVariable Long id) {
        modelService.deleteModel(id);
        return R.ok();
    }
}
```

---

## 3. JPA 数据访问层

### 3.1 Entity 规范

```java
@Entity
@Table(name = "m_model", schema = "model")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE model.m_model SET is_deleted = true, updated_at = NOW() WHERE id = ?")
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ModelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_code", nullable = false, length = 32)
    private String modelCode;

    @Column(name = "model_name", nullable = false, length = 128)
    private String modelName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false, length = 32)
    private ModelType modelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 32)
    private TaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Framework framework;

    @Column(name = "input_schema", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode inputSchema;

    @Column(name = "output_schema", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode outputSchema;

    @Column(length = 256)
    private String tags;

    @Column(length = 32)
    private String license;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "project_id")
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ModelStatus status = ModelStatus.DRAFT;

    // ===== 审计字段（JPA Auditing 自动填充） =====

    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 64, updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedBy
    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "org_id", nullable = false)
    private Long orgId;
}
```

**关键注解说明**:

| 注解 | 作用 |
|------|------|
| `@Table(schema = "model")` | 指定 Schema，无需运行时拦截器 |
| `@Where(clause = "is_deleted = false")` | 自动过滤软删除记录 |
| `@SQLDelete(sql = "UPDATE ...")` | DELETE 操作转为逻辑删除 |
| `@DynamicUpdate` | 只更新变更字段 |
| `@EntityListeners(AuditingEntityListener.class)` | 启用审计字段自动填充 |
| `@CreatedBy/@CreatedDate` | 自动填充创建人/创建时间 |
| `@LastModifiedBy/@LastModifiedDate` | 自动填充更新人/更新时间 |

### 3.2 AuditorAware 配置（自动注入 created_by/updated_by）

```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            // 从 Spring Security 上下文获取当前用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Optional.of("system");
            }
            return Optional.of(auth.getName());
        };
    }
}
```

### 3.3 Repository 规范

```java
// 基础 CRUD 继承 JpaRepository，动态查询继承 JpaSpecificationExecutor
public interface ModelRepository extends
        JpaRepository<ModelEntity, Long>,
        JpaSpecificationExecutor<ModelEntity> {

    // 方法命名查询
    Optional<ModelEntity> findByModelCodeAndOrgId(String modelCode, Long orgId);

    List<ModelEntity> findByStatusAndOrgId(ModelStatus status, Long orgId);

    boolean existsByModelCodeAndOrgId(String modelCode, Long orgId);

    // @Query 自定义查询（JPQL）
    @Query("SELECT m FROM ModelEntity m WHERE m.orgId = :orgId " +
           "AND (:keyword IS NULL OR m.modelName LIKE %:keyword%) " +
           "AND (:modelType IS NULL OR m.modelType = :modelType) " +
           "ORDER BY m.createdAt DESC")
    Page<ModelEntity> searchModels(@Param("orgId") Long orgId,
                                    @Param("keyword") String keyword,
                                    @Param("modelType") ModelType modelType,
                                    Pageable pageable);

    // 统计查询
    @Query("SELECT COUNT(m) FROM ModelEntity m WHERE m.orgId = :orgId AND m.status = :status")
    long countByOrgIdAndStatus(@Param("orgId") Long orgId, @Param("status") ModelStatus status);
}
```

### 3.4 JpaSpecificationExecutor 动态查询

```java
public class ModelSpecification {

    public static Specification<ModelEntity> buildSearchSpec(
            Long orgId, String keyword, String modelType, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // org_id 必须匹配
            predicates.add(cb.equal(root.get("orgId"), orgId));

            // 关键词模糊搜索
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.or(
                    cb.like(root.get("modelName"), "%" + keyword + "%"),
                    cb.like(root.get("modelCode"), "%" + keyword + "%"),
                    cb.like(root.get("tags"), "%" + keyword + "%")
                ));
            }

            // 精确过滤
            if (StringUtils.hasText(modelType)) {
                predicates.add(cb.equal(root.get("modelType"), ModelType.valueOf(modelType)));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), ModelStatus.valueOf(status)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
```

**Service 层调用**:
```java
@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {

    private final ModelRepository modelRepository;
    private final ModelMapper modelMapper;

    @Override
    public PageResult<ModelVO> listModels(int page, int pageSize,
                                           String modelType, String status, String keyword) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        Specification<ModelEntity> spec = ModelSpecification
            .buildSearchSpec(orgId, keyword, modelType, status);

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending());
        Page<ModelEntity> result = modelRepository.findAll(spec, pageable);

        return PageResult.of(result.map(modelMapper::toVO));
    }
}
```

### 3.5 推理日志分区表处理

```java
// 推理日志分区表用原生 SQL 查询
@Entity
@Table(name = "m_inference_log", schema = "model")
public class InferenceLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deployment_id", nullable = false)
    private Long deploymentId;

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    // ... 其他字段
}

// Repository 使用原生 SQL 查询分区表
public interface InferenceLogRepository extends JpaRepository<InferenceLogEntity, Long> {

    @Query(value = "SELECT * FROM model.m_inference_log " +
           "WHERE deployment_id = :deploymentId " +
           "AND created_at BETWEEN :startTime AND :endTime " +
           "ORDER BY created_at DESC",
           nativeQuery = true)
    Page<InferenceLogEntity> findByDeploymentAndTimeRange(
            @Param("deploymentId") Long deploymentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);
}
```

### 3.6 分页封装

```java
@Data
public class PageResult<T> {
    private List<T> items;
    private long total;
    private int page;
    private int pageSize;
    private int totalPages;

    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setItems(page.getContent());
        result.setTotal(page.getTotalElements());
        result.setPage(page.getNumber() + 1);
        result.setPageSize(page.getSize());
        result.setTotalPages(page.getTotalPages());
        return result;
    }
}
```

---

## 4. 消息队列集成

### 4.1 RabbitMQ 配置模板

```java
@Configuration
public class ModelRabbitMqConfig {

    // Exchange
    @Bean
    public DirectExchange modelExchange() {
        return new DirectExchange("maidc.model", true, false);
    }

    // Queue
    @Bean
    public Queue evaluationQueue() {
        return QueueBuilder.durable("model.evaluation")
                .withArgument("x-dead-letter-exchange", "maidc.dlx")
                .withArgument("x-dead-letter-routing-key", "model.evaluation")
                .build();
    }

    @Bean
    public Queue evaluationResultQueue() {
        return QueueBuilder.durable("model.evaluation.result").build();
    }

    // Binding
    @Bean
    public Binding evaluationBinding() {
        return BindingBuilder.bind(evaluationQueue())
                .to(modelExchange())
                .with("evaluation");
    }

    @Bean
    public Binding evaluationResultBinding() {
        return BindingBuilder.bind(evaluationResultQueue())
                .to(modelExchange())
                .with("evaluation.result");
    }
}
```

### 4.2 消息生产者

```java
@Component
@RequiredArgsConstructor
public class ModelMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendEvaluationTask(Long evaluationId, Long modelVersionId, Long datasetId) {
        Map<String, Object> payload = Map.of(
            "evaluationId", evaluationId,
            "modelVersionId", modelVersionId,
            "datasetId", datasetId
        );

        MaidcMessage message = MaidcMessage.builder()
                .traceId(MDC.get("traceId"))
                .eventType("MODEL_EVALUATION")
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .source("maidc-model")
                .build();

        rabbitTemplate.convertAndSend("maidc.model", "evaluation", message);
    }
}
```

### 4.3 消息消费者规范

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class EvaluationResultConsumer {

    private final EvaluationService evaluationService;

    @RabbitListener(queues = "model.evaluation.result")
    public void handleEvaluationResult(MaidcMessage message) {
        String traceId = message.getTraceId();
        MDC.put("traceId", traceId);
        try {
            log.info("收到评估结果: {}", message);
            Map<String, Object> payload = message.getPayload();
            Long evaluationId = ((Number) payload.get("evaluationId")).longValue();

            evaluationService.updateEvaluationResult(evaluationId, payload);
            log.info("评估结果处理完成: evaluationId={}", evaluationId);
        } catch (Exception e) {
            log.error("评估结果处理失败: {}", e.getMessage(), e);
            throw new AmqpRejectAndDontRequeueException("处理失败，转入死信队列", e);
        } finally {
            MDC.remove("traceId");
        }
    }
}
```

### 4.4 消息基类

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaidcMessage implements Serializable {
    private String traceId;
    private String eventType;
    private Map<String, Object> payload;
    private LocalDateTime timestamp;
    private String source;
}
```

---

## 5. 统一异常处理

### 5.1 异常分类

```java
// 业务异常（可预期的）
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}

// 错误码枚举
@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 通用
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),

    // 模型管理
    MODEL_NOT_FOUND(4001, "模型不存在"),
    MODEL_CODE_DUPLICATE(4002, "模型编码已存在"),
    VERSION_NOT_FOUND(4011, "版本不存在"),
    VERSION_NO_DUPLICATE(4012, "版本号已存在"),
    EVALUATION_NOT_FOUND(4021, "评估任务不存在"),
    APPROVAL_NOT_FOUND(4031, "审批不存在"),
    APPROVAL_NOT_PENDING(4032, "审批状态不允许操作"),
    DEPLOYMENT_NOT_FOUND(4041, "部署不存在"),
    DEPLOYMENT_NOT_RUNNING(4042, "部署未运行"),

    // 数据管理
    PATIENT_NOT_FOUND(5001, "患者不存在"),
    DATASET_NOT_FOUND(5011, "数据集不存在"),
    ETL_TASK_FAILED(5021, "ETL任务执行失败");

    private final int code;
    private final String message;
}
```

### 5.2 全局异常处理器

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return R.fail(400, message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public R<Void> handleAccessDeniedException(AccessDeniedException e) {
        return R.fail(403, "无权限访问");
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return R.fail(500, "系统内部错误");
    }
}
```

### 5.3 统一响应体

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {
    private int code;
    private String message;
    private T data;
    private String traceId;

    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data, MDC.get("traceId"));
    }

    public static <T> R<T> ok() {
        return new R<>(200, "success", null, MDC.get("traceId"));
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null, MDC.get("traceId"));
    }
}
```

---

## 6. 编码约定

### 6.1 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 包名 | 全小写，模块开头 | `com.maidc.model.controller` |
| 类名 | 大驼峰 | `ModelService`, `ModelServiceImpl` |
| 方法名 | 小驼峰，动词开头 | `createModel()`, `listModels()` |
| 常量 | 全大写下划线 | `MAX_PAGE_SIZE` |
| REST路径 | 小写连字符 | `/api/v1/alert-rules` |
| 枚举值 | 全大写下划线 | `ModelStatus.DRAFT` |

### 6.2 日志规范

```java
// 使用 SLF4J，禁止 System.out
private static final Logger log = LoggerFactory.getLogger(ModelService.class);

// TraceId 通过 MDC 自动注入日志格式
// logback-spring.xml 配置:
// %d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId}] [%X{userId}] %-5level %logger{36} - %msg%n

// 关键业务操作必须记录
log.info("创建模型: modelCode={}, operator={}", dto.getModelCode(), SecurityUtils.getCurrentUsername());
log.info("模型评估完成: evaluationId={}, auc={}", evalId, metrics.getAuc());

// 异常必须记录堆栈
log.error("模型部署失败: deploymentId={}", deploymentId, e);
```

### 6.3 并发规范

```java
// 分布式锁（使用 Redis）
@Autowired
private RedisLockService redisLockService;

public void executeEvaluation(Long evaluationId) {
    String lockKey = "maidc:lock:model:eval:" + evaluationId;
    if (!redisLockService.tryLock(lockKey, Duration.ofHours(1))) {
        throw new BusinessException(ErrorCode.CONFLICT, "评估任务正在执行中");
    }
    try {
        // 执行评估逻辑
    } finally {
        redisLockService.unlock(lockKey);
    }
}
```

### 6.4 幂等性保证

```java
// 消息消费幂等：通过 evaluationId 去重
@RabbitListener(queues = "model.evaluation.result")
public void handleEvaluationResult(MaidcMessage message) {
    Long evaluationId = getEvaluationId(message);
    // 检查是否已处理
    EvaluationEntity eval = evaluationRepository.findById(evaluationId).orElse(null);
    if (eval != null && eval.getStatus() != EvaluationStatus.RUNNING) {
        log.info("评估结果已处理，跳过: evaluationId={}", evaluationId);
        return;
    }
    // 处理结果...
}
```

### 6.5 安全规范

```java
// SQL注入：JPA参数绑定天然防护，禁止字符串拼接
// ✅ 正确
@Query("SELECT m FROM ModelEntity m WHERE m.modelName LIKE %:keyword%")
List<ModelEntity> search(@Param("keyword") String keyword);

// ❌ 错误（禁止）
@Query(value = "SELECT * FROM model WHERE name LIKE '%" + keyword + "%'", nativeQuery = true)

// XSS：前端输入统一通过 DTO 校验
@Size(max = 128)
@Pattern(regexp = "^[^<>]*$")  // 禁止HTML标签
private String modelName;

// 敏感字段加密：通过 AttributeConverter
@Convert(converter = AesEncryptor.class)
@Column(name = "id_card_no")
private String idCardNo;
```

---

> **文档结束** - MAIDC 后端开发指南 v1.0
