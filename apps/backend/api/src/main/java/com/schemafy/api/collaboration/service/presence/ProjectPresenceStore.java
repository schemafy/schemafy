package com.schemafy.api.collaboration.service.presence;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProjectPresenceStore {

  Mono<ProjectPresenceSession> register(String projectId, String sessionId,
      String userId, String userName);

  Mono<ProjectPresenceSession> refresh(String projectId, String sessionId);

  Mono<ProjectPresenceSession> remove(String projectId, String sessionId);

  Flux<ProjectPresenceSession> findParticipants(String projectId);

  Flux<String> findActiveProjectIds();

  Flux<ProjectPresenceSession> removeExpired(String projectId);

}
