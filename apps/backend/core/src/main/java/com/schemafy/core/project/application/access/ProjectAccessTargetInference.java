package com.schemafy.core.project.application.access;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
class ProjectAccessTargetInference {

  List<ProjectAccessTarget> resolveTargets(
      RequireProjectAccess access,
      Class<?> requestType) {
    List<ProjectAccessTarget> explicitTargets = parseExplicitTargets(access);
    validateAccessors(requestType, explicitTargets);
    return explicitTargets;
  }

  private List<ProjectAccessTarget> parseExplicitTargets(RequireProjectAccess access) {
    List<AccessTarget> targets = new ArrayList<>();
    if (access.target().value() != ProjectAccessResourceType.NONE) {
      targets.add(access.target());
    }
    targets.addAll(List.of(access.targets()));
    return targets.stream()
        .map(this::parseExplicitTarget)
        .toList();
  }

  private ProjectAccessTarget parseExplicitTarget(AccessTarget target) {
    if (target.value() == ProjectAccessResourceType.NONE || target.id().isBlank()) {
      throw new IllegalStateException("Project access target must define type and id accessor");
    }
    return new ProjectAccessTarget(target.value(), target.id());
  }

  void validateAccessors(Class<?> requestType, List<ProjectAccessTarget> targets) {
    for (ProjectAccessTarget target : targets) {
      validateStringAccessor(requestType, target.accessorName());
    }
  }

  private void validateStringAccessor(Class<?> requestType, String accessorName) {
    Method accessor;
    try {
      accessor = requestType.getMethod(accessorName);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(
          "Project access target requires accessor " + accessorName + "() on "
              + requestType.getSimpleName(),
          e);
    }
    if (!String.class.equals(accessor.getReturnType())) {
      throw new IllegalStateException(
          "Project access target requires accessor " + accessorName
              + "() to return String on " + requestType.getSimpleName());
    }
  }

}
