package com.schemafy.core.collaboration.lock;

import reactor.core.publisher.Mono;

public interface CanvasEditLockService {

  boolean isEnabled();

  Mono<CanvasEditLockResult> acquire(String projectId, CanvasEditLockTarget target,
      String resourceId, String sessionId);

  Mono<CanvasEditLockResult> renew(String projectId, CanvasEditLockTarget target,
      String resourceId, String sessionId);

  Mono<CanvasEditLockResult> release(String projectId, CanvasEditLockTarget target,
      String resourceId, String sessionId);

  Mono<Void> releaseAll(String projectId, String sessionId);

}
