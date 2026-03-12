package com.schemafy.core.project.application.port.in;

import com.schemafy.core.project.domain.Project;

import reactor.core.publisher.Mono;

public interface AccessShareLinkUseCase {

  Mono<Project> accessShareLink(AccessShareLinkQuery query);

}
