package com.schemafy.core.erd.column.application.port.in;

import java.util.List;

public record ChangeColumnTypeCommand(
    String columnId,
    String dataType,
    Integer length,
    Integer precision,
    Integer scale,
    List<String> values) {

  public ChangeColumnTypeCommand(
      String columnId,
      String dataType,
      Integer length,
      Integer precision,
      Integer scale) {
    this(columnId, dataType, length, precision, scale, null);
  }

}
