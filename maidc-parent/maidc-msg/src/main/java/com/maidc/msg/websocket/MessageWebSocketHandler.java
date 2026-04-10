package com.maidc.msg.websocket;

import com.maidc.msg.vo.UnreadCountVO;
import com.maidc.msg.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageWebSocketHandler {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 在线用户集合
     */
    private final Set<String> connectedUsers = ConcurrentHashMap.newKeySet();

    /**
     * WebSocket连接事件 - 推送未读消息数
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        Principal user = event.getUser();
        if (user != null) {
            String userId = user.getName();
            connectedUsers.add(userId);
            log.info("WebSocket用户连接: userId={}, 在线人数={}", userId, connectedUsers.size());

            // 连接后立即推送未读消息数
            try {
                UnreadCountVO unreadCount = messageService.getUnreadCount(Long.parseLong(userId));
                messagingTemplate.convertAndSendToUser(
                        userId,
                        "/queue/unread-count",
                        unreadCount
                );
            } catch (NumberFormatException e) {
                log.warn("无法解析用户ID: {}", userId);
            }
        }
    }

    /**
     * WebSocket断开事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        if (user != null) {
            String userId = user.getName();
            connectedUsers.remove(userId);
            log.info("WebSocket用户断开: userId={}, 在线人数={}", userId, connectedUsers.size());
        }
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineCount() {
        return connectedUsers.size();
    }

    /**
     * 判断用户是否在线
     */
    public boolean isUserOnline(String userId) {
        return connectedUsers.contains(userId);
    }
}
