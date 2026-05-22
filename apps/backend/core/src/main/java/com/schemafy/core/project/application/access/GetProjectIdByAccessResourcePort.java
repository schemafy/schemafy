package com.schemafy.core.project.application.access;

import reactor.core.publisher.Mono;

public interface GetProjectIdByAccessResourcePort {

  Mono<String> findProjectId(ProjectAccessResourceType type, String id);

}
