package com.maidc.data.controller.followup;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.R;
import com.maidc.data.entity.ScaleDefinitionEntity;
import com.maidc.data.service.followup.ScaleBindingService;
import com.maidc.data.service.followup.ScaleDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 量表数据绑定解析：按患者执行 definition.items[].binding，返回带入值/枚举选项。
 * 前端填写组件按返回渲染只读快照 / 预填 / 动态选项。
 */
@RestController
@RequestMapping("/api/v1/cdr/scales/{scaleCode}/bindings")
@RequiredArgsConstructor
public class ScaleBindingController {

    private final ScaleDefinitionService scaleService;
    private final ScaleBindingService bindingService;

    @PreAuthorize("hasPermission('disease:followup:work')")
    @GetMapping
    public R<Map<String, Object>> resolve(@PathVariable String scaleCode, @RequestParam Long patientId) {
        ScaleDefinitionEntity scale = scaleService.latestActive(scaleCode);
        String resolved = bindingService.resolveForPatient(patientId, scale.getDefinition());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> items = new com.fasterxml.jackson.databind.ObjectMapper().readValue(resolved, Map.class);
            return R.ok(items);
        } catch (Exception e) {
            throw new BusinessException(500, "绑定解析结果序列化失败");
        }
    }
}
