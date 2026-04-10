package com.maidc.msg.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import com.maidc.msg.entity.MessageEntity;
import com.maidc.msg.mapper.MsgMapper;
import com.maidc.msg.repository.MessageRepository;
import com.maidc.msg.vo.MessageVO;
import com.maidc.msg.vo.UnreadCountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final MsgMapper msgMapper;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 分页查询用户消息列表
     */
    public PageResult<MessageVO> listMessages(Long userId, String type, Boolean isRead, int page, int pageSize) {
        Page<MessageEntity> result = messageRepository.findByUserIdWithFilters(
                userId, type, isRead, PageRequest.of(page - 1, pageSize));
        return PageResult.of(result.map(msgMapper::toMessageVO));
    }

    /**
     * 标记单条消息为已读
     */
    @Transactional
    public void markAsRead(Long messageId) {
        MessageEntity message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!message.getIsRead()) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
            messageRepository.save(message);
            log.info("消息已标记为已读: id={}", messageId);
        }
    }

    /**
     * 标记用户所有消息为已读
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        messageRepository.markAllAsReadByUserId(userId);
        log.info("用户所有消息已标记为已读: userId={}", userId);
    }

    /**
     * 获取用户未读消息数
     */
    public UnreadCountVO getUnreadCount(Long userId) {
        long count = messageRepository.countByUserIdAndIsReadFalseAndIsDeletedFalse(userId);
        return UnreadCountVO.builder().count(count).build();
    }

    /**
     * 发送站内消息（内部方法，供MQ消费者和其他服务调用）
     */
    @Transactional
    public MessageVO sendMessage(Long userId, String title, String content, String type,
                                  Long bizId, String bizType) {
        MessageEntity message = new MessageEntity();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setIsRead(false);
        message.setBizId(bizId);
        message.setBizType(bizType);

        message = messageRepository.save(message);
        log.info("消息创建成功: id={}, userId={}, type={}", message.getId(), userId, type);

        // 通过WebSocket实时推送通知给用户
        MessageVO vo = msgMapper.toMessageVO(message);
        pushToUser(userId, vo);

        return vo;
    }

    /**
     * 基于模板发送消息
     */
    @Transactional
    public MessageVO sendTemplatedMessage(Long userId, String templateCode,
                                           Map<String, String> variables,
                                           Long bizId, String bizType) {
        // 模板查询和变量替换由 NotificationService 协同处理
        // 此处直接构建消息
        log.info("基于模板发送消息: userId={}, templateCode={}", userId, templateCode);
        // 实际模板渲染在 NotificationService 中完成
        return null;
    }

    /**
     * 通过WebSocket向指定用户推送消息
     */
    private void pushToUser(Long userId, MessageVO messageVO) {
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/messages",
                    messageVO
            );
            log.debug("WebSocket推送消息: userId={}", userId);
        } catch (Exception e) {
            log.warn("WebSocket推送失败: userId={}, error={}", userId, e.getMessage());
        }
    }
}
