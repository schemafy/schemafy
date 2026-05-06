package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record ChangeTableNameInverse(
    String tableId,
    String oldName,
    List<ConstraintRename> constraintRenames,
    List<RelationshipRename> relationshipRenames) implements InversePayload {

  public ChangeTableNameInverse {
    constraintRenames = constraintRenames == null
        ? List.of()
        : List.copyOf(constraintRenames);
    relationshipRenames = relationshipRenames == null
        ? List.of()
        : List.copyOf(relationshipRenames);
  }

  public record ConstraintRename(String constraintId, String oldName) {

  }

  public record RelationshipRename(
      String relationshipId,
      String oldName,
      String fkTableId,
      String pkTableId) {

    public RelationshipRename(String relationshipId, String oldName) {
      this(relationshipId, oldName, null, null);
    }

  }

}
