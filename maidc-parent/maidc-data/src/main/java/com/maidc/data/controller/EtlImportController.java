package com.maidc.data.controller;

import com.maidc.data.service.OdsImportService;
import com.maidc.data.vo.ImportStatusVO;
import com.maidc.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * ODS 数据导入 API
 */
@RestController
@RequestMapping("/api/v1/etl/import")
@RequiredArgsConstructor
public class EtlImportController {

    private final OdsImportService odsImportService;

    @PostMapping("/start")
    public R<String> startImport() {
        try {
            String batchId = odsImportService.startImport();
            return R.ok(batchId);
        } catch (IllegalStateException e) {
            return R.fail(409, e.getMessage());
        }
    }

    @GetMapping("/status")
    public R<ImportStatusVO> getStatus() {
        return R.ok(odsImportService.getImportStatus());
    }

    @PostMapping("/retry/{tableName}")
    public R<Void> retry(@PathVariable String tableName) {
        try {
            odsImportService.retryTable(tableName);
            return R.ok(null);
        } catch (Exception e) {
            return R.fail(400, e.getMessage());
        }
    }
}
