package com.maidc.msg.mapper;

import com.maidc.msg.entity.MessageEntity;
import com.maidc.msg.entity.MessageTemplateEntity;
import com.maidc.msg.entity.NotificationSettingEntity;
import com.maidc.msg.vo.MessageVO;
import com.maidc.msg.vo.NotificationSettingVO;
import com.maidc.msg.vo.TemplateVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MsgMapper {

    @Mapping(target = "isRead", source = "isRead")
    MessageVO toMessageVO(MessageEntity entity);

    List<MessageVO> toMessageVOList(List<MessageEntity> entities);

    NotificationSettingVO toNotificationSettingVO(NotificationSettingEntity entity);

    List<NotificationSettingVO> toNotificationSettingVOList(List<NotificationSettingEntity> entities);

    TemplateVO toTemplateVO(MessageTemplateEntity entity);

    List<TemplateVO> toTemplateVOList(List<MessageTemplateEntity> entities);
}
