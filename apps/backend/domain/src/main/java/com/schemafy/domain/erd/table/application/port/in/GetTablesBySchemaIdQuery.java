package com.schemafy.domain.erd.table.application.port.in;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.table.domain.exception.TableErrorCode;

public record GetTablesBySchemaIdQuery(String schemaId) {

  public GetTablesBySchemaIdQuery {
    if (schemaId == null || schemaId.isBlank()) {
      throw new DomainException(TableErrorCode.INVALID_VALUE, "schemaId must not be blank");
    }
  }

}
