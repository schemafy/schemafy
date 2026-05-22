package com.schemafy.core.project.application.access;

import java.util.List;
import java.util.Set;

import reactor.core.publisher.Mono;

interface ProjectAccessResourceResolver {

  Set<ProjectAccessResourceType> resourceTypes();

  List<ProjectAccessAccessorRule> accessorRules();

  Mono<ProjectAccessResourceRef> resolveParent(ProjectAccessResourceType type, String id);

}
