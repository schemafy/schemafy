package com.schemafy.core.collaboration.lock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpCanvasEditLockService implements CanvasEditLockService {

  private static final CanvasEditLockResult UNAVAILABLE = new CanvasEditLockResult(
      CanvasEditLockState.UNAVAILABLE, null);

  @Override
  public boolean isEnabled() { return false; }

  @Override
  public Mono<CanvasEditLockResult> acquire(String projectId, CanvasEditLockTarget target,
      String resourceId, String sessionId) {
    return Mono.just(UNAVAILABLE);
  }

  @Override
  public Mono<CanvasEditLockResult> renew(String projectId, CanvasEditLockTarget target,
      String resourceId, String sessionId) {
    return Mono.just(UNAVAILABLE);
  }

  @Override
  public Mono<CanvasEditLockResult> release(String projectId, CanvasEditLockTarget target,
      String resourceId, String sessionId) {
    return Mono.just(UNAVAILABLE);
  }

  @Override
  public Mono<Void> releaseAll(String projectId, String sessionId) {
    return Mono.empty();
  }

}
