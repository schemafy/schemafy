package com.schemafy.domain.erd.index.application.port.in;

public record GetIndexQuery(String indexId) {

  public GetIndexQuery {
    if (indexId == null || indexId.isBlank()) {
      throw new IllegalArgumentException("indexId must not be blank");
    }
  }

}
