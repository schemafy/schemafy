package com.schemafy.api.collaboration.service.presence;

import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import com.schemafy.api.common.config.ConditionalOnRedisEnabled;
import com.schemafy.core.common.json.JsonCodec;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class RedisProjectPresenceStore implements ProjectPresenceStore {

  private static final String ACTIVE_PROJECTS_KEY = "collaboration:presence:projects";
  private static final String PROJECT_KEY_PREFIX = "collaboration:presence:project:";
  private static final RedisScript<String> REMOVE_EXPIRED_SESSION_SCRIPT = RedisScript
      .of("""
          local participantsKey = KEYS[1]
          local expiresKey = KEYS[2]
          local sessionId = ARGV[1]
          local now = tonumber(ARGV[2])
          local score = redis.call('ZSCORE', expiresKey, sessionId)

          if not score or tonumber(score) > now then
            return nil
          end

          local payload = redis.call('HGET', participantsKey, sessionId)
          redis.call('HDEL', participantsKey, sessionId)
          redis.call('ZREM', expiresKey, sessionId)
          return payload
          """, String.class);
  private static final RedisScript<Long> CLEANUP_EMPTY_PROJECT_SCRIPT = RedisScript
      .of("""
          local activeProjectsKey = KEYS[1]
          local participantsKey = KEYS[2]
          local expiresKey = KEYS[3]
          local projectId = ARGV[1]

          if redis.call('HLEN', participantsKey) > 0 then
            return 0
          end

          redis.call('SREM', activeProjectsKey, projectId)
          redis.call('DEL', participantsKey, expiresKey)
          return 1
          """, Long.class);

  private final ReactiveStringRedisTemplate redisTemplate;
  private final JsonCodec jsonCodec;
  private final CollaborationPresenceProperties properties;

  @Override
  public Mono<ProjectPresenceSession> register(String projectId,
      String sessionId, String userId, String userName) {
    long now = now();

    return findSession(projectId, sessionId)
        .defaultIfEmpty(new ProjectPresenceSession(sessionId, userId,
            userName, now, now))
        .map(existing -> new ProjectPresenceSession(
            sessionId,
            userId,
            userName,
            existing.joinedAt(),
            now))
        .flatMap(session -> writeSession(projectId, session));
  }

  @Override
  public Mono<ProjectPresenceSession> refresh(String projectId,
      String sessionId) {
    long now = now();

    return findSession(projectId, sessionId)
        .map(existing -> new ProjectPresenceSession(
            existing.sessionId(),
            existing.userId(),
            existing.userName(),
            existing.joinedAt(),
            now))
        .flatMap(session -> writeSession(projectId, session));
  }

  @Override
  public Mono<ProjectPresenceSession> remove(String projectId,
      String sessionId) {
    return findSession(projectId, sessionId)
        .flatMap(presenceSession -> deleteSession(projectId, sessionId)
            .thenReturn(presenceSession))
        .switchIfEmpty(deleteSession(projectId, sessionId)
            .then(Mono.<ProjectPresenceSession>empty()));
  }

  @Override
  public Flux<ProjectPresenceSession> findParticipants(String projectId) {
    return redisTemplate.<String, String>opsForHash()
        .values(participantsKey(projectId))
        .flatMap(this::deserializeSession)
        .sort(Comparator
            .comparingLong(ProjectPresenceSession::joinedAt)
            .thenComparing(ProjectPresenceSession::sessionId));
  }

  @Override
  public Flux<String> findActiveProjectIds() {
    return redisTemplate.opsForSet()
        .members(ACTIVE_PROJECTS_KEY);
  }

  @Override
  public Flux<ProjectPresenceSession> removeExpired(String projectId) {
    long now = now();
    Range<Double> expiredRange = Range.closed(0.0, (double) now);

    return redisTemplate.opsForZSet()
        .rangeByScore(expiresKey(projectId), expiredRange)
        .collectList()
        .flatMapMany(sessionIds -> {
          if (sessionIds.isEmpty()) {
            return cleanupProjectIfEmpty(projectId).thenMany(Flux.empty());
          }
          return Flux.fromIterable(sessionIds)
              .flatMap(sessionId -> removeExpiredSession(projectId, sessionId,
                  now));
        });
  }

  private Mono<ProjectPresenceSession> removeExpiredSession(String projectId,
      String sessionId, long now) {
    return redisTemplate.execute(REMOVE_EXPIRED_SESSION_SCRIPT,
        List.of(participantsKey(projectId), expiresKey(projectId)),
        List.of(sessionId, Long.toString(now)))
        .next()
        .flatMap(this::deserializeSession)
        .flatMap(presenceSession -> cleanupProjectIfEmpty(projectId)
            .thenReturn(presenceSession));
  }

  private Mono<ProjectPresenceSession> findSession(String projectId,
      String sessionId) {
    return redisTemplate.<String, String>opsForHash()
        .get(participantsKey(projectId), sessionId)
        .flatMap(this::deserializeSession);
  }

  private Mono<ProjectPresenceSession> writeSession(String projectId,
      ProjectPresenceSession presenceSession) {
    return serializeSession(presenceSession)
        .flatMap(payload -> redisTemplate.<String, String>opsForHash()
            .put(participantsKey(projectId), presenceSession.sessionId(),
                payload)
            .then(redisTemplate.opsForZSet()
                .add(expiresKey(projectId), presenceSession.sessionId(),
                    expiresAt(presenceSession.lastSeenAt())))
            .then(redisTemplate.opsForSet()
                .add(ACTIVE_PROJECTS_KEY, projectId))
            .thenReturn(presenceSession));
  }

  private Mono<Void> deleteSession(String projectId, String sessionId) {
    return Mono.when(
        redisTemplate.<String, String>opsForHash()
            .remove(participantsKey(projectId), sessionId),
        redisTemplate.opsForZSet()
            .remove(expiresKey(projectId), sessionId))
        .then(cleanupProjectIfEmpty(projectId));
  }

  private Mono<Void> cleanupProjectIfEmpty(String projectId) {
    return redisTemplate.execute(CLEANUP_EMPTY_PROJECT_SCRIPT,
        List.of(ACTIVE_PROJECTS_KEY, participantsKey(projectId),
            expiresKey(projectId)),
        List.of(projectId))
        .then();
  }

  private Mono<String> serializeSession(
      ProjectPresenceSession presenceSession) {
    return Mono.fromCallable(() -> jsonCodec.serialize(presenceSession))
        .onErrorMap(IllegalArgumentException.class,
            e -> new RuntimeException(
                "[RedisProjectPresenceStore] failed to serialize presence session",
                e));
  }

  private Mono<ProjectPresenceSession> deserializeSession(
      String payload) {
    return Mono.fromCallable(
        () -> jsonCodec.parse(payload, ProjectPresenceSession.class))
        .onErrorResume(error -> {
          log.warn(
              "[RedisProjectPresenceStore] Ignoring invalid presence session payload: {}",
              error.getMessage());
          return Mono.empty();
        });
  }

  private String participantsKey(String projectId) {
    return PROJECT_KEY_PREFIX + projectId + ":participants";
  }

  private String expiresKey(String projectId) {
    return PROJECT_KEY_PREFIX + projectId + ":expires";
  }

  private double expiresAt(long lastSeenAt) {
    return lastSeenAt + properties.getSessionTtl().toMillis();
  }

  private long now() {
    return System.currentTimeMillis();
  }

}
