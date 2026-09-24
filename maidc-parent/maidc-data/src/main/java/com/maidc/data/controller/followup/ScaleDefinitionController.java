package com.maidc.data.controller.followup;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.ScaleDefinitionEntity;
import com.maidc.data.service.followup.ScaleDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 量表定义 API：读 = work（填写端要拉 definition），写 = scale:manage
 */
@RestController
@RequestMapping("/api/v1/cdr/scales")
@RequiredArgsConstructor
public class ScaleDefinitionController {

    private final ScaleDefinitionService service;

    @PreAuthorize("hasPermission('disease:followup:work')")
    @GetMapping
    public R<List<ScaleDefinitionEntity>> list(@RequestParam(required = false) String keyword) {
        return R.ok(service.list(keyword));
    }

    @PreAuthorize("hasPermission('disease:followup:work')")
    @GetMapping("/{scaleCode}")
    public R<ScaleDefinitionEntity> latest(@PathVariable String scaleCode) {
        return R.ok(service.latestActive(scaleCode));
    }

    @PreAuthorize("hasPermission('disease:followup:work')")
    @GetMapping("/{scaleCode}/versions")
    public R<List<ScaleDefinitionEntity>> versions(@PathVariable String scaleCode) {
        return R.ok(service.versions(scaleCode));
    }

    @PreAuthorize("hasPermission('disease:scale:manage')")
    @PostMapping
    public R<ScaleDefinitionEntity> create(@RequestBody Map<String, Object> req) {
        return R.ok(service.create(req));
    }

    /** 发布新版本 = version+1 新行，历史评估快照不受影响 */
    @PreAuthorize("hasPermission('disease:scale:manage')")
    @PutMapping("/{scaleCode}")
    public R<ScaleDefinitionEntity> publishNewVersion(@PathVariable String scaleCode, @RequestBody Map<String, Object> req) {
        req.put("scaleCode", scaleCode);
        return R.ok(service.publishNewVersion(req));
    }

    @PreAuthorize("hasPermission('disease:scale:manage')")
    @PostMapping("/{id}/status")
    public R<ScaleDefinitionEntity> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> req) {
        return R.ok(service.updateStatus(id, req.get("status")));
    }
}
