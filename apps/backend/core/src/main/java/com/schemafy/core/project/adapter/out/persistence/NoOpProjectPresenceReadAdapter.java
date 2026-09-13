package com.schemafy.core.project.adapter.out.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.schemafy.core.collaboration.dto.ProjectPresenceParticipant;
import com.schemafy.core.project.application.port.out.ProjectPresenceReadPort;

import reactor.core.publisher.Flux;

@Service
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpProjectPresenceReadAdapter implements ProjectPresenceReadPort {

  @Override
  public Flux<ProjectPresenceParticipant> findParticipants(String projectId) {
    return Flux.empty();
  }

}
