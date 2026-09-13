package com.schemafy.core.project.application.port.out;

import com.schemafy.core.collaboration.dto.ProjectPresenceParticipant;

import reactor.core.publisher.Flux;

public interface ProjectPresenceReadPort {

  Flux<ProjectPresenceParticipant> findParticipants(String projectId);

}
