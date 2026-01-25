package com.schemafy.domain.erd.column.application.port.in;

public record GetColumnQuery(String columnId) {

  public GetColumnQuery {
    if (columnId == null || columnId.isBlank()) {
      throw new IllegalArgumentException("columnId must not be blank");
    }
  }

}
