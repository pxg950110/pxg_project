package com.maidc.msg.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private MessageTemplateRepository messageTemplateRepository;

    @Mock
    private MsgMapper msgMapper;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void getSettings_returnsUserSettings() {
        // Arrange
        Long userId = 1L;

        NotificationSettingEntity entity1 = new NotificationSettingEntity();
        entity1.setId(10L);
        entity1.setUserId(userId);
        entity1.setChannel("EMAIL");
        entity1.setEventType("TASK_COMPLETE");
        entity1.setEnabled(true);

        NotificationSettingEntity entity2 = new NotificationSettingEntity();
        entity2.setId(11L);
        entity2.setUserId(userId);
        entity2.setChannel("SMS");
        entity2.setEventType("SYSTEM_ALERT");
        entity2.setEnabled(false);

        List<NotificationSettingEntity> entities = List.of(entity1, entity2);

        NotificationSettingVO vo1 = NotificationSettingVO.builder()
                .id(10L).userId(userId).channel("EMAIL").eventType("TASK_COMPLETE").enabled(true).build();
        NotificationSettingVO vo2 = NotificationSettingVO.builder()
                .id(11L).userId(userId).channel("SMS").eventType("SYSTEM_ALERT").enabled(false).build();

        when(notificationSettingRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(entities);
        when(msgMapper.toNotificationSettingVOList(entities)).thenReturn(List.of(vo1, vo2));

        // Act
        List<NotificationSettingVO> result = notificationService.getSettings(userId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("EMAIL", result.get(0).getChannel());
        assertEquals("SMS", result.get(1).getChannel());
        verify(notificationSettingRepository).findByUserIdAndIsDeletedFalse(userId);
    }

    @Test
    void updateSetting_existingSetting_updatesAndReturns() {
        // Arrange
        Long userId = 1L;
        Long settingId = 10L;

        NotificationSettingEntity entity = new NotificationSettingEntity();
        entity.setId(settingId);
        entity.setUserId(userId);
        entity.setChannel("EMAIL");
        entity.setEventType("TASK_COMPLETE");
        entity.setEnabled(true);

        NotificationSettingDTO dto = NotificationSettingDTO.builder()
                .channel("SMS")
                .enabled(false)
                .build();

        NotificationSettingVO expectedVO = NotificationSettingVO.builder()
                .id(settingId)
                .userId(userId)
                .channel("SMS")
                .eventType("TASK_COMPLETE")
                .enabled(false)
                .build();

        when(notificationSettingRepository.findByIdAndUserIdAndIsDeletedFalse(settingId, userId))
                .thenReturn(Optional.of(entity));
        when(notificationSettingRepository.save(any(NotificationSettingEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(msgMapper.toNotificationSettingVO(any(NotificationSettingEntity.class))).thenReturn(expectedVO);

        // Act
        NotificationSettingVO result = notificationService.updateSetting(userId, settingId, dto);

        // Assert
        assertNotNull(result);
        assertEquals("SMS", result.getChannel());
        assertEquals(false, result.getEnabled());
        verify(notificationSettingRepository).save(argThat(e ->
                "SMS".equals(e.getChannel()) && Boolean.FALSE.equals(e.getEnabled())
        ));
    }

    @Test
    void updateSetting_nonExistingSetting_throwsNotFound() {
        // Arrange
        Long userId = 1L;
        Long settingId = 999L;

        NotificationSettingDTO dto = NotificationSettingDTO.builder()
                .channel("EMAIL")
                .eventType("TASK_COMPLETE")
                .build();

        when(notificationSettingRepository.findByIdAndUserIdAndIsDeletedFalse(settingId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BusinessException.class,
                () -> notificationService.updateSetting(userId, settingId, dto));
        verify(notificationSettingRepository, never()).save(any());
    }

    @Test
    void createTemplate_uniqueCode_savesSuccessfully() {
        // Arrange
        TemplateCreateDTO dto = TemplateCreateDTO.builder()
                .code("TASK_COMPLETE_NOTIFY")
                .titleTemplate("任务${taskName}已完成")
                .contentTemplate("任务${taskName}已于${completedAt}完成，共处理${recordCount}条记录。")
                .channel("EMAIL")
                .eventType("TASK_COMPLETE")
                .build();

        when(messageTemplateRepository.existsByCodeAndIsDeletedFalse("TASK_COMPLETE_NOTIFY")).thenReturn(false);
        when(messageTemplateRepository.save(any(MessageTemplateEntity.class))).thenAnswer(inv -> {
            MessageTemplateEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        TemplateVO expectedVO = TemplateVO.builder()
                .id(1L)
                .code("TASK_COMPLETE_NOTIFY")
                .titleTemplate("任务${taskName}已完成")
                .contentTemplate("任务${taskName}已于${completedAt}完成，共处理${recordCount}条记录。")
                .channel("EMAIL")
                .eventType("TASK_COMPLETE")
                .build();

        when(msgMapper.toTemplateVO(any(MessageTemplateEntity.class))).thenReturn(expectedVO);

        // Act
        TemplateVO result = notificationService.createTemplate(dto);

        // Assert
        assertNotNull(result);
        assertEquals("TASK_COMPLETE_NOTIFY", result.getCode());
        assertEquals("EMAIL", result.getChannel());
        verify(messageTemplateRepository).save(argThat(e ->
                "TASK_COMPLETE_NOTIFY".equals(e.getCode())
                && "任务${taskName}已完成".equals(e.getTitleTemplate())
        ));
    }

    @Test
    void createTemplate_duplicateCode_throwsConflict() {
        // Arrange
        TemplateCreateDTO dto = TemplateCreateDTO.builder()
                .code("EXISTING_CODE")
                .titleTemplate("标题")
                .contentTemplate("内容")
                .build();

        when(messageTemplateRepository.existsByCodeAndIsDeletedFalse("EXISTING_CODE")).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.createTemplate(dto));

        assertEquals(409, exception.getCode());
        assertTrue(exception.getMessage().contains("EXISTING_CODE"));
        verify(messageTemplateRepository, never()).save(any());
    }

    @Test
    void renderTemplate_replacesVariables() {
        // Arrange
        String template = "任务${taskName}已于${completedAt}完成";
        Map<String, String> variables = Map.of(
                "taskName", "数据导入",
                "completedAt", "2025-03-15 10:30"
        );

        // Act
        String result = notificationService.renderTemplate(template, variables);

        // Assert
        assertEquals("任务数据导入已于2025-03-15 10:30完成", result);
    }

    @Test
    void renderTemplate_nullInputs_returnsTemplate() {
        // Act & Assert
        assertEquals("template", notificationService.renderTemplate("template", null));
        assertNull(notificationService.renderTemplate(null, Map.of("key", "value")));
    }

    @Test
    void getTemplates_returnsAllTemplates() {
        // Arrange
        MessageTemplateEntity entity1 = new MessageTemplateEntity();
        entity1.setId(1L);
        entity1.setCode("TPL_001");
        entity1.setTitleTemplate("模板1");

        MessageTemplateEntity entity2 = new MessageTemplateEntity();
        entity2.setId(2L);
        entity2.setCode("TPL_002");
        entity2.setTitleTemplate("模板2");

        TemplateVO vo1 = TemplateVO.builder().id(1L).code("TPL_001").titleTemplate("模板1").build();
        TemplateVO vo2 = TemplateVO.builder().id(2L).code("TPL_002").titleTemplate("模板2").build();

        when(messageTemplateRepository.findByIsDeletedFalse()).thenReturn(List.of(entity1, entity2));
        when(msgMapper.toTemplateVOList(List.of(entity1, entity2))).thenReturn(List.of(vo1, vo2));

        // Act
        List<TemplateVO> result = notificationService.getTemplates();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("TPL_001", result.get(0).getCode());
        assertEquals("TPL_002", result.get(1).getCode());
        verify(messageTemplateRepository).findByIsDeletedFalse();
    }
}
