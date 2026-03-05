package com.schemafy.domain.erd.column.application.port.in;

import java.util.List;

public record CreateColumnCommand(
    String tableId,
    String name,
    String dataType,
    Integer length,
    Integer precision,
    Integer scale,
    boolean autoIncrement,
    String charset,
    String collation,
    String comment,
    List<String> values) {

  public CreateColumnCommand(
      String tableId,
      String name,
      String dataType,
      Integer length,
      Integer precision,
      Integer scale,
      boolean autoIncrement,
      String charset,
      String collation,
      String comment) {
    this(
        tableId,
        name,
        dataType,
        length,
        precision,
        scale,
        autoIncrement,
        charset,
        collation,
        comment,
        null);
  }

}
