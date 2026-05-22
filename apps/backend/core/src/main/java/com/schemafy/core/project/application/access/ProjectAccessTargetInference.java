package com.schemafy.core.project.application.access;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ProjectAccessTargetInference {

  private static final String ERD_PACKAGE_PREFIX = "com.schemafy.core.erd.";

  private final ProjectAccessResourceRegistry resourceRegistry;

  boolean isErdType(Class<?> requestType) {
    Package requestPackage = requestType.getPackage();
    return requestPackage != null
        && requestPackage.getName().startsWith(ERD_PACKAGE_PREFIX);
  }

  List<ProjectAccessTarget> resolveTargets(
      RequireProjectAccess access,
      Class<?> requestType) {
    List<ProjectAccessTarget> explicitTargets = parseExplicitTargets(access);
    if (!explicitTargets.isEmpty()) {
      validateAccessors(requestType, explicitTargets);
      return explicitTargets;
    }
    if (!isErdType(requestType)) {
      return List.of();
    }
    return inferTargets(requestType);
  }

  private List<ProjectAccessTarget> parseExplicitTargets(RequireProjectAccess access) {
    List<String> targetSpecs = new ArrayList<>();
    if (!access.target().isBlank()) {
      targetSpecs.add(access.target());
    }
    targetSpecs.addAll(Arrays.asList(access.targets()));
    return targetSpecs.stream()
        .filter(spec -> !spec.isBlank())
        .map(this::parseExplicitTarget)
        .toList();
  }

  private ProjectAccessTarget parseExplicitTarget(String spec) {
    String[] parts = spec.split(":", -1);
    if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
      throw new IllegalStateException(
          "Project access target must use type:accessor format: " + spec);
    }
    return new ProjectAccessTarget(
        ProjectAccessResourceType.fromToken(parts[0]),
        parts[1]);
  }

  private List<ProjectAccessTarget> inferTargets(Class<?> requestType) {
    List<ProjectAccessTarget> targets = Arrays.stream(requestType.getMethods())
        .filter(method -> method.getParameterCount() == 0)
        .filter(method -> String.class.equals(method.getReturnType()))
        .map(Method::getName)
        .map(this::inferTarget)
        .flatMap(List::stream)
        .toList();

    if (targets.size() <= 1) {
      return targets;
    }
    if (isRelationshipTablePair(targets)) {
      return targets;
    }
    throw new IllegalStateException(
        "Ambiguous project access targets on " + requestType.getSimpleName()
            + ": " + targets.stream().map(ProjectAccessTarget::accessorName).toList());
  }

  private List<ProjectAccessTarget> inferTarget(String accessorName) {
    ProjectAccessResourceType type = resourceRegistry.inferType(accessorName);
    if (type == null) {
      return List.of();
    }
    return List.of(new ProjectAccessTarget(type, accessorName));
  }

  private boolean isRelationshipTablePair(List<ProjectAccessTarget> targets) {
    if (targets.size() != 2) {
      return false;
    }
    boolean hasFkTableId = false;
    boolean hasPkTableId = false;
    for (ProjectAccessTarget target : targets) {
      if (target.type() != ProjectAccessResourceType.TABLE) {
        return false;
      }
      hasFkTableId = hasFkTableId || target.accessorName().equals("fkTableId");
      hasPkTableId = hasPkTableId || target.accessorName().equals("pkTableId");
    }
    return hasFkTableId && hasPkTableId;
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
