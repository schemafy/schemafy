package com.schemafy.core.project.adapter.out.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.schemafy.core.collaboration.dto.ProjectPresenceParticipant;
import com.schemafy.core.project.application.port.out.ProjectPresenceReadPort;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpProjectPresenceReadAdapter implements ProjectPresenceReadPort {

  @Override
  public Flux<ProjectPresenceParticipant> findParticipants(String projectId) {
    return Flux.empty();
  }

  @Override
  public Mono<ProjectPresenceParticipant> findSession(String projectId, String sessionId) {
    return Mono.empty();
  }

}
