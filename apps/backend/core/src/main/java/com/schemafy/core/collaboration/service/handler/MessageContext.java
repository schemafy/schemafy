package com.schemafy.core.collaboration.service.handler;

public record MessageContext(
        String projectId,
        String sessionId) {

    public static MessageContext of(String projectId, String sessionId) {
        return new MessageContext(projectId, sessionId);
    }

}

