package com.maidc.task.mapper;

import com.maidc.task.dto.PersonalTaskCreateDTO;
import com.maidc.task.entity.PersonalTaskEntity;
import com.maidc.task.vo.PersonalTaskVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PersonalTaskMapper {

    PersonalTaskVO toVO(PersonalTaskEntity entity);

    @Mapping(target = "priority", defaultValue = "MEDIUM")
    @Mapping(target = "dueDate", dateFormat = "yyyy-MM-dd HH:mm:ss")
    PersonalTaskEntity toEntity(PersonalTaskCreateDTO dto);
}
