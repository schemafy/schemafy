package com.schemafy.core.erd.operation.application.service;

import java.util.Objects;

record ResolvedErdMutationTarget(
    String projectId,
    String schemaId,
    String touchedEntityId) {

  ResolvedErdMutationTarget {
    projectId = Objects.requireNonNull(projectId, "projectId");
  }

  ResolvedErdMutationTarget withTouchedEntityId(String nextTouchedEntityId) {
    return new ResolvedErdMutationTarget(projectId, schemaId, nextTouchedEntityId);
  }

}
