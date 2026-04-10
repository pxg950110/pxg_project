package com.maidc.msg.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.msg.dto.NotificationSettingDTO;
import com.maidc.msg.dto.TemplateCreateDTO;
import com.maidc.msg.entity.MessageTemplateEntity;
import com.maidc.msg.entity.NotificationSettingEntity;
import com.maidc.msg.mapper.MsgMapper;
import com.maidc.msg.repository.MessageTemplateRepository;
import com.maidc.msg.repository.NotificationSettingRepository;
import com.maidc.msg.vo.NotificationSettingVO;
import com.maidc.msg.vo.TemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final MessageTemplateRepository messageTemplateRepository;
    private final MsgMapper msgMapper;

    /**
     * 获取用户通知设置列表
     */
    public List<NotificationSettingVO> getSettings(Long userId) {
        List<NotificationSettingEntity> settings = notificationSettingRepository
                .findByUserIdAndIsDeletedFalse(userId);
        return msgMapper.toNotificationSettingVOList(settings);
    }

    /**
     * 更新用户通知设置
     */
    @Transactional
    public NotificationSettingVO updateSetting(Long userId, Long settingId, NotificationSettingDTO dto) {
        NotificationSettingEntity setting = notificationSettingRepository
                .findByIdAndUserIdAndIsDeletedFalse(settingId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (dto.getChannel() != null) {
            setting.setChannel(dto.getChannel());
        }
        if (dto.getEventType() != null) {
            setting.setEventType(dto.getEventType());
        }
        if (dto.getEnabled() != null) {
            setting.setEnabled(dto.getEnabled());
        }

        setting = notificationSettingRepository.save(setting);
        log.info("通知设置已更新: id={}, userId={}", setting.getId(), userId);
        return msgMapper.toNotificationSettingVO(setting);
    }

    /**
     * 创建用户通知设置
     */
    @Transactional
    public NotificationSettingVO createSetting(Long userId, NotificationSettingDTO dto) {
        NotificationSettingEntity setting = new NotificationSettingEntity();
        setting.setUserId(userId);
        setting.setChannel(dto.getChannel());
        setting.setEventType(dto.getEventType());
        setting.setEnabled(dto.getEnabled());

        setting = notificationSettingRepository.save(setting);
        log.info("通知设置已创建: id={}, userId={}", setting.getId(), userId);
        return msgMapper.toNotificationSettingVO(setting);
    }

    /**
     * 获取所有消息模板
     */
    public List<TemplateVO> getTemplates() {
        List<MessageTemplateEntity> templates = messageTemplateRepository.findByIsDeletedFalse();
        return msgMapper.toTemplateVOList(templates);
    }

    /**
     * 创建消息模板
     */
    @Transactional
    public TemplateVO createTemplate(TemplateCreateDTO dto) {
        if (messageTemplateRepository.existsByCodeAndIsDeletedFalse(dto.getCode())) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(), "模板编码已存在: " + dto.getCode());
        }

        MessageTemplateEntity template = new MessageTemplateEntity();
        template.setCode(dto.getCode());
        template.setTitleTemplate(dto.getTitleTemplate());
        template.setContentTemplate(dto.getContentTemplate());
        template.setChannel(dto.getChannel());
        template.setEventType(dto.getEventType());

        template = messageTemplateRepository.save(template);
        log.info("消息模板已创建: id={}, code={}", template.getId(), dto.getCode());
        return msgMapper.toTemplateVO(template);
    }

    /**
     * 更新消息模板
     */
    @Transactional
    public TemplateVO updateTemplate(Long id, TemplateCreateDTO dto) {
        MessageTemplateEntity template = messageTemplateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (dto.getCode() != null && !dto.getCode().equals(template.getCode())) {
            if (messageTemplateRepository.existsByCodeAndIsDeletedFalse(dto.getCode())) {
                throw new BusinessException(ErrorCode.CONFLICT.getCode(), "模板编码已存在: " + dto.getCode());
            }
            template.setCode(dto.getCode());
        }
        if (dto.getTitleTemplate() != null) {
            template.setTitleTemplate(dto.getTitleTemplate());
        }
        if (dto.getContentTemplate() != null) {
            template.setContentTemplate(dto.getContentTemplate());
        }
        if (dto.getChannel() != null) {
            template.setChannel(dto.getChannel());
        }
        if (dto.getEventType() != null) {
            template.setEventType(dto.getEventType());
        }

        template = messageTemplateRepository.save(template);
        log.info("消息模板已更新: id={}", id);
        return msgMapper.toTemplateVO(template);
    }

    /**
     * 渲染模板内容 - 将变量替换到模板中
     */
    public String renderTemplate(String templateContent, Map<String, String> variables) {
        if (templateContent == null || variables == null) {
            return templateContent;
        }
        String result = templateContent;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
