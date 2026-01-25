package com.schemafy.domain.erd.index.application.port.in;

public record GetIndexColumnsByIndexIdQuery(String indexId) {

  public GetIndexColumnsByIndexIdQuery {
    if (indexId == null || indexId.isBlank()) {
      throw new IllegalArgumentException("indexId must not be blank");
    }
  }

}
