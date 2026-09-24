package com.maidc.data.service.etl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Embulk 进程执行器：把 YAML 配置通过 stdin 交给 `embulk run -`，
 * 捕获输出并跟踪运行中的进程（供取消时终止）。
 */
@Slf4j
@Component
public class EmbulkProcessRunner {

    /** 单步执行输出/错误信息落库前的截断上限，与 errorMessage 列容量匹配 */
    private static final int MAX_OUTPUT_CHARS = 4000;

    private final Map<Long, Process> runningProcesses = new ConcurrentHashMap<>();

    public record RunResult(int exitCode, String output) {
        public boolean success() {
            return exitCode == 0;
        }
    }

    /**
     * 阻塞执行一步 Embulk 任务。调用方应在独立线程中调用。
     *
     * @param executionId 执行记录 ID，用于取消时定位进程
     * @param embulkYaml  完整的 Embulk YAML 配置
     */
    public RunResult run(Long executionId, String embulkYaml) throws IOException, InterruptedException {
        try {
            ProcessBuilder pb = new ProcessBuilder("embulk", "run", "-");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            runningProcesses.put(executionId, process);

            try (var os = process.getOutputStream()) {
                os.write(embulkYaml.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            return new RunResult(exitCode, output.toString());

        } finally {
            runningProcesses.remove(executionId);
        }
    }

    /** 终止指定执行对应的 Embulk 进程（若由当前 JVM 启动且仍在运行）。 */
    public void cancel(Long executionId) {
        Process process = runningProcesses.remove(executionId);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            log.info("Killed process for execution {}", executionId);
        }
    }

    public static String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...(truncated)";
    }

    public static String truncateOutput(String output) {
        return truncate(output, MAX_OUTPUT_CHARS);
    }
}
