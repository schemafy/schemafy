package com.schemafy.domain.project.application.port.out;

import com.schemafy.domain.project.domain.Workspace;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorkspacePort {

  Mono<Workspace> save(Workspace workspace);

  Mono<Workspace> findByIdAndNotDeleted(String workspaceId);

  Flux<Workspace> findByUserIdWithPaging(String userId, int limit, int offset);

  Mono<Long> countByUserId(String userId);

}
