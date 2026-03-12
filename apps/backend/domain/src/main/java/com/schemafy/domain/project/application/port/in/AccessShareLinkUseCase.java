package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.project.domain.Project;

import reactor.core.publisher.Mono;

public interface AccessShareLinkUseCase {

  Mono<Project> accessShareLink(AccessShareLinkQuery query);

}
