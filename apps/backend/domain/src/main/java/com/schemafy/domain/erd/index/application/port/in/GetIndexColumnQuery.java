package com.schemafy.domain.erd.index.application.port.in;

public record GetIndexColumnQuery(String indexColumnId) {

  public GetIndexColumnQuery {
    if (indexColumnId == null || indexColumnId.isBlank()) {
      throw new IllegalArgumentException("indexColumnId must not be blank");
    }
  }

}
