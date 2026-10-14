package com.schemafy.core.project.adapter.out.persistence;

import java.time.Duration;
import java.util.Comparator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import com.schemafy.core.collaboration.dto.ProjectPresenceParticipant;
import com.schemafy.core.common.config.ConditionalOnRedisEnabled;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.project.application.port.out.ProjectPresenceReadPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class RedisProjectPresenceReadAdapter implements ProjectPresenceReadPort {

  private static final String PROJECT_KEY_PREFIX = "collaboration:presence:project:";

  private final ReactiveStringRedisTemplate redisTemplate;
  private final JsonCodec jsonCodec;

  @Value("${collaboration.presence.session-ttl:90s}")
  private Duration sessionTtl;

  @Override
  public Flux<ProjectPresenceParticipant> findParticipants(String projectId) {
    return redisTemplate.<String, String>opsForHash()
        .values(participantsKey(projectId))
        .flatMap(this::deserialize)
        .filter(this::isActive)
        .map(session -> new ProjectPresenceParticipant(
            session.sessionId(), session.userId(), session.userName(), null))
        .sort(Comparator.comparing(ProjectPresenceParticipant::sessionId));
  }

  private Flux<PresenceSession> deserialize(String payload) {
    return Flux.defer(() -> Flux.just(jsonCodec.fromJson(payload, PresenceSession.class)))
        .onErrorResume(error -> {
          log.warn("Ignoring invalid project presence payload: {}", error.getMessage());
          return Flux.empty();
        });
  }

  private boolean isActive(PresenceSession session) {
    return session.lastSeenAt() + sessionTtl.toMillis() > System.currentTimeMillis();
  }

  private String participantsKey(String projectId) {
    return PROJECT_KEY_PREFIX + projectId + ":participants";
  }

  private record PresenceSession(
      String sessionId,
      String userId,
      String userName,
      long joinedAt,
      long lastSeenAt) {
  }

}
