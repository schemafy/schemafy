package com.schemafy.core.collaboration.dto;

import java.util.Optional;
import java.util.function.Consumer;

import reactor.core.publisher.Sinks;

public record BroadcastMessage(
        String projectId,
        String excludeSessionId,
        String message,
        Optional<Consumer<Sinks.EmitResult>> onFailure) {

    public static BroadcastMessage of(String projectId, String excludeSessionId,
            String message) {
        return new BroadcastMessage(projectId, excludeSessionId, message,
                Optional.empty());
    }

    public static BroadcastMessage of(String projectId, String excludeSessionId,
            String message, Consumer<Sinks.EmitResult> onFailure) {
        return new BroadcastMessage(projectId, excludeSessionId, message,
                Optional.ofNullable(onFailure));
    }

}

