package com.maidc.msg.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.msg.entity.MessageEntity;
import com.maidc.msg.entity.MessageTemplateEntity;
import com.maidc.msg.mapper.MsgMapper;
import com.maidc.msg.repository.MessageRepository;
import com.maidc.msg.repository.MessageTemplateRepository;
import com.maidc.msg.vo.MessageVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageTemplateRepository templateRepository;

    @Mock
    private MsgMapper msgMapper;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageService messageService;

    @Test
    void sendTemplatedMessage_foundTemplate_rendersAndSends() {
        // Arrange
        MessageTemplateEntity template = new MessageTemplateEntity();
        template.setId(1L);
        template.setCode("MODEL_DEPLOYED");
        template.setTitleTemplate("模型 ${modelName} 部署完成");
        template.setContentTemplate("模型 ${modelName} 已成功部署到 ${envName} 环境。");
        template.setEventType("SYSTEM");

        when(templateRepository.findByCodeAndIsDeletedFalse("MODEL_DEPLOYED"))
                .thenReturn(Optional.of(template));

        // The underlying sendMessage calls messageRepository.save
        MessageEntity savedEntity = new MessageEntity();
        savedEntity.setId(100L);
        savedEntity.setUserId(42L);
        savedEntity.setTitle("模型 GPT-4 部署完成");
        savedEntity.setContent("模型 GPT-4 已成功部署到 生产 环境。");
        savedEntity.setType("SYSTEM");
        savedEntity.setIsRead(false);
        when(messageRepository.save(any(MessageEntity.class))).thenReturn(savedEntity);

        MessageVO expectedVo = MessageVO.builder()
                .id(100L)
                .userId(42L)
                .title("模型 GPT-4 部署完成")
                .content("模型 GPT-4 已成功部署到 生产 环境。")
                .type("SYSTEM")
                .isRead(false)
                .build();
        when(msgMapper.toMessageVO(any(MessageEntity.class))).thenReturn(expectedVo);

        Map<String, String> variables = Map.of(
                "modelName", "GPT-4",
                "envName", "生产"
        );

        // Act
        MessageVO result = messageService.sendTemplatedMessage(
                42L, "MODEL_DEPLOYED", variables, 999L, "DEPLOYMENT");

        // Assert - verify messageRepository.save was called with correctly rendered content
        ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(messageRepository).save(captor.capture());

        MessageEntity saved = captor.getValue();
        assertEquals("模型 GPT-4 部署完成", saved.getTitle());
        assertEquals("模型 GPT-4 已成功部署到 生产 环境。", saved.getContent());
        assertEquals("SYSTEM", saved.getType());
        assertEquals(42L, saved.getUserId());
        assertEquals(999L, saved.getBizId());
        assertEquals("DEPLOYMENT", saved.getBizType());

        // Verify the returned VO is not null
        assertNotNull(result);
        assertEquals(100L, result.getId());
    }

    @Test
    void sendTemplatedMessage_templateNotFound_throws() {
        // Arrange
        when(templateRepository.findByCodeAndIsDeletedFalse("NONEXISTENT"))
                .thenReturn(Optional.empty());

        Map<String, String> variables = Map.of();

        // Act & Assert
        assertThrows(BusinessException.class, () ->
                messageService.sendTemplatedMessage(1L, "NONEXISTENT", variables, null, null));

        // Verify no message was ever saved
        verify(messageRepository, never()).save(any());
    }
}
