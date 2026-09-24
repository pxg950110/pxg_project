package com.maidc.label.mapper;

import com.maidc.label.entity.LabelRecordEntity;
import com.maidc.label.entity.LabelTaskEntity;
import com.maidc.label.vo.LabelStatsVO;
import com.maidc.label.vo.LabelTaskDetailVO;
import com.maidc.label.vo.LabelTaskVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LabelMapper {

    LabelTaskVO toTaskVO(LabelTaskEntity entity);

    @Mapping(target = "guidelines", source = "guidelines")
    @Mapping(target = "config", source = "config")
    LabelTaskDetailVO toTaskDetailVO(LabelTaskEntity entity);
}
