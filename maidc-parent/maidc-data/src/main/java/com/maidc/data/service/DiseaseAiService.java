package com.maidc.data.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiseaseAiService {

    public Map<String, Object> suggestRules(String diseaseName) {
        try {
            // TODO: 接入实际 AI 服务，当前返回空结果
            return Map.of("groups", List.of(), "confidence", 0, "source", "AI");
        } catch (Exception e) {
            log.warn("AI suggest failed for: {}", diseaseName, e);
            return Map.of("groups", List.of(), "confidence", 0, "source", "AI");
        }
    }
}
