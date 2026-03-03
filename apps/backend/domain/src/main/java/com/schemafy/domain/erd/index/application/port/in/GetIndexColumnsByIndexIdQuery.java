package com.schemafy.domain.erd.index.application.port.in;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.index.domain.exception.IndexErrorCode;

public record GetIndexColumnsByIndexIdQuery(String indexId) {

  public GetIndexColumnsByIndexIdQuery {
    if (indexId == null || indexId.isBlank()) {
      throw new DomainException(IndexErrorCode.INVALID_VALUE, "indexId must not be blank");
    }
  }

}
