package com.schemafy.api.collaboration.security;

import org.springframework.stereotype.Component;

import com.schemafy.api.common.config.ConditionalOnRedisEnabled;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.access.AccessVerifier;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class ProjectAccessValidator {

  private final ProjectPort projectPort;
  private final AccessVerifier accessVerifier;

  public Mono<Boolean> canAccess(String projectId, String userId) {
    return accessVerifier.requireProjectAccess(projectId, userId,
        ProjectRole.VIEWER)
        .then(Mono.defer(() -> projectPort.findByIdAndNotDeleted(projectId)
            .hasElement()))
        .onErrorResume(
            DomainException.hasErrorCode(ProjectErrorCode.ACCESS_DENIED),
            error -> Mono.just(false));
  }

}
