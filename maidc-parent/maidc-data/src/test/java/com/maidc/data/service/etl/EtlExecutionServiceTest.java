package com.maidc.data.service.etl;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.config.EtlProperties;
import com.maidc.data.entity.DataSourceEntity;
import com.maidc.data.entity.EtlExecutionEntity;
import com.maidc.data.entity.EtlPipelineEntity;
import com.maidc.data.entity.EtlStepEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.DataSourceRepository;
import com.maidc.data.repository.EtlExecutionRepository;
import com.maidc.data.repository.EtlPipelineRepository;
import com.maidc.data.repository.EtlStepRepository;
import com.maidc.data.vo.EtlExecutionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EtlExecutionServiceTest {

    @Mock
    private EtlPipelineRepository pipelineRepository;
    @Mock
    private EtlStepRepository stepRepository;
    @Mock
    private EtlExecutionRepository executionRepository;
    @Mock
    private EtlConfigGenerator configGenerator;
    @Mock
    private EmbulkProcessRunner embulkRunner;
    @Mock
    private DataSourceRepository dataSourceRepository;
    @Mock
    private DataMapper dataMapper;

    /** 后台任务内联同步执行，便于断言触发后的最终状态 */
    private final ExecutorService syncExecutor = new SameThreadExecutorService();

    private EtlExecutionService service;
    private EtlPipelineEntity pipeline;
    private EtlExecutionEntity pipelineExecution;

    /** 记录最近一次 generateEmbulkConfig 的调用参数（step, sHost, sPort, sDb, sUser, sPass, tHost, tPort, tDb, tUser, tPass） */
    private Object[] configArgs;

    @BeforeEach
    void setUp() {
        service = new EtlExecutionService(pipelineRepository, stepRepository, executionRepository,
                configGenerator, embulkRunner, dataSourceRepository, new EtlProperties(), syncExecutor, dataMapper);
    }

    private EtlPipelineEntity pipeline(long id) {
        EtlPipelineEntity p = new EtlPipelineEntity();
        p.setId(id);
        p.setPipelineName("p");
        p.setOrgId(0L);
        return p;
    }

    @Test
    void triggerExecution_pipelineAlreadyRunning_throws() {
        pipeline = pipeline(1L);
        when(pipelineRepository.findById(1L)).thenReturn(Optional.of(pipeline));
        when(executionRepository.findByPipelineIdAndStatusAndIsDeletedFalse(1L, "RUNNING"))
                .thenReturn(List.of(new EtlExecutionEntity()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.triggerExecution(1L, "MANUAL"));
        assertEquals(ErrorCode.TASK_ALREADY_RUNNING.getCode(), ex.getCode());
    }

    @Test
    void triggerExecution_withoutSteps_finalizesPipelineAsSuccess() {
        pipeline = pipeline(1L);
        when(pipelineRepository.findById(1L)).thenReturn(Optional.of(pipeline));
        when(executionRepository.findByPipelineIdAndStatusAndIsDeletedFalse(1L, "RUNNING"))
                .thenReturn(List.of());
        when(executionRepository.save(any(EtlExecutionEntity.class))).thenAnswer(inv -> {
            EtlExecutionEntity e = inv.getArgument(0);
            if (e.getId() == null) e.setId(100L);
            pipelineExecution = e;
            return e;
        });
        when(executionRepository.findById(100L)).thenAnswer(inv -> Optional.of(pipelineExecution));
        when(stepRepository.findByPipelineIdAndIsDeletedFalseOrderByStepOrder(1L))
                .thenReturn(List.of());
        when(dataMapper.toEtlExecutionVO(any(EtlExecutionEntity.class))).thenReturn(new EtlExecutionVO());

        EtlExecutionVO vo = service.triggerExecution(1L, "MANUAL");

        assertNotNull(vo);
        assertEquals("SUCCESS", pipelineExecution.getStatus());
        assertNotNull(pipelineExecution.getEndTime());
        assertNotNull(pipeline.getLastRunTime());
    }

    @Test
    void cancelExecution_notRunning_throws() {
        EtlExecutionEntity finished = new EtlExecutionEntity();
        finished.setId(9L);
        finished.setStatus("SUCCESS");
        when(executionRepository.findById(9L)).thenReturn(Optional.of(finished));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.cancelExecution(9L));
        assertEquals(ErrorCode.TASK_NOT_RUNNING.getCode(), ex.getCode());
    }

    // ------------------------------------------------ 连接参数解析（pipeline.sourceId → DataSourceEntity，缺失兜底 EtlProperties）

    private void stubSingleStepRun(EtlProperties props) throws IOException, InterruptedException {
        pipeline = pipeline(1L);
        pipeline.setSourceId(7L);
        when(pipelineRepository.findById(1L)).thenReturn(Optional.of(pipeline));
        when(executionRepository.findByPipelineIdAndStatusAndIsDeletedFalse(1L, "RUNNING"))
                .thenReturn(List.of());
        when(executionRepository.save(any(EtlExecutionEntity.class))).thenAnswer(inv -> {
            EtlExecutionEntity e = inv.getArgument(0);
            if (e.getId() == null) e.setId(100L);
            pipelineExecution = e;
            return e;
        });
        when(executionRepository.findById(100L)).thenAnswer(inv -> Optional.of(pipelineExecution));

        EtlStepEntity step = new EtlStepEntity();
        step.setId(5L);
        step.setStepOrder(1);
        when(stepRepository.findByPipelineIdAndIsDeletedFalseOrderByStepOrder(1L))
                .thenReturn(List.of(step));

        when(configGenerator.generateEmbulkConfig(any(), anyString(), org.mockito.ArgumentMatchers.anyInt(),
                anyString(), anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt(),
                anyString(), anyString(), anyString())).thenAnswer(inv -> {
            configArgs = inv.getArguments();
            return "cfg";
        });
        when(embulkRunner.run(any(), anyString()))
                .thenReturn(new EmbulkProcessRunner.RunResult(0, "ok"));
        when(dataMapper.toEtlExecutionVO(any(EtlExecutionEntity.class))).thenReturn(new EtlExecutionVO());
    }

    private Object[] runSingleStepAndCaptureConfig(DataSourceEntity source, EtlProperties props)
            throws IOException, InterruptedException {
        stubSingleStepRun(props);
        if (source != null) {
            when(dataSourceRepository.findById(7L)).thenReturn(Optional.of(source));
        }
        service.triggerExecution(1L, "MANUAL");
        return configArgs;
    }

    @Test
    void executeStep_resolvesSourceConnectionFromDataSourceEntity() throws Exception {
        DataSourceEntity ds = new DataSourceEntity();
        ds.setHost("src-host");
        ds.setPort(5433);
        ds.setDatabaseName("src-db");
        ds.setUsername("src-user");
        ds.setPassword("src-pass");

        runSingleStepAndCaptureConfig(ds, new EtlProperties());

        assertNotNull(configArgs);
        assertEquals("src-host", configArgs[1]);
        assertEquals(5433, configArgs[2]);
        assertEquals("src-db", configArgs[3]);
        assertEquals("src-user", configArgs[4]);
        assertEquals("src-pass", configArgs[5]);
        // target 无实体引用：走 EtlProperties（部署侧指向 CDR 本体连接）
        assertEquals("localhost", configArgs[6]);
        assertEquals(5432, configArgs[7]);
        assertEquals("target_db", configArgs[8]);
    }

    @Test
    void executeStep_fallsBackToEtlPropertiesWhenSourceEntityMissing() throws Exception {
        runSingleStepAndCaptureConfig(null, new EtlProperties());

        assertNotNull(configArgs);
        assertEquals("localhost", configArgs[1]);
        assertEquals(5432, configArgs[2]);
        assertEquals("source_db", configArgs[3]);
        assertEquals("source_user", configArgs[4]);
        assertEquals("source_pass", configArgs[5]);
    }

    private static final class SameThreadExecutorService extends AbstractExecutorService {
        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }
    }
}
