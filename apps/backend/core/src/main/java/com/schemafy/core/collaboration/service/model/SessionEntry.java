package com.schemafy.core.collaboration.service.model;

import org.springframework.web.reactive.socket.WebSocketSession;

import com.schemafy.core.collaboration.security.WebSocketAuthInfo;

public record SessionEntry(WebSocketSession session,
        WebSocketAuthInfo authInfo) {

}
