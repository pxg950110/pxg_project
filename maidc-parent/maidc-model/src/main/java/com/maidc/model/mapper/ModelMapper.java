package com.maidc.model.mapper;

import com.maidc.model.entity.*;
import com.maidc.model.vo.*;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ModelMapper {

    ModelVO toModelVO(ModelEntity entity);

    ModelDetailVO toModelDetailVO(ModelEntity entity);

    VersionVO toVersionVO(ModelVersionEntity entity);

    EvaluationVO toEvaluationVO(EvaluationEntity entity);

    ApprovalVO toApprovalVO(ApprovalEntity entity);

    DeploymentVO toDeploymentVO(DeploymentEntity entity);

    RouteVO toRouteVO(DeployRouteEntity entity);

    AlertRuleVO toAlertRuleVO(AlertRuleEntity entity);

    AlertRecordVO toAlertRecordVO(AlertRecordEntity entity);
}
