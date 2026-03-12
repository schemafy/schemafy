package com.schemafy.core.erd.table.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;

public record GetTableQuery(String tableId) {

  public GetTableQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new DomainException(TableErrorCode.INVALID_VALUE, "tableId must not be blank");
    }
  }

}
