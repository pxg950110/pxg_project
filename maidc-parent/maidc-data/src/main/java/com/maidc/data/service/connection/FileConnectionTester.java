package com.maidc.data.service.connection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.Map;

@Slf4j
@Component
public class FileConnectionTester implements ConnectionTester {

    @Override
    public String getType() { return "FILE_CHECK"; }

    @Override
    public ConnectionTestResult test(Map<String, Object> params) {
        String filePath = (String) params.get("file_path");
        if (filePath == null || filePath.isBlank()) {
            return ConnectionTestResult.fail("文件路径不能为空");
        }

        long start = System.currentTimeMillis();
        File file = new File(filePath);
        int latency = (int) (System.currentTimeMillis() - start);

        if (!file.exists()) {
            return ConnectionTestResult.fail("文件不存在: " + filePath);
        }
        if (!file.canRead()) {
            return ConnectionTestResult.fail("文件不可读: " + filePath);
        }

        long sizeKB = file.length() / 1024;
        return ConnectionTestResult.ok(latency, Map.of("sizeKB", sizeKB, "fileName", file.getName()));
    }
}
