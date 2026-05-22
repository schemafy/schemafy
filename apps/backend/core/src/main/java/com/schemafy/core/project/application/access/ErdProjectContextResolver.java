package com.schemafy.core.project.application.access;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class ErdProjectContextResolver {

  private final ProjectAccessResourceRegistry registry;

  Mono<String> resolveProjectId(ProjectAccessResourceType type, String id) {
    return resolveProjectId(type, id, new HashMap<>());
  }

  Mono<String> resolveProjectId(
      ProjectAccessResourceType type,
      String id,
      Map<ProjectAccessResourceRef, Mono<String>> cache) {
    return resolveProjectId(type, id, new HashSet<>(), cache);
  }

  private Mono<String> resolveProjectId(
      ProjectAccessResourceType type,
      String id,
      Set<ProjectAccessResourceRef> visited,
      Map<ProjectAccessResourceRef, Mono<String>> cache) {
    if (type == ProjectAccessResourceType.PROJECT) {
      return Mono.just(id);
    }

    ProjectAccessResourceRef current = new ProjectAccessResourceRef(type, id);
    if (!visited.add(current)) {
      return Mono.error(new IllegalStateException(
          "Project access resource graph contains a cycle at " + type + ":" + id));
    }

    return cache.computeIfAbsent(current, ignored -> registry.resolver(type)
        .resolveParent(type, id)
        .flatMap(parent -> resolveProjectId(
            parent.type(),
            parent.id(),
            new HashSet<>(visited),
            cache))
        .cache());
  }

}
