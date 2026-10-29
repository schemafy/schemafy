package com.schemafy.core.collaboration.lock;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import com.schemafy.core.common.config.ConditionalOnRedisEnabled;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class RedisCanvasEditLockService implements CanvasEditLockService {

  private static final String KEY_PREFIX = "collaboration:canvas-edit-lock:";
  private static final RedisScript<String> ACQUIRE = RedisScript.of("""
      local owner = redis.call('GET', KEYS[1])
      if not owner then
        redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
        redis.call('SADD', KEYS[2], KEYS[1])
        redis.call('PEXPIRE', KEYS[2], ARGV[3])
        return 'ACQUIRED:' .. ARGV[1]
      end
      if owner == ARGV[1] then
        redis.call('PEXPIRE', KEYS[1], ARGV[2])
        redis.call('SADD', KEYS[2], KEYS[1])
        redis.call('PEXPIRE', KEYS[2], ARGV[3])
        return 'RENEWED:' .. owner
      end
      return 'REJECTED:' .. owner
      """, String.class);
  private static final RedisScript<String> RENEW = RedisScript.of("""
      local owner = redis.call('GET', KEYS[1])
      if owner == ARGV[1] then
        redis.call('PEXPIRE', KEYS[1], ARGV[2])
        redis.call('SADD', KEYS[2], KEYS[1])
        redis.call('PEXPIRE', KEYS[2], ARGV[3])
        return 'RENEWED:' .. owner
      end
      if owner then return 'REJECTED:' .. owner end
      return 'REJECTED:'
      """, String.class);
  private static final RedisScript<String> RELEASE = RedisScript.of("""
      local owner = redis.call('GET', KEYS[1])
      if owner == ARGV[1] then
        redis.call('DEL', KEYS[1])
        redis.call('SREM', KEYS[2], KEYS[1])
        return 'RELEASED:' .. owner
      end
      if owner then return 'REJECTED:' .. owner end
      return 'RELEASED:'
      """, String.class);

  private final ReactiveStringRedisTemplate redisTemplate;

  @Value("${collaboration.canvas-edit-lock.lease-ttl:10s}")
  private Duration leaseTtl;

  @Override
  public boolean isEnabled() { return true; }

  @Override
  public Mono<CanvasEditLockResult> acquire(String projectId, CanvasEditLockTarget target,
      String resourceId, String sessionId) {
    return execute(ACQUIRE, projectId, target, resourceId, sessionId);
  }

  @Override
  public Mono<CanvasEditLockResult> renew(String projectId, CanvasEditLockTarget target,
      String resourceId, String sessionId) {
    return execute(RENEW, projectId, target, resourceId, sessionId);
  }

  @Override
  public Mono<CanvasEditLockResult> release(String projectId, CanvasEditLockTarget target,
      String resourceId, String sessionId) {
    String lockKey = lockKey(projectId, target, resourceId);
    return redisTemplate.execute(RELEASE, List.of(lockKey, sessionKey(projectId, sessionId)),
        List.of(sessionId))
        .next()
        .map(this::toResult)
        .defaultIfEmpty(unavailable())
        .onErrorReturn(unavailable());
  }

  @Override
  public Mono<Void> releaseAll(String projectId, String sessionId) {
    String sessionKey = sessionKey(projectId, sessionId);
    return redisTemplate.opsForSet().members(sessionKey)
        .flatMap(lockKey -> releaseLockKey(lockKey, sessionKey, sessionId))
        .then(redisTemplate.delete(sessionKey))
        .then()
        .onErrorResume(error -> Mono.empty());
  }

  private Mono<CanvasEditLockResult> execute(RedisScript<String> script, String projectId,
      CanvasEditLockTarget target, String resourceId, String sessionId) {
    String lockKey = lockKey(projectId, target, resourceId);
    List<String> keys = List.of(lockKey, sessionKey(projectId, sessionId));
    return redisTemplate.execute(script, keys,
        List.of(sessionId, Long.toString(leaseTtl.toMillis()),
            Long.toString(leaseTtl.multipliedBy(2).toMillis())))
        .next()
        .map(this::toResult)
        .defaultIfEmpty(unavailable())
        .onErrorReturn(unavailable());
  }

  private Mono<Void> releaseLockKey(String lockKey, String sessionKey, String sessionId) {
    return redisTemplate.execute(RELEASE, List.of(lockKey, sessionKey), List.of(sessionId))
        .then();
  }

  private CanvasEditLockResult toResult(String raw) {
    String[] parts = raw.split(":", 2);
    CanvasEditLockState state = CanvasEditLockState.valueOf(parts[0]);
    String owner = parts.length == 2 && !parts[1].isBlank() ? parts[1] : null;
    return new CanvasEditLockResult(state, owner);
  }

  private CanvasEditLockResult unavailable() {
    return new CanvasEditLockResult(CanvasEditLockState.UNAVAILABLE, null);
  }

  private String lockKey(String projectId, CanvasEditLockTarget target, String resourceId) {
    return KEY_PREFIX + projectId + ":" + target + ":" + resourceId;
  }

  private String sessionKey(String projectId, String sessionId) {
    return KEY_PREFIX + "session:" + projectId + ":" + sessionId;
  }

}
