package com.maidc.task.mapper;

import com.maidc.task.dto.TaskCreateDTO;
import com.maidc.task.dto.TaskUpdateDTO;
import com.maidc.task.entity.TaskEntity;
import com.maidc.task.entity.TaskExecutionEntity;
import com.maidc.task.vo.TaskExecutionVO;
import com.maidc.task.vo.TaskVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    TaskVO toVO(TaskEntity entity);

    @Mapping(source = "name", target = "taskName")
    TaskEntity toEntity(TaskCreateDTO dto);

    TaskExecutionVO toExecutionVO(TaskExecutionEntity entity);

    @Mapping(source = "name", target = "taskName")
    void updateEntity(TaskUpdateDTO dto, @MappingTarget TaskEntity entity);
}
