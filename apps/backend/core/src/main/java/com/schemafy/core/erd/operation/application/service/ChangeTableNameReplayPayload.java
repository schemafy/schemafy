package com.schemafy.core.erd.operation.application.service;

import java.util.List;

import com.schemafy.core.erd.table.application.port.in.ChangeTableNameCommand;

public record ChangeTableNameReplayPayload(
    String tableId,
    String newName,
    List<NameRestore> constraintNameRestores,
    List<NameRestore> relationshipNameRestores) {

  public ChangeTableNameReplayPayload {
    constraintNameRestores = copyOrEmpty(constraintNameRestores);
    relationshipNameRestores = copyOrEmpty(relationshipNameRestores);
  }

  ChangeTableNameCommand toCommand() {
    return new ChangeTableNameCommand(tableId, newName);
  }

  private static List<NameRestore> copyOrEmpty(List<NameRestore> restores) {
    return restores == null ? List.of() : List.copyOf(restores);
  }

  public record NameRestore(
      String id,
      String name) {
  }

}
