package com.schemafy.core.erd.index.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;

public record GetIndexColumnQuery(String indexColumnId) {

  public GetIndexColumnQuery {
    if (indexColumnId == null || indexColumnId.isBlank()) {
      throw new DomainException(IndexErrorCode.INVALID_VALUE, "indexColumnId must not be blank");
    }
  }

}
