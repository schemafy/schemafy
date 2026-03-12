package com.schemafy.core.erd.column.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;

public record GetColumnQuery(String columnId) {

  public GetColumnQuery {
    if (columnId == null || columnId.isBlank()) {
      throw new DomainException(ColumnErrorCode.INVALID_VALUE, "columnId must not be blank");
    }
  }

}
