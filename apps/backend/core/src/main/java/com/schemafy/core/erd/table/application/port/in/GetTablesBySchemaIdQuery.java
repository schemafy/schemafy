package com.schemafy.core.erd.table.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;

public record GetTablesBySchemaIdQuery(String schemaId) {

  public GetTablesBySchemaIdQuery {
    if (schemaId == null || schemaId.isBlank()) {
      throw new DomainException(TableErrorCode.INVALID_VALUE, "schemaId must not be blank");
    }
  }

}
