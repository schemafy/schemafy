package com.schemafy.core.collaboration.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.schemafy.core.collaboration.security.WebSocketAuthInfo;
import com.schemafy.core.collaboration.service.model.SessionEntry;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class SessionService {

    // projectId -> (sessionId -> SessionEntry)
    private final Map<String, Map<String, SessionEntry>> projectSessions = new ConcurrentHashMap<>();

    public void addSession(String projectId, String sessionId,
            WebSocketSession session, WebSocketAuthInfo authInfo) {
        projectSessions
                .computeIfAbsent(projectId, k -> new ConcurrentHashMap<>())
                .put(sessionId, new SessionEntry(session, authInfo));
        log.info(
                "[SessionService] Session added: projectId={}, sessionId={}, current session count={}",
                projectId, sessionId, getSessionCount(projectId));
    }

    public void removeSession(String projectId, String sessionId) {
        projectSessions.computeIfPresent(projectId, (pid, sessions) -> {
            sessions.remove(sessionId);
            return sessions.isEmpty() ? null : sessions;
        });
        log.info(
                "[SessionService] Session removed: projectId={}, sessionId={}, current session count={}",
                projectId, sessionId, getSessionCount(projectId));
    }

    public Mono<Void> broadcast(String projectId, String excludeSessionId,
            String message) {
        Map<String, SessionEntry> sessions = projectSessions.get(projectId);
        if (sessions == null || sessions.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(sessions.entrySet())
                .filter(entry -> !entry.getKey().equals(excludeSessionId))
                .filter(entry -> entry.getValue().session().isOpen())
                .flatMap(entry -> {
                    WebSocketSession session = entry.getValue().session();
                    return session.send(Mono.just(session.textMessage(message)))
                            .onErrorResume(e -> {
                                log.warn(
                                        "[SessionService] Message send failed: sessionId={}, error={}",
                                        entry.getKey(), e.getMessage());
                                return Mono.empty();
                            });
                })
                .then();
    }

    public int getSessionCount(String projectId) {
        Map<String, SessionEntry> sessions = projectSessions.get(projectId);
        return sessions != null ? sessions.size() : 0;
    }

    public Optional<WebSocketAuthInfo> getAuthInfo(String sessionId) {
        return projectSessions.values()
                .stream()
                .map(map -> map.get(sessionId))
                .filter(entry -> entry != null)
                .findFirst()
                .map(SessionEntry::authInfo);
    }

}
