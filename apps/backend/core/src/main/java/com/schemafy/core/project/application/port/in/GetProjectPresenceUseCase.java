package com.schemafy.core.project.application.port.in;

import java.util.List;

import com.schemafy.core.collaboration.dto.ProjectPresenceParticipant;

import reactor.core.publisher.Mono;

public interface GetProjectPresenceUseCase {

  Mono<List<ProjectPresenceParticipant>> getProjectPresence(
      GetProjectPresenceQuery query);

}
