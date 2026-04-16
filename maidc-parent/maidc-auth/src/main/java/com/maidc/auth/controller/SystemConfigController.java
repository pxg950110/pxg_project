package com.maidc.auth.controller;

import com.maidc.auth.entity.SystemConfigEntity;
import com.maidc.auth.repository.SystemConfigRepository;
import com.maidc.common.core.result.R;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/configs")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigRepository configRepository;

    @GetMapping
    public R<Map<String, Object>> listConfigs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int page_size) {
        Page<SystemConfigEntity> result = configRepository.findAll(PageRequest.of(page - 1, page_size));
        return R.ok(Map.of(
                "items", result.getContent().stream().map(this::toMap).toList(),
                "total", result.getTotalElements()
        ));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> updateConfig(@PathVariable Long id, @RequestBody ConfigUpdateDTO dto) {
        SystemConfigEntity entity = configRepository.findById(id).orElseThrow();
        if (dto.getConfigValue() != null) {
            entity.setConfigValue(dto.getConfigValue());
        }
        if (dto.getConfigGroup() != null) {
            entity.setConfigGroup(dto.getConfigGroup());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        configRepository.save(entity);
        return R.ok(toMap(entity));
    }

    private Map<String, Object> toMap(SystemConfigEntity e) {
        return Map.of(
                "id", e.getId(),
                "config_key", e.getConfigKey(),
                "config_value", e.getConfigValue(),
                "config_type", e.getConfigType(),
                "config_group", e.getConfigGroup(),
                "description", e.getDescription() != null ? e.getDescription() : "",
                "is_encrypted", e.getIsEncrypted()
        );
    }

    @Data
    public static class ConfigUpdateDTO {
        private String configKey;
        private String configValue;
        private String configGroup;
        private String description;
    }
}
