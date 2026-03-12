package com.schemafy.core.erd.index.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;

public record GetIndexQuery(String indexId) {

  public GetIndexQuery {
    if (indexId == null || indexId.isBlank()) {
      throw new DomainException(IndexErrorCode.INVALID_VALUE, "indexId must not be blank");
    }
  }

}
