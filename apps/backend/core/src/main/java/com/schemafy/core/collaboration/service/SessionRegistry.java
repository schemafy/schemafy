package com.schemafy.core.collaboration.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.schemafy.core.collaboration.dto.BroadcastMessage;
import com.schemafy.core.collaboration.security.WebSocketAuthInfo;
import com.schemafy.core.collaboration.service.model.SessionEntry;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

@Slf4j
@Service
public class SessionRegistry {

    // projectId -> (sessionId -> SessionEntry)
    private final Map<String, Map<String, SessionEntry>> projectSessions = new ConcurrentHashMap<>();

    public SessionEntry addSession(String projectId, String sessionId,
            WebSocketSession session, WebSocketAuthInfo authInfo) {
        SessionEntry entry = new SessionEntry(session, authInfo);
        projectSessions
                .computeIfAbsent(projectId, k -> new ConcurrentHashMap<>())
                .put(sessionId, entry);
        log.info(
                "[SessionRegistry] Session added: projectId={}, sessionId={}, current session count={}",
                projectId, sessionId, getSessionCount(projectId));
        return entry;
    }

    public void removeSession(String projectId, String sessionId) {
        projectSessions.computeIfPresent(projectId, (pid, sessions) -> {
            SessionEntry entry = sessions.remove(sessionId);
            if (entry != null) {
                entry.complete();
            }
            return sessions.isEmpty() ? null : sessions;
        });
        log.info(
                "[SessionRegistry] Session removed: projectId={}, sessionId={}, current session count={}",
                projectId, sessionId, getSessionCount(projectId));
    }

    public void broadcast(BroadcastMessage request) {
        Map<String, SessionEntry> sessions = projectSessions
                .get(request.projectId());
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        sessions.forEach((sessionId, entry) -> {
            if (!sessionId.equals(request.excludeSessionId())
                    && entry.isOpen()) {
                Sinks.EmitResult result = entry.send(request.message());
                if (!result.isSuccess()) {
                    request.onFailure()
                            .ifPresent(consumer -> consumer.accept(result));
                }
            }
        });
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

    public Optional<SessionEntry> getSessionEntry(String projectId,
            String sessionId) {
        Map<String, SessionEntry> sessions = projectSessions.get(projectId);
        if (sessions == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(sessionId));
    }

}

