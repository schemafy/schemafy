package com.schemafy.core.project.application.port.out;

import com.schemafy.core.project.domain.Workspace;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorkspacePort {

  Mono<Workspace> save(Workspace workspace);

  Mono<Workspace> findByIdAndNotDeleted(String workspaceId);

  Mono<Workspace> findByIdAndNotDeletedForUpdate(String workspaceId);

  Flux<Workspace> findByUserIdWithPaging(String userId, int limit, int offset);

  Mono<Long> countByUserId(String userId);

}
