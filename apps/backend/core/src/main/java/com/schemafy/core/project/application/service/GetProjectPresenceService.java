package com.schemafy.core.project.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.core.collaboration.dto.ProjectPresenceParticipant;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.GetProjectPresenceQuery;
import com.schemafy.core.project.application.port.in.GetProjectPresenceUseCase;
import com.schemafy.core.project.application.port.out.ProjectPresenceReadPort;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetProjectPresenceService implements GetProjectPresenceUseCase {

  private final ProjectPresenceReadPort projectPresenceReadPort;

  @Override
  @RequireProjectAccess(role = ProjectRole.VIEWER)
  public Mono<List<ProjectPresenceParticipant>> getProjectPresence(
      GetProjectPresenceQuery query) {
    return projectPresenceReadPort.findParticipants(query.projectId())
        .collectList();
  }

}
