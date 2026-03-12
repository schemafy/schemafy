package com.schemafy.core.erd.index.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;

public record GetIndexesByTableIdQuery(String tableId) {

  public GetIndexesByTableIdQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new DomainException(IndexErrorCode.INVALID_VALUE, "tableId must not be blank");
    }
  }

}
