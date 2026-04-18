package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.DesensitizeRuleEntity;
import com.maidc.data.service.DesensitizeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cdr/desensitize-rules")
@RequiredArgsConstructor
public class DesensitizeRuleController {

    private final DesensitizeRuleService desensitizeRuleService;

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping
    public R<Page<DesensitizeRuleEntity>> listDesensitizeRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(desensitizeRuleService.listDesensitizeRules(page, size));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{id}")
    public R<DesensitizeRuleEntity> getDesensitizeRule(@PathVariable Long id) {
        return R.ok(desensitizeRuleService.getDesensitizeRule(id));
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping
    public R<DesensitizeRuleEntity> createDesensitizeRule(@RequestBody DesensitizeRuleEntity entity) {
        return R.ok(desensitizeRuleService.createDesensitizeRule(entity));
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PutMapping("/{id}")
    public R<DesensitizeRuleEntity> updateDesensitizeRule(@PathVariable Long id, @RequestBody DesensitizeRuleEntity entity) {
        return R.ok(desensitizeRuleService.updateDesensitizeRule(id, entity));
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/{id}")
    public R<Void> deleteDesensitizeRule(@PathVariable Long id) {
        desensitizeRuleService.deleteDesensitizeRule(id);
        return R.ok();
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PutMapping("/{id}/toggle")
    public R<DesensitizeRuleEntity> toggleDesensitizeRule(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return R.ok(desensitizeRuleService.toggleDesensitizeRule(id, body.get("enabled")));
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @PostMapping("/preview")
    public R<Map<String, String>> previewDesensitize(@RequestBody Map<String, Object> body) {
        String field = (String) body.getOrDefault("field", "");
        String strategy = (String) body.getOrDefault("strategy", "MASK");
        String desensitized = switch (strategy) {
            case "MASK" -> field.replaceAll("(?<=.{1}).", "*");
            case "HASH" -> Integer.toHexString(field.hashCode());
            case "REPLACE" -> "***";
            case "DELETE" -> "";
            default -> field;
        };
        return R.ok(Map.of("original", field, "desensitized", desensitized));
    }
}
