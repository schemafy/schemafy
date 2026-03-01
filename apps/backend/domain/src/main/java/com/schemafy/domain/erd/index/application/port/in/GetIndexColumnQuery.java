package com.schemafy.domain.erd.index.application.port.in;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.index.domain.exception.IndexErrorCode;

public record GetIndexColumnQuery(String indexColumnId) {

  public GetIndexColumnQuery {
    if (indexColumnId == null || indexColumnId.isBlank()) {
      throw new DomainException(IndexErrorCode.INVALID_VALUE, "indexColumnId must not be blank");
    }
  }

}
