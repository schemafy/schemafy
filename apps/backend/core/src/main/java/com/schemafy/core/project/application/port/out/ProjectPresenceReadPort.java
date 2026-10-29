package com.schemafy.core.project.application.port.out;

import com.schemafy.core.collaboration.dto.ProjectPresenceParticipant;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProjectPresenceReadPort {

  Flux<ProjectPresenceParticipant> findParticipants(String projectId);

  Mono<ProjectPresenceParticipant> findSession(String projectId, String sessionId);

}
