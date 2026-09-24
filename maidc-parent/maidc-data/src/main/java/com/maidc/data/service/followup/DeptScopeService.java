package com.maidc.data.service.followup;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.security.context.CurrentUser;
import com.maidc.common.security.context.PermissionContext;
import com.maidc.common.security.scope.DataScopeHelper;
import com.maidc.common.security.store.PermissionStore;
import com.maidc.data.entity.InstitutionEntity;
import com.maidc.data.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 随访模块 DEPT 数据范围解析（与 PatientEncounterService ∃ 语义一致）：
 * 随访实例可见性 = 患者可见性——患者任一就诊科室 == 用户科室即可见。
 * 返回 empty = 无需过滤（无用户上下文的内部调用 / SELF/ALL 范围）。
 */
@Component
@RequiredArgsConstructor
public class DeptScopeService {

    private final PermissionStore permissionStore;
    private final InstitutionRepository institutionRepository;

    /**
     * 解析 DEPT 过滤科室名；DEPT 范围下任何一步不可得 → fail-closed 抛 notFound（不暴露存在性）。
     */
    public Optional<String> deptFilterName(ErrorCode notFound) {
        Long userId = CurrentUser.userId();
        if (userId == null) {
            // 无用户上下文（内部调用/定时任务），认证与接口级权限由网关/权限切面负责
            return Optional.empty();
        }
        PermissionContext ctx = permissionStore.load(userId);
        if (ctx == null) {
            throw new BusinessException(notFound);
        }
        if (!DataScopeHelper.needDeptFilter(ctx)) {
            return Optional.empty();
        }
        Long deptId = DataScopeHelper.deptId(ctx);
        if (deptId == null) {
            throw new BusinessException(notFound);
        }
        return institutionRepository.findById(deptId)
                .map(InstitutionEntity::getName)
                .or(() -> { throw new BusinessException(notFound); });
    }
}
