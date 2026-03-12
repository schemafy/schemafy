package com.schemafy.core.erd.column.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;

public record GetColumnsByTableIdQuery(String tableId) {

  public GetColumnsByTableIdQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new DomainException(ColumnErrorCode.INVALID_VALUE, "tableId must not be blank");
    }
  }

}
