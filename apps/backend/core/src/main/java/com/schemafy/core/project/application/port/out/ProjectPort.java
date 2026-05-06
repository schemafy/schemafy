package com.schemafy.core.project.application.port.out;

import com.schemafy.core.project.domain.Project;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProjectPort {

  Mono<Project> save(Project project);

  Mono<Project> findById(String projectId);

  Mono<Project> findByIdAndNotDeleted(String projectId);

  Flux<Project> findByWorkspaceIdAndNotDeleted(String workspaceId);

  Flux<Project> findByWorkspaceId(String workspaceId);

  Mono<Long> countByWorkspaceIdAndNotDeleted(String workspaceId);

  Flux<Project> findByWorkspaceIdAndUserIdWithPaging(
      String workspaceId,
      String userId,
      int limit,
      int offset);

  Flux<Project> findSharedByUserIdWithPaging(String userId, int limit,
      int offset);

}
