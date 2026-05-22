package com.schemafy.core.project.application.access;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
class ProjectAccessResourceRegistry {

  private final Map<ProjectAccessResourceType, ProjectAccessResourceResolver> resolvers;
  private final Map<String, ProjectAccessResourceType> accessorTypes;

  ProjectAccessResourceRegistry(List<ProjectAccessResourceResolver> resourceResolvers) {
    Map<ProjectAccessResourceType, ProjectAccessResourceResolver> resolverMap = new HashMap<>();
    Map<String, ProjectAccessResourceType> accessorTypeMap = new HashMap<>();
    accessorTypeMap.put("projectId", ProjectAccessResourceType.PROJECT);

    for (ProjectAccessResourceResolver resolver : resourceResolvers) {
      for (ProjectAccessResourceType type : resolver.resourceTypes()) {
        ProjectAccessResourceResolver previous = resolverMap.putIfAbsent(type, resolver);
        if (previous != null) {
          throw new IllegalStateException(
              "Duplicate project access resource resolver for type: " + type);
        }
      }
      for (ProjectAccessAccessorRule rule : resolver.accessorRules()) {
        ProjectAccessResourceType previous = accessorTypeMap.putIfAbsent(
            rule.accessorName(),
            rule.type());
        if (previous != null && previous != rule.type()) {
          throw new IllegalStateException(
              "Duplicate project access accessor rule for accessor: " + rule.accessorName());
        }
      }
    }

    this.resolvers = Map.copyOf(resolverMap);
    this.accessorTypes = Map.copyOf(accessorTypeMap);
  }

  ProjectAccessResourceType inferType(String accessorName) {
    return accessorTypes.get(accessorName);
  }

  ProjectAccessResourceResolver resolver(ProjectAccessResourceType type) {
    ProjectAccessResourceResolver resolver = resolvers.get(type);
    if (resolver == null) {
      throw new IllegalStateException("Project access resource resolver is missing for type: " + type);
    }
    return resolver;
  }

}
