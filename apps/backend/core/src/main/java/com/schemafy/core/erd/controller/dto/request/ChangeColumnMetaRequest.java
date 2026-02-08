package com.schemafy.core.erd.controller.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

public record ChangeColumnMetaRequest(
    JsonNullable<Boolean> autoIncrement,
    JsonNullable<String> charset,
    JsonNullable<String> collation,
    JsonNullable<String> comment) {

  public ChangeColumnMetaRequest {
    if (autoIncrement == null) {
      autoIncrement = JsonNullable.undefined();
    }
    if (charset == null) {
      charset = JsonNullable.undefined();
    }
    if (collation == null) {
      collation = JsonNullable.undefined();
    }
    if (comment == null) {
      comment = JsonNullable.undefined();
    }
  }

}
