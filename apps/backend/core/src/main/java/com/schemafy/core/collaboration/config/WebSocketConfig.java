package com.schemafy.core.collaboration.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import com.schemafy.core.collaboration.handler.CollaborationWebSocketHandler;

@Configuration
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketConfig {

    @Bean
    public HandlerMapping webSocketHandlerMapping(
            CollaborationWebSocketHandler handler) {
        // /ws/collaboration?projectId={projectId}&token={accessToken}
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/collaboration", handler);

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

}
