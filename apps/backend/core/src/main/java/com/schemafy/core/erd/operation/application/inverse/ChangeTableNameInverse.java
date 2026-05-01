package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record ChangeTableNameInverse(
    String tableId,
    String oldName,
    String oldPkConstraintId,
    String oldPkConstraintName,
    List<ConstraintRename> constraintRenames,
    List<RelationshipRename> relationshipRenames) implements InversePayload {

  public ChangeTableNameInverse {
    constraintRenames = normalizeConstraintRenames(
        constraintRenames,
        oldPkConstraintId,
        oldPkConstraintName);
    relationshipRenames = relationshipRenames == null
        ? List.of()
        : List.copyOf(relationshipRenames);
  }

  public ChangeTableNameInverse(
      String tableId,
      String oldName,
      String oldPkConstraintId,
      String oldPkConstraintName,
      List<RelationshipRename> relationshipRenames) {
    this(
        tableId,
        oldName,
        oldPkConstraintId,
        oldPkConstraintName,
        null,
        relationshipRenames);
  }

  private static List<ConstraintRename> normalizeConstraintRenames(
      List<ConstraintRename> constraintRenames,
      String oldPkConstraintId,
      String oldPkConstraintName) {
    if (constraintRenames != null) {
      return List.copyOf(constraintRenames);
    }
    if (oldPkConstraintId == null || oldPkConstraintName == null) {
      return List.of();
    }
    return List.of(new ConstraintRename(oldPkConstraintId, oldPkConstraintName));
  }

  public record ConstraintRename(String constraintId, String oldName) {

  }

  public record RelationshipRename(String relationshipId, String oldName) {

  }

}
