package com.schemafy.core.project.application.access;

import java.util.Set;

import reactor.core.publisher.Mono;

interface ProjectAccessResourceResolver {

  Set<ProjectAccessResourceType> resourceTypes();

  Mono<ProjectAccessResourceRef> resolveParent(ProjectAccessResourceType type, String id);

}
