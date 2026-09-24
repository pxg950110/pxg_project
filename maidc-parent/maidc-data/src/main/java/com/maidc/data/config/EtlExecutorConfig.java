package com.maidc.data.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ETL 异步执行线程池：取代散落的裸 Thread，
 * 并发上限由 maidc.etl.parallel 控制，线程为守护线程（进程退出即终止，与历史行为一致）。
 */
@Configuration
@RequiredArgsConstructor
public class EtlExecutorConfig {

    private final EtlProperties etlProperties;

    @Bean(destroyMethod = "shutdown")
    public ExecutorService etlExecutor() {
        AtomicLong seq = new AtomicLong();
        return Executors.newFixedThreadPool(
                Math.max(1, etlProperties.getParallel()),
                r -> {
                    Thread t = new Thread(r, "etl-exec-" + seq.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                });
    }
}
