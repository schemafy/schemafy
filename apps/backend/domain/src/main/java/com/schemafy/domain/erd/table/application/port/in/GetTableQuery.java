package com.schemafy.domain.erd.table.application.port.in;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.table.domain.exception.TableErrorCode;

public record GetTableQuery(String tableId) {

  public GetTableQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new DomainException(TableErrorCode.INVALID_VALUE, "tableId must not be blank");
    }
  }

}
