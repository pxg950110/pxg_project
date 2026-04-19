package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.service.etl.EtlMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cdr/etl/metadata")
@RequiredArgsConstructor
public class EtlMetadataController {

    private final EtlMetadataService metadataService;

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping("/schemas")
    public R<List<String>> listSchemas() {
        return R.ok(metadataService.listSchemas());
    }

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping("/schemas/{schema}/tables")
    public R<List<Map<String, Object>>> listTables(@PathVariable String schema) {
        return R.ok(metadataService.listTables(schema));
    }

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping("/tables/{schema}.{table}/columns")
    public R<List<Map<String, Object>>> listColumns(@PathVariable String schema, @PathVariable String table) {
        return R.ok(metadataService.listColumns(schema, table));
    }
}
