package com.schemafy.domain.erd.index.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetIndexQuery(String indexId) {

  public GetIndexQuery {
    if (indexId == null || indexId.isBlank()) {
      throw new InvalidValueException("indexId must not be blank");
    }
  }

}
