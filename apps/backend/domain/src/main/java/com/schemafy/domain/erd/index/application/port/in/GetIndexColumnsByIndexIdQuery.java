package com.schemafy.domain.erd.index.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetIndexColumnsByIndexIdQuery(String indexId) {

  public GetIndexColumnsByIndexIdQuery {
    if (indexId == null || indexId.isBlank()) {
      throw new InvalidValueException("indexId must not be blank");
    }
  }

}
