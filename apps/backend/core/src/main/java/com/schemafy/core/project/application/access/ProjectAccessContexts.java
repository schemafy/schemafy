package com.schemafy.core.project.application.access;

import java.util.function.Function;

import com.schemafy.core.project.domain.ProjectRole;

import reactor.util.context.Context;
import reactor.util.context.ContextView;

public final class ProjectAccessContexts {

  private static final Object PROJECT_ACCESS_VERIFIED_CONTEXT_KEY = new Object();

  private ProjectAccessContexts() {}

  public static Function<Context, Context> markVerified(
      String projectId,
      String requesterId,
      ProjectRole role) {
    return context -> putVerifiedMarker(context, projectId, requesterId, role);
  }

  static Context putVerifiedMarker(
      Context context,
      String projectId,
      String requesterId,
      ProjectRole role) {
    return context.put(
        PROJECT_ACCESS_VERIFIED_CONTEXT_KEY,
        new VerifiedProjectAccess(projectId, requesterId, role));
  }

  static boolean hasVerifiedAccess(
      ContextView contextView,
      String projectId,
      String requesterId,
      ProjectRole requiredRole) {
    Object typedValue = contextView.getOrDefault(PROJECT_ACCESS_VERIFIED_CONTEXT_KEY, null);
    return typedValue instanceof VerifiedProjectAccess verified
        && verified.matches(projectId, requesterId, requiredRole);
  }

  private record VerifiedProjectAccess(
      String projectId,
      String requesterId,
      ProjectRole role) {

    private boolean matches(String projectId, String requesterId, ProjectRole requiredRole) {
      return this.projectId.equals(projectId)
          && this.requesterId.equals(requesterId)
          && this.role.isHigherOrEqualThan(requiredRole);
    }

  }

}
