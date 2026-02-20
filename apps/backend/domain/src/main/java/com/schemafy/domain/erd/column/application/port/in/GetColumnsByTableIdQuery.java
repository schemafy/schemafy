package com.schemafy.domain.erd.column.application.port.in;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.column.domain.exception.ColumnErrorCode;

public record GetColumnsByTableIdQuery(String tableId) {

  public GetColumnsByTableIdQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new DomainException(ColumnErrorCode.INVALID_VALUE, "tableId must not be blank");
    }
  }

}
