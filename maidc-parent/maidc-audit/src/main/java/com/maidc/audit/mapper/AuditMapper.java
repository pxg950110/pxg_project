package com.maidc.audit.mapper;

import com.maidc.audit.entity.AuditLogEntity;
import com.maidc.audit.entity.DataAccessLogEntity;
import com.maidc.audit.entity.SystemEventEntity;
import com.maidc.audit.vo.AuditLogVO;
import com.maidc.audit.vo.DataAccessLogVO;
import com.maidc.audit.vo.SystemEventVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditMapper {

    AuditLogVO toAuditLogVO(AuditLogEntity entity);

    DataAccessLogVO toDataAccessLogVO(DataAccessLogEntity entity);

    SystemEventVO toSystemEventVO(SystemEventEntity entity);
}
