package com.schemafy.core.project.application.access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
class ProjectAccessTargetRegistry {

  private final Map<ProjectAccessResourceType, List<ProjectAccessResourceResolver>> resolvers;

  ProjectAccessTargetRegistry(List<ProjectAccessResourceResolver> resourceResolvers) {
    Map<ProjectAccessResourceType, List<ProjectAccessResourceResolver>> resolverMap = new HashMap<>();

    for (ProjectAccessResourceResolver resolver : resourceResolvers) {
      for (ProjectAccessResourceType type : resolver.resourceTypes()) {
        resolverMap.computeIfAbsent(type, ignored -> new ArrayList<>())
            .add(resolver);
      }
    }

    this.resolvers = resolverMap.entrySet().stream()
        .collect(java.util.stream.Collectors.toUnmodifiableMap(
            Map.Entry::getKey,
            entry -> List.copyOf(entry.getValue())));
  }

  ProjectAccessResourceResolver resolver(ProjectAccessResourceType type) {
    List<ProjectAccessResourceResolver> matchingResolvers = resolvers.get(type);
    if (matchingResolvers == null || matchingResolvers.isEmpty()) {
      throw new IllegalStateException("Project access resource resolver is missing for type: " + type);
    }
    if (matchingResolvers.size() > 1) {
      throw new IllegalStateException(
          "Duplicate project access resource resolver for type: " + type);
    }
    return matchingResolvers.getFirst();
  }

}
