package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.common.log.annotation.OperLog;
import com.maidc.data.dto.EncounterDetailDTO;
import com.maidc.data.dto.PatientEncounterListDTO;
import com.maidc.data.service.PatientEncounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 患者就诊360视图控制器
 * <p>挂载 /api/v1/cdr/patient-encounters：与 CdrController 的 /api/v1/cdr/patients/{p}/encounters
 * （EncounterService 扁平列表）路径隔离，且符合全局 /api/v1 前缀约定。
 */
@RestController
@RequestMapping("/api/v1/cdr/patient-encounters")
@RequiredArgsConstructor
public class PatientEncounterController {

    private final PatientEncounterService patientEncounterService;

    /**
     * 查询患者就诊列表
     *
     * @param patientId 患者ID
     * @param page      页码（从1开始）
     * @param size      每页大小
     * @return 患者就诊列表
     */
    @OperLog(module = "患者就诊", operation = "查询患者就诊列表")
    @GetMapping("/patients/{patientId}/encounters")
    public R<PatientEncounterListDTO> getPatientEncounters(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        // 创建分页对象（page-1转换为零基索引，按入院时间降序排序）
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("admitTime").descending());

        PatientEncounterListDTO result = patientEncounterService.getPatientEncounterList(patientId, pageable);
        return R.ok(result);
    }

    /**
     * 查询就诊详情
     *
     * @param encounterId 就诊ID
     * @return 就诊详情
     */
    @OperLog(module = "患者就诊", operation = "查询就诊详情")
    @GetMapping("/encounters/{encounterId}")
    public R<EncounterDetailDTO> getEncounterDetail(@PathVariable Long encounterId) {
        EncounterDetailDTO result = patientEncounterService.getEncounterDetail(encounterId);
        return R.ok(result);
    }
}
