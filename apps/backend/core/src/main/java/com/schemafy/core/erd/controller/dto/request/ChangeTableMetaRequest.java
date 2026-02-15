package com.schemafy.core.erd.controller.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

public record ChangeTableMetaRequest(
    JsonNullable<String> charset,
    JsonNullable<String> collation) {

  public ChangeTableMetaRequest {
    if (charset == null) {
      charset = JsonNullable.undefined();
    }
    if (collation == null) {
      collation = JsonNullable.undefined();
    }
  }

}
