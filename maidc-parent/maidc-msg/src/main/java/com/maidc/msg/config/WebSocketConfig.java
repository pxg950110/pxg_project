package com.maidc.msg.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 服务端推送目的地前缀
        registry.enableSimpleBroker("/queue", "/topic");
        // 客户端发送消息目的地前缀
        registry.setApplicationDestinationPrefixes("/app");
        // 用户点对点消息前缀
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        // 同时提供纯WebSocket端点（供非浏览器客户端使用）
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
