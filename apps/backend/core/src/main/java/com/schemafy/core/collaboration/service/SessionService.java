package com.schemafy.core.collaboration.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SessionService {

    // projectId -> (sessionId -> WebSocketSession)
    private final Map<String, Map<String, WebSocketSession>> projectSessions = new ConcurrentHashMap<>();

    public void addSession(String projectId, String sessionId,
            WebSocketSession session) {
        projectSessions
                .computeIfAbsent(projectId, k -> new ConcurrentHashMap<>())
                .put(sessionId, session);
        log.info("[SessionService] Session added: projectId={}, sessionId={}, current session count={}",
                projectId, sessionId, getSessionCount(projectId));
    }

    public void removeSession(String projectId, String sessionId) {
        Map<String, WebSocketSession> sessions = projectSessions.get(projectId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                projectSessions.remove(projectId);
            }
            log.info("[SessionService] Session removed: projectId={}, sessionId={}, current session count={}",
                    projectId, sessionId, getSessionCount(projectId));
        }
    }

    public Mono<Void> broadcast(String projectId, String excludeSessionId,
            String message) {
        Map<String, WebSocketSession> sessions = projectSessions.get(projectId);
        if (sessions == null || sessions.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(sessions.entrySet())
                .filter(entry -> !entry.getKey().equals(excludeSessionId))
                .filter(entry -> entry.getValue().isOpen())
                .flatMap(entry -> {
                    WebSocketSession session = entry.getValue();
                    return session.send(Mono.just(session.textMessage(message)))
                            .onErrorResume(e -> {
                                log.warn("[SessionService] Message send failed: sessionId={}, error={}",
                                        entry.getKey(), e.getMessage());
                                return Mono.empty();
                            });
                })
                .then();
    }

    public Mono<Void> broadcastAll(String projectId, String message) {
        return broadcast(projectId, null, message);
    }

    public int getSessionCount(String projectId) {
        Map<String, WebSocketSession> sessions = projectSessions.get(projectId);
        return sessions != null ? sessions.size() : 0;
    }

}
