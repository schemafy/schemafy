package com.schemafy.core.project.application.access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
class ProjectAccessResourceRegistry {

  private final Map<ProjectAccessResourceType, List<ProjectAccessResourceResolver>> resolvers;
  private final Map<String, ProjectAccessResourceType> accessorTypes;
  private final Set<String> ambiguousAccessors;

  ProjectAccessResourceRegistry(List<ProjectAccessResourceResolver> resourceResolvers) {
    Map<ProjectAccessResourceType, List<ProjectAccessResourceResolver>> resolverMap = new HashMap<>();
    Map<String, ProjectAccessResourceType> accessorTypeMap = new HashMap<>();
    Set<String> ambiguousAccessorSet = new HashSet<>();
    accessorTypeMap.put("projectId", ProjectAccessResourceType.PROJECT);

    for (ProjectAccessResourceResolver resolver : resourceResolvers) {
      for (ProjectAccessResourceType type : resolver.resourceTypes()) {
        resolverMap.computeIfAbsent(type, ignored -> new ArrayList<>())
            .add(resolver);
      }
      for (ProjectAccessAccessorRule rule : resolver.accessorRules()) {
        ProjectAccessResourceType previous = accessorTypeMap.putIfAbsent(
            rule.accessorName(),
            rule.type());
        if (previous != null && previous != rule.type()) {
          ambiguousAccessorSet.add(rule.accessorName());
        }
      }
    }

    this.resolvers = resolverMap.entrySet().stream()
        .collect(java.util.stream.Collectors.toUnmodifiableMap(
            Map.Entry::getKey,
            entry -> List.copyOf(entry.getValue())));
    this.accessorTypes = Map.copyOf(accessorTypeMap);
    this.ambiguousAccessors = Set.copyOf(ambiguousAccessorSet);
  }

  ProjectAccessResourceType inferType(String accessorName) {
    if (ambiguousAccessors.contains(accessorName)) {
      throw new IllegalStateException(
          "Duplicate project access accessor rule for accessor: " + accessorName);
    }
    return accessorTypes.get(accessorName);
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
